package org.appxi.tome.cbeta;

import org.appxi.tome.TomeHelper;
import org.appxi.tome.model.Chapter;
import org.appxi.util.DigestHelper;
import org.appxi.util.NumberHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class ChapterTreeParser<T> {
    public final CbetaBook book;
    private final T volChapters, tocChapters;
    private final Object markParsedKey;

    protected ChapterTreeParser(CbetaBook book, T tocChapters, T volChapters, Object markParsedKey) {
        this.book = book;
        this.volChapters = volChapters;
        this.tocChapters = tocChapters;
        this.markParsedKey = markParsedKey;
    }

    public final T getVolChapters() {
        ensureBookChaptersParsed();
        return volChapters;
    }

    public final T getTocChapters() {
        ensureBookChaptersParsed();
        return tocChapters;
    }

    private void ensureBookChaptersParsed() {
        if (null != markParsedKey && book.hasAttr(markParsedKey))
            return;
        if (StringHelper.isBlank(book.path)) {
            // do nothing
        } else if (book.path.startsWith("toc/")) {
//            System.out.println("init chapters: " + book.path);
            Document doc = TomeHelper.xml(CbetaHelper.resolveData(book.path));
            Element body = doc.body();

            //
            Element navCatalog = body.selectFirst("> nav[type=catalog]");
            Set<String> ctxVolumes = new LinkedHashSet<>();
            if (null != navCatalog) {
                initTocChapterAndChildren(book, tocChapters, navCatalog.selectFirst("> ol"), ctxVolumes);
            }
            //
            Element navJuan = body.selectFirst("> nav[type=juan]");
            if (null != navJuan) {
                navJuan.select("li > [href]").forEach(link -> ctxVolumes.add(link.attr("href").split("#", 2)[0]));
            }
            final Attributes ctx = new Attributes();
            ctxVolumes.stream().sorted().forEach(vol -> initVolChapterAndFixSequence(book, volChapters, vol, ctx));
            fixVolChapters();
        } else {
            createTreeItem(tocChapters, createChapter().setId(StringHelper.concat(book.id, "-1")).setType("article").setTitle(book.title).setPath(book.path));
        }
        if (null != markParsedKey)
            book.attr(markParsedKey, true);
    }

    private void initVolChapterAndFixSequence(CbetaBook book, T parent, String linkHref, Attributes ctx) {
        String linkPath = linkHref.split("#", 2)[0];
        String[] nameInfo = linkPath.substring(linkPath.lastIndexOf("/") + 1, linkPath.lastIndexOf(".")).split("[n_]");

        String currSerial = nameInfo[0];
        int currIdx = Integer.parseInt(nameInfo[2]);

        String prevSerial = ctx.attrStr("prevSerial");
        int prevIdx = ctx.hasAttr("prevIdx") ? ctx.attr("prevIdx") : -1;

        //
        int fixFrom = -1;
        if (null == prevSerial && currIdx > 1) {
            // start not from _001.xml, need fix
            fixFrom = 1;
        } else if (prevIdx != -1 && currIdx - prevIdx > 1) {
            // between curr and prev more than 1, need fix
            fixFrom = prevIdx + 1;
        }
        // need fix
        if (fixFrom > -1) {
            String tmpLink = linkPath.substring(0, linkPath.lastIndexOf("_") + 1);
            String tmpIdx;
            for (int i = fixFrom; i < currIdx; i++) {
                tmpIdx = StringHelper.concat(i < 10 ? "00" : (i < 100 ? "0" : null), i);
                createTreeItem(parent, createChapter("title",
                        StringHelper.concat(nameInfo[0], "n", nameInfo[1], "_", tmpIdx),
                        StringHelper.concat("卷", NumberHelper.toChineseNumberOld(i)),
                        StringHelper.concat(tmpLink, tmpIdx, ".xml"),
                        null));
            }
        }
        // normal
        createTreeItem(parent, createChapter("article",
                StringHelper.concat(nameInfo[0], "n", nameInfo[1], "_", nameInfo[2]),
                StringHelper.concat("卷", NumberHelper.toChineseNumberOld(currIdx)),
                linkPath,
                null));

        // keep curr as prev for next
        ctx.attr("prevSerial", currSerial);
        ctx.attr("prevIdx", currIdx);
    }

    protected Chapter createChapter() {
        return new Chapter();
    }

    private Chapter createChapter(String type, String id, String title, String path, Object start) {
        Chapter chapter = createChapter();
        chapter.type = type;
        chapter.id = id;
        chapter.title = title;
        chapter.path = path;
        chapter.start = start;
        return chapter;
    }

    private void initTocChapterAndChildren(CbetaBook book, T parent, Element element, Set<String> ctxVolumes) {
        Elements lis = element.select("> li");
        Element linkEle;
        String linkHref, linkText;
        String[] linkInfo;
        T child;
        Chapter childVal;
        for (Element liEle : lis) {
            linkEle = liEle.selectFirst("> [href]");
            linkHref = linkEle.attr("href");
            linkInfo = linkHref.split("#", 2);
            linkText = linkEle.text();

            ctxVolumes.add(linkInfo[0]);

            childVal = createChapter("article",
                    StringHelper.concat(book.id, "-", DigestHelper.crc32c(linkHref, linkText)),
                    linkText,
                    linkInfo[0],
                    linkInfo.length == 2 ? linkInfo[1] : null);
            child = createTreeItem(parent, childVal);

            linkEle = liEle.selectFirst("> ol");
            if (null != linkEle) {
                initTocChapterAndChildren(book, child, linkEle, ctxVolumes);
                childVal.type = "title";
            }
        }
    }

    protected abstract T createTreeItem(T parent, Chapter chapter);

    private void fixVolChapters() {
        // start fix vols in chapters
        String baseVolId, baseVolPath, volId, volPath;
        int volIdx = 0;
        boolean changed = false;
        for (CbetaBook.SerialVol sv : book.serialVols) {
            baseVolId = StringHelper.concat(book.tripitakaId, sv.serial(), "n", book.number, "_");
            baseVolPath = StringHelper.concat("XML/", book.tripitakaId, "/", book.tripitakaId, sv.serial(), "/");
            for (int i = sv.startVol(); i <= sv.endVol(); i++) {
                volIdx++;
                //
                volId = StringHelper.concat(baseVolId, volIdx < 10 ? "00" : (volIdx < 100 ? "0" : null), volIdx);
                volPath = StringHelper.concat(baseVolPath, volId, ".xml");

                // check exists in volChapters
                if (!existsItemInTree(volPath, volChapters)) {
                    changed = true;
                    createTreeItem(volChapters, createChapter("title", volId,
                            StringHelper.concat("卷", NumberHelper.toChineseNumberOld(volIdx)),
                            volPath, null));
                }
            }
        }
        if (changed) {
            sortChildrenOfTree(volChapters);
        }
        book.serialVols.clear();
        // end fix
    }

    protected abstract boolean existsItemInTree(String path, T tree);

    protected abstract void sortChildrenOfTree(T tree);
}

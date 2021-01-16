package org.appxi.tome.parse;

import org.appxi.tome.BookHelper;
import org.appxi.tome.model.Book;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Node;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.zip.ZipFile;

public abstract class BookByEpubParser {
    private BookByEpubParser() {
    }

    public static Book parse(String file) throws IOException {
        return parse(file, null, null);
    }

    public static Book parse(String file, Function<String, String> chapterFilter) throws IOException {
        return parse(file, chapterFilter, null);
    }

    public static Book parse(String file, BiConsumer<Book, Elements> chaptersBuilder) throws IOException {
        return parse(file, null, chaptersBuilder);
    }

    public static Book parse(String file, Function<String, String> chapterFilter,
                             BiConsumer<Book, Elements> chaptersBuilder) throws IOException {
        final Book book = new Book();
        try (ZipFile zip = new ZipFile(file)) {
            Document doc = Jsoup.parse(zip.getInputStream(zip.getEntry("META-INF/container.xml")),
                    "UTF-8", "", Parser.xmlParser());
            String rootFile = doc.selectFirst("rootfile").attr("full-path");
            String rootPath = rootFile.substring(0, rootFile.indexOf("/") + 1);

            doc = Jsoup.parse(zip.getInputStream(zip.getEntry(rootFile)),
                    "UTF-8", "", Parser.xmlParser());
            String ncxFile = rootPath + doc.selectFirst("manifest > item[id=ncx]").attr("href");

            doc = Jsoup.parse(zip.getInputStream(zip.getEntry(ncxFile)),
                    "UTF-8", "", Parser.xmlParser());
            Element ncx = doc.selectFirst("> ncx");

            book.title = ncx.selectFirst("> docTitle > text").text().strip();
            ncx.select("> docAuthor").forEach(ele -> book.authors.add(ele.text().strip()));
            book.authorInfo = StringHelper.join(",", book.authors);

            Elements navPoints = ncx.select("> navMap > navPoint");
            if (null != chaptersBuilder)
                chaptersBuilder.accept(book, navPoints);
            else walkAndParseChaptersTree(rootPath, book.chapters, navPoints, chapterFilter);

            book.chapters.traverse((level, node, chapter) -> {
                if (chapter.hasParagraphs())
                    chapter.paragraphs.forEach(para -> {
                        if (file.equals(para.caption))
                            para.caption = null;
                        if (null != para.content) {
                            if (para.content.startsWith("${rootPath}"))
                                para.content = para.content.replace("${rootPath}", rootPath);
                            if (para.content.startsWith(rootPath))
                                para.content = parseZipEntryBody(zip, para.content);
                        }
                    });
            });
        }
        return book;
    }

    private static void walkAndParseChaptersTree(String rootPath, Node<Chapter> node, Elements navPoints,
                                                 Function<String, String> chapterFilter) {
        for (Element navPoint : navPoints) {
            String chapterTitle = navPoint.selectFirst("> navLabel > text").text();
            if (StringHelper.indexOf(chapterTitle, "目次", "目录"))
                continue;
            if (null != chapterFilter) {
                chapterTitle = chapterFilter.apply(chapterTitle);
                if (null == chapterTitle)
                    continue;
            }
            Node<Chapter> child = node.add(new Chapter());
            child.value.setType("toc");
            child.value.setTitle(chapterTitle);

            Elements childNavPoints = navPoint.select("> navPoint");
            if (childNavPoints.isEmpty()) {
                String navPath = navPoint.selectFirst("> content").attr("src");
                child.value.addParagraph(null, rootPath + navPath);
            } else walkAndParseChaptersTree(rootPath, child, childNavPoints, chapterFilter);
        }
    }

    static String parseZipEntryBody(ZipFile zipFile, String path) {
        try {
            if (path.contains("#"))
                path = path.substring(0, path.indexOf("#"));
            return Jsoup.parse(zipFile.getInputStream(zipFile.getEntry(path)),
                    "UTF-8", "").body().html();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Book prepareBook(Book book) {
        book.chapters.traverse((level, node, chapter) -> {
            if (null == chapter.title)
                throw new RuntimeException();
            chapter.setTitle(chapter.title.replaceAll("\s+", " "));
            if (chapter.hasParagraphs())
                chapter.paragraphs.forEach(para -> BookHelper.cleanupAfterParsedHtmlParagraph(book, chapter, para));
        });
        return book;
    }
}

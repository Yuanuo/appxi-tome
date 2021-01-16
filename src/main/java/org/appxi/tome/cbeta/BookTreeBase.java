package org.appxi.tome.cbeta;

import org.appxi.tome.TomeHelper;
import org.appxi.util.DigestHelper;
import org.appxi.util.StringHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BookTreeBase<T> {
    public final BookMap books;
    protected final T tree;
    private BookTreeMode mode;

    public BookTreeBase(BookMap books, T tree, BookTreeMode mode) {
        this.books = books;
        this.tree = tree;
        this.mode = null != mode ? mode : BookTreeMode.catalog;
    }

    public BookTreeMode getMode() {
        return mode;
    }

    private boolean lazyCalled;

    public T getDataTree() {
        if (this.lazyCalled)
            return this.tree;
        this.lazyCalled = true;

        Path navFile = CbetaHelper.resolveData(this.mode.file);
        if (Files.notExists(navFile)) {
            this.mode = null;
            for (BookTreeMode mode : BookTreeMode.values()) {
                navFile = CbetaHelper.resolveData(mode.file);
                if (Files.exists(navFile)) {
                    this.mode = mode;
                    break;
                }
            }
        }
        if (null == this.mode)
            return this.tree;

        try (InputStream inStream = new BufferedInputStream(Files.newInputStream(navFile))) {
            final Document doc = TomeHelper.xml(inStream);
            final Element nav = doc.body().selectFirst("nav");
            createTreeItem(tree, nav);
            nav.select("> li").forEach(li -> createTreeItem(tree, li));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.gc();
        }
        return this.tree;
    }

    private void createTreeItem(T parent, Element li) {
        if (null == li)
            return;
        final Element span = li.selectFirst("> span");

        if (null != span) {
            final T nav = createTreeItem(parent, new CbetaCatalog(span.text()));
            li.selectFirst("> ol").select("> li").forEach(ele -> createTreeItem(nav, ele));
            return;
        }

        final Element linkEle = li.selectFirst("[href]");
        if (null == linkEle)
            return;
        createTreeItem(parent, createBookByLinkInfo(linkEle));
    }

    private CbetaBook createBookByLinkInfo(Element linkEle) {
        final String text = linkEle.text();
        final String link = linkEle.attr("href");

        if (link.startsWith("XML/")) {
            final String[] tmpArr = text.split("[ 　]", 2);
            return books.getDataMap().get(tmpArr[0]);
        } else if (text.matches("^[a-zA-Z].*")) {
            final String[] tmpArr = text.split(" ", 2);
            return books.getDataMap().get(tmpArr[0]);
        }
        final CbetaBook result = createCbetaBook();
        result.id = DigestHelper.crc32c(link);
        result.title = text;
        result.path = link;
        return result;
    }

    protected CbetaBook createCbetaBook() {
        return new CbetaBook();
    }

    protected abstract T createTreeItem(T parent, CbetaBook book);

    private static class CbetaCatalog extends CbetaBook {
        CbetaCatalog(String text) {
            super();
            if (!text.contains(" ")) {
                this.title = text;
                return;
            }
            final String[] groups = text.replace(", ", ",,")
                    .replace("etc.", " ")
                    .replace("  ", " ")
                    .split("／");
            final StringBuilder result = new StringBuilder();
            for (String group : groups) {
                final StringBuilder buf = new StringBuilder();
                for (String tmp : StringHelper.split(group, " ", "[\\(\\)]")) {
                    if (tmp.matches("^[a-zA-Z=].*"))
                        continue;
                    if (buf.length() == 0 && tmp.matches("^[0-9].*"))
                        continue;
                    buf.append(tmp).append(' ');
                }
                if (buf.length() > 0) {
                    if (result.length() > 0)
                        result.append('／');
                    result.append(buf.toString().strip());
                }
            }
            if (result.length() == 0) {
                this.title = text;
            } else {
                this.title = result.toString();
//                this.attr("title", text);
            }
        }
    }
}

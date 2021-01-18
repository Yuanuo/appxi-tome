package org.appxi.tome.cbeta;

import org.appxi.tome.TomeHelper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class BookTreeParser<T> {
    protected final T tree;
    private BookTreeMode mode;

    public BookTreeParser(T tree, BookTreeMode mode) {
        this.tree = tree;
        this.mode = null != mode ? mode : BookTreeMode.catalog;
    }

    public BookTreeMode getMode() {
        return mode;
    }

    private boolean lazyParsed;

    public T getDataTree() {
        if (this.lazyParsed)
            return this.tree;
        this.lazyParsed = true;

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
            createTreeItems(tree, nav.select(":root, :root > li"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.gc();
        }
        return this.tree;
    }

    private void createTreeItems(T parent, Elements elements) {
        final List<T> children = new ArrayList<>(elements.size());
        for (Element ele : elements) {
            children.add(createTreeItem(ele));
        }
        setTreeChildren(parent, children);
    }

    private T createTreeItem(Element li) {
        if (null == li)
            return null;
        final Element span = li.selectFirst("> span");
        if (null != span) {
            final T node = createTreeItem(null, span.text(), span);
            createTreeItems(node, li.selectFirst("> ol").select("> li"));
            return node;
        }

        final Element linkEle = li.selectFirst("[href]");
        if (null == linkEle)
            return null;
        return createTreeItem(linkEle.attr("href"), linkEle.text(), linkEle);
    }

    protected abstract T createTreeItem(String link, String text, Element element);

    protected abstract void setTreeChildren(T parent, List<T> children);
}

package org.appxi.tome.cbeta;

import org.appxi.util.DigestHelper;
import org.jsoup.nodes.Element;

public abstract class BookTreeBase<T> extends BookTreeParser<T> {
    public final BookMap books;

    public BookTreeBase(BookMap books, T tree, BookTreeMode mode) {
        super(tree, mode);
        this.books = books;
    }

    @Override
    protected final T createTreeItem(String link, String text, Element element) {
        CbetaBook book;
        if (null == link) {
            book = new CbetaBook();
            book.title = CbetaHelper.parseNavCatalogInfo(text);
        } else if (link.startsWith("XML/")) {
            final String[] tmpArr = text.split("[ 　]", 2);
            book = books.getDataMap().get(tmpArr[0]);
        } else {
            book = new CbetaBook();
            book.id = DigestHelper.crc32c(link);
            book.title = text;
            book.path = link;
            books.getDataMap().put(book.id, book);
        }
        return this.createTreeItem(book);
    }

    protected abstract T createTreeItem(CbetaBook itemValue);
}

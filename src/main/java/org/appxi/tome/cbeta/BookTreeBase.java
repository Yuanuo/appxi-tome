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
            if (null == book && tmpArr[0].matches(".*[a-z]$")) {
                book = books.getDataMap().get(tmpArr[0].substring(0, tmpArr[0].length() - 1));
                if (null != book) {
                    book.attr("cloned", true);
                    book = book.clone();
                    book.id = tmpArr[0];
                    book.title = text;
                    book.attr("start", link);
                    books.getDataMap().put(book.id, book);
                }
            }
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

package org.appxi.tome.cbeta;

import org.appxi.util.ext.Node;

public class BookTree extends BookTreeBase<Node<CbetaBook>> {

    public BookTree(BookMap books, BookTreeMode mode) {
        super(books, new Node<>(), mode);
    }

    @Override
    protected Node<CbetaBook> createTreeItem(Node<CbetaBook> parent, CbetaBook book) {
        return parent.add(book);
    }
}

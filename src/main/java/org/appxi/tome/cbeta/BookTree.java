package org.appxi.tome.cbeta;

import org.appxi.util.ext.Node;

import java.util.List;

public class BookTree extends BookTreeBase<Node<CbetaBook>> {
    public BookTree(BookMap books, BookTreeMode mode) {
        super(books, new Node<>(), mode);
    }

    @Override
    protected Node<CbetaBook> createTreeItem(CbetaBook itemValue) {
        return new Node<CbetaBook>().setValue(itemValue);
    }

    @Override
    protected void setTreeChildren(Node<CbetaBook> parent, List<Node<CbetaBook>> children) {
        parent.children.addAll(children);
    }
}

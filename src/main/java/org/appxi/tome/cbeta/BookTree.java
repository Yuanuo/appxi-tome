package org.appxi.tome.cbeta;

import org.appxi.util.ext.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        children.forEach(c -> c.setParent(parent));
    }

    public static class WithMap extends BookTree {
        final Map<String, Node<CbetaBook>> dataMap = new HashMap<>(1024);

        public WithMap(BookMap books, BookTreeMode mode) {
            super(books, mode);
        }

        @Override
        protected Node<CbetaBook> createTreeItem(CbetaBook itemValue) {
            Node<CbetaBook> node = super.createTreeItem(itemValue);
            if (null != itemValue && null != itemValue.id)
                dataMap.put(itemValue.id, node);
            return node;
        }

        public Map<String, Node<CbetaBook>> getDataMap() {
            return dataMap;
        }
    }
}

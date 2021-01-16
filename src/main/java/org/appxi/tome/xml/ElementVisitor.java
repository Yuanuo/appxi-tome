package org.appxi.tome.xml;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

public class ElementVisitor implements NodeVisitor {

    @Override
    public void head(Node node, int depth) {
        if (node instanceof Element element)
            this.head(element, depth);
    }

    protected void head(Element element, int depth) {
    }

    @Override
    public void tail(Node node, int depth) {
        if (node instanceof Element element)
            this.tail(element, depth);
    }

    protected void tail(Element element, int depth) {
    }
}

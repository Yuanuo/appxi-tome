package org.appxi.tome.xml;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter;

public class ElementFilter implements NodeFilter {

    @Override
    public FilterResult head(Node node, int depth) {
        if (node instanceof Element element)
            return this.head(element, depth);
        return FilterResult.CONTINUE;
    }

    protected FilterResult head(Element element, int depth) {
        return FilterResult.CONTINUE;
    }

    @Override
    public FilterResult tail(Node node, int depth) {
        if (node instanceof Element element)
            return this.tail(element, depth);
        return FilterResult.CONTINUE;
    }

    protected FilterResult tail(Element element, int depth) {
        return FilterResult.CONTINUE;
    }
}

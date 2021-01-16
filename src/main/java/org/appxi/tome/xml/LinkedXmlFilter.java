package org.appxi.tome.xml;

import org.jsoup.nodes.Element;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class LinkedXmlFilter extends LinkedFilter<Element> {
    public LinkedXmlFilter(Predicate<Element> start, Predicate<Element> stop, Consumer<Element> resultHandle) {
        super(start, stop, resultHandle);
    }
}

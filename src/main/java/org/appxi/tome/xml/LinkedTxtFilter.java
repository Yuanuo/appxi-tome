package org.appxi.tome.xml;

import org.jsoup.nodes.Element;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class LinkedTxtFilter extends LinkedFilter<String> {
    public LinkedTxtFilter(Predicate<Element> start, Predicate<Element> stop, Consumer<String> resultHandle) {
        super(start, stop, resultHandle);
    }
}

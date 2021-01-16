package org.appxi.tome.xml;

import org.jsoup.nodes.Element;

import java.util.function.Consumer;
import java.util.function.Predicate;

class LinkedFilter<R> {
    public Predicate<Element> start, stop;
    public Consumer<R> resultHandle;
    public LinkedFilter<R> next;

    public LinkedFilter(Predicate<Element> start, Predicate<Element> stop, Consumer<R> resultHandle) {
        this.start = start;
        this.stop = stop;
        this.resultHandle = resultHandle;
    }

    public LinkedFilter<R> setNext(LinkedFilter<R> next) {
        this.next = next;
        return this;
    }
}

package org.appxi.tome.xml;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;

import java.util.function.Consumer;

public abstract class FilteredProcessor<R> implements NodeFilter {
    private LinkedFilter<R> linkedFilter;
    private boolean handling;
    private Node root;

    public FilteredProcessor(LinkedFilter<R> linkedFilter) {
        this.linkedFilter = linkedFilter;
        this.handling = null == linkedFilter || null == linkedFilter.start;
    }

    @Override
    public final FilterResult head(Node node, int depth) {
        if (null == root)
            root = node;
        if (node instanceof TextNode txt) {
            return this.handling ? handleHeadText(txt) : FilterResult.CONTINUE;
        }
        // to avoid dead loop
        if (depth == 0)
            return FilterResult.CONTINUE;
        //
        final Element ele = (Element) node;
        if (!this.handling) {
            this.handling = isStartHandling(ele);
            if (!this.handling)
                return FilterResult.CONTINUE;
        } else if (null != this.linkedFilter && null != this.linkedFilter.stop && this.linkedFilter.stop.test(ele)) {
            // callback to handle current filtered
            handleFilteredResultAndReadyForNextRound(this.linkedFilter.resultHandle);
            // move to next filter
            this.linkedFilter = this.linkedFilter.next;
            if (null == this.linkedFilter) {
                // no more filters
                return FilterResult.STOP;
            } else {
                this.handling = isStartHandling(ele);
                if (!this.handling)
                    return FilterResult.CONTINUE;
            }
        }
        //
        return handleHeadElement(ele);
    }

    private boolean isStartHandling(Element ele) {
        return null == this.linkedFilter || null == this.linkedFilter.start || this.linkedFilter.start.test(ele);
    }

    protected abstract FilterResult handleHeadText(TextNode txt);

    protected abstract FilterResult handleHeadElement(Element ele);

    protected abstract void handleFilteredResultAndReadyForNextRound(Consumer<R> resultHandle);

    @Override
    public final FilterResult tail(Node node, int depth) {
        if (node == this.root) {
            // handle the result of last processed, if already handled, then current linkedFilter must be null
            if (null != this.linkedFilter)
                handleFilteredResultAndReadyForNextRound(this.linkedFilter.resultHandle);
            return FilterResult.CONTINUE;
        }
        if (node instanceof Element ele) {
            return this.handling ? handleTailElement(ele) : FilterResult.CONTINUE;
        }
        return FilterResult.CONTINUE;
    }

    protected FilterResult handleTailElement(Element ele) {
        return FilterResult.CONTINUE;
    }

}

package org.appxi.tome.cbeta;

import org.appxi.tome.xml.FilteredProcessor;
import org.appxi.tome.xml.LinkedTxtFilter;
import org.appxi.util.DevtoolHelper;
import org.appxi.util.NumberHelper;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class VolumeXml2TextProcessor extends FilteredProcessor<String> {
    public final VolumeDocument volDocument;
    private StringBuilder buff = new StringBuilder();

    public VolumeXml2TextProcessor(VolumeDocument volDocument) {
        this(volDocument, null);
    }

    public VolumeXml2TextProcessor(VolumeDocument volDocument, LinkedTxtFilter linkedFilter) {
        super(linkedFilter);
        this.volDocument = volDocument;
    }

    @Override
    protected FilterResult handleHeadText(TextNode txt) {
        buff.append(txt.text());
        return FilterResult.CONTINUE;
    }

    @Override
    protected FilterResult handleHeadElement(Element ele) {
        final String tagName = ele.tagName().toLowerCase();
        switch (tagName) {
            case "note":
                if (ele.attrIs("place", "inline")) {
                    buff.append("（").append(volDocument.getElementText(ele)).append("）");
                    return FilterResult.SKIP_ENTIRELY;
                }
                if (ele.attrIn("type", "cf1", "orig", "mod")) {
                    return FilterResult.SKIP_ENTIRELY; // not support now
                }
                break;
            case "g":
                buff.append(volDocument.getDeclarationText(ele.attr("ref")));
                return FilterResult.SKIP_ENTIRELY;
            case "app":
                buff.append(volDocument.getElementText(ele.selectFirst("lem, rdg")));
                return FilterResult.SKIP_ENTIRELY;
            case "cb:tt":
                if (ele.attrIs("type", "app")) {
                    buff.append(volDocument.getElementText(ele.selectFirst("cb|t[xml:lang=zh-Hant], cb|t")));
                    return FilterResult.SKIP_ENTIRELY;
                }
            case "space":
                if (ele.hasAttr("quantity"))
                    buff.append(" ".repeat(NumberHelper.toInt(ele.attr("quantity"), 0)));
                return FilterResult.SKIP_ENTIRELY;
            case "milestone", "pb", "lb", "unclear": // for now, skip it
                return FilterResult.SKIP_ENTIRELY;
            case "cb:juan", "cb:mulu", "cb:jhead", "title":
                break;
            default:
                if (!unhandledTags.contains(tagName)) {
                    unhandledTags.add(tagName);
                    DevtoolHelper.LOG.info("ToText.?> " + tagName);
                }
                break;
        }
        return FilterResult.CONTINUE;
    }

    private static final Set<String> unhandledTags = new HashSet<>();

    @Override
    protected void handleFilteredResultAndReadyForNextRound(Consumer<String> resultHandle) {
        if (null != resultHandle) {
            resultHandle.accept(this.buff.toString());
            this.buff = new StringBuilder();
        }
    }

    @Override
    public String toString() {
        return buff.toString();
    }
}

package org.appxi.tome.cbeta;

import org.appxi.tome.xml.FilteredProcessor;
import org.appxi.tome.xml.LinkedXmlFilter;
import org.appxi.util.DevtoolHelper;
import org.appxi.util.NumberHelper;
import org.appxi.util.StringHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class VolumeXml2HtmlProcessor extends FilteredProcessor<Element> {
    public final VolumeDocument volDocument;
    private Element body;
    private Element buff;

    public VolumeXml2HtmlProcessor(VolumeDocument volDocument) {
        this(volDocument, null);
    }

    public VolumeXml2HtmlProcessor(VolumeDocument volDocument, LinkedXmlFilter linkedFilter) {
        super(linkedFilter);
        this.volDocument = volDocument;
        reset();
    }

    private void reset() {
        this.body = Jsoup.parse("").body();
        this.buff = this.body;
    }

    @Override
    protected FilterResult handleHeadText(TextNode txt) {
        final String str = txt.text().strip();
        if (buff.hasAttr("nobody")) {
            buff.attr("data-t", buff.hasAttr("data-t")
                    ? StringHelper.concat(buff.attr("data-t"), ",", str)
                    : str);
        } else buff.appendText(str);
        return FilterResult.CONTINUE;
    }

    @Override
    protected FilterResult handleHeadElement(Element ele) {
        final String tag = ele.tagName().toLowerCase();
        switch (tag) {
            case "body", "div", "p":
                newBuff(tag, ele);
                break;
            case "cb:div":
                newBuff("div", ele).addClass(ele.attr("type"));
                break;
            case "entry", "form", "cb:def", "lg", "l":
                newBuff("div", ele);
                break;
            case "milestone":
                addBuff("span", ele)
                        .attr("id", StringHelper.concat(ele.attr("unit"), '-', ele.attr("n")));
                return FilterResult.SKIP_ENTIRELY;
            case "cb:docnumber":
                newBuff("div", ele);
                break;
            case "byline":
                newBuff("div", ele).addClass(ele.attr("cb:type"));
                break;
            case "cb:juan":
                newBuff("div", ele).addClass(ele.attr("fun"));
                break;
            case "cb:mulu":
                newBuff("div", ele) //.attr("nobody", true)
                        .attr("leveln", StringHelper.concat(ele.attr("level"), '-', ele.attr("n")))
                        .attr("type", ele.attr("type"));
                break;
            case "head", "cb:jhead":
                Element prevEleSib = ele.previousElementSibling();
                if (null != prevEleSib && prevEleSib.is("cb|mulu") && ele.text().equals(prevEleSib.text()))
                    return FilterResult.SKIP_ENTIRELY;
                newBuff("div", ele);
                break;
            case "title":
                newBuff("span", ele);
                break;
            case "lb", "pb":
                addBuff("span", ele).attr("id", StringHelper.concat('p', ele.attr("n")))
                        .appendElement("br");
                return FilterResult.SKIP_ENTIRELY;
            case "g":
                addBuff("span", ele).text(volDocument.getDeclarationText(ele.attr("ref")));
                return FilterResult.SKIP_ENTIRELY;
            case "note":
                if (ele.attrIs("place", "inline")) {
                    newBuff("span", ele).addClass("place-inline");
                    break;
                }
                if (ele.attrIn("type", "cf1", "cf2", "cf3", "cf4")) {
                    return FilterResult.SKIP_ENTIRELY; // not support now
                }
                if (ele.hasAttr("type")) {
                    addBuff("span", ele).addClass(ele.attr("type"))
                            .attr("data-n", ele.attr("n"))
                            .attr("data-t", ele.text());
                    return FilterResult.SKIP_ENTIRELY;
                }

                break;
            case "app", "term":
                newBuff("span", ele);
                break;
            case "lem":
                newBuff("span", ele).attr("wit", ele.attr("wit"));
                break;
            case "rdg":
                newBuff("span", ele)
                        .attr("wit", ele.attr("wit"))
                        .attr("resp", ele.attr("resp"))
                        .attr("nobody", true);
                break;
            case "cb:tt":
                if (ele.attrIs("type", "app")) {
                    newBuff("span", ele).addClass("app");
                    break;
                }
            case "cb:t":
                if (ele.attrIs("xml:lang", "zh-Hant")) {
                    newBuff("span", ele).addClass("zh-hant");
                } else {
                    newBuff("span", ele).attr("nobody", true);
                }
                break;
            case "space":
                if (ele.hasAttr("quantity"))
                    buff.appendText("&nbsp;".repeat(NumberHelper.toInt(ele.attr("quantity"), 0)));
                return FilterResult.SKIP_ENTIRELY;
            case "unclear", "caesura":
                addBuff("span", ele);
                return FilterResult.SKIP_ENTIRELY;
            case "figure":
                newBuff(tag, ele);
                break;
            case "graphic", "img":
                addBuff("img", ele).attr("src", ele.attr("img".equals(tag) ? "src" : "url"));
                return FilterResult.SKIP_ENTIRELY;
            case "table", "tbody", "tr", "td", "ul", "ol", "li":
                newBuff(tag, ele);
                break;
            case "row":
                newBuff("tr", ele);
                break;
            case "cell":
                newBuff("td", ele);
                break;
            case "list":
                newBuff("ol", ele);
                break;
            case "item":
                newBuff("li", ele);
                break;
            case "a":
                newBuff(tag, ele).attr("href", ele.attr("href"));
                break;
            case "br":
                addBuff(tag, ele);
                break;
            default:
                newBuff("span", ele).addClass("unhandled");
                if (!unhandledTags.contains(tag)) {
                    unhandledTags.add(tag);
                    DevtoolHelper.LOG.info("ToHtml.?> " + tag);
                }
                break;
        }
        return FilterResult.CONTINUE;
    }

    private static final Set<String> unhandledTags = new HashSet<>();

    @Override
    protected void handleFilteredResultAndReadyForNextRound(Consumer<Element> resultHandle) {
        if (null != resultHandle) {
            resultHandle.accept(this.body);
            reset();
        }
    }

    @Override
    protected FilterResult handleTailElement(Element ele) {
        if (buff.getUserData() == ele) {
            buff = buff.parent();// close a element, this is important
        }
        return FilterResult.CONTINUE;
    }

    @Override
    public String toString() {
        return body.html();
    }

    public Element toElement() {
        return body;
    }

    /**
     * append element to {@link #buff}, but no change {@link #buff} instance
     */
    private Element addBuff(String tag, Element visited) {
        final Element added = this.buff.appendElement(tag);

        String id;
        if (!(id = visited.id()).isEmpty() || !(id = visited.attr("xml:id")).isEmpty())
            added.attr("id", id);

        final String eleTag = visited.tagName();
        if (!eleTag.endsWith(tag)) {
            if (eleTag.contains(":"))
                added.addClass(eleTag.replace(':', '-'));
            else added.addClass(eleTag);
        }
        return added;
    }

    /**
     * append element to {@link #buff}, and relink new created element to {@link #buff}
     */
    private Element newBuff(String tag, Element visited) {
        final Element added = this.addBuff(tag, visited);
        added.setUserData(visited);
        return this.buff = added;
    }
}

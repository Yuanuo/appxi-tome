package org.appxi.tome.cbeta;

import org.appxi.tome.xml.LinkedTxtFilter;
import org.appxi.tome.xml.LinkedXmlFilter;
import org.appxi.util.DigestHelper;
import org.appxi.util.StringHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeTraversor;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class VolumeDocument {
    public final CbetaBook book;
    public final String volume;
    public final Path volumeXml;

    private Document document;
    private int simpleIdCounter = 1;

    public VolumeDocument(CbetaBook book, String volume) {
        this.book = book;
        this.volume = volume;
        this.volumeXml = CbetaHelper.resolveData(volume);
    }

    public boolean exists() {
        return Files.exists(this.volumeXml);
    }

    public boolean notExists() {
        return !this.exists();
    }

    public Document getDocument() {
        if (null != this.document)
            return this.document;
        //
        if (this.notExists())
            this.document = Jsoup.parse("");
        else
            try (InputStream inStream = new BufferedInputStream(Files.newInputStream(this.volumeXml))) {
                System.out.println("Jsoup.parseXml: " + this.volumeXml.toAbsolutePath());
                this.document = Jsoup.parse(inStream, "UTF-8", "/", Parser.xmlParser());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        //
        this.document.attr("vol", this.volume).attr("xml", this.volumeXml.toString());
        return this.document;
    }

    public Element getDocumentBody() {
        return this.getDocument().body();
    }

    public Document getPreparedDocument() {
        final Document doc = this.getDocument();
        if (doc.hasAttr("prepared"))
            return doc;

        // do some init

        // fill id fo all div(s)
//        doc.body().traverse(new VolumeXmlIdGenerator(this.vol).withDiv());
//        doc.body().select("cb|mulu, cb|juan[fun=open]").forEach(this::ensureId);

        // mark it already prepared
        doc.attr("prepared", true);
        return doc;
    }

    public Element getPreparedDocumentBody() {
        return this.getPreparedDocument().body();
    }

    public String ensureId(Element element) {
        return ensureId(element, this.volume);
    }

    public String ensureId(Element element, String salt) {
        String id = element.id();
        if (!id.isBlank())
            return id;
        id = element.attr("xml:id");
        if (id.isBlank()) {
            id = String.valueOf(simpleIdCounter++);
            id = null == salt ? id : DigestHelper.crc32c(id, salt);
            id = StringHelper.concat('z', id);
        }
        element.attr("id", id);
        return id;
    }

    public Element getDeclarationChar(String id) {
        if (null == id || id.isBlank())
            return null; // not a valid ele
        if (id.charAt(0) == '#')
            id = id.substring(1);
        return this.getDocument().selectFirst("> TEI > teiHeader > encodingDesc > charDecl > char[xml:id=" + id + "]");
    }

    public String getDeclarationText(String id) {
        final Element declareChar = getDeclarationChar(id);
        if (null == declareChar)
            return "@Declare@" + id;

        Element propEle = declareChar.selectFirst("> charProp > localName:contains(normalized form)");
        if (null == propEle)
            propEle = declareChar.selectFirst("> charProp > localName:contains(composition)");
        if (null == propEle) {
            // FIXME
            return declareChar.selectFirst("charName").text();
        }
        return propEle.nextElementSibling().text();
    }

    protected String getElementText(Element element) {
        final VolumeXml2TextProcessor processor = new VolumeXml2TextProcessor(this);
        NodeTraversor.filter(processor, element);
        return processor.toString();
    }

//    protected Element getStandardHtmlElement(Element element) {
//        final VolumeXml2HtmlProcessor processor = new VolumeXml2HtmlProcessor(this);
//        NodeTraversor.filter(processor, element);
//        return processor.toElement();
//    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void filterStandardText(LinkedTxtFilter linkedFilter) {
        final VolumeXml2TextProcessor processor = new VolumeXml2TextProcessor(this, linkedFilter);
        NodeTraversor.filter(processor, getPreparedDocumentBody());
    }

    public void filterStandardHtml(LinkedXmlFilter linkedFilter) {
        final VolumeXml2HtmlProcessor processor = new VolumeXml2HtmlProcessor(this, linkedFilter);
        NodeTraversor.filter(processor, getPreparedDocumentBody());
    }

    public String getStandardText() {
        return getStandardText((String) null, null);
    }

    public String getStandardText(String startSelector, String stopSelector) {
        return getStandardText(null == startSelector ? null : (ele) -> ele.is(startSelector),
                null == stopSelector ? null : (ele) -> ele.is(stopSelector));
    }

    public String getStandardText(Predicate<Element> startFilter, Predicate<Element> stopFilter) {
        final VolumeXml2TextProcessor processor = new VolumeXml2TextProcessor(this,
                new LinkedTxtFilter(startFilter, stopFilter, null));
        NodeTraversor.filter(processor, getPreparedDocumentBody());
        return processor.toString();
    }


    public Element getStandardHtml() {
        return getStandardHtml((Predicate<Element>) null, null);
    }

    public Element getStandardHtml(String startSelector, String stopSelector) {
        return getStandardHtml(null == startSelector ? null : (ele) -> ele.is(startSelector),
                null == stopSelector ? null : (ele) -> ele.is(stopSelector));
    }

    public Element getStandardHtml(Predicate<Element> startFilter, Predicate<Element> stopFilter) {
        final VolumeXml2HtmlProcessor processor = new VolumeXml2HtmlProcessor(this,
                new LinkedXmlFilter(startFilter, stopFilter, null));
        NodeTraversor.filter(processor, getPreparedDocumentBody());
        return processor.toElement();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String toStandardHtmlDocument(Supplier<Element> elementSupplier, Function<Element, Object> bodyWrapper, String... includes) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<!DOCTYPE html><html lang=\"zh\"><head><meta charset=\"UTF-8\">");
        //
        final List<String> scripts = new ArrayList<>(), styles = new ArrayList<>();
        for (String include : includes) {
            if (include.endsWith(".js")) {
                buf.append("\r\n<script type=\"text/javascript\" src=\"").append(include).append("\"></script>");
            } else if (include.endsWith(".css")) {
                buf.append("\r\n<link rel=\"stylesheet\" href=\"").append(include).append("\"/>");
            } else if (include.startsWith("<script") || include.startsWith("<style")
                    || include.startsWith("<link") || include.startsWith("<meta")) {
                buf.append("\r\n").append(include);
            } else if (include.startsWith("var ") || include.startsWith("function")) {
                scripts.add(include);
            } else {
                styles.add(include);
            }
        }
        if (!scripts.isEmpty()) {
            buf.append("\r\n<script type=\"text/javascript\">").append(StringHelper.joinLines(scripts)).append("</script>");
        }
        if (!styles.isEmpty()) {
            buf.append("\r\n<style type=\"text/css\">").append(StringHelper.joinLines(styles)).append("</style>");
        }
        //
        buf.append("</head>");
        final Element body = null != elementSupplier ? elementSupplier.get() : this.getStandardHtml();
        if (null == bodyWrapper) {
            buf.append(body.outerHtml());
        } else {
            final Object bodyWrapped = bodyWrapper.apply(body);
            if (bodyWrapped instanceof Node node) {
                buf.append(node.outerHtml());
            } else {
                final String bodyHtml = bodyWrapped.toString();
                if (bodyHtml.startsWith("<body"))
                    buf.append(bodyHtml);
                else buf.append("<body>").append(bodyHtml).append("</body>");
            }
        }
        buf.append("</html>");
        return buf.toString();
    }

    public String toStandardTextDocument(Supplier<Element> elementSupplier) {
        Element element = null != elementSupplier ? elementSupplier.get() : null;
        return null != element ? element.text() : this.getStandardText();
    }

//    private List<Chapter> chaptersList;
//
//    public List<Chapter> getChaptersList() {
//        if (null != this.chaptersList)
//            return this.chaptersList;
//        this.chaptersList = new ArrayList<>();
//
//        this.getPreparedDocument().body().select("cb|mulu, cb|juan").forEach(ele -> {
//            final Chapter chapter = new Chapter(this.chapter);
//            chaptersList.add(chapter);
//
//            chapter.id = ele.id();
//            chapter.name = this.getStandardText(ele);
//
//            switch (ele.tagName()) {
//                case "cb:mulu": {
//                    chapter.type = ele.attr("type");
//                    String level = ele.attr("level");
//                    if (level.isBlank())
//                        level = ele.attrOr("n", "1");
//                    chapter.level = NumberHelper.toInt(level, 1);
//                    break;
//                }
//                case "cb:juan": {
//                    chapter.type = ele.attr("fun");
//                    chapter.level = NumberHelper.toInt(ele.attr("n"), 1);
//                    break;
//                }
//                default:
//                    break;
//            }
//        });
//        return chaptersList;
//    }
}

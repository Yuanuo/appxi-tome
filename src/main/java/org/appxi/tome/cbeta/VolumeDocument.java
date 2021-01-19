package org.appxi.tome.cbeta;

import org.appxi.tome.xml.LinkedTxtFilter;
import org.appxi.tome.xml.LinkedXmlFilter;
import org.appxi.util.DevtoolHelper;
import org.appxi.util.DigestHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.HanLang;
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
    public final Path volumeFile;

    private Document document;
    private int simpleIdCounter = 1;

    public VolumeDocument(CbetaBook book, String volume) {
        this.book = book;
        this.volume = volume;
        this.volumeFile = CbetaHelper.resolveData(volume);
    }

    public boolean exists() {
        return Files.exists(this.volumeFile);
    }

    public boolean notExists() {
        return !this.exists();
    }

    public boolean isXmlVolume() {
        return volume.endsWith(".xml") && volume.startsWith("XML/");
    }

    public boolean notXmlVolume() {
        return !isXmlVolume();
    }

    public Document getDocument() {
        if (null != this.document)
            return this.document;
        //
        if (this.notExists())
            this.document = Jsoup.parse("");
        else
            try (InputStream inStream = new BufferedInputStream(Files.newInputStream(this.volumeFile))) {
                DevtoolHelper.LOG.info(StringHelper.msg("Jsoup.parseXml: " + this.volumeFile.toAbsolutePath()));
                this.document = Jsoup.parse(inStream, "UTF-8", "/", isXmlVolume() ? Parser.xmlParser() : Parser.htmlParser());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        //
        this.document.attr("vol", this.volume).attr("xml", this.volumeFile.toString());
        return this.document;
    }

    public Element getDocumentBody() {
        Document document = this.getDocument();
        return isXmlVolume() ? document.selectFirst("> TEI > text > body") : document.body();
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
        if (null == propEle)
            propEle = declareChar.selectFirst("> charProp > localName:contains(Character in the Siddham font)");
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

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void filterStandardText(LinkedTxtFilter linkedFilter) {
        final VolumeXml2TextProcessor processor = new VolumeXml2TextProcessor(this, linkedFilter);
        NodeTraversor.filter(processor, getDocumentBody());
    }

    public void filterStandardHtml(LinkedXmlFilter linkedFilter) {
        final VolumeXml2HtmlProcessor processor = new VolumeXml2HtmlProcessor(this, linkedFilter);
        NodeTraversor.filter(processor, getDocumentBody());
    }

    public String getStandardText() {
        if (this.notXmlVolume())
            return this.getDocumentBody().text();
        return getStandardText((String) null, null);
    }

    public String getStandardText(String startSelector, String stopSelector) {
        return getStandardText(null == startSelector ? null : (ele) -> ele.is(startSelector),
                null == stopSelector ? null : (ele) -> ele.is(stopSelector));
    }

    public String getStandardText(Predicate<Element> startFilter, Predicate<Element> stopFilter) {
        final VolumeXml2TextProcessor processor = new VolumeXml2TextProcessor(this,
                new LinkedTxtFilter(startFilter, stopFilter, null));
        NodeTraversor.filter(processor, getDocumentBody());
        return processor.toString();
    }


    public Element getStandardHtml() {
        if (this.notXmlVolume())
            return this.getDocument();
        return getStandardHtml((Predicate<Element>) null, null);
    }

    public Element getStandardHtml(String startSelector, String stopSelector) {
        return getStandardHtml(null == startSelector ? null : (ele) -> ele.is(startSelector),
                null == stopSelector ? null : (ele) -> ele.is(stopSelector));
    }

    public Element getStandardHtml(Predicate<Element> startFilter, Predicate<Element> stopFilter) {
        final VolumeXml2HtmlProcessor processor = new VolumeXml2HtmlProcessor(this,
                new LinkedXmlFilter(startFilter, stopFilter, null));
        NodeTraversor.filter(processor, getDocumentBody());
        return processor.toElement();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String toStandardHtmlDocument(HanLang hanLang, Supplier<Element> bodySupplier, Function<Element, Object> bodyWrapper, String... includes) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<!DOCTYPE html><html lang=\"").append(hanLang.lang).append("\"><head><meta charset=\"UTF-8\">");
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
        final Element body = null != bodySupplier ? bodySupplier.get() : (isXmlVolume() ? this.getStandardHtml() : getDocumentBody());
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
}

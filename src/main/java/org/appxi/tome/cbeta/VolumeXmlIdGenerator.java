package org.appxi.tome.cbeta;

import org.appxi.tome.xml.ElementVisitor;
import org.appxi.util.DigestHelper;
import org.appxi.util.StringHelper;
import org.jsoup.nodes.Element;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VolumeXmlIdGenerator extends ElementVisitor {
    public final Set<String> tagNames = new HashSet<>();
    public final String salt;

    public VolumeXmlIdGenerator(String salt) {
        this.salt = salt;
    }

    public VolumeXmlIdGenerator withDiv() {
        this.tagNames.add("div");
        return this;
    }

    public VolumeXmlIdGenerator with(String... tagNames) {
        this.tagNames.addAll(Arrays.asList(tagNames));
        return this;
    }

    @Override
    protected void head(Element element, int depth) {
        if (this.tagNames.contains(element.tagName()))
            VolumeXmlIdGenerator.ensureId(element, salt);
    }

    public static String ensureId(Element element, String salt) {
        String id = element.id();
        if (!id.isBlank())
            return id;
        id = element.attr("xml:id");
        if (id.isBlank()) {
            id = DigestHelper.crc32c(element.cssSelector(), salt);
            id = StringHelper.concat('z', id);
        }
        element.attr("id", id);
        return id;
    }
}

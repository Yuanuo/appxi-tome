package org.appxi.tome.model;

import org.appxi.util.ext.Attributes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Chapter extends Attributes implements Serializable {
    public String type, id, title, description;
    public String path;
    public Object start, stop;

    public List<Paragraph> paragraphs;

    public Chapter() {
    }

    public Chapter(String type, String id, String title, String path, Object start) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.path = path;
        this.start = start;
    }

    public Chapter setType(String type) {
        this.type = type;
        return this;
    }

    public Chapter setId(String id) {
        this.id = id;
        return this;
    }

    public Chapter setTitle(String title) {
        this.title = title;
        return this;
    }

    public Chapter setDescription(String description) {
        this.description = description;
        return this;
    }

    public Chapter setPath(String path) {
        this.path = path;
        return this;
    }

    public Chapter addParagraph(String caption, String content) {
        if (null == this.paragraphs)
            this.paragraphs = new ArrayList<>();
        this.paragraphs.add(new Paragraph(caption, content));
        return this;
    }

    public boolean hasParagraphs() {
        return null != this.paragraphs && !this.paragraphs.isEmpty();
    }

    @Override
    public String toString() {
        return title;
    }
}

package org.appxi.tome.model;

import org.appxi.util.StringHelper;

public class Paragraph {
    public String caption, content;

    public Paragraph() {
    }

    public Paragraph(String content) {
        this(null, content);
    }

    public Paragraph(String caption, String content) {
        this.caption = caption;
        this.content = content;
    }

    public Paragraph appendContent(String content) {
        if (null == this.content)
            this.content = content;
        else this.content = StringHelper.concat(this.content, "\n", content);
        return this;
    }
}

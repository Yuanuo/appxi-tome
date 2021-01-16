package org.appxi.tome.cbeta;

public enum BookTreeMode {
    advance("advance_nav.xhtml"),//
    catalog("bulei_nav.xhtml"),//
    simple("simple_nav.xhtml");

    public final String file;

    BookTreeMode(String file) {
        this.file = file;
    }
}

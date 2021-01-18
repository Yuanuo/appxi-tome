package org.appxi.tome.model;

import org.appxi.util.ext.Attributes;
import org.appxi.util.ext.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class Book extends Attributes implements Serializable {
    public String id;
    public String title;
    public String summary;

    public final Node<Chapter> chapters = new Node<>();
    //
    public String authorInfo;
    public final Collection<String> periods = new HashSet<>();
    public final Collection<String> authors = new HashSet<>();
    //
    public String catalog;
    public String location;
    public String copyright;
    //
    public final Collection<String> extras = new ArrayList<>();
    //
    public String path;

    public Book setId(String id) {
        this.id = id;
        return this;
    }

    public Book setTitle(String title) {
        this.title = title;
        return this;
    }

    public Book setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public String authorInfo() {
        return this.authorInfo;
    }

    @Override
    public String toString() {
        return this.title;
    }
}

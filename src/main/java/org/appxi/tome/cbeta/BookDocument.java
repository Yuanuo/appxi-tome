package org.appxi.tome.cbeta;

import org.appxi.tome.model.Chapter;
import org.appxi.tome.xml.LinkedTxtFilter;
import org.appxi.tome.xml.LinkedXmlFilter;
import org.appxi.util.ext.Node;
import org.jsoup.nodes.Element;

import java.util.Objects;
import java.util.function.Function;

public class BookDocument {
    private static final Object AK_CHAPTERS = new Object();
    public final CbetaBook book;
    protected VolumeDocument cachedVolDocument;

    public BookDocument(CbetaBook book) {
        this.book = book;
    }

    public Node<Chapter> getChapters() {
        if (!book.hasAttr(AK_CHAPTERS)) {
            ChapterTree.getOrInitBookChapters(book);
            book.attr(AK_CHAPTERS, true);
        }
        return this.book.chapters;
    }

    public VolumeDocument getVolumeDocument(String volume) {
        if (null != cachedVolDocument && Objects.equals(volume, cachedVolDocument.volume))
            return cachedVolDocument;
        final VolumeDocument volDocument = createVolumeDocument(volume);
        this.cachedVolDocument = volDocument;
        return volDocument;
    }

    protected VolumeDocument createVolumeDocument(String volume) {
        return new VolumeDocument(book, volume);
    }

    public String getVolumeHtmlDocument(String volume, Function<Element, Object> bodyWrapper, String... includes) {
        final VolumeDocument volDocument = getVolumeDocument(volume);
        return volDocument.toStandardHtmlDocument(null, bodyWrapper, includes);
    }

    public String getVolumeTextDocument(String volume) {
        final VolumeDocument volDocument = getVolumeDocument(volume);
        return volDocument.toStandardTextDocument(null);
    }

    public void filterVolumeText(String volume, LinkedTxtFilter linkedFilter) {
        final VolumeDocument volDocument = getVolumeDocument(volume);
        volDocument.filterStandardText(linkedFilter);
    }

    public void filterVolumeHtml(String volume, LinkedXmlFilter linkedFilter) {
        final VolumeDocument volDocument = getVolumeDocument(volume);
        volDocument.filterStandardHtml(linkedFilter);
    }

//    public String getChapterHtmlDocument(String volume, String startSelector, String stopSelector, Function<Element, Object> bodyWrapper, String... includes) {
//        final VolumeDocument volDocument = getVolumeDocument(volume);
//        return volDocument.toStandardHtmlDocument(() -> volDocument.getStandardHtml(startSelector, stopSelector), bodyWrapper, includes);
//    }
//
//    public String getChapterTextDocument(String volume, String startSelector, String stopSelector) {
//        final VolumeDocument volDocument = getVolumeDocument(volume);
//        return volDocument.toStandardTextDocument(() -> volDocument.getStandardHtml(startSelector, stopSelector));
//    }

//    public LinkedHashSet<Chapter> getChaptersList() {
//        if (null != this.chaptersList)
//            return this.chaptersList;
//        this.chaptersList = new LinkedHashSet<>();
//        book.getVols().forEach(chapter -> this.chaptersList.addAll(getVolDocument(chapter).getChaptersList()));
//        return this.chaptersList;
//    }
}

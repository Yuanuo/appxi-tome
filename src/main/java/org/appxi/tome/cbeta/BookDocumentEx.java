package org.appxi.tome.cbeta;

import org.appxi.prefs.UserPrefs;
import org.appxi.tome.model.Chapter;
import org.appxi.util.FileHelper;
import org.appxi.util.ext.Node;

import java.nio.file.Path;

public class BookDocumentEx extends BookDocument {
    private static final String VERSION = "21.01.00";
    private Node<Chapter> chapters;

    public BookDocumentEx(CbetaBook book) {
        super(book);
    }

    @Override
    public VolumeDocumentEx getVolumeDocument(String volume) {
        return (VolumeDocumentEx) super.getVolumeDocument(volume);
    }

    @Override
    protected VolumeDocument createVolumeDocument(String volume) {
        return new VolumeDocumentEx(book, volume);
    }

    public final String getIdentificationInfo() {
        final StringBuilder result = new StringBuilder(VERSION);
        result.append('|').append(book.id);
        result.append('|').append(book.path);
        return result.toString();
    }

    @Override
    public Node<Chapter> getChapters() {
        if (null != this.chapters)
            return this.chapters;

        final String cachePath = FileHelper.makeEncodedPath(getIdentificationInfo(), ".temp");
        final Path cacheFile = UserPrefs.cacheDir().resolve(cachePath);
        if (FileHelper.exists(cacheFile)) {
            this.chapters = FileHelper.readObject(cacheFile);
            if (null != this.chapters)
                return this.chapters;
        }

        this.chapters = super.getChapters();
        final boolean success = FileHelper.writeObject(cacheFile, this.chapters);
        if (success)
            System.out.println("Cached chapters : " + cacheFile.toAbsolutePath());
        else throw new RuntimeException("cannot cache chapters");// for debug only

        return this.chapters;
    }
}

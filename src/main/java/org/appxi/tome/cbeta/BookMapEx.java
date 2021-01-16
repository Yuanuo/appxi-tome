package org.appxi.tome.cbeta;

import org.appxi.prefs.UserPrefs;
import org.appxi.util.FileHelper;

import java.nio.file.Path;
import java.util.Map;

public class BookMapEx extends BookMap {
    private static final String VERSION = "21.01.07";

    public BookMapEx() {
        super();
    }

    public final String getIdentificationInfo() {
        final StringBuilder result = new StringBuilder(VERSION);
        result.append('|').append(FileHelper.getIdentificationInfo(CbetaHelper.resolveData("catalog.txt")));
        return result.toString();
    }

    private Map<String, CbetaBook> data;

    @Override
    public Map<String, CbetaBook> getDataMap() {
        if (null != this.data)
            return this.data;

        final String cachePath = FileHelper.makeEncodedPath(getIdentificationInfo(), ".temp");
        final Path cacheFile = UserPrefs.cacheDir().resolve(cachePath);
        if (FileHelper.exists(cacheFile)) {
            this.data = FileHelper.readObject(cacheFile);
            if (null != this.data)
                return this.data;
        }

        this.data = super.getDataMap();
        this.data.forEach((k, v) -> ChapterTree.getOrInitBookChapters(v));
        final boolean success = FileHelper.writeObject(cacheFile, this.data);
        if (success)
            System.out.println("Cached book : " + cacheFile.toAbsolutePath());
        else throw new RuntimeException("cannot cache book");// for debug only

        return this.data;
    }
}

package org.appxi.tome.cbeta;

import org.appxi.prefs.UserPrefs;
import org.appxi.util.FileHelper;
import org.appxi.util.StringHelper;
import org.jsoup.nodes.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public class VolumeDocumentEx extends VolumeDocument {
    private static final String VERSION = "21.01.00";

    public VolumeDocumentEx(CbetaBook book, String volume) {
        super(book, volume);
    }

    public final String getIdentificationInfo() {
        return FileHelper.getIdentificationInfo(volumeXml, this.volume, VERSION);
    }

    @Override
    public String toStandardHtmlDocument(Supplier<Element> elementSupplier, Function<Element, Object> bodyWrapper, String... includes) {
        final StringBuilder cacheInfo = new StringBuilder(getIdentificationInfo());
        cacheInfo.append(StringHelper.join("|", includes));
        final String cachePath = FileHelper.makeEncodedPath(cacheInfo.toString(), ".html");
        final Path cacheFile = UserPrefs.cacheDir().resolve(cachePath);
        if (Files.notExists(cacheFile)) {
            final String stdHtmlDoc = super.toStandardHtmlDocument(elementSupplier, bodyWrapper, includes);
            final boolean success = FileHelper.writeString(cacheFile, stdHtmlDoc);
            if (success)
                System.out.println("Cached : " + cacheFile.toAbsolutePath());
            else throw new RuntimeException("cannot cache stdHtmlDoc");// for debug only
        }
        return cacheFile.toAbsolutePath().toString();
    }

    @Override
    public String toStandardTextDocument(Supplier<Element> elementSupplier) {
        final String cachePath = FileHelper.makeEncodedPath(getIdentificationInfo(), ".text");
        final Path cacheFile = UserPrefs.cacheDir().resolve(cachePath);
        if (Files.notExists(cacheFile)) {
            final String stdTextDoc = super.toStandardTextDocument(elementSupplier);
            final boolean success = FileHelper.writeString(cacheFile, stdTextDoc);
            if (success)
                System.out.println("Cached : " + cacheFile.toAbsolutePath());
            else throw new RuntimeException("cannot cache stdTextDoc");// for debug only
        }
        return cacheFile.toAbsolutePath().toString();
    }
}

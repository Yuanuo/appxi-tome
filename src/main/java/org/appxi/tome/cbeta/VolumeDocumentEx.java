package org.appxi.tome.cbeta;

import org.appxi.prefs.UserPrefs;
import org.appxi.util.DevtoolHelper;
import org.appxi.util.DigestHelper;
import org.appxi.util.FileHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.HanLang;
import org.jsoup.nodes.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public class VolumeDocumentEx extends VolumeDocument {
    private static final String VERSION = "21.02.09.1";

    public VolumeDocumentEx(CbetaBook book, String volume) {
        super(book, volume);
    }

    public final String getIdentificationInfo() {
        return FileHelper.getIdentificationInfo(volumeFile, this.volume, VERSION);
    }

    @Override
    public String toStandardHtmlDocument(HanLang hanLang, Supplier<Element> bodySupplier, Function<Element, Object> bodyWrapper, String... includes) {
        final StringBuilder cacheInfo = new StringBuilder(getIdentificationInfo());
        cacheInfo.append(hanLang.lang);
        cacheInfo.append(StringHelper.join("|", includes));
        final String cachePath = StringHelper.concat(".tmp.", DigestHelper.md5(cacheInfo.toString()), ".html");
        final Path cacheFile;
        if (this.isXmlVolume()) {
            cacheFile = CbetaHelper.resolveData(".temp").resolve(cachePath);
        } else {
            cacheFile = this.volumeFile.getParent().resolve(cachePath);
        }
        if (Files.notExists(cacheFile)) {
            final String stdHtmlDoc = super.toStandardHtmlDocument(hanLang, bodySupplier, bodyWrapper, includes);
            final boolean success = FileHelper.writeString(stdHtmlDoc, cacheFile);
            if (success)
                DevtoolHelper.LOG.info("Cached : " + cacheFile.toAbsolutePath());
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
            final boolean success = FileHelper.writeString(stdTextDoc, cacheFile);
            if (success)
                DevtoolHelper.LOG.info("Cached : " + cacheFile.toAbsolutePath());
            else throw new RuntimeException("cannot cache stdTextDoc");// for debug only
        }
        return cacheFile.toAbsolutePath().toString();
    }
}

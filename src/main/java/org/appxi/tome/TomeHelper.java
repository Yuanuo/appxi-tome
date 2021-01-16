package org.appxi.tome;

import org.appxi.prefs.UserPrefs;
import org.appxi.util.DigestHelper;
import org.appxi.util.FileHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class TomeHelper {
    private static final String UTF_8 = "UTF-8";

    private TomeHelper() {
    }

    public static Document web(String pageUrl) {
        return web(pageUrl, UTF_8);
    }

    public static Document web(String pageUrl, String charset) {
        try (InputStream stream = webCachedStream(pageUrl, charset)) {
            return Jsoup.parse(stream, charset, pageUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Path webCachedFile(String pageUrl) {
        return UserPrefs.workDir().resolve(".tome-cache").resolve(FileHelper.makeEncodedPath(pageUrl, ".temp"));
    }

    public static InputStream webCachedStream(String pageUrl) throws IOException {
        return webCachedStream(pageUrl, UTF_8);
    }

    public static InputStream webCachedStream(String pageUrl, String charset) throws IOException {
        final Path file = webCachedFile(pageUrl);
        if (FileHelper.notExists(file))
            try (InputStream bodyStream = webOnlineStream(pageUrl, charset)) {
                FileHelper.makeParents(file);
                Files.copy(bodyStream, file);
            }
        return new BufferedInputStream(Files.newInputStream(file));
    }

    public static InputStream webOnlineStream(String pageUrl) throws IOException {
        return webOnlineStream(pageUrl, UTF_8);
    }

    public static InputStream webOnlineStream(String pageUrl, String charset) throws IOException {
        return Jsoup.connect(pageUrl)
                .maxBodySize(0)
                .followRedirects(true)
                .execute()
                .charset(charset)
                .bodyStream();
    }

    public static Document xml(String file) {
        return xml(file, UTF_8);
    }

    public static Document xml(String file, String charset) {
        try (InputStream stream = new FileInputStream(file)) {
            return xml(stream, charset);
        } catch (Exception e) {
            e.printStackTrace();
            return Jsoup.parse("");
        }
    }

    public static Document xml(Path file) {
        return xml(file, UTF_8);
    }

    public static Document xml(Path file, String charset) {
        try (InputStream stream = new BufferedInputStream(Files.newInputStream(file))) {
            return xml(stream, charset);
        } catch (Exception e) {
            e.printStackTrace();
            return Jsoup.parse("");
        }
    }

    public static Document xml(InputStream inputStream) {
        return xml(inputStream, UTF_8);
    }

    public static Document xml(InputStream inputStream, String charset) {
        try {
            return Jsoup.parse(inputStream, charset, "", Parser.xmlParser());
        } catch (Exception e) {
            e.printStackTrace();
            return Jsoup.parse("");
        }
    }

    public static boolean saveXml(Document document, Path targetFile) {
        return saveDocument(document, targetFile, true);
    }

    public static boolean saveHtml(Document document, Path targetFile) {
        return saveDocument(document, targetFile, false);
    }

    public static boolean saveDocument(Document document, Path targetFile, boolean xmlMode) {
        FileHelper.makeParents(targetFile);
        System.out.println("\tSave document: " + targetFile);
        try {
            if (xmlMode)
                document.outputSettings().prettyPrint(false).syntax(Document.OutputSettings.Syntax.xml);
            Files.writeString(targetFile, document.outerHtml());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String randomString(int count) {
        return DigestHelper.uid();
    }
}

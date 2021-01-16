package org.appxi.tome;

import org.appxi.tome.model.Book;
import org.appxi.tome.model.Chapter;
import org.appxi.tome.model.Paragraph;
import org.appxi.tome.xml.ElementHelper;
import org.appxi.util.DigestHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Attributes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Objects;

public abstract class BookHelper {
    public static final Object AK_IGNORED = new Object();

    private BookHelper() {
    }

    public static void ignore(Attributes attrs) {
        attrs.attr(AK_IGNORED, true);
    }

    public static void prepareBook(Book book) {
        if (null == book || book.hasAttr(AK_IGNORED))
            return;

        if (StringHelper.isBlank(book.id)) {
            book.id = "bk-" + DigestHelper.crc32c(book.title, book.path);
        }
        //
        book.chapters.traverse((level, node, chapter) -> {
            if ("@".equals(chapter.title))
                chapter.title = book.title;

            if (node.hasChildren())
                return;

            if (StringHelper.isBlank(chapter.id)) {
                chapter.id = book.id + "-" + TomeHelper.randomString(6);
            }
        });
    }

    public static void cleanupAfterParsedHtmlParagraph(Book book, Chapter chapter, Paragraph paragraph) {
        //
        Document doc = Jsoup.parse(paragraph.content);
        Element body = doc.body();
        ElementHelper.cleanupAllUnwanted(body, chapter.title, paragraph.caption, "目录.*", "上一页.*", "下一页.*");
        // update
        paragraph.content = body.html();
        //
        if (Objects.equals(chapter.title, paragraph.caption))
            paragraph.caption = null;
    }
}

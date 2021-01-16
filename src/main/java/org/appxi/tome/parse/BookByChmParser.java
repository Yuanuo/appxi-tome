package org.appxi.tome.parse;

import org.appxi.tome.BookHelper;
import org.appxi.tome.chmlib.ChmFile;
import org.appxi.tome.chmlib.ChmTopicsTree;
import org.appxi.tome.model.Book;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Node;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class BookByChmParser {
    private BookByChmParser() {
    }

    public static Book parse(String file) {
        return parse(file, null, null);
    }

    public static Book parse(String file, String startPah) {
        return parse(file, startPah, null);
    }

    public static Book parse(String file, BiFunction<String, String, String> chapterFilter) {
        return parse(file, null, chapterFilter);
    }

    public static Book parse(String file, String startPah, BiFunction<String, String, String> chapterFilter) {
        return parse(file, startPah, chapterFilter, null);
    }

    public static Book parse(String file, String startPah, BiFunction<String, String, String> chapterFilter,
                             BiConsumer<ChmFile, ChmTopicsTree> chaptersBuilder) {
        if (null == startPah)
            startPah = "/";
        final Book book = new Book();

        try (ChmFile chm = new ChmFile(file)) {
            book.title = chm.getTitle();

            ChmTopicsTree toc = chm.getTopicsTree();
            if (null == toc) {
                toc = new ChmTopicsTree();
                if (null != chaptersBuilder)
                    chaptersBuilder.accept(chm, toc);
            }

            walkAndParseChaptersTree(book.chapters, toc, startPah, chm, chapterFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return book;
    }

    private static void walkAndParseChaptersTree(Node<Chapter> node, ChmTopicsTree toc, String startPah, ChmFile chm,
                                                 BiFunction<String, String, String> chapterFilter) {
        if (null != toc.parent && (toc.path.isEmpty() || toc.path.startsWith(startPah))) {
            String chapterTitle = toc.title;
            String topicPath = URLDecoder.decode(toc.path, StandardCharsets.UTF_8);
            if (null != chapterFilter) {
                chapterTitle = chapterFilter.apply(toc.title, topicPath);
                if (null == chapterTitle)
                    return;
            }
            chapterTitle = chapterTitle.strip();
            if (StringHelper.isNotBlank(chapterTitle)) {
                node = node.add(new Chapter());
                node.value.setType("toc");
                node.value.setTitle(chapterTitle);
            }

            if (StringHelper.isNotBlank(topicPath)) {
                node.value.type = "article";

                String topicText = chm.retrieveObjectAsString(chm.resolveObject(topicPath));
                Document topicDoc = Jsoup.parse(topicText);
                Element realBody = topicDoc.body();
                if (null != realBody) {
                    Elements temps = realBody.select("table table table > tbody > tr > td");
                    if (temps.isEmpty())
                        temps = realBody.select("table table > tbody > tr > td");
                    if (temps.isEmpty())
                        temps = realBody.select("table > tbody > tr > td");
                    if (temps.size() == 5) {
                        realBody = temps.get(2);
                    } else if (temps.size() == 3) {
                        realBody = temps.get(1);
                    } else {
                        for (Element tmp : temps) {
                            String tmpTxt = tmp.text().strip();
                            if (tmpTxt.isBlank() || tmpTxt.startsWith("上一页") || tmpTxt.endsWith("下一页") || tmpTxt.startsWith("目录")
                                    || tmpTxt.equals(chapterTitle) || tmpTxt.equals(toc.title)) {
                                tmp.remove();
                            }
                        }
                    }
                }
                node.value.addParagraph(toc.title, (null == realBody ? topicDoc : realBody).html());
            }
        }

        for (ChmTopicsTree child : toc.children)
            walkAndParseChaptersTree(node, child, startPah, chm, chapterFilter);
    }

    public static Book prepareBook(Book book) {
        book.chapters.traverse((level, node, chapter) -> {
            if (null == chapter.title)
                throw new RuntimeException();
            chapter.setTitle(chapter.title.replaceAll("\s+", " "));
            if (chapter.hasParagraphs())
                chapter.paragraphs.forEach(para -> BookHelper.cleanupAfterParsedHtmlParagraph(book, chapter, para));
        });
        return book;
    }

}

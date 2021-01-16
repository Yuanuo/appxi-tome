package org.appxi.tome.parse;

import org.appxi.tome.model.Book;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public abstract class BookByTextParser {
    private BookByTextParser() {
    }

    public static class Lines {
        public int from, to;
        public String textDef;

        public Lines textDef(String textDef) {
            this.textDef = textDef;
            return this;
        }

        public boolean isValid() {
            return from < to && from > -1;
        }

        public static Lines of(int idx) {
            return of(idx, idx + 1);
        }

        public static Lines of(int from, int to) {
            Lines result = new Lines();
            result.from = from;
            result.to = to;
            return result;
        }
    }

    public static class Section extends Lines {
        public String type;
        public Lines head;

        public Section type(String type) {
            this.type = type;
            return this;
        }

        public Section head(Lines head) {
            this.head = head;
            return this;
        }

        public static Section of(int idx) {
            return of(idx, idx + 1);
        }

        public static Section of(int from, int to) {
            Section result = new Section();
            result.from = from;
            result.to = to;
            return result;
        }

        public static Section of(String headDef, int from, int to) {
            return of(from, to).head(Lines.of(-1, -1).textDef(headDef));
        }

        public static Section of(int headIdx, int from, int to) {
            return of(from, to).head(Lines.of(headIdx));
        }
    }

    public static Book parseWithTOC(final List<String> lines, Lines title, Lines author, Lines toc,
                                    BiPredicate<String, String> customDetector, Section... sections) {
        final Book book = new Book();

        parseBookTitleAndAuthors(book, lines, title, author);

        final Node<Chapter> definedChapters = new Node<>();
        if (null != toc) {
            final List<String> subList = lines.subList(toc.from, toc.to);
            for (String line : subList) {
                if (line.isBlank())
                    continue;
                String cleanText = line.strip();
                // toc with page number
                if (cleanText.contains("…")) {
                    cleanText = cleanText.substring(0, cleanText.indexOf("…"));
                }
//                cleanText = cleanText.replaceAll(" ", "");
                definedChapters.add(new Chapter().setType("article").setTitle(cleanText));
            }
        }

        if (null != sections) {
            for (Section section : sections) {
                if ("detect".equals(section.type)) {
                    final List<String> subList = lines.subList(section.from, section.to);
                    Iterator<Node<Chapter>> definedChaptersIterator = definedChapters.children().iterator();
                    Node<Chapter> currChapter = definedChaptersIterator.next(), nextChapter = null;
                    for (String line : subList) {
                        if (null == nextChapter && definedChaptersIterator.hasNext())
                            nextChapter = definedChaptersIterator.next();

                        line = line.strip();
                        if (null != nextChapter) {
                            if (line.equals(nextChapter.value.title)
                                    || null != customDetector && customDetector.test(nextChapter.value.title, line)) {
                                currChapter = nextChapter;
                                nextChapter = null;
                                continue;
                            }
                        }
                        if (currChapter.value.title.equals(line))
                            continue;

                        if (!currChapter.value.hasParagraphs())
                            currChapter.value.addParagraph(null, line);
                        else currChapter.value.paragraphs.get(0).appendContent(line);
                    }
                    book.chapters.merge(definedChapters);
                } else {
                    parseBookSection(book, lines, section);
                }
            }
        }
        return book;
    }

    public static Book parseWithoutTOC(final List<String> lines, Lines title, Lines author,
                                       BiPredicate<List<String>, Integer> customDetector, Section... sections) {
        final Book book = new Book();

        parseBookTitleAndAuthors(book, lines, title, author);

        if (null != sections) {
            for (Section section : sections) {
                if ("detect".equals(section.type)) {
                    final List<String> subList = lines.subList(section.from, section.to);
                    String line, caption = null;
                    List<String> content = new ArrayList<>();
                    for (int i = 0; i < subList.size(); i++) {
                        line = subList.get(i);
                        if (null != customDetector && customDetector.test(subList, i)) {
                            if (!content.isEmpty()) {
                                book.chapters.add(new Chapter().setType("article")
                                        .addParagraph(caption, StringHelper.join("\n", content))
                                        .setTitle(caption));
                            }
                            //
                            caption = line.strip();
                            content.clear();
                        } else {
                            content.add(line.strip());
                        }
                    }
                    if (!content.isEmpty()) {
                        book.chapters.add(new Chapter().setType("article")
                                .addParagraph(caption, StringHelper.join("\n", content))
                                .setTitle(caption));
                    }
                } else {
                    parseBookSection(book, lines, section);
                }
            }
        }
        return book;
    }

    private static List<String> subCleanedLines(List<String> lines, Lines mark) {
        return lines.subList(mark.from, mark.to).stream().map(String::strip).collect(Collectors.toList());
    }

    private static void parseBookSection(Book book, List<String> lines, Section section) {
        String caption = null, content = null;
        //
        if (null != section.head && section.head.isValid())
            caption = StringHelper.join("", subCleanedLines(lines, section.head));
        else if (null != section.head && StringHelper.isNotBlank(section.head.textDef))
            caption = section.head.textDef;
        if (StringHelper.isBlank(caption))
            caption = "NO-CAPTION";
        //
        if (section.isValid())
            content = StringHelper.join("\n", subCleanedLines(lines, section));
        if (StringHelper.isBlank(content) && StringHelper.isNotBlank(section.textDef))
            content = section.textDef;
        //
        book.chapters.add(new Chapter().setType(null != section.type ? section.type : "article")
                .addParagraph(caption, content)
                .setTitle(caption));
    }

    private static void parseBookTitleAndAuthors(Book book, List<String> lines, Lines title, Lines author) {
        if (null != title) {
            if (title.isValid())
                book.title = StringHelper.join(" ", subCleanedLines(lines, title));
            if (StringHelper.isBlank(book.title) && StringHelper.isNotBlank(title.textDef))
                book.title = title.textDef;
        }

        if (null != author) {
            if (author.isValid()) {
                final List<String> subList = subCleanedLines(lines, author);
                book.authorInfo = StringHelper.join(", ", subList);
                for (String line : subList) {
                    if (line.isBlank())
                        continue;
                    if (line.matches(".*[译著編]$"))
                        line = line.substring(0, line.length() - 1).strip();
                    for (String s : line.split("[ ,，]")) {
                        if (!s.isBlank())
                            book.authors.add(s.strip());
                    }
                }
            }
        }

    }

    public static Book prepareBook(Book book) {
        book.chapters.traverse((level, node, chapter) -> {
            if (null == chapter.title)
                throw new RuntimeException();
            chapter.setTitle(chapter.title.replaceAll("\s+", " "));
            if (chapter.hasParagraphs())
                chapter.paragraphs.forEach(para -> {
                    if (Objects.equals(chapter.title, para.caption))
                        para.caption = null;
                    para.content = "<p>" + para.content.replaceAll("\n\n", "\n").replaceAll("\n", "</p>\n<p>") + "</p>";
                });
        });
        return book;
    }
}

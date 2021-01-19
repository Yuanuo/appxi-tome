package org.appxi.tome.cbeta;

import org.appxi.tome.model.Chapter;
import org.appxi.util.ext.Node;

import java.util.Comparator;
import java.util.function.Supplier;

public class ChapterTree extends ChapterTreeParser<Node<Chapter>> {
    private static final Object AK_PARSED = new Object();

    public ChapterTree(CbetaBook book) {
        super(book, getOrAddTocChapters(book), getOrAddVolChapters(book), AK_PARSED);
    }

    public ChapterTree(CbetaBook book, Node<Chapter> tocChapters, Node<Chapter> volChapters) {
        super(book.removeAttr(AK_PARSED), tocChapters, volChapters, AK_PARSED);
    }

    @Override
    protected Node<Chapter> createTreeItem(Node<Chapter> parent, Chapter chapter) {
        Chapter parentVal = parent.value;
        // fix the start at first time
        if (null != parentVal.path && !parent.hasChildren() && parentVal.path.equals(chapter.path) && null != parentVal.start) {
            chapter.start = parentVal.start;
        }
        return parent.add(chapter);
    }

    @Override
    protected boolean existsItemInTree(String path, Node<Chapter> tree) {
        return null != tree.findFirst(n -> null != n.value && path.equals(n.value.path));
    }

    @Override
    protected void sortChildrenOfTree(Node<Chapter> tree) {
        tree.children().sort(Comparator.comparing(o -> o.value.path));
    }

    private static Node<Chapter> getOrAddTocChapters(CbetaBook book) {
        Node<Chapter> chapters = book.chapters.findFirst(node -> "tocs".equals(node.value.id));
        if (null == chapters)
            chapters = book.chapters.add(new Chapter().setId("tocs").setType("title").setTitle("章节目录"));
        return chapters;
    }

    private static Node<Chapter> getOrAddVolChapters(CbetaBook book) {
        Node<Chapter> chapters = book.chapters.findFirst(node -> "vols".equals(node.value.id));
        if (null == chapters)
            chapters = book.chapters.add(new Chapter().setId("vols").setType("title").setTitle("卷次目录"));
        return chapters;
    }

    public static ChapterTree getOrInitBookChapters(CbetaBook book) {
        return getOrInitBookChapters(book, null);
    }

    public static ChapterTree getOrInitBookChapters(CbetaBook book, Supplier<Chapter> chapterSupplier) {
        ChapterTree tree = null == chapterSupplier
                ? new ChapterTree(book)
                : new ChapterTree(book) {
            @Override
            protected Chapter createChapter() {
                return chapterSupplier.get();
            }
        };
        tree.getTocChapters();
        return tree;
    }
}

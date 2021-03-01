package org.appxi.tome.cbeta;

import org.appxi.tome.TomeHelper;
import org.appxi.tome.model.Book;
import org.appxi.util.FileHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.XmlSaxHelper;
import org.appxi.util.ext.Node;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public abstract class CbetaHelper {
    private static long dataTimeNewest = -1;
    private static Path DIR_DATA = Path.of("");

    public static Path dataDir() {
        return DIR_DATA;
    }

    public static long getDataTimeNewest() {
        return dataTimeNewest;
    }

    public static String getDataDirectory() {
        return null == DIR_DATA ? null : DIR_DATA.toString();
    }

    public static boolean setDataDirectory(String dir) {
        final Path homeDir = Path.of(null == dir ? "" : dir);
        if (Files.notExists(homeDir))
            return false;

        Path dataDir = validDataDirectory(homeDir);
        if (null == dataDir)
            dataDir = validDataDirectory(homeDir.resolve("CBETA"));
        if (null == dataDir)
            dataDir = validDataDirectory(homeDir.resolve("Bookcase/CBETA"));
        if (null == dataDir)
            dataDir = validDataDirectory(homeDir.resolve("cbeta/Bookcase/CBETA"));
        if (null == dataDir)
            return false;
        DIR_DATA = dataDir;
        return true;
    }

    public static Path resolveData(String other) {
        return DIR_DATA.resolve(other);
    }

    public static Path validDataDirectory(Path dir) {
        if (Files.notExists(dir))
            return null;
        if (!Files.isDirectory(dir))
            return null;
        if (!Files.isReadable(dir))
            return null;

        final String[] stdFiles = new String[]{"advance_nav.xhtml", "bookdata.txt", "bulei_nav.xhtml", "catalog.txt",
                "menu_nav.xhtml", "simple_nav.xhtml", "spine.txt"};
        final List<Path> stdFilesList = new ArrayList<>();
        for (String stdFile : stdFiles) {
            final Path file = dir.resolve(stdFile);
            if (Files.notExists(file))
                return null;
            if (Files.isDirectory(file))
                return null;
            if (!Files.isReadable(file))
                return null;
            try {
                if (Files.size(file) < 1)
                    return null;
            } catch (IOException e) {
                return null;
            }
            stdFilesList.add(file);
        }
        dataTimeNewest = FileHelper.fileTimeNewest(stdFilesList.toArray(new Path[0]));
        return dir;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private static final String REGEX_AUTHOR_R2 = "(並序|主編|會編|彙輯|刪定|刪合|校定|重刻|重編|編錄|記錄|整理|御製|合校|校訂|增修|脩定|編訂|續修|摘要|定本|追擬|譯註|撰輯|排定|" +
            "校勘|校釋|校注|校註|編次|編註|編纂|編集|手錄|撰述|重訂|重校|續集|纂集|纂補|纂輯|譯纂|釋論|原詩|譯經|編目|編閱|請啟|編緝|御選|纂閱|錄存|錄出|證義|解義|演義|造本論|標註|口述|" +
            "造頌|譯釋|譯講|科攝|譯述|譯漢|口譯|筆錄|述疏|繪圖|集證|重修|編修|註釋|提唱|譯英|集註|科註|詮註|改寫|詮次|參閱|並註|略註|補註|纂註|評註|宗通|造論|譯抄之|等編訂|等編|重輯并)$";
    private static final String REGEX_AUTHOR_R1 = "([造譯釋記說傳講述撰解編錄集制輯著纂疏補序注問答和跋鈔定詩評頌製註本糅作較校抄])$";

    private static final String REGEX_AUTHOR_M2 = ".*" + REGEX_AUTHOR_R2;
    private static final String REGEX_AUTHOR_M1 = ".*" + REGEX_AUTHOR_R1;

    static void parseBookAuthorInfo(Book book) {
        String author = book.authorInfo;
        if (null == author || author.length() < 2)
            return;
        if (author.contains("（"))
            author = author.substring(0, author.indexOf("（"));
        author = author.replaceAll("\\(.+?\\)", "");
        book.authorInfo = author; // keep the full info
        //
        String[] tmpArr;
        String nameStr;
        for (String str : author.split("(　|  )")) {
            tmpArr = str.split(" ", 2);
            nameStr = str;
            if (tmpArr.length == 2) {
                if (tmpArr[0].contains("．")) {
                    nameStr = str;
                } else if (!StringHelper.indexOf(tmpArr[0], "德溥","守詮","嗣端")) {
                    if (tmpArr[0].length() < 3 || StringHelper.indexOf(tmpArr[0], "乞伏秦", "南北朝", "宇文周")) {
                        book.periods.add(tmpArr[0]);
                        nameStr = tmpArr[1];
                    }
                }
            }
            //
            tmpArr = nameStr.split("[ ]");
            for (int i = 0; i < tmpArr.length; i++) {
                nameStr = tmpArr[i];
                if ("失譯".equals(nameStr)) {
                    book.authors.add(nameStr);
                    if (tmpArr.length > i + 1 && tmpArr[i + 1].startsWith("附"))
                        tmpArr[i + 1] = tmpArr[i + 1].substring(1);
                    continue;
                }
                boolean valid = false;
                if (nameStr.matches(REGEX_AUTHOR_M2)) {
                    valid = true;
                    nameStr = nameStr.replaceAll(REGEX_AUTHOR_R2, "");
                    nameStr = nameStr.replaceAll(REGEX_AUTHOR_R1, "");
                } else if (nameStr.matches(REGEX_AUTHOR_M1)) {
                    valid = true;
                    nameStr = nameStr.replaceAll(REGEX_AUTHOR_R1, "");
                }
                if (valid || nameStr.contains("．") || nameStr.equals(author) || nameStr.length() > 1 && nameStr.length() < 5) {
                    nameStr = nameStr.replaceAll("(等)$", "");
                    book.authors.addAll(Arrays.asList(nameStr.split("[、．]")));
                } else {
                    // ignore for now
//                    DevtoolHelper.LOG.info("unknown author: " + nameStr + "  ///  " + author);
                }
            }
        }
    }

    public static String parseNavCatalogInfo(String text) {
        if (!text.contains(" ")) {
            return text;
        }
        final String[] groups = text.replace(", ", ",,")
                .replace("etc.", " ")
                .replace("  ", " ")
                .split("／");
        final StringBuilder result = new StringBuilder();
        for (String group : groups) {
            final StringBuilder buf = new StringBuilder();
            for (String tmp : StringHelper.split(group, " ", "[\\(\\)]")) {
                if (tmp.matches("^[a-zA-Z=].*"))
                    continue;
                if (buf.length() == 0 && tmp.matches("^[0-9].*"))
                    continue;
                buf.append(tmp).append(' ');
            }
            if (buf.length() > 0) {
                if (result.length() > 0)
                    result.append('／');
                result.append(buf.toString().strip());
            }
        }
        return result.length() == 0 ? text : result.toString();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static void walkTocChapters(CbetaBook book, BiPredicate<String, String> visitor) {
        walkBookChapters(book, true, false, visitor);
    }

    public static void walkVolChapters(CbetaBook book, BiPredicate<String, String> visitor) {
        walkBookChapters(book, false, true, visitor);
    }

    public static void walkBookChapters(CbetaBook book, BiPredicate<String, String> visitor) {
        walkBookChapters(book, true, true, visitor);
    }

    private static void walkBookChapters(CbetaBook book, boolean walkTocs, boolean walkVols, BiPredicate<String, String> visitor) {
        if (null == book || null == visitor || !walkTocs && !walkVols)
            return;
        if (null != book.path && book.path.startsWith("toc/")) {
            final Document doc = TomeHelper.xml(CbetaHelper.resolveData(book.path));
            final Element body = doc.body();
            //
            final Element[] targets = new Element[]{
                    walkTocs ? body.selectFirst("> nav[type=catalog]") : null,
                    walkVols ? body.selectFirst("> nav[type=juan]") : null
            };
            for (Element target : targets) {
                if (null != target) {
                    for (Element linkEle : target.select("cblink[href], a[href]")) {
                        if (visitor.test(linkEle.attr("href"), linkEle.text()))
                            return;
                    }
                }
            }
        }
    }


    public static void walkTocChaptersByXmlSAX(CbetaBook book, BiConsumer<String, String> visitor) {
        walkBookChaptersByXmlSAX(book, true, false, visitor);
    }

    public static void walkVolChaptersByXmlSAX(CbetaBook book, BiConsumer<String, String> visitor) {
        walkBookChaptersByXmlSAX(book, false, true, visitor);
    }

    public static void walkBookChaptersByXmlSAX(CbetaBook book, BiConsumer<String, String> visitor) {
        walkBookChaptersByXmlSAX(book, true, true, visitor);
    }

    public static void walkBookChaptersByXmlSAX(CbetaBook book, boolean walkTocs, boolean walkVols, BiConsumer<String, String> visitor) {
        if (null == book || null == visitor || !walkTocs && !walkVols)
            return;
        if (null != book.path && book.path.startsWith("toc/")) {
            try {
                XmlSaxHelper.walk(CbetaHelper.resolveData(book.path),
                        new XmlSaxHelper.XmlSaxVisitor() {
                            @Override
                            public boolean accept(Node<String> node, Attributes attributes) {
                                if ("nav".equals(node.value)) {
                                    node.attr("type", attributes.getValue("type"));
                                    return false;
                                }
                                if (StringHelper.indexOf(node.value, "cblink", "a")) {
                                    if (walkTocs && node.findParent(n -> n.hasAttr("type", "catalog")) != null)
                                        return true;
                                    return walkVols && node.findParent(n -> n.hasAttr("type", "juan")) != null;
                                }
                                return false;
                            }

                            @Override
                            public void preVisitElement(Node<String> node, Attributes attributes) {
                                node.attr("href", attributes.getValue("href"));
                            }

                            @Override
                            public void visitElementContent(Node<String> node, String text) {
                                visitor.accept(node.attr("href"), text);
                            }
                        }
                );
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}

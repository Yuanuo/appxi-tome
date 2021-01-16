package org.appxi.tome.xml;

import org.appxi.util.StringHelper;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeVisitor;

public abstract class ElementHelper {
    public static void cleanupAllUnwanted(Element element, String... matches) {
        cleanupAnyComments(element);
        cleanupAnyImages(element);
        cleanupAnyAttributes(element);
        cleanupAnyWordStyles(element);
        if (null != matches && matches.length > 0)
            ElementHelper.cleanupAnyMatches(element, matches);
        cleanupAnyEmpties(element);
        cleanupAnySingles(element);
    }

    public static void cleanupAnyAttributes(Element element) {
        element.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof Element ele)
                    ele.clearAttributes();
            }

            @Override
            public void tail(Node node, int depth) {
            }
        });
    }

    public static void cleanupAnyComments(Element element) {
        element.filter(new NodeFilter() {
            @Override
            public FilterResult tail(Node node, int depth) {
                return node instanceof Comment ? FilterResult.REMOVE : FilterResult.CONTINUE;
            }

            @Override
            public FilterResult head(Node node, int depth) {
                return node instanceof Comment ? FilterResult.REMOVE : FilterResult.CONTINUE;
            }
        });
    }

    public static void cleanupAnyEmpties(Element element) {
        while (true) {
            element.select("* *:matches(^([ 　]+)$)").remove();
            Elements elements = element.select("* *:empty:not(br)");
            if (elements.isEmpty())
                break; // no more empty found
            elements.remove();
        }
    }

    public static void cleanupAnyElements(Element element, String... tags) {
        element.select(StringHelper.join(", ", tags)).remove();
    }

    public static void cleanupAnyImages(Element element) {
        element.select("img").remove();
    }

    public static void cleanupAnyMatches(Element element, String... matches) {
        String regex = "^(" + StringHelper.join("|", matches) + ")$";
        element.select("* *:matches(" + regex + ")").remove();
    }

    public static void cleanupAnySingles(Element element) {
        while (true) {
            Elements elements = element.select("*:only-child:not(br, [xx=yy])");
            if (elements.isEmpty() || elements.size() == 1 && elements.first().parent() == element)
                break;
            for (Element ele : elements) {
                Element prt = ele.parent();
                if (prt == element || !prt.tagName().equals(ele.tagName())) {
                    ele.attr("xx", "yy");
                    continue;
                }
                prt.replaceWith(ele);
            }
        }
        element.select("[xx=yy]").forEach(ele -> ele.removeAttr("xx"));
    }

    public static void cleanupAnySinglesNested(Element element) {
        while (true) {
            Elements elements = element.select("*:only-child:not(br)");
            if (elements.isEmpty() || elements.size() == 1 && elements.first().parent() == element)
                break;
            for (Element ele : elements) {
                Element prt = ele.parent();
                if (prt == element)
                    continue;
                prt.replaceWith(ele);
            }
        }
    }

    public static void cleanupChildrenSingles(Element element) {
        while (true) {
            Elements eleChildren = element.children();
            if (eleChildren.size() == 1) {
                Elements subChildren = eleChildren.get(0).children();
                if (subChildren.size() == 1) {
                    Element eleChild = eleChildren.get(0);
                    Element subChild = subChildren.get(0);
                    subChild.remove();
                    eleChild.replaceWith(subChild);
                    continue;
                }
            }
            break;
        }
    }

    public static void cleanupAnyWordStyles(Element element) {
        element.select("*|*").forEach(ele -> {
            if (ele.is("o|p")) {
                ele.tagName("p");
                return;
            }
            String eleTag = ele.tagName();

            if (eleTag.startsWith("v:") || eleTag.startsWith("o:")) {
                ele.remove();
                return;
            }
            if (eleTag.startsWith("st1:") || eleTag.contains(":")) {
                ele.tagName("span");
                return;
            }
        });
    }

}

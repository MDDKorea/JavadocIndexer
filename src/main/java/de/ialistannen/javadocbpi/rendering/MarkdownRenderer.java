package de.ialistannen.javadocbpi.rendering;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import de.ialistannen.javadocbpi.rendering.links.LinkResolver;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import spoon.javadoc.api.elements.JavadocElement;

public class MarkdownRenderer {

  private final LinkResolver linkResolver;

  public MarkdownRenderer(LinkResolver linkResolver) {
    this.linkResolver = linkResolver;
  }

  public static String render(String html) {
    return FlexmarkHtmlConverter.builder()
        .build()
        .convert(processHtml(html), -1);
  }

  private static String processHtml(String html) {
    // Clean up pre elements without code as a direct child - the markdown renderer will ignore them
    // and it might be a bit nicer to always have a "code" element.
    Document document = Jsoup.parseBodyFragment(html);

    // Unwrap double pre
    for (Element pre : document.getElementsByTag("pre")) {
      if (pre.childrenSize() == 1 && pre.child(0).tagName().equals("pre")) {
        pre.unwrap();
      }
    }

    for (Element pre : document.getElementsByTag("pre")) {
      if (pre.children().size() != 1 || !pre.children().get(0).tagName().equals("code")) {
        // Structure:
        //   <pre>
        // Transform to:
        //   <code>
        // and then
        //   <pre>
        //     <code>
        pre.tagName("code");
        pre.attr("class", "language-java");

        Element newPre = Objects.requireNonNull(pre.parent()).appendElement("pre");
        newPre.appendChild(pre);
      }
    }

    // Unwrap double codes
    for (Element code : document.getElementsByTag("code")) {
      if (code.childrenSize() == 1 && code.child(0).tagName().equals("code")) {
        code.unwrap();
      }
    }

    for (Element code : document.getElementsByTag("code")) {
      for (TextNode node : code.textNodes()) {
        String content = node.getWholeText();
        boolean hasNewline = content.endsWith("\n");
        if (hasNewline) {
          content = content.substring(0, content.length() - 1);
        }
        node.text(content.stripIndent() + (hasNewline ? "\n" : ""));
      }
    }

    for (Element pre : document.getElementsByTag("pre")) {
      Element quote = pre.parent();
      if (quote != null && quote.tagName().equals("blockquote")) {
        quote.unwrap();
      }
    }

    return document.getElementsByTag("body").get(0).html();
  }

}

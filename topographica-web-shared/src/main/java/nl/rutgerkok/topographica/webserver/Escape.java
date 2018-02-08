package nl.rutgerkok.topographica.webserver;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.google.common.html.HtmlEscapers;

public class Escape {

    /**
     * Escapes for use in HTML. Note that
     * {@code "<input type=number value=" + Escape.forHtml(value) + ">"} is
     * still not safe, as someone can just pass "4 onclick=evilMethod()" as the
     * value. However, if you use quotes (double or single) you should be safe.
     *
     * @param plainText
     *            The text.
     * @return The escaped string.
     */
    public static String forHtml(String plainText) {
        return HtmlEscapers.htmlEscaper().escape(plainText);
    }

    /**
     * Escapes for use in a URL query param. Note that the '=' is also escaped.
     *
     * @param plainText
     *            The plain text.
     * @return The escaped string.
     */
    public static String forQueryParam(String plainText) {
        try {
            return URLEncoder.encode(plainText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not supported??", e);
        }
    }

}

package nl.rutgerkok.topographica.marker;

import java.util.Objects;

import nl.rutgerkok.topographica.webserver.Escape;

public final class HtmlString {

    /**
     * Creates a HTML string. Newlines (\n) are converted to line break tags.
     *
     * @param plainText
     *            Plain text.
     * @return The HTML string.
     */
    public static HtmlString fromPlainText(String plainText) {
        String raw = Escape.forHtml(plainText).replace("\n", "<br>");
        return fromRawHtml(raw);
    }

    /**
     * Wraps exiting HTML into a {@link HtmlString} object. Note: the validity
     * of the HTML is not checked!
     *
     * @param raw
     *            The raw HTML, like "{@code Hi <i>everyone</i>!}".
     * @return A HTML object.
     */
    public static HtmlString fromRawHtml(String raw) {
        return new HtmlString(raw);
    }

    private final String raw;

    private HtmlString(String raw) {
        this.raw = Objects.requireNonNull(raw, "raw");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HtmlString other = (HtmlString) obj;
        if (!raw.equals(other.raw)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public String toString() {
        return raw;
    }
}

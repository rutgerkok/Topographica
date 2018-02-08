package nl.rutgerkok.topographica.webserver;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EscapeTest {

    @Test
    public void html() {
        // Really basic test, we trust Guava to have figured it out
        assertEquals("&lt;hi&gt;", Escape.forHtml("<hi>"));
    }

    @Test
    public void urlParam() {
        assertEquals("foo%2C+bar+and+baz+%3D+random+text", Escape.forQueryParam("foo, bar and baz = random text"));
    }
}

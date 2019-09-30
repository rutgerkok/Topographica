package nl.rutgerkok.topographica.marker;

import com.google.gson.JsonElement;

/**
 * Used to indicate that an object can be converted to JSON using
 * {@link #toJsonElement()}.
 */
public interface JsonAware {

    /**
     * Converts the contents of this object to a JSON element
     *
     * @return The JSON element.
     */
    JsonElement toJsonElement();

}

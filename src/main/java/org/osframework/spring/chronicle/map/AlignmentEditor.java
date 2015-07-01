package org.osframework.spring.chronicle.map;

import net.openhft.chronicle.map.Alignment;

import java.beans.PropertyEditorSupport;

/**
 * Supports setting and displaying JavaBean properties of type {@code Alignment}.
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @since 0.0.1
 */
public class AlignmentEditor extends PropertyEditorSupport {

    public AlignmentEditor() {
        super();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (null == text) {
            throw new IllegalArgumentException("Argument 'text' cannot be null");
        }
        setValue(Alignment.valueOf(text.trim().toUpperCase()));
    }

    @Override
    public String getAsText() {
        Alignment a = (Alignment)getValue();
        return (null == a) ? null : a.name();
    }

}

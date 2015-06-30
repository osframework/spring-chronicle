package org.osframework.spring.chronicle.map;

import net.openhft.chronicle.map.Alignment;

import java.beans.PropertyEditorSupport;

/**
 * Created by dave on 6/29/15.
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

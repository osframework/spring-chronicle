/*
   Copyright 2016 OSFramework Project

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
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

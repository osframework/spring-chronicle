package org.osframework.spring.chronicle.map;


import net.openhft.chronicle.map.Alignment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for {@code AlignmentEditor}.
 *
 * @author <a href="mailto:dave@osframework.org">Dave Joyce</a>
 * @version 0.0.1
 * @since 2015-06-30
 */
public class AlignmentEditorTest {

    private AlignmentEditor editor = null;

    @BeforeMethod
    public void createEditor() {
        editor = new AlignmentEditor();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSetAsTextNullArg() {
        editor.setAsText(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSetAsTextBadValue() {
        editor.setAsText("BAD_VALUE");
    }

    @Test
    public void testSetAsTextGoodValueLowercase() {
        editor.setAsText("no_alignment");
        Alignment actual = (Alignment)editor.getValue();
        assertEquals(actual, Alignment.NO_ALIGNMENT);
    }

    @Test
    public void testSetAsTextGoodValueUppercase() {
        editor.setAsText("NO_ALIGNMENT");
        Alignment actual = (Alignment)editor.getValue();
        assertEquals(actual, Alignment.NO_ALIGNMENT);
    }

    @Test
    public void testGetAsText() {
        editor.setValue(Alignment.NO_ALIGNMENT);
        assertEquals(editor.getAsText(), "NO_ALIGNMENT");
    }

}

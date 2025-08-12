package hudson.tasks._ant;

import hudson.MarkupText;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for the {@link AntTargetNote} class.
 */
class AntTargetNoteTest {

    private boolean antTargetNoteEnabled;

    @BeforeEach
    void setUp() {
        antTargetNoteEnabled = AntTargetNote.ENABLED;
        AntTargetNote.ENABLED = true;
    }

    @AfterEach
    void tearDown() {
        AntTargetNote.ENABLED = antTargetNoteEnabled;
    }

    @Test
    void testAnnotateTarget() {
        assertEquals("<b class=ant-target>TARGET</b>:", annotate("TARGET:"));
    }

    @Test
    void testAnnotateTargetContainingColon() {
        // See HUDSON-7026.
        assertEquals("<b class=ant-target>TEST:TARGET</b>:", annotate("TEST:TARGET:"));
    }

    @Test
    void testDisabled() {
        AntTargetNote.ENABLED = false;
        assertEquals("TARGET:", annotate("TARGET:"));
    }

    private static String annotate(String text) {
        MarkupText markupText = new MarkupText(text);
        new AntTargetNote().annotate(new Object(), markupText, 0);
        return markupText.toString(true);
    }
}

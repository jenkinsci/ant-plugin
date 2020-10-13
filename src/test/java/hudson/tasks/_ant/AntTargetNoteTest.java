package hudson.tasks._ant;

import hudson.MarkupText;
import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.jvnet.hudson.test.FlagRule;

/**
 * Unit test for the {@link AntTargetNote} class.
 */
public class AntTargetNoteTest {

    @Rule
    public TestRule antTargetNoteEnabled = new FlagRule<Boolean>(() -> AntTargetNote.ENABLED, x -> AntTargetNote.ENABLED = x, true);

    @Test
    public void testAnnotateTarget() {
        assertEquals("<b class=ant-target>TARGET</b>:", annotate("TARGET:"));
    }

    @Test
    public void testAnnotateTargetContainingColon() {
        // See HUDSON-7026.
        assertEquals("<b class=ant-target>TEST:TARGET</b>:", annotate("TEST:TARGET:"));
    }

    @Test
    public void testDisabled() {
        AntTargetNote.ENABLED = false;
        assertEquals("TARGET:", annotate("TARGET:"));
    }

    private String annotate(String text) {
        MarkupText markupText = new MarkupText(text);
        new AntTargetNote().annotate(new Object(), markupText, 0);
        return markupText.toString(true);
    }
}

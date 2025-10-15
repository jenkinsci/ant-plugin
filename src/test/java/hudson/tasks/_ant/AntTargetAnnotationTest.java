package hudson.tasks._ant;

import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Ant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.ToolInstallations;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
public class AntTargetAnnotationTest {
    
    private JenkinsRule r;
    @TempDir
    private File tmp;
    
    private boolean antTargetNoteEnabled;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        r = rule;
        antTargetNoteEnabled = AntTargetNote.ENABLED;
    }

    @AfterEach
    void tearDown() {
        AntTargetNote.ENABLED = antTargetNoteEnabled;
    }

    @Test
    void test1() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        TemporaryFolder temporaryFolder = new TemporaryFolder(tmp);
        temporaryFolder.create();
        Ant.AntInstallation ant = ToolInstallations.configureDefaultAnt(temporaryFolder);
        p.getBuildersList().add(new Ant("foo",ant.getName(),null,null,null));
        p.setScm(new SingleFileSCM("build.xml",getClass().getResource("simple-build.xml")));
        FreeStyleBuild b = r.buildAndAssertSuccess(p);

        AntTargetNote.ENABLED = true;
        WebClient wc = r.createWebClient();
        HtmlPage c = wc.getPage(b, "console");
        System.out.println(c.asNormalizedText());
        DomElement o = c.getElementById("console-outline");

        assertEquals(2,o.getByXPath(".//LI").size());
    }
    
}

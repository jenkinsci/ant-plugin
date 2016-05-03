package hudson.tasks._ant;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Ant;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.ToolInstallations;

import static org.junit.Assert.assertEquals;
/**
 * @author Kohsuke Kawaguchi
 */
public class AntTargetAnnotationTest {
    
    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    @Test
    public void test1() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        Ant.AntInstallation ant = ToolInstallations.configureDefaultAnt(tmp);
        p.getBuildersList().add(new Ant("foo",ant.getName(),null,null,null));
        p.setScm(new SingleFileSCM("build.xml",getClass().getResource("simple-build.xml")));
        FreeStyleBuild b = r.buildAndAssertSuccess(p);

        AntTargetNote.ENABLED = true;
        try {
            WebClient wc = r.createWebClient();
            HtmlPage c = wc.getPage(b, "console");
            System.out.println(c.asText());
            DomElement o = c.getElementById("console-outline");

            assertEquals(2,o.getByXPath(".//LI").size());
        } finally {
            AntTargetNote.ENABLED = false;
        }
    }
    
}

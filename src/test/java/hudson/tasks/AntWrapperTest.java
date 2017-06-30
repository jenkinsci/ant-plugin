/*
 * The MIT License
 *
 * Copyright 2016 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.tasks;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

public class AntWrapperTest {

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule public RestartableJenkinsRule r = new RestartableJenkinsRule();
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void configRoundTrip() throws Exception {
        r.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                Ant.AntInstallation installation = ToolInstallations.configureDefaultAnt(tmp);
                FreeStyleProject p = r.j.createFreeStyleProject(); // no configRoundTrip(BuildWrapper) it seems
                AntWrapper aw1 = new AntWrapper();
                p.getBuildWrappersList().add(aw1);
                p = r.j.configRoundtrip(p);
                AntWrapper aw2 = p.getBuildWrappersList().get(AntWrapper.class);
                r.j.assertEqualDataBoundBeans(aw1, aw2);
                aw2.setInstallation(installation.getName());
                p = r.j.configRoundtrip(p);
                AntWrapper aw3 = p.getBuildWrappersList().get(AntWrapper.class);
                r.j.assertEqualDataBoundBeans(aw2, aw3);
            }
        });
    }

    /** @see hudson.tasks._ant.AntTargetAnnotationTest#test1 */
    @Test public void smokes() throws Exception {
        r.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                ToolInstallations.configureDefaultAnt(tmp); // TODO could instead use DockerRule<JavaContainer> to run against a specified JDK location
                WorkflowJob p = r.j.createProject(WorkflowJob.class, "p");
                r.j.jenkins.getWorkspaceFor(p).child("build.xml").copyFrom(AntWrapperTest.class.getResource("_ant/simple-build.xml"));
                p.setDefinition(new CpsFlowDefinition("node {withAnt(installation: 'default') {if (isUnix()) {sh 'ant foo'} else {bat 'ant foo'}}}", true));
                WorkflowRun b = r.j.buildAndAssertSuccess(p);
                // TODO passes locally, fails in jenkins.ci: AntConsoleAnnotator processes AntOutcomeNote but not AntTargetNote
                // (perhaps because it seems to have set ANT_HOME=/opt/ant/latest? yet the output looks right)
                AntTest.assertHtmlLogContains(b, "<b class=ant-target>foo</b>");
                AntTest.assertHtmlLogContains(b, "<b class=ant-target>bar</b>");
                JenkinsRule.WebClient wc = r.j.createWebClient();
                HtmlPage c = wc.getPage(b, "console");
                DomElement o = c.getElementById("console-outline");
                assertEquals(2, o.getByXPath(".//LI").size());
            }
        });
    }

    @Test public void durability() throws Exception {
        r.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                ToolInstallations.configureDefaultAnt(tmp);
                WorkflowJob p = r.j.createProject(WorkflowJob.class, "p");
                r.j.jenkins.getWorkspaceFor(p).child("build.xml").copyFrom(AntWrapperTest.class.getResource("_ant/pauses.xml"));
                p.setDefinition(new CpsFlowDefinition("node {withAnt(installation: 'default') {if (isUnix()) {sh 'ant'} else {bat 'ant'}}}", true));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                r.j.waitForMessage("before signal created", b);
                AntTest.assertHtmlLogContains(b, "<b class=ant-target>prep</b>");
                AntTest.assertHtmlLogNotContains(b, "<b class=ant-target>main</b>");
            }
        });
        r.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                WorkflowJob p = r.j.jenkins.getItemByFullName("p", WorkflowJob.class);
                WorkflowRun b = p.getBuildByNumber(1);
                r.j.jenkins.getWorkspaceFor(p).child("signal").write("here", null);
                r.j.waitForCompletion(b);
                r.j.assertLogContains("after signal created", b);
                AntTest.assertHtmlLogContains(b, "<b class=ant-target>main</b>");
            }
        });
    }

}

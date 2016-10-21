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
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
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

public class AntStepTest {

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule public RestartableJenkinsRule r = new RestartableJenkinsRule();
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void configRoundTrip() throws Exception {
        r.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                ToolInstallations.configureDefaultAnt(tmp);
                AntStep step = new AntStep("compile");
                step.setTool("default");
                AntStep step2 = new StepConfigTester(r.j).configRoundTrip(step);
                r.j.assertEqualDataBoundBeans(step, step2);
                step.setOpts("-Dwhatever");
                step2 = new StepConfigTester(r.j).configRoundTrip(step);
                r.j.assertEqualDataBoundBeans(step, step2);
            }
        });
    }

    /** @see hudson.tasks._ant.AntTargetAnnotationTest#test1 */
    @Test public void smokes() throws Exception {
        r.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                ToolInstallations.configureDefaultAnt(tmp);
                WorkflowJob p = r.j.createProject(WorkflowJob.class, "p");
                r.j.jenkins.getWorkspaceFor(p).child("build.xml").copyFrom(AntStepTest.class.getResource("_ant/simple-build.xml"));
                p.setDefinition(new CpsFlowDefinition("node {ant targets: 'foo', tool: 'default'}", true));
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
                r.j.jenkins.getWorkspaceFor(p).child("build.xml").copyFrom(AntStepTest.class.getResource("_ant/pauses.xml"));
                p.setDefinition(new CpsFlowDefinition("node {ant tool: 'default'}", true));
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

    @Test public void opts() throws Exception {
        r.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                ToolInstallations.configureDefaultAnt(tmp);
                WorkflowJob p = r.j.createProject(WorkflowJob.class, "p");
                r.j.jenkins.getWorkspaceFor(p).child("build.xml").copyFrom(AntStepTest.class.getResource("_ant/simple-build.xml"));
                p.setDefinition(new CpsFlowDefinition("node {ant targets: 'foo', tool: 'default', opts: '-showversion'}", true));
                WorkflowRun b = r.j.buildAndAssertSuccess(p);
                r.j.assertLogContains("java version \"", b);
            }
        });
    }

}

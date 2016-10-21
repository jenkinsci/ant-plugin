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
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

public class AntStepTest {

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule public JenkinsRule r = new JenkinsRule();
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void configRoundTrip() throws Exception {
        ToolInstallations.configureDefaultAnt(tmp);
        AntStep step = new AntStep("compile");
        step.tool = "default";
        AntStep step2 = new StepConfigTester(r).configRoundTrip(step);
        r.assertEqualDataBoundBeans(step, step2);
    }

    /** @see hudson.tasks._ant.AntTargetAnnotationTest#test1 */
    @Test public void smokes() throws Exception {
        ToolInstallations.configureDefaultAnt(tmp);
        WorkflowJob p = r.createProject(WorkflowJob.class, "p");
        r.jenkins.getWorkspaceFor(p).child("build.xml").copyFrom(getClass().getResource("_ant/simple-build.xml"));
        p.setDefinition(new CpsFlowDefinition("node {ant targets: 'foo', tool: 'default'}", true));
        WorkflowRun b = r.buildAndAssertSuccess(p);
        JenkinsRule.WebClient wc = r.createWebClient();
        HtmlPage c = wc.getPage(b, "console");
        System.out.println(c.asText());
        DomElement o = c.getElementById("console-outline");
        assertEquals(2, o.getByXPath(".//LI").size());
    }

    // TODO restart test

}

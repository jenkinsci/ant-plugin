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

import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlPage;
import hudson.console.ConsoleNote;
import hudson.model.FreeStyleProject;
import hudson.slaves.DumbSlave;

import java.io.File;
import java.util.logging.Level;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.*;
import org.jvnet.hudson.test.junit.jupiter.BuildWatcherExtension;
import org.jvnet.hudson.test.junit.jupiter.JenkinsSessionExtension;

class AntWrapperTest {

    @SuppressWarnings("unused")
    private static final BuildWatcherExtension BUILD_WATCHER = new BuildWatcherExtension();
    @RegisterExtension
    private final JenkinsSessionExtension r = new JenkinsSessionExtension();
    @TempDir
    private File tmp;
    private final LogRecorder logging = new LogRecorder();

    @Test
    void configRoundTrip() throws Throwable {
        r.then(j -> {
            TemporaryFolder temporaryFolder = new TemporaryFolder(tmp);
            temporaryFolder.create();
            Ant.AntInstallation installation = ToolInstallations.configureDefaultAnt(temporaryFolder);
            FreeStyleProject p = j.createFreeStyleProject(); // no configRoundTrip(BuildWrapper) it seems
            AntWrapper aw1 = new AntWrapper();
            p.getBuildWrappersList().add(aw1);
            p = j.configRoundtrip(p);
            AntWrapper aw2 = p.getBuildWrappersList().get(AntWrapper.class);
            j.assertEqualDataBoundBeans(aw1, aw2);
            aw2.setInstallation(installation.getName());
            p = j.configRoundtrip(p);
            AntWrapper aw3 = p.getBuildWrappersList().get(AntWrapper.class);
            j.assertEqualDataBoundBeans(aw2, aw3);
        });
    }

    /** @see hudson.tasks._ant.AntTargetAnnotationTest#test1 */
    @Test
    void smokes() throws Throwable {
        r.then(j -> {
            TemporaryFolder temporaryFolder = new TemporaryFolder(tmp);
            temporaryFolder.create();
            ToolInstallations.configureDefaultAnt(temporaryFolder); // TODO could instead use DockerRule<JavaContainer> to run against a specified JDK location
            DumbSlave s = j.createOnlineSlave();
            logging.recordPackage(ConsoleNote.class, Level.FINE);
            WorkflowJob p = j.createProject(WorkflowJob.class, "p");
            s.getWorkspaceFor(p).child("build.xml").copyFrom(AntWrapperTest.class.getResource("_ant/simple-build.xml"));
            p.setDefinition(new CpsFlowDefinition("node('!master') {withAnt(installation: 'default') {if (isUnix()) {sh 'ant foo'} else {bat 'ant foo'}}}", true));
            WorkflowRun b = j.buildAndAssertSuccess(p);
            b.getLogText().writeRawLogTo(0, System.err);
            AntTest.assertHtmlLogContains(b, "<b class=ant-target>foo</b>");
            AntTest.assertHtmlLogContains(b, "<b class=ant-target>bar</b>");
            JenkinsRule.WebClient wc = j.createWebClient();
            HtmlPage c = wc.getPage(b, "console");
            DomElement o = c.getElementById("console-outline");
            assertEquals(2, o.getByXPath(".//LI").size());
        });
    }

    @Test
    void durability() throws Throwable {
        r.then(j -> {
            TemporaryFolder temporaryFolder = new TemporaryFolder(tmp);
            temporaryFolder.create();
            ToolInstallations.configureDefaultAnt(temporaryFolder);
            WorkflowJob p = j.createProject(WorkflowJob.class, "p");
            j.jenkins.getWorkspaceFor(p).child("build.xml").copyFrom(AntWrapperTest.class.getResource("_ant/pauses.xml"));
            p.setDefinition(new CpsFlowDefinition("node {withAnt(installation: 'default') {if (isUnix()) {sh 'ant'} else {bat 'ant'}}}", true));
            WorkflowRun b = p.scheduleBuild2(0).waitForStart();
            j.waitForMessage("before signal created", b);
            AntTest.assertHtmlLogContains(b, "<b class=ant-target>prep</b>");
            AntTest.assertHtmlLogNotContains(b, "<b class=ant-target>main</b>");
        });
        r.then(j -> {
            WorkflowJob p = j.jenkins.getItemByFullName("p", WorkflowJob.class);
            WorkflowRun b = p.getBuildByNumber(1);
            j.jenkins.getWorkspaceFor(p).child("signal").write("here", null);
            j.waitForCompletion(b);
            j.assertLogContains("after signal created", b);
            AntTest.assertHtmlLogContains(b, "<b class=ant-target>main</b>");
        });
    }

}

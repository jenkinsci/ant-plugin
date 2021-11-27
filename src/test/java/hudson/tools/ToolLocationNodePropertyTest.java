/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Tom Huybrechts
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
package hudson.tools;

import hudson.EnvVars;
import hudson.Functions;
import hudson.model.Build;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.tasks.Ant;
import hudson.tasks.Ant.AntInstallation;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.ToolInstallations;

/**
 * This class tests that environment variables from node properties are applied, and that the
 * priority is maintained: parameters > slave node properties > master node properties
 */
public class ToolLocationNodePropertyTest {

    @Rule public JenkinsRule j = new JenkinsRule();
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private DumbSlave slave;
    private FreeStyleProject project;

    private void configureDumpEnvBuilder() {
        if (Functions.isWindows()) project.getBuildersList().add(new BatchFile("set"));
        else project.getBuildersList().add(new Shell("export"));
    }

    @Test
    public void ant() throws Exception {
        Ant.AntInstallation ant = ToolInstallations.configureDefaultAnt(tmp);
        String antPath = ant.getHome();
        Jenkins.get()
                .getDescriptorByType(Ant.DescriptorImpl.class)
                .setInstallations(new AntInstallation("ant", "THIS IS WRONG"));

        project.setScm(new SingleFileSCM("build.xml", "<project name='foo'/>"));
        project.getBuildersList().add(new Ant("-version", "ant", null, null, null));
        configureDumpEnvBuilder();

        Build build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);

        ToolLocationNodeProperty property =
                new ToolLocationNodeProperty(
                        new ToolLocationNodeProperty.ToolLocation(
                                j.jenkins.getDescriptorByType(AntInstallation.DescriptorImpl.class),
                                "ant",
                                antPath));
        slave.getNodeProperties().add(property);

        build = project.scheduleBuild2(0).get();
        System.out.println(build.getLog());
        j.assertBuildStatus(Result.SUCCESS, build);
    }

    @Before
    public void setUp() throws Exception {
        EnvVars env = new EnvVars();
        // we don't want Maven, Ant, etc. to be discovered in the path for this test to work,
        // but on Unix these tools rely on other basic Unix tools (like env) for its operation,
        // so empty path breaks the test.
        env.put("PATH", "/bin:/usr/bin");
        env.put("M2_HOME", "empty");
        slave = j.createSlave(new LabelAtom("slave"), env);
        project = j.createFreeStyleProject();
        project.setAssignedLabel(slave.getSelfLabel());
    }
}

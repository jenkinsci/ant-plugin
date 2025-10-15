/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Yahoo! Inc.
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

import hudson.tasks._ant.AntTargetAnnotationTest;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import hudson.EnvVars;
import hudson.Functions;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterDefinition;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.tasks.Ant.AntInstallation;
import hudson.tasks.Ant.AntInstallation.DescriptorImpl;
import hudson.tasks.Ant.AntInstaller;
import hudson.tasks._ant.AntTargetNote;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;
import hudson.util.DescribableList;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class AntTest {

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

    /**
     * Tests the round-tripping of the configuration.
     */
    @Test
    void testConfigRoundtrip() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        p.getBuildersList().add(new Ant("a",null,"-b","c.xml","d=e"));

        WebClient webClient = r.createWebClient();
        HtmlPage page = webClient.getPage(p,"configure");

        HtmlForm form = page.getFormByName("config");
        r.submit(form);

        Ant a = p.getBuildersList().get(Ant.class);
        assertNotNull(a);
        assertEquals("a",a.getTargets());
        assertNull(a.getAnt());
        assertEquals("-b",a.getAntOpts());
        assertEquals("c.xml",a.getBuildFile());
        assertEquals("d=e",a.getProperties());
    }

    /**
     * Simulates the addition of the new Ant via UI and makes sure it works.
     */
    @Test
    void testGlobalConfigAjax() throws Exception {
        HtmlPage p = r.createWebClient().goTo("configureTools");
        HtmlForm f = p.getFormByName("config");
        HtmlButton b = r.getButtonByCaption(f, "Add Ant");
        b.click();
        r.findPreviousInputElement(b,"name").setValue("myAnt");
        r.findPreviousInputElement(b,"home").setValue("/tmp/foo");
        r.submit(f);
        verify();

        // another submission and verify it survives a roundtrip
        p = r.createWebClient().goTo("configure");
        f = p.getFormByName("config");
        r.submit(f);
        verify();
    }

    private void verify() throws Exception {
        AntInstallation[] l = r.get(DescriptorImpl.class).getInstallations();
        assertEquals(1,l.length);
        r.assertEqualBeans(l[0],new AntInstallation("myAnt","/tmp/foo", JenkinsRule.NO_PROPERTIES),"name,home");

        // Verify that PATH+ANT is set.
        EnvVars envVars = new EnvVars();
        l[0].buildEnvVars(envVars);
        assertTrue(envVars.containsKey("PATH+ANT"));
        assertEquals(l[0].getHome() + "/bin", envVars.get("PATH+ANT"));

        // by default we should get the auto installer
        DescribableList<ToolProperty<?>,ToolPropertyDescriptor> props = l[0].getProperties();
        assertEquals(1,props.size());
        InstallSourceProperty isp = props.get(InstallSourceProperty.class);
        assertEquals(1,isp.installers.size());
        assertNotNull(isp.installers.get(AntInstaller.class));
    }

    @Test
    void testSensitiveParameters() throws Exception {
        //TODO perhaps better way to check the requirement
        assumeTrue(!Functions.isWindows(), "Ant.bat is not automatically present on Windows");
        
        FreeStyleProject project = r.createFreeStyleProject();
        ParametersDefinitionProperty pdb = new ParametersDefinitionProperty(
                new StringParameterDefinition("string", "defaultValue", "string description"),
                new PasswordParameterDefinition("password", "12345", "password description"),
                new StringParameterDefinition("string2", "Value2", "string description")
        );
        project.addProperty(pdb);
        project.setScm(new SingleFileSCM("build.xml", AntTargetAnnotationTest.class.getResource("simple-build.xml")));

        project.getBuildersList().add(new Ant("foo",null,null,null,null));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        r.assertLogNotContains("-Dpassword=12345", build);
    }

    @Test
    void testParameterExpansion() throws Exception {
        if (!Functions.isWindows()) {
            String antName = configureDefaultAnt().getName();
            // Use a matrix project so we have env stuff via builtins, parameters and matrix axis.
            MatrixProject project = r.createProject(MatrixProject.class, "test project");// Space in name
            project.setAxes(new AxisList(new Axis("AX", "is")));
            project.addProperty(new ParametersDefinitionProperty(
                    new StringParameterDefinition("FOO", "bar", "")));
            project.setScm(new ExtractResourceSCM(getClass().getResource("ant-job.zip")));
            project.getBuildersList().add(new Ant("", antName, null, null,
                    """
                            vNUM=$BUILD_NUMBER
                            vID=$BUILD_ID
                            vJOB=$JOB_NAME
                            vTAG=$BUILD_TAG
                            vEXEC=$EXECUTOR_NUMBER
                            vNODE=$NODE_NAME
                            vLAB=$NODE_LABELS
                            vJAV=$JAVA_HOME
                            vWS=$WORKSPACE
                            vHURL=$HUDSON_URL
                            vBURL=$BUILD_URL
                            vJURL=$JOB_URL
                            vHH=$HUDSON_HOME
                            vJH=$JENKINS_HOME
                            vFOO=$FOO
                            vAX=$AX"""));
            r.assertBuildStatusSuccess(project.scheduleBuild2(0));
            MatrixRun build = project.getItem("AX=is").getLastBuild();
            String log = JenkinsRule.getLog(build);
            assertTrue(log.contains("vNUM=1"), "Missing $BUILD_NUMBER: " + log);
            // TODO 1.597+: assertTrue("Missing $BUILD_ID: " + log, log.contains("vID=1"));
            assertTrue(log.contains(project.getName()), "Missing $JOB_NAME: " + log);
            // Odd build tag, but it's constructed with getParent().getName() and the parent is the
            // matrix configuration, not the project.. if matrix build tag ever changes, update
            // expected value here:
            assertTrue(log.contains("vTAG=jenkins-test project-AX\\=is-1"), "Missing $BUILD_TAG: " + log);
            assertTrue(log.matches("(?s).*vEXEC=\\d.*"), "Missing $EXECUTOR_NUMBER: " + log);
            // $NODE_NAME is expected to be empty when running on master.. not checking.
            String builtInNodeLabel = r.jenkins.getSelfLabel().getExpression(); // compatibility with 2.307+
            assertTrue(log.contains("vLAB=" + builtInNodeLabel), "Missing $NODE_LABELS: " + log);
            assertTrue(log.matches("(?s).*vJH=[^\\r\\n].*"), "Missing $JAVA_HOME: " + log);
            assertTrue(log.matches("(?s).*vWS=[^\\r\\n].*"), "Missing $WORKSPACE: " + log);
            assertTrue(log.contains("vHURL=http"), "Missing $HUDSON_URL: " + log);
            assertTrue(log.contains("vBURL=http"), "Missing $BUILD_URL: " + log);
            assertTrue(log.contains("vJURL=http"), "Missing $JOB_URL: " + log);
            assertTrue(log.matches("(?s).*vHH=[^\\r\\n].*"), "Missing $HUDSON_HOME: " + log);
            assertTrue(log.matches("(?s).*vJH=[^\\r\\n].*"), "Missing $JENKINS_HOME: " + log);
            assertTrue(log.contains("vFOO=bar"), "Missing build parameter $FOO: " + log);
            assertTrue(log.contains("vAX=is"), "Missing matrix axis $AX: " + log);
        }
    }

    private AntInstallation configureDefaultAnt() throws Exception {
        TemporaryFolder temporaryFolder = new TemporaryFolder(tmp);
        temporaryFolder.create();
        return ToolInstallations.configureDefaultAnt(temporaryFolder);
    }

    @Issue("JENKINS-7442")
    @Test
    void testParameterExpansionByShell() throws Exception {
        assumeFalse(Functions.isWindows(), "TODO ant.bat seems to be leaving %HOME% unevaluated; unclear what the expected behavior is");
        String antName = configureDefaultAnt().getName();
        FreeStyleProject project = r.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("ant-job.zip")));
        String homeVar = Functions.isWindows() ? "%HOME%" : "$HOME";
        project.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("vFOO", homeVar, ""),
                new StringParameterDefinition("vBAR", "Home sweet " + homeVar + ".", "")));
        project.getBuildersList().add(new Ant("", antName, null, null,
                "vHOME=" + homeVar + "\nvFOOHOME=Foo " + homeVar + "\n"));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        r.assertBuildStatusSuccess(build);
        String log = JenkinsRule.getLog(build);
        if (!Functions.isWindows()) homeVar = "\\" + homeVar; // Regex escape for $
        assertTrue(log.matches("(?s).*vFOO=(?!" + homeVar + ").*"),
                   "Missing simple HOME parameter: " + log);
        assertTrue(log.matches("(?s).*vBAR=Home sweet (?!" + homeVar + ")[^\\r\\n]*\\..*"),
                   "Missing HOME parameter with other text: " + log);
        assertTrue(log.matches("(?s).*vHOME=(?!" + homeVar + ").*"),
                   "Missing HOME ant property: " + log);
        assertTrue(log.matches("(?s).*vFOOHOME=Foo (?!" + homeVar + ").*"),
                   "Missing HOME ant property with other text: " + log);
    }

    @Issue("JENKINS-7108")
    @Test
    void testEscapeXmlInParameters() throws Exception {
        String antName = configureDefaultAnt().getName();
        FreeStyleProject project = r.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("ant-job.zip")));
        project.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("vFOO", "<xml/>", "")));
        project.getBuildersList().add(new Ant("", antName, null, null, "vBAR=<xml/>\n"));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        r.assertBuildStatusSuccess(build);
        r.assertLogContains("vFOO=<xml/>", build);
        r.assertLogContains("vBAR=<xml/>", build);
    }

    @Test
    void invokeCustomTargetTest() throws Exception {
        FreeStyleProject project = createSimpleAntProject("clean compile", null, null, null);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
       
        assertEquals(Result.SUCCESS, build.getResult());

        assertHtmlLogContains(build, "<b class=ant-target>clean</b>");
        assertHtmlLogContains(build, "<b class=ant-target>compile</b>");
        assertHtmlLogNotContains(build, "<b class=ant-target>jar</b>");
        assertHtmlLogNotContains(build, "<b class=ant-target>run</b>");
        assertHtmlLogNotContains(build, "<b class=ant-target>main</b>");
    }

    @Test
    void invokeDefaultTargetTest() throws Exception {
        FreeStyleProject project = createSimpleAntProject("", null, null, null);
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
       
        assertEquals(Result.SUCCESS, build.getResult());

        assertHtmlLogContains(build, "<b class=ant-target>clean</b>");
        assertHtmlLogContains(build, "<b class=ant-target>compile</b>");
        assertHtmlLogContains(build, "<b class=ant-target>jar</b>");
        assertHtmlLogContains(build, "<b class=ant-target>run</b>");
        assertHtmlLogContains(build, "<b class=ant-target>main</b>");
    }

    @Test
    void jenkinsEnvVarsFromBuildScriptTest() throws Exception {
        String testPropertyValue="FooBar";

        FreeStyleProject project = createSimpleAntProject("", null, "build-properties.xml", "testProperty="+testPropertyValue);
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
       
        assertEquals(Result.SUCCESS, build.getResult());
        r.assertLogContains("[echo] My test property: FooBar", build);
    }

    @Test
    void optionsInTargetFieldTest() throws Exception {
        FreeStyleProject project = createSimpleAntProject("-projecthelp", null, null, null);
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
       
        assertEquals(Result.SUCCESS, build.getResult());
        r.assertLogContains("Default target: main", build);
    }

    @Test
    void customBuildFileTest() throws Exception {
        FreeStyleProject project = createSimpleAntProject("", null, "build-custom-name.xml", null);
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
       
        assertEquals(Result.SUCCESS, build.getResult());

        assertHtmlLogContains(build, "<b class=ant-target>clean-custom</b>");
        assertHtmlLogContains(build, "<b class=ant-target>compile-custom</b>");
        assertHtmlLogContains(build, "<b class=ant-target>jar-custom</b>");
        assertHtmlLogContains(build, "<b class=ant-target>run-custom</b>");
        assertHtmlLogContains(build, "<b class=ant-target>main-custom</b>");
    }

    @Test
    void unexistingCustomBuildFileTest() throws Exception {
        FreeStyleProject project = createSimpleAntProject("", null, "unexsisting.xml", null);
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
       
        assertEquals(Result.FAILURE, build.getResult());
        r.assertLogContains("Unable to find build script", build);
    }

    @Test
    @Issue("JENKINS-33712")
    void emptyParameterTest() throws Exception {
        FreeStyleProject project = createSimpleAntProject("", null, null, "property=");
        
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        
        assertEquals(Result.SUCCESS, build.getResult());
    }

    @Test
    void propertyReplacedByVariable() throws Exception {
        testVariableReplaced("x");
    }

    @Test
    @Issue("JENKINS-41801")
    void propertyReplacedByEmptyBuildParameter() throws Exception {
        testVariableReplaced("");
    }

    private void testVariableReplaced(String variableValue) throws Exception {
        FreeStyleProject project = createSimpleAntProject("", null, "build-properties.xml", "testProperty=$variable");

        //SECURITY-170
        ParameterDefinition paramDef = new StringParameterDefinition("variable", "");
        ParametersDefinitionProperty paramsDef = new ParametersDefinitionProperty(paramDef);
        project.addProperty(paramsDef);
        
        Set<String> safeParams = new HashSet<>();
        safeParams.add("variable");
        ParametersAction parameters = new ParametersAction(Arrays.asList(new ParameterValue[] { new StringParameterValue("variable", variableValue) }), safeParams);
        
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserIdCause(), parameters).get();

        assertEquals(Result.SUCCESS, build.getResult());
        List<String> logs = build.getLog(Integer.MAX_VALUE);

        // Find ant command line in logs
        String commandLineLine = null;
        for (String line : logs) {
            if (line.contains("ant") && line.contains("build-properties.xml")
                    && line.contains("-DtestProperty=")) {
                commandLineLine = line;
                break;
            }
        }

        assertNotNull(commandLineLine, "Unable to find ant command line");

        // space so we can look for empty value
        commandLineLine += " ";

        // Check value is expected (with and without quotes)
        assertTrue(commandLineLine.contains("-DtestProperty=\"" + variableValue + "\" ") || commandLineLine.contains("-DtestProperty=" + variableValue + " "),
                "-DtestProperty param not '" + variableValue + "': " + commandLineLine);
    }

    /**
     * Creates a FreeStyleProject with an Ant build step with parameters passed as parameter.
     * The Project to use is always the same (sample-helloworld-ant.zip)
     * 
     * @param targets targets to invoke
     * @param (optional) ops ant ops to use
     * @param (optional) buildFile build file to use. 
     * @param (optional) properties properties to pass to the build
     * 
     * @return a FreeStyleProject with an Ant build step
     * @throws Exception
     */
    private FreeStyleProject createSimpleAntProject(String targets, String ops, String buildFile, String properties) throws Exception {
        if (targets == null) {
            fail("Ant targets must be different from null");
        }
        String antName = configureDefaultAnt().getName();
        FreeStyleProject project = r.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("sample-helloworld-ant.zip")));
        project.getBuildersList().add(new Ant(targets, antName, ops, buildFile, properties));
        //Make sure that state is the expected when running sequentially
        AntTargetNote.ENABLED = true;
        return project;
    }

    // TODO consider inclusion in JenkinsRule; can use WebClient to obtain …/console but is slow and contains other junk
    private static String getHtmlLog(Run<?, ?> build) throws IOException {
        StringWriter w = new StringWriter();
        build.getLogText().writeHtmlTo(0, w);
        return w.toString();
    }

    static void assertHtmlLogContains(Run<?, ?> build, String text) throws IOException {
        assertThat(getHtmlLog(build), containsString(text));
    }

    static void assertHtmlLogNotContains(Run<?, ?> build, String text) throws IOException {
        assertThat(getHtmlLog(build), not(containsString(text)));
    }

}

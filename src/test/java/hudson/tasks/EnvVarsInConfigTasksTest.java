package hudson.tasks;

import static hudson.tasks._ant.Messages.Ant_ExecutableNotFound;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.tasks.Ant.AntInstallation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;
import org.jvnet.hudson.test.junit.jupiter.BuildWatcherExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;

@WithJenkins
class EnvVarsInConfigTasksTest {
    private static final String DUMMY_LOCATION_VARNAME = "TOOLS_DUMMY_LOCATION";

    private DumbSlave agentEnv = null;
    private DumbSlave agentRegular = null;

    @SuppressWarnings("unused")
    private static final BuildWatcherExtension BUILD_WATCHER = new BuildWatcherExtension();

    private JenkinsRule j;

    @TempDir
    private File tmp;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        JDK defaultJDK = j.jenkins.getJDK(null);
        JDK varJDK = new JDK("varJDK", withVariable(defaultJDK.getHome()));
        j.jenkins.getJDKs().add(varJDK);

        // Ant with a variable in its path
        TemporaryFolder temporaryFolder = new TemporaryFolder(tmp);
        temporaryFolder.create();
        AntInstallation ant = ToolInstallations.configureDefaultAnt(temporaryFolder);
        AntInstallation antInstallation =
                new AntInstallation(
                        "varAnt", withVariable(ant.getHome()), JenkinsRule.NO_PROPERTIES);
        j.jenkins.getDescriptorByType(Ant.DescriptorImpl.class).setInstallations(antInstallation);

        // create agents
        EnvVars additionalEnv = new EnvVars(DUMMY_LOCATION_VARNAME, "");
        agentEnv = j.createSlave(new LabelAtom("agentEnv"), additionalEnv);
        agentRegular = j.createSlave(new LabelAtom("agentRegular"));
    }

    private static String withVariable(String s) {
        return s + "${" + DUMMY_LOCATION_VARNAME + "}";
    }

    @Test
    void testFreeStyleAntOnAgent() throws Exception {
        assumeFalse(
                j.jenkins.getDescriptorByType(Ant.DescriptorImpl.class).getInstallations().length
                        == 0,
                "Cannot do testFreeStyleAntOnAgent without ANT_HOME");

        FreeStyleProject project = j.createFreeStyleProject();
        project.setJDK(j.jenkins.getJDK("varJDK"));
        project.setScm(new ExtractResourceSCM(getClass().getResource("/simple-projects.zip"))); 
        String buildFile = "build.xml${" + DUMMY_LOCATION_VARNAME + "}";
        // we need additional escapes because bash itself expanding
        project.getBuildersList()
                .add(
                        new Ant(
                                "-Dtest.property=cor${" + DUMMY_LOCATION_VARNAME + "}rect",
                                "varAnt",
                                "",
                                buildFile,
                                ""));

        // test the regular agent - variable not expanded
        project.setAssignedLabel(agentRegular.getSelfLabel());
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        j.assertBuildStatus(Result.FAILURE, build);

        j.assertLogContains(Ant_ExecutableNotFound("varAnt"), build);

        // test the agent with prepared environment
        project.setAssignedLabel(agentEnv.getSelfLabel());
        build = project.scheduleBuild2(0).get();

        j.assertBuildStatusSuccess(build);

        // Check variable was expanded
        j.assertLogContains("Ant home: ", build);
        j.assertLogContains("Test property: correct", build);
        assertFalse(
                JenkinsRule.getLog(build)
                        .matches("(?s)^.*Ant home: [^\\n\\r]*" + DUMMY_LOCATION_VARNAME + ".*$"));
        assertFalse(
                JenkinsRule.getLog(build)
                        .matches(
                                "(?s)^.*Test property: [^\\n\\r]*"
                                        + DUMMY_LOCATION_VARNAME
                                        + ".*$"));
    }
}

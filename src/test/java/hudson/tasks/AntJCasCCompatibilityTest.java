package hudson.tasks;

import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AntJCasCCompatibilityTest extends RoundTripAbstractTest {

    @Override
    protected void assertConfiguredAsExpected(RestartableJenkinsRule restartableJenkinsRule, String s) {
        final Jenkins jenkins = restartableJenkinsRule.j.jenkins;

        final Ant.DescriptorImpl descriptor = (Ant.DescriptorImpl) jenkins.getDescriptor(Ant.class);
        assertTrue("The descriptor should not be null", descriptor != null);

        final Ant.AntInstallation[] installations = descriptor.getInstallations();
        assertEquals("The installation has not retrieved", 2, installations.length);

        Ant.AntInstallation installation = installations[0];
        assertEquals(String.format("The name should be %s", "Ant 1.10.5"), "Ant 1.10.5", installation.getName());
        String installerId = installation.getProperties().get(InstallSourceProperty.class).installers.get(Ant.AntInstaller.class).id;
        assertEquals(String.format("The id for the installer should be %s", "1.10.5"), "1.10.5", installerId);

        installation = installations[1];
        assertEquals(String.format("The name should be %s", "Ant 1.10.6"), "Ant 1.10.6", installation.getName());
        assertEquals(String.format("The home path should be %s", "/home/ant"), "/home/ant", installation.getHome());
    }

    @Override
    protected String stringInLogExpected() {
        return "Setting class hudson.tools.InstallSourceProperty. installers = [{antFromApache={}}]";
    }
}

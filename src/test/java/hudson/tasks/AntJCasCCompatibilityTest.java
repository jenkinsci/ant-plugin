package hudson.tasks;

import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.junit.jupiter.AbstractRoundTripTest;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
class AntJCasCCompatibilityTest extends AbstractRoundTripTest {

    @Override
    protected void assertConfiguredAsExpected(JenkinsRule j, String s) {
        final Jenkins jenkins = j.jenkins;

        final Ant.DescriptorImpl descriptor = (Ant.DescriptorImpl) jenkins.getDescriptor(Ant.class);
        assertNotNull(descriptor, "The descriptor should not be null");

        final Ant.AntInstallation[] installations = descriptor.getInstallations();
        assertEquals(2, installations.length, "The installation has not retrieved");

        Ant.AntInstallation installation = installations[0];
        assertEquals("Ant 1.10.5", installation.getName(), String.format("The name should be %s", "Ant 1.10.5"));
        String installerId = installation.getProperties().get(InstallSourceProperty.class).installers.get(Ant.AntInstaller.class).id;
        assertEquals("1.10.5", installerId, String.format("The id for the installer should be %s", "1.10.5"));

        installation = installations[1];
        assertEquals("Ant 1.10.6", installation.getName(), String.format("The name should be %s", "Ant 1.10.6"));
        assertEquals("/home/ant", installation.getHome(), String.format("The home path should be %s", "/home/ant"));
    }

    @Override
    protected String stringInLogExpected() {
        return "Setting class hudson.tools.InstallSourceProperty.installers = [{antFromApache={}}]";
    }
}

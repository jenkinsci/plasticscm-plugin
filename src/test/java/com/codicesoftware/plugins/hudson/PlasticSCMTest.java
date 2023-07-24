package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.WorkingMode;
import com.codicesoftware.plugins.jenkins.SelectorTemplates;
import hudson.model.FreeStyleProject;
import hudson.scm.SCM;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlasticSCMTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Test
    public void testProjectConfig() throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        PlasticSCM scm = new PlasticSCM(
            SelectorTemplates.DEFAULT,
            CleanupMethod.MINIMAL,
            WorkingMode.NONE,
            null,
            false,
            null,
            false,
            "");

        project.setScm(scm);
        SCM testScm = project.getScm();
        assertEquals("com.codicesoftware.plugins.hudson.PlasticSCM", testScm.getType());
        assertEquals(testScm, project.getScm());

        assertTrue(testScm.supportsPolling());
        assertTrue(testScm.requiresWorkspaceForPolling());
    }

    @Test
    public void testProjectConfigWithControllerPolling() throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        PlasticSCM scm = new PlasticSCM(
            SelectorTemplates.DEFAULT,
            CleanupMethod.MINIMAL,
            WorkingMode.NONE,
            null,
            false,
            null,
            true,
            "");

        project.setScm(scm);
        SCM testScm = project.getScm();
        assertEquals("com.codicesoftware.plugins.hudson.PlasticSCM", testScm.getType());
        assertEquals(testScm, project.getScm());

        assertTrue(testScm.supportsPolling());
        assertFalse(testScm.requiresWorkspaceForPolling());
    }

    @Test
    public void testServerConfiguration() throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        String newLine = System.getProperty("line.separator");
        String selector = String.join(
                newLine,
                "repository \"myRepo@my.plasticscm.server.com:8087\"",
                "  path \"/\"",
                "    smartbranch \"/main/develop\""
        );
        PlasticSCM scm = new PlasticSCM(
                selector,
                CleanupMethod.MINIMAL,
                WorkingMode.NONE,
                null,
                false,
                null,
                true,
                "");
        assertEquals("--server=my.plasticscm.server.com:8087", scm.buildClientConfigurationArguments(project, selector).getServerParam());
    }
}

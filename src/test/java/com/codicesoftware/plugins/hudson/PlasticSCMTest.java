package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import com.codicesoftware.plugins.hudson.model.WorkingMode;
import hudson.model.FreeStyleProject;
import hudson.scm.SCM;
import hudson.util.FormValidation;
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
            PlasticSCM.DEFAULT_SELECTOR,
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
            PlasticSCM.DEFAULT_SELECTOR,
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
}

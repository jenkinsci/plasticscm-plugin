package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.model.CleanupMethod;
import hudson.model.FreeStyleProject;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlasticSCMTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    //@Test
    public void testCheckExecutable() {
        PlasticSCM.DescriptorImpl descriptor = new PlasticSCM.DescriptorImpl();
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckExecutable("cm").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckExecutable("").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckExecutable("/usr/bin/cm").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckExecutable("/invalid-path/cm").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckExecutable("cm-invalid").kind);
    }

    @Test
    public void testProjectConfig() throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        PlasticSCM scm = new PlasticSCM(PlasticSCM.DEFAULT_SELECTOR, CleanupMethod.MINIMAL, false, null, "");

        project.setScm(scm);
        SCM testScm = project.getScm();
        assertEquals("com.codicesoftware.plugins.hudson.PlasticSCM", testScm.getType());
        assertEquals(testScm, project.getScm());

        assertTrue(testScm.supportsPolling());
        assertTrue(testScm.requiresWorkspaceForPolling());
    }

}

package com.codicesoftware.plugins.hudson;

import hudson.util.FormValidation;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

public class PlasticSCMTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    @Ignore
    public void testCheckExecutable() {
        PlasticSCM.DescriptorImpl descriptor = new PlasticSCM.DescriptorImpl();
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckExecutable("/usr/bin/cm").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckExecutable("cm").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckExecutable("").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckExecutable("/invalid/path/cm").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckExecutable("missing").kind);
    }

}
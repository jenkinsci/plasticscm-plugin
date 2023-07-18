package com.codicesoftware.plugins.jenkins.mergebot;

import hudson.AbortException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class UpdateToSpecTest {

    @Test
    public void nullOrEmptySpec() throws AbortException {
        UpdateToSpec underTest = UpdateToSpec.parse(null);
        assertNull(underTest);

        underTest = UpdateToSpec.parse("");
        assertNull(underTest);
    }

    @Test
    public void branchWithoutPrefix() throws AbortException {
        UpdateToSpec underTest = UpdateToSpec.parse("main/scm001@myrepo@myserver");

        assertNotNull(underTest);
        assertEquals(ObjectSpecType.Branch, underTest.getObjectType());
        assertEquals("main/scm001", underTest.getObjectName());
        assertEquals("myrepo", underTest.getRepName());
        assertEquals("myserver", underTest.getRepServer());
        assertEquals("br:main/scm001@myrepo@myserver", underTest.getFullObjectSpec());
    }

    @Test
    public void branchWithPrefix() throws AbortException {
        UpdateToSpec underTest = UpdateToSpec.parse("br:main/scm001@rep:myrepo@repserver:myserver");

        assertNotNull(underTest);
        assertEquals(ObjectSpecType.Branch, underTest.getObjectType());
        assertEquals("main/scm001", underTest.getObjectName());
        assertEquals("myrepo", underTest.getRepName());
        assertEquals("myserver", underTest.getRepServer());
        assertEquals("br:main/scm001@myrepo@myserver", underTest.getFullObjectSpec());
    }

    @Test
    public void changeset() throws AbortException {
        UpdateToSpec underTest = UpdateToSpec.parse("cs:123@myrepo@repserver:myserver");

        assertNotNull(underTest);
        assertEquals(ObjectSpecType.Changeset, underTest.getObjectType());
        assertEquals("123", underTest.getObjectName());
        assertEquals("myrepo", underTest.getRepName());
        assertEquals("myserver", underTest.getRepServer());
        assertEquals("cs:123@myrepo@myserver", underTest.getFullObjectSpec());
    }

    @Test
    public void shelve() throws AbortException {
        UpdateToSpec underTest = UpdateToSpec.parse("sh:123@rep:myrepo@myserver");

        assertNotNull(underTest);
        assertEquals(ObjectSpecType.Shelve, underTest.getObjectType());
        assertEquals("123", underTest.getObjectName());
        assertEquals("myrepo", underTest.getRepName());
        assertEquals("myserver", underTest.getRepServer());
        assertEquals("sh:123@myrepo@myserver", underTest.getFullObjectSpec());
    }

    @Test
    public void label() throws AbortException {
        UpdateToSpec underTest = UpdateToSpec.parse("lb:mylabel@myrepo@myserver");

        assertNotNull(underTest);
        assertEquals(ObjectSpecType.Label, underTest.getObjectType());
        assertEquals("mylabel", underTest.getObjectName());
        assertEquals("myrepo", underTest.getRepName());
        assertEquals("myserver", underTest.getRepServer());
        assertEquals("lb:mylabel@myrepo@myserver", underTest.getFullObjectSpec());
    }
}

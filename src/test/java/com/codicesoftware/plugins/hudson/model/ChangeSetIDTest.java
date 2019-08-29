package com.codicesoftware.plugins.hudson.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChangeSetIDTest {

    @Test
    public void testConstructor() {
        ChangeSetID underTest = new ChangeSetID(111, "testing", "domain.com:8084");
        assertEquals(111, underTest.getId());
        assertEquals("testing", underTest.getRepository());
        assertEquals("domain.com:8084", underTest.getServer());
    }

    @Test
    public void testConstructorSsl() {
        ChangeSetID underTest = new ChangeSetID(111, "testing", "ssl://domain.com:8088");
        assertEquals(111, underTest.getId());
        assertEquals("testing", underTest.getRepository());
        assertEquals("ssl://domain.com:8088", underTest.getServer());
    }

    @Test
    public void testConstructorCloud() {
        ChangeSetID underTest = new ChangeSetID(111, "testing", "myOrganization@cloud");
        assertEquals(111, underTest.getId());
        assertEquals("testing", underTest.getRepository());
        assertEquals("myOrganization@cloud", underTest.getServer());
    }
}

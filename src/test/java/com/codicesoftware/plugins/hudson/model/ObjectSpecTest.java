package com.codicesoftware.plugins.hudson.model;

import com.codicesoftware.plugins.jenkins.ObjectSpecType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ObjectSpecTest {

    @Test
    public void testConstructor() {
        ObjectSpec underTest = new ObjectSpec(ObjectSpecType.Changeset, 111, "testing", "domain.com:8084");
        assertEquals(ObjectSpecType.Changeset, underTest.getType());
        assertEquals(111, underTest.getId());
        assertEquals("testing", underTest.getRepository());
        assertEquals("domain.com:8084", underTest.getServer());
        assertEquals("cs:111@testing@domain.com:8084", underTest.getFullSpec());
    }

    @Test
    public void testConstructorSsl() {
        ObjectSpec underTest = new ObjectSpec(ObjectSpecType.Shelve, 111, "testing", "ssl://domain.com:8088");
        assertEquals(ObjectSpecType.Shelve, underTest.getType());
        assertEquals(111, underTest.getId());
        assertEquals("testing", underTest.getRepository());
        assertEquals("ssl://domain.com:8088", underTest.getServer());
        assertEquals("sh:111@testing@ssl://domain.com:8088", underTest.getFullSpec());
    }

    @Test
    public void testConstructorCloud() {
        ObjectSpec underTest = new ObjectSpec(ObjectSpecType.Changeset, 111, "testing", "myOrganization@cloud");
        assertEquals(ObjectSpecType.Changeset, underTest.getType());
        assertEquals(111, underTest.getId());
        assertEquals("testing", underTest.getRepository());
        assertEquals("myOrganization@cloud", underTest.getServer());
        assertEquals("cs:111@testing@myOrganization@cloud", underTest.getFullSpec());
    }
}

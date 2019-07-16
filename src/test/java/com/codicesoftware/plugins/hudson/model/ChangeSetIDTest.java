package com.codicesoftware.plugins.hudson.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChangeSetIDTest {

    @Test
    public void testConstructor() {
        ChangeSetID underTest = new ChangeSetID(111, "testing", "domain.com", 1234);
        assertEquals(111, underTest.getId());
        assertEquals("testing", underTest.getRepository());
        assertEquals("domain.com", underTest.getHost());
        assertEquals(1234, underTest.getPort());
        assertEquals("plastic", underTest.getProtocol());
        assertEquals(false, underTest.isSslProtocol());
    }

    @Test
    public void testConstructorSsl() {
        ChangeSetID underTest = new ChangeSetID(111, "testing", "ssl://domain.com", 1234);
        assertEquals(111, underTest.getId());
        assertEquals("testing", underTest.getRepository());
        assertEquals("domain.com", underTest.getHost());
        assertEquals(1234, underTest.getPort());
        assertEquals("ssl", underTest.getProtocol());
        assertEquals(true, underTest.isSslProtocol());
    }

}

package com.codicesoftware.plugins.hudson.model;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ChangeSetListTest {

    ChangeSetList underTest = new ChangeSetList(null, null, new ArrayList<>());

    @Test
    public void testGetKind() {
        assertEquals("plasticscm", underTest.getKind());
    }

    @Test
    public void testIsEmptySet() {
        assertEquals(true, underTest.isEmptySet());
    }
}

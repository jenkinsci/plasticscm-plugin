package com.codicesoftware.plugins.hudson.model;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChangeSetListTest {

    private final ChangeSetList underTest = new ChangeSetList(null, null, new ArrayList<>());

    @Test
    public void testGetKind() {
        assertEquals("plasticscm", underTest.getKind());
    }

    @Test
    public void testIsEmptySet() {
        assertTrue(underTest.isEmptySet());
    }
}

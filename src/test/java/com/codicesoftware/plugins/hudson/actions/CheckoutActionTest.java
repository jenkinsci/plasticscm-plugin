package com.codicesoftware.plugins.hudson.actions;

import com.codicesoftware.plugins.hudson.model.Workspace;
import hudson.FilePath;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CheckoutActionTest {

    @Test
    public void testSameWorkspacePath() {
        assertFalse(CheckoutAction.isSameWorkspacePath("X:\\workspace\\test", ""));
        assertFalse(CheckoutAction.isSameWorkspacePath("/workspace/test", ""));

        assertTrue(CheckoutAction.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test"));
        assertTrue(CheckoutAction.isSameWorkspacePath("/workspace/test", "/workspace/test"));
        assertTrue(CheckoutAction.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace\\TEST"));
        assertFalse(CheckoutAction.isSameWorkspacePath("/workspace/test", "/workspace/TEST"));
        assertTrue(CheckoutAction.isSameWorkspacePath("X:\\workspace\\test", "X:\\workSPACE\\test"));
        assertFalse(CheckoutAction.isSameWorkspacePath("/workspace/test", "/workSPACE/test"));

        assertFalse(CheckoutAction.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace\\other"));
        assertFalse(CheckoutAction.isSameWorkspacePath("/workspace/test", "/workspace/other"));

        assertFalse(CheckoutAction.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test\\inner"));
        assertFalse(CheckoutAction.isSameWorkspacePath("/workspace/test", "/workspace/test/inner"));

        assertFalse(CheckoutAction.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace"));
        assertFalse(CheckoutAction.isSameWorkspacePath("/workspace/test", "/workspace"));
    }

    @Test
    public void testNestedWorkspacePath() {
        assertFalse(CheckoutAction.isNestedWorkspacePath("X:\\workspace\\test", ""));
        assertFalse(CheckoutAction.isNestedWorkspacePath("/workspace/test", ""));

        assertFalse(CheckoutAction.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test"));
        assertFalse(CheckoutAction.isNestedWorkspacePath("/workspace/test", "/workspace/test"));

        assertFalse(CheckoutAction.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\other"));
        assertFalse(CheckoutAction.isNestedWorkspacePath("/workspace/test", "/workspace/other"));

        assertTrue(CheckoutAction.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test\\inner"));
        assertTrue(CheckoutAction.isNestedWorkspacePath("/workspace/test", "/workspace/test/inner"));
        assertTrue(CheckoutAction.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\TEST\\inner"));
        assertFalse(CheckoutAction.isNestedWorkspacePath("/workspace/test", "/workspace/TEST/inner"));

        assertTrue(CheckoutAction.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test\\inner\\deep"));
        assertTrue(CheckoutAction.isNestedWorkspacePath("/workspace/test", "/workspace/test/inner/deep"));
        assertTrue(CheckoutAction.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\TEST\\inner\\deep"));
        assertFalse(CheckoutAction.isNestedWorkspacePath("/workspace/test", "/workspace/TEST/inner/deep"));
        assertTrue(CheckoutAction.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test\\INNER\\deep"));
        assertTrue(CheckoutAction.isNestedWorkspacePath("/workspace/test", "/workspace/test/INNER/deep"));

        assertFalse(CheckoutAction.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace"));
        assertFalse(CheckoutAction.isNestedWorkspacePath("/workspace/test", "/workspace"));
    }

    @Test
    public void testFindWorkspacesInside() {
        List<Workspace> workspaces = new ArrayList<>();
        Workspace a1 = new Workspace("nameA1", "jobA", "guidA1");
        workspaces.add(a1);
        Workspace b1 = new Workspace("nameB1", "jobB/wk1", "guidB1");
        workspaces.add(b1);
        Workspace b2 = new Workspace("nameB2", "jobB/wk2", "guidB2");
        workspaces.add(b2);
        Workspace c1 = new Workspace("nameC1", "jobC/inner/wk1", "guidC1");
        workspaces.add(c1);
        Workspace c2 = new Workspace("nameC2", "jobC/inner/deep/wk2", "guidC2");
        workspaces.add(c2);

        FilePath testPath;
        List<Workspace> foundWorkspaces;

        testPath = new FilePath(new File("jobA"));
        foundWorkspaces = CheckoutAction.findWorkspacesInsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobB"));
        foundWorkspaces = CheckoutAction.findWorkspacesInsidePath(testPath, workspaces);
        assertEquals(2, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(b1, b2));

        testPath = new FilePath(new File("jobC"));
        foundWorkspaces = CheckoutAction.findWorkspacesInsidePath(testPath, workspaces);
        assertEquals(2, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(c1, c2));

        testPath = new FilePath(new File("jobX"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());
    }

    @Test
    public void testFindWorkspacesOutside() {
        List<Workspace> workspaces = new ArrayList<>();
        Workspace a1 = new Workspace("nameA1", "jobA", "guidA1");
        workspaces.add(a1);
        Workspace b1 = new Workspace("nameB1", "jobB/wk1", "guidB1");
        workspaces.add(b1);
        Workspace b2 = new Workspace("nameB2", "jobB/wk2", "guidB2");
        workspaces.add(b2);
        Workspace c1 = new Workspace("nameC1", "jobC/inner/wk1", "guidC1");
        workspaces.add(c1);
        Workspace c2 = new Workspace("nameC2", "jobC/inner/deep/wk2", "guidC2");
        workspaces.add(c2);

        FilePath testPath;
        List<Workspace> foundWorkspaces;

        testPath = new FilePath(new File("jobA"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobA/inner"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(1, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(a1));

        testPath = new FilePath(new File("jobA/inner/deep"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(1, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(a1));

        testPath = new FilePath(new File("jobB"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobB/inner"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobC"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobC/inner"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobC/inner/deep"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobX"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());
    }
}

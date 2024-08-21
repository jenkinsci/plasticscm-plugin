package com.codicesoftware.plugins.hudson;

import com.codicesoftware.plugins.hudson.model.Workspace;
import hudson.FilePath;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorkspaceManagerTest {

    @Test
    public void testSameWorkspacePath() {
        assertFalse(WorkspaceManager.isSameWorkspacePath("X:\\workspace\\test", ""));
        assertFalse(WorkspaceManager.isSameWorkspacePath("/workspace/test", ""));

        assertTrue(WorkspaceManager.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test"));
        assertTrue(WorkspaceManager.isSameWorkspacePath("/workspace/test", "/workspace/test"));
        assertTrue(WorkspaceManager.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace\\TEST"));
        assertFalse(WorkspaceManager.isSameWorkspacePath("/workspace/test", "/workspace/TEST"));
        assertTrue(WorkspaceManager.isSameWorkspacePath("X:\\workspace\\test", "X:\\workSPACE\\test"));
        assertFalse(WorkspaceManager.isSameWorkspacePath("/workspace/test", "/workSPACE/test"));

        assertFalse(WorkspaceManager.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace\\other"));
        assertFalse(WorkspaceManager.isSameWorkspacePath("/workspace/test", "/workspace/other"));

        assertFalse(WorkspaceManager.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test\\inner"));
        assertFalse(WorkspaceManager.isSameWorkspacePath("/workspace/test", "/workspace/test/inner"));

        assertFalse(WorkspaceManager.isSameWorkspacePath("X:\\workspace\\test", "X:\\workspace"));
        assertFalse(WorkspaceManager.isSameWorkspacePath("/workspace/test", "/workspace"));
    }

    @Test
    public void testNestedWorkspacePath() {
        assertFalse(WorkspaceManager.isNestedWorkspacePath("X:\\workspace\\test", ""));
        assertFalse(WorkspaceManager.isNestedWorkspacePath("/workspace/test", ""));

        assertFalse(WorkspaceManager.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test"));
        assertFalse(WorkspaceManager.isNestedWorkspacePath("/workspace/test", "/workspace/test"));

        assertFalse(WorkspaceManager.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\other"));
        assertFalse(WorkspaceManager.isNestedWorkspacePath("/workspace/test", "/workspace/other"));

        assertTrue(WorkspaceManager.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test\\inner"));
        assertTrue(WorkspaceManager.isNestedWorkspacePath("/workspace/test", "/workspace/test/inner"));
        assertTrue(WorkspaceManager.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\TEST\\inner"));
        assertFalse(WorkspaceManager.isNestedWorkspacePath("/workspace/test", "/workspace/TEST/inner"));

        assertTrue(WorkspaceManager.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test\\inner\\deep"));
        assertTrue(WorkspaceManager.isNestedWorkspacePath("/workspace/test", "/workspace/test/inner/deep"));
        assertTrue(WorkspaceManager.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\TEST\\inner\\deep"));
        assertFalse(WorkspaceManager.isNestedWorkspacePath("/workspace/test", "/workspace/TEST/inner/deep"));
        assertTrue(WorkspaceManager.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace\\test\\INNER\\deep"));
        assertTrue(WorkspaceManager.isNestedWorkspacePath("/workspace/test", "/workspace/test/INNER/deep"));

        assertFalse(WorkspaceManager.isNestedWorkspacePath("X:\\workspace\\test", "X:\\workspace"));
        assertFalse(WorkspaceManager.isNestedWorkspacePath("/workspace/test", "/workspace"));
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
        foundWorkspaces = WorkspaceManager.findWorkspacesInsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobB"));
        foundWorkspaces = WorkspaceManager.findWorkspacesInsidePath(testPath, workspaces);
        assertEquals(2, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(b1, b2));

        testPath = new FilePath(new File("jobC"));
        foundWorkspaces = WorkspaceManager.findWorkspacesInsidePath(testPath, workspaces);
        assertEquals(2, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(c1, c2));

        testPath = new FilePath(new File("jobX"));
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
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
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobA/inner"));
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(1, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(a1));

        testPath = new FilePath(new File("jobA/inner/deep"));
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(1, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(a1));

        testPath = new FilePath(new File("jobB"));
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobB/inner"));
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobC"));
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobC/inner"));
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobC/inner/deep"));
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("jobX"));
        foundWorkspaces = WorkspaceManager.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());
    }
}

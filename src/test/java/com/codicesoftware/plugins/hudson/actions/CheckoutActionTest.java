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
    public void testFindWorkspacesInside() {
        List<Workspace> workspaces = new ArrayList<>();
        Workspace a1 = new Workspace("nameA1", "workspaces/jobA", "guidA1");
        workspaces.add(a1);
        Workspace b1 = new Workspace("nameB1", "workspaces/jobB/wk1", "guidB1");
        workspaces.add(b1);
        Workspace b2 = new Workspace("nameB2", "workspaces/jobB/wk2", "guidB2");
        workspaces.add(b2);
        Workspace c1 = new Workspace("nameC1", "workspaces/jobC/inner/wk1", "guidC1");
        workspaces.add(c1);
        Workspace c2 = new Workspace("nameC2", "workspaces/jobC/inner/deep/wk2", "guidC2");
        workspaces.add(c2);

        FilePath testPath;
        List<Workspace> foundWorkspaces;

        testPath = new FilePath(new File("workspaces/jobA"));
        foundWorkspaces = CheckoutAction.findWorkspacesInsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("workspaces/jobB"));
        foundWorkspaces = CheckoutAction.findWorkspacesInsidePath(testPath, workspaces);
        assertEquals(2, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(b1, b2));

        testPath = new FilePath(new File("workspaces/jobC"));
        foundWorkspaces = CheckoutAction.findWorkspacesInsidePath(testPath, workspaces);
        assertEquals(2, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(c1, c2));

        testPath = new FilePath(new File("workspaces/jobX"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());
    }

    @Test
    public void testFindWorkspacesOutside() {
        List<Workspace> workspaces = new ArrayList<>();
        Workspace a1 = new Workspace("nameA1", "workspaces/jobA", "guidA1");
        workspaces.add(a1);
        Workspace b1 = new Workspace("nameB1", "workspaces/jobB/wk1", "guidB1");
        workspaces.add(b1);
        Workspace b2 = new Workspace("nameB2", "workspaces/jobB/wk2", "guidB2");
        workspaces.add(b2);
        Workspace c1 = new Workspace("nameC1", "workspaces/jobC/inner/wk1", "guidC1");
        workspaces.add(c1);
        Workspace c2 = new Workspace("nameC2", "workspaces/jobC/inner/deep/wk2", "guidC2");
        workspaces.add(c2);

        FilePath testPath;
        List<Workspace> foundWorkspaces;

        testPath = new FilePath(new File("workspaces/jobA"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("workspaces/jobA/inner"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(1, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(a1));

        testPath = new FilePath(new File("workspaces/jobA/inner/deep"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(1, foundWorkspaces.size());
        assertThat(foundWorkspaces, hasItems(a1));

        testPath = new FilePath(new File("workspaces/jobB"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("workspaces/jobB/inner"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("workspaces/jobC"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("workspaces/jobC/inner"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("workspaces/jobC/inner/deep"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());

        testPath = new FilePath(new File("workspaces/jobX"));
        foundWorkspaces = CheckoutAction.findWorkspacesOutsidePath(testPath, workspaces);
        assertEquals(0, foundWorkspaces.size());
    }

    @Test
    public void testWorkspacePathDetection() {
        assertTrue(CheckoutAction.isSameWorkspacePath("workspace", "workspace"));
        assertFalse(CheckoutAction.isSameWorkspacePath("workspace", "other"));

        assertFalse(CheckoutAction.isNestedWorkspacePath("workspace", "workspace"));
        assertFalse(CheckoutAction.isNestedWorkspacePath("workspace", "other"));
        assertFalse(CheckoutAction.isNestedWorkspacePath("workspace", "work"));
        assertFalse(CheckoutAction.isNestedWorkspacePath("workspace", "workspaceship"));
        assertTrue(CheckoutAction.isNestedWorkspacePath("workspace", "workspace/main"));

        assertFalse(CheckoutAction.isNestedWorkspacePath("workspace/check", "workspace/check"));
        assertFalse(CheckoutAction.isNestedWorkspacePath("workspace/check", "workspace/other"));
        assertTrue(CheckoutAction.isNestedWorkspacePath("workspace/check", "workspace/check/main"));
    }
}

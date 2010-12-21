package com.codicesoftware.plugins.hudson.model;

import hudson.model.InvisibleAction;
import java.io.Serializable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * An action for storing Plastic SCM configuration data in a build
 *
 * @author Erik Ramfelt, redsolo
 * @author Dick Porter
 */
public class WorkspaceConfiguration extends InvisibleAction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String workspaceName;
    private final String selector;
    private boolean workspaceExists;

    public WorkspaceConfiguration(String workspaceName, String selector) {
        this.workspaceName = workspaceName;
        this.selector = selector;
        this.workspaceExists = true;
    }

    public WorkspaceConfiguration(WorkspaceConfiguration configuration) {
        this.workspaceName = configuration.workspaceName;
        this.selector = configuration.selector;
        this.workspaceExists = configuration.workspaceExists;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getSelector() {
        return selector;
    }

    public boolean workspaceExists() {
        return workspaceExists;
    }

    public void setWorkspaceWasRemoved() {
        this.workspaceExists = false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 27).append(workspaceName).append(selector).append(workspaceExists).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if ((obj == null) || (getClass() != obj.getClass()))
            return false;
        final WorkspaceConfiguration other = (WorkspaceConfiguration)obj;
        EqualsBuilder builder = new EqualsBuilder();
        builder.append(this.workspaceName, other.workspaceName);
        builder.append(this.selector, other.selector);
        builder.append(this.workspaceExists, other.workspaceExists);
        return builder.isEquals();
    }
}
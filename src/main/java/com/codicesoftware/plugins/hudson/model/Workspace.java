package com.codicesoftware.plugins.hudson.model;

import com.codicesoftware.plugins.hudson.commands.GetSelectorCommand;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Workspace {
    private final Server server;
    private final String name;
    private final String path;
    private String selector;

    public Workspace (Server server, String name, String path, String selector) {
        this.server = server;
        this.name = name;
        this.path = path;
        this.selector = selector;
    }

    public boolean exists() throws IOException, InterruptedException {
        return server.getWorkspaces().exists(this);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getSelector() {
        if (selector == null) {
            // Get the selector from the server
            GetSelectorCommand command = new GetSelectorCommand(server, name);
            Reader reader = null;
            try {
                reader = server.execute(command.getArguments());
                selector = command.parse(reader);
            } catch (IOException e) {
            } catch (InterruptedException e) {
            } catch (ParseException e) {
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 27).append(name).append(path).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if ((obj == null) || (getClass() != obj.getClass()))
            return false;
        final Workspace other = (Workspace)obj;
        EqualsBuilder builder = new EqualsBuilder();
        builder.append(this.name, other.name);
        builder.append(this.path, other.path);
        return builder.isEquals();
    }
}
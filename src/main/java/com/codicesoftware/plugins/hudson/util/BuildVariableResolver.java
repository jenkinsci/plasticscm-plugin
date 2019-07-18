package com.codicesoftware.plugins.hudson.util;

import hudson.FilePath;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Job;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A {@link VariableResolver} that resolves certain Build variables.
 * <p>
 * The build variable resolver will resolve the following:
 * <ul>
 * <li> JOB_NAME - The name of the job</li>
 * <li> USER_NAME - The system property "user.name" on the Node that the Launcher
 * is being executed on (slave or master)</li>
 * <li> NODE_NAME - The name of the node the current build is running on</li>
 * <li> EXECUTOR_NUMBER - The unique number that identifies the current executor</li>
 * <li> Any environment variable that is set on the Node that the Launcher is
 * being executed on (slave or master)</li>
 * </ul>
 */
public class BuildVariableResolver implements VariableResolver<String> {

    private static final Logger LOGGER = Logger.getLogger(BuildVariableResolver.class.getName());
    private final Computer computer;
    private Map<String, LazyResolver> lazyResolvers = new HashMap<>();
    private List<VariableResolver<String>> otherResolvers = new ArrayList<>();

    public BuildVariableResolver(final Job<?, ?> job) {
        computer = null;
        lazyResolvers.put("JOB_NAME", new LazyResolver() {
            public String getValue() {
                return job.getName();
            }
        });
    }

    public BuildVariableResolver(final Job<?, ?> project, final Computer computer, final FilePath workspace) {
        this.computer = computer;
        lazyResolvers.put("JOB_NAME", new LazyResolver() {
            public String getValue() {
                return project.getName();
            }
        });
        lazyResolvers.put("NODE_NAME", new LazyComputerResolver() {
            public String getValue(Computer computer) {
                if (computer == null || Util.fixEmpty(computer.getName()) == null) {
                    return "master";
                }
                return computer.getName();
            }
        });
        lazyResolvers.put("USER_NAME", new LazyComputerResolver() {
            public String getValue(Computer computer) throws IOException, InterruptedException {
                if (computer == null) {
                    return "DEFAULT";
                }
                return (String) computer.getSystemProperties().get("user.name");
            }
        });
        lazyResolvers.put("EXECUTOR_NUMBER", new LazyComputerResolver() {
            public String getValue(Computer computer) throws IOException, InterruptedException {
                return ExecutorVariableHelper.getExecutorID(workspace);
            }
        });
    }

    public String resolve(String variable) {
        try {
            if (lazyResolvers.containsKey(variable)) {
                return lazyResolvers.get(variable).getValue();
            } else {
                if (computer != null) {
                    otherResolvers.add(new VariableResolver.ByMap<>(computer.getEnvironment()));
                }
                return new VariableResolver.Union<>(otherResolvers).resolve(variable);
            }
        } catch (Exception e) {
            LOGGER.warning("Variable name '" + variable + "' look up failed because of " + e);
        }
        return null;
    }

    /**
     * Simple lazy variable resolver
     */
    private interface LazyResolver {
        String getValue() throws IOException, InterruptedException;
    }

    /**
     * Class to handle cases when a Launcher was not created from a computer.
     *
     * @see hudson.Launcher#getComputer()
     */
    private abstract class LazyComputerResolver implements LazyResolver {
        protected abstract String getValue(Computer computer) throws IOException, InterruptedException;

        public String getValue() throws IOException, InterruptedException {
            return getValue(computer);
        }
    }
}

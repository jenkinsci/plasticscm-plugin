package com.codicesoftware.plugins.jenkins.tools;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.init.Initializer;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static hudson.init.InitMilestone.EXTENSIONS_AUGMENTED;

public class CmTool extends ToolInstallation implements NodeSpecific<CmTool>, EnvironmentSpecific<CmTool> {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT = "Default";
    private static final String DEFAULT_CM = "cm";

    private final boolean useInvariantCulture;
    @SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "This field is calculated")
    private transient Platform platform;

    @DataBoundConstructor
    public CmTool(String name, String home, boolean useInvariantCulture, List<? extends ToolProperty<?>> properties) {
        this(name, home, useInvariantCulture, properties, null);
    }

    private CmTool(
            String name,
            String home,
            boolean useInvariantCulture,
            List<? extends ToolProperty<?>> properties,
            Platform platform) {

        super(name, home, properties);
        this.useInvariantCulture = useInvariantCulture;
        this.platform = platform;
    }

    @Override
    public CmTool forEnvironment(EnvVars environment) {
        return new CmTool(
            getName(), environment.expand(getHome()), useInvariantCulture, getProperties().toList(), platform);
    }

    @Override
    public CmTool forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
        Platform nodePlatform = Platform.of(node);
        return new CmTool(
            getName(),
            translateFor(node, log),
            useInvariantCulture && nodePlatform == Platform.LINUX,
            getProperties().toList(),
            nodePlatform);
    }

    @Exported
    public boolean isUseInvariantCulture() {
        return useInvariantCulture;
    }

    public String getCmPath() {
        String home = getHome();

        if (Util.fixEmptyAndTrim(home) == null) {
            return platform.getToolName();
        }

        return home;
    }

    private static CmTool[] getInstallations(DescriptorImpl descriptor) {
        CmTool[] installations;
        try {
            installations = descriptor.getInstallations();
        } catch (NullPointerException e) {
            installations = new CmTool[0];
        }
        return installations;
    }

    public static CmTool get(Node node, EnvVars envVars, TaskListener log) throws IOException, InterruptedException {
        return (CmTool) getDefaultOrFirstInstallation().translate(node, envVars, log);
    }

    static CmTool getDefaultOrFirstInstallation() {
        Jenkins jenkinsInstance = Jenkins.getInstance();
        DescriptorImpl descriptor = jenkinsInstance.getDescriptorByType(CmTool.DescriptorImpl.class);
        CmTool tool = descriptor.getInstallation(DEFAULT);

        if (tool != null) {
            return tool;
        }

        CmTool[] installations = descriptor.getInstallations();

        if (installations.length > 0) {
            return installations[0];
        }

        createDefaultInstallation(descriptor);
        onLoaded();
        return descriptor.getInstallations()[0];

    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Initializer(after = EXTENSIONS_AUGMENTED)
    public static void onLoaded() {
        DescriptorImpl descriptor = (DescriptorImpl) Jenkins.getInstance().getDescriptor(CmTool.class);
        CmTool[] installations = getInstallations(descriptor);

        if (installations == null || installations.length == 0) {
            createDefaultInstallation(descriptor);
        }
    }

    private static void createDefaultInstallation(DescriptorImpl descriptor) {
        List<ToolProperty<?>> properties = Collections.emptyList();
        try {
            properties = Collections.singletonList(new InstallSourceProperty(descriptor.getDefaultInstallers()));
        } catch (IOException ignored) {
        }

        descriptor.setInstallations(new CmTool(DEFAULT, DEFAULT_CM, true, properties));
    }

    @Extension
    @Symbol("cmtool")
    public static class DescriptorImpl extends ToolDescriptor<CmTool> {

        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public String getId() {
            return "plasticscm-cli";
        }

        @RequirePOST
        @Override
        public FormValidation doCheckHome(@QueryParameter File value) {
            // FormValidation.validateExecutable checks admin access
            FormValidation result = FormValidation.validateExecutable(value.getPath());

            if (result.kind == FormValidation.Kind.ERROR) {
                return FormValidation.warning(result.getMessage() + " But it might exist in agents.");
            }
            return result;
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new CmToolInstaller(false));
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Plastic SCM";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            boolean result = super.configure(req, json);
            save();
            return result;
        }

        @Override
        public void setInstallations(CmTool... installations) {
            super.setInstallations(installations);
            save();
        }

        public CmTool getInstallation(String name) {
            getPropertyDescriptors();
            for (CmTool i : getInstallations()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }
    }
}

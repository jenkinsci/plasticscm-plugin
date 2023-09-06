package com.codicesoftware.plugins.jenkins.tools;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstallerDescriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CmToolInstaller extends DownloadFromUrlInstaller {
    private static final String ID = "LATEST";
    private final boolean ignoreSystemTool;

    @DataBoundConstructor
    public CmToolInstaller(boolean ignoreSystemTool) {
        super(ID);
        this.ignoreSystemTool = ignoreSystemTool;
    }

    @Override
    public Installable getInstallable() throws IOException {
        Installable installable = super.getInstallable();
        return installable != null ? new PlasticScmInstallable(installable) : installable;
    }

    @Override
    public boolean appliesTo(Node node) {
        // We'll support any node
        return true;
    }

    @Override
    public ToolInstallerDescriptor<?> getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(CmToolInstaller.class);
    }

    @Override
    public FilePath performInstallation(
            ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {

        // Let's prepare a tool with empty home so preferredLocation() uses its name to build the path
        // That method is declared final, so it can't be overriden
        ToolInstallation installableTool = new CmTool(
            tool.getName(), null, ((CmTool) tool).isUseInvariantCulture(), tool.getProperties());

        FilePath installablePath = preferredLocation(installableTool, node);

        // Skip system tool check if it was previously installed
        // Admins can always disable the automatic installer to fall back to the path they specified for the tool
        if (!ignoreSystemTool && !installablePath.child(".installedFrom").exists()) {
            FilePath existingHome = new FilePath(node.getChannel(), Util.fixEmpty(tool.getHome()));
            FilePath systemToolExistsPath = installablePath.child(".systemtool");

            if (systemToolExistsPath.exists()) {
                return existingHome;
            }

            log.getLogger().printf(
                "Checking whether tool '%s' exists in path '%s'...%n", tool.getName(), tool.getHome());
            try {
                Launcher launcher = existingHome.createLauncher(log);
                int processResult = launcher
                    .launch()
                    .envs("DOTNET_SYSTEM_GLOBALIZATION_INVARIANT=1") // Avoid issues
                    .cmds(tool.getHome(), "version")
                    .start()
                    .join();

                if (processResult == 0) {
                    systemToolExistsPath.write("true", "UTF-8");
                    return existingHome;
                }
            } catch (IOException | InterruptedException e) {
                log.getLogger().printf(
                    "Plastic SCM tool not found in system at '%s': %s%n", tool.getHome(), e.getMessage());
            }
        }

        return super.performInstallation(installableTool, node, log).child(Platform.of(node).getToolName());
    }

    @Exported
    public boolean isIgnoreSystemTool() {
        return ignoreSystemTool;
    }

    protected final class PlasticScmInstallable extends NodeSpecificInstallable {

        public PlasticScmInstallable(Installable inst) {
            super(inst);
        }

        @Override
        public NodeSpecificInstallable forNode(
                @Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
            Platform platform = Platform.of(node);
            if (platform == Platform.OTHER) {
                throw new InterruptedException("Unsupported platform");
            }

            Installable result = new Installable();
            result.id = id;
            result.name = name;
            result.url = url + platform.getDownloadPlatform();

            return new PlasticScmInstallable(result);
        }
    }

    @Extension
    public static class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<CmToolInstaller> {
        public DescriptorImpl() {
            // Avoid creating downloadables - we're not using the UpdateCenter
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Download from plasticscm.com";
        }

        @Override
        public List<? extends Installable> getInstallables() throws IOException {
            Installable installable = new Installable() {
                {
                    name = "Latest";
                    id = ID;
                    url = "https://www.plasticscm.com/download/clientbundle/latest/client-";
                }
            };

            return Collections.singletonList(new CmToolInstaller(false).new PlasticScmInstallable(installable));
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == CmTool.class;
        }
    }
}

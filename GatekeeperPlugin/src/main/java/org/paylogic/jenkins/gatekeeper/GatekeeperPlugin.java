package org.paylogic.jenkins.gatekeeper;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.Util;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.paylogic.jenkins.advancedmercurial.AdvancedMercurialManager;

import java.io.IOException;
import java.io.PrintStream;

public class GatekeeperPlugin extends Builder {

    @Getter private final boolean doGatekeeping;

    @DataBoundConstructor
    public GatekeeperPlugin(boolean doGatekeeping) {
        this.doGatekeeping = doGatekeeping;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.
        PrintStream l = listener.getLogger();

        EnvVars envVars = build.getEnvironment(listener);
        EnvironmentVariablesNodeProperty globalProperties = Jenkins.getInstance().getGlobalNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        envVars.putAll(globalProperties.getEnvVars());

        String givenNodeId = Util.replaceMacro("$NODE_ID", envVars);
        l.println("Resolved node id: "+ givenNodeId);



        AdvancedMercurialManager amm = new AdvancedMercurialManager(build, launcher);


        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }


    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Gatekeeper plugin";
        }
    }
}


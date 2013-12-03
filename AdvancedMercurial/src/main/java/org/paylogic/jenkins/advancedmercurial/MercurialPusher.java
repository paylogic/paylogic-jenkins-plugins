package org.paylogic.jenkins.advancedmercurial;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import lombok.extern.java.Log;
import org.kohsuke.stapler.DataBoundConstructor;
import org.paylogic.jenkins.advancedmercurial.exceptions.MercurialException;

import java.util.logging.Level;

/**
 * Fetches a list of branches to push from Redis.
 * Meant to be used in combination with GatekeeperPlugin and/or UpmergePlugin.
 */
@Log
public class MercurialPusher extends Builder {

    @DataBoundConstructor
    public MercurialPusher() {}


    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        AdvancedMercurialManager amm = null;
        try {
            amm = new AdvancedMercurialManager(build, launcher, listener);
        } catch (Exception e) {
            log.log(Level.SEVERE, "AdvancedMercurialManager could not be instantiated.", e);
            return false;
        }

        try {
            amm.push();
        } catch (MercurialException e) {
            log.log(Level.SEVERE, "Could not push changes...", e);
        }
        return true;
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Perform a Mercurial Push command.";
        }
    }
}

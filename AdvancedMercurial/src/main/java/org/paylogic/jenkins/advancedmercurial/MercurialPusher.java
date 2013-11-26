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
import org.paylogic.redis.RedisProvider;
import redis.clients.jedis.Jedis;

import java.util.List;
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
        RedisProvider redisProvider = new RedisProvider();
        Jedis redis = redisProvider.getConnection();
        AdvancedMercurialManager amm = new AdvancedMercurialManager(build, launcher);

        try {
            amm.push();
        } catch (MercurialException e) {
            log.log(Level.SEVERE, "Could not push changes...", e);
        }
        return true;
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * Returns true if this task is applicable to the given project.
         *
         * @return true to allow user to configure this post-promotion task for the given project.
         * @see hudson.model.AbstractProject.AbstractProjectDescriptor#isApplicable(hudson.model.Descriptor)
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        /**
         * Human readable name of this kind of configurable object.
         */
        @Override
        public String getDisplayName() {
            return "Mercurial Pusher";
        }
    }
}

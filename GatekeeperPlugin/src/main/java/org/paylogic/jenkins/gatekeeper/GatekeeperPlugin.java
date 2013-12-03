package org.paylogic.jenkins.gatekeeper;


import hudson.EnvVars;
import hudson.Launcher;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import lombok.Getter;
import lombok.extern.java.Log;
import org.kohsuke.stapler.DataBoundConstructor;
import org.paylogic.fogbugz.FogbugzCase;
import org.paylogic.fogbugz.FogbugzConstants;
import org.paylogic.jenkins.advancedmercurial.AdvancedMercurialManager;
import org.paylogic.jenkins.advancedmercurial.exceptions.MercurialException;
import org.paylogic.jenkins.fogbugz.FogbugzNotifier;
import org.paylogic.redis.RedisProvider;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;


/**
 * Main extension of the GatekeeperPlugin. Does Gatekeepering.
 */
@Log
public class GatekeeperPlugin extends Builder {

    @Getter private final boolean doGatekeeping;

    @DataBoundConstructor
    public GatekeeperPlugin(boolean doGatekeeping) {
        this.doGatekeeping = doGatekeeping;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        PrintStream l = listener.getLogger();

        /* Set up enviroment and resolve some variables. */
        EnvVars envVars = build.getEnvironment(listener);
        l.print("Build uuid: " + build.getExternalizableId());

        String givenCaseId = Util.replaceMacro("$CASE_ID", envVars);
        String givenNodeId = Util.replaceMacro("$NODE_ID", envVars);
        l.println("Resolved case id: "+ givenCaseId);
        int iGivenCaseId = Integer.parseInt(givenCaseId);
        int usableCaseId = 0;

        AdvancedMercurialManager amm = null;
        try {
            amm = new AdvancedMercurialManager(build, launcher, listener);
        } catch (Exception e) {
            log.log(Level.SEVERE, "AdvancedMercurialManager could not be instantiated.", e);
            return false;
        }

        /* Try to resolve case ID using parameters or branch name */
        String branchName = "";
        if (iGivenCaseId != 0 && !givenNodeId.isEmpty()) {
            branchName = givenNodeId;
            usableCaseId = iGivenCaseId;
        } else {
            branchName = amm.getBranch();
        }
        String featureBranch = "";
        String targetBranch = "";

        if (!branchName.matches(FogbugzConstants.FEATUREBRANCH_REGEX)) {
            log.info("No case IDs found, aborting...");
            return false;
        } else {
            usableCaseId = Integer.parseInt(branchName.substring(1, branchName.length()));
        }

        /* Fetch branch information from Fogbugz */
        log.info("Current branch '" + branchName + "' matches feature branch regex.");
        FogbugzCase fallbackCase = new FogbugzNotifier().getFogbugzCaseManager().getCaseById(usableCaseId);
        featureBranch = fallbackCase.getFeatureBranch().split("#")[1];
        targetBranch = fallbackCase.getTargetBranch();

        if (!branchName.equals(featureBranch)) {
            log.info("Branchnames are not equal: " + branchName + " -- " + featureBranch);
        }

        /* Actual Gatekeepering logic. */
        try {
            //amm.pull();
            amm.update(targetBranch);
            amm.mergeWorkspaceWith(featureBranch);
            amm.commit("[Jenkins Integration Merge] Merge " + targetBranch + " with " + featureBranch);
        } catch (MercurialException e) {
            log.log(Level.SEVERE, "Exception during update:", e);
            return false;
        }

        /* Set the featureBranch we merged with in redis to other plugin know this was actually the build's branch */
        RedisProvider redisProvider = new RedisProvider();
        Jedis redis = redisProvider.getConnection();
        redis.set("old_" + build.getExternalizableId(), featureBranch);

        /* Add commit to list of things to push. */
        redis.rpush("topush_" + build.getExternalizableId(), targetBranch);
        redisProvider.returnConnection(redis);
        l.println("Pushed value to redis");
        return true;
    }


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
            return "Perform Gatekeerping.";
        }
    }
}


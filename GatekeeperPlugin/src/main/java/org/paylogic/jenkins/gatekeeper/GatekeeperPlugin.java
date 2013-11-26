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
import org.paylogic.jenkins.fogbugz.casecache.CachedCase;
import org.paylogic.jenkins.fogbugz.casecache.CaseStorageApi;
import org.paylogic.redis.RedisProvider;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


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

        EnvVars envVars = build.getEnvironment(listener);
        RedisProvider redisProvider = new RedisProvider();
        Jedis redis = redisProvider.getConnection();
        l.print("Build uuid: " + build.getExternalizableId());

        String givenNodeId = Util.replaceMacro("$NODE_ID", envVars);
        l.println("Resolved node id: "+ givenNodeId);

        AdvancedMercurialManager amm = new AdvancedMercurialManager(build, launcher);
        CaseStorageApi caseManager = CaseStorageApi.get();
        String branchName = "";
        if (givenNodeId.matches(FogbugzConstants.FEATUREBRANCH_REGEX)) {
            branchName = givenNodeId;
        } else {
            branchName = amm.getBranch();
        }
        List<CachedCase> cc = new ArrayList<CachedCase>();
        /*
        try {
             cc = caseManager.getCasesByFeatureBranch(branchName);
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Exception while trying to fetch case from cache.", e);
        }
        */

        String featureBranch = "";
        String targetBranch = "";

        CachedCase fbCase = null;
        if (cc.size() == 0) {
            log.info("No case found in cache, falling back to 'guess' mode.");
            if (!branchName.matches(FogbugzConstants.FEATUREBRANCH_REGEX)) {
                log.info("No cases found, aborting...");
                redis.disconnect();
                return false;
            }
            log.info("Current branch '" + branchName + "' matches feature branch regex.");
            log.info("Fetching case by branch name from Fogbugz.");
            FogbugzCase fallbackCase = new FogbugzNotifier().getFogbugzCaseManager().getCaseById(
                    Integer.parseInt(branchName.substring(1, branchName.length())));
            log.info("Case retrieved, pulling data..");
            featureBranch = fallbackCase.getFeatureBranch().split("#")[1];
            targetBranch = fallbackCase.getTargetBranch();
        } else {
            log.info("Pulling data from case in cache");
            fbCase = cc.get(0);
            featureBranch = fbCase.featureBranch;
            targetBranch = fbCase.targetBranch;
        }

        log.info("Branches: " + featureBranch + ", " + targetBranch + ", " + branchName);

        if (!branchName.equals(featureBranch)) {
            log.info("Branchnames are not equal: " + branchName + " -- " + featureBranch);
        }

        try {
            amm.pull();
            amm.update(targetBranch);
            amm.mergeWorkspaceWith(featureBranch);
            amm.commit("[Jenkins Integration Merge] Merge " + targetBranch + " with " + featureBranch);
        } catch (MercurialException e) {
            log.log(Level.SEVERE, "Exception during update:", e);
            return false;
        }

        redis.set("old_" + build.getExternalizableId(), featureBranch);

        // Add commit to list of things to push.
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
            return "Gatekeeper plugin";
        }
    }
}


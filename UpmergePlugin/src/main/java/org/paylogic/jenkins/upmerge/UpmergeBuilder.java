package org.paylogic.jenkins.upmerge;
import hudson.Launcher;
import hudson.Extension;
import hudson.Plugin;
import hudson.Util;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.paylogic.fogbugz.FogbugzCase;
import org.paylogic.fogbugz.FogbugzCaseManager;
import org.paylogic.jenkins.advancedmercurial.AdvancedMercurialManager;
import org.paylogic.jenkins.advancedmercurial.MercurialBranch;
import org.paylogic.jenkins.advancedmercurial.exceptions.MercurialException;
import org.paylogic.jenkins.advancedmercurial.exceptions.UnknownRevisionException;
import org.paylogic.jenkins.fogbugz.FogbugzNotifier;
import org.paylogic.jenkins.upmerge.releasebranch.ReleaseBranch;
import org.paylogic.jenkins.upmerge.releasebranch.ReleaseBranchImpl;
import org.paylogic.jenkins.upmerge.releasebranch.ReleaseBranchInvalidException;
import org.paylogic.redis.RedisProvider;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * UpmergeBuilder!
 */
@Log
public class UpmergeBuilder extends Builder {

    @Getter
    private final boolean run_always;

    @DataBoundConstructor
    public UpmergeBuilder(boolean run_always) {
        this.run_always = run_always;
    }

    /**
     * Here we should do upmerging.
     *
     * So:
     * - Fetch case info using CASE_ID parameter (should be given).
     * - NOT !! Create 'ReleaseBranch' object from case info and a nextBranch object which is releasebranch.copy().next();
     * - Create ReleaseBranch object from current branch, we may expect that the GatekeeperPlugin set the correct one.
     * - Initiate UpMerge sequence....
     *   - Try to pull new code from nextBranch.getNext();
     *   - Try to merge this new code with releaseBranch();
     *   - Commit this shiny new code.
     *   - Set a flag somewhere, indicating that this upmerge has been done.
     *   - Repeat UpMerge sequence for next releases until there are no moar releases.
     * - In some post-build thingy, push these new branches if all went well.
     * - We SHOULD not have to do any cleanup actions, because workspace is updated every build.
     * - Rely on the FogbugzPlugin (dependency, see pom.xml) to do reporting of our upmerges.
     * - Trigger new builds on all branches that have been merged.
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        PrintStream l = listener.getLogger();
        l.println("----------------------------------------------------------");
        l.println("--------------------- Now Upmerging ----------------------");
        l.println("----------------------------------------------------------");
        try {
            return this.doPerform(build, launcher, listener);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception during Gatekeeepring.", e);
            l.append("Exception occured, build aborting...");
            l.append(e.toString());
            return false;
        }
    }

    private boolean doPerform(AbstractBuild build, Launcher launcher, BuildListener listener) throws Exception {
        PrintStream l = listener.getLogger();
        if (build.getResult() != Result.SUCCESS && !this.run_always) {
            log.info("Not running build due to failed tests in previous steps.");
            l.append("Not running build due to failed tests in previous steps.");
            return false;
        }

        /* Get case ID using build parameters */
        int resolvedCaseId = Integer.parseInt(Util.replaceMacro("$CASE_ID", build.getEnvironment(listener)));

        /* Fetch case with the resolved case id */
        FogbugzCaseManager fbManager = new FogbugzNotifier().getFogbugzCaseManager();
        FogbugzCase fbCase = fbManager.getCaseById(resolvedCaseId);

        /* Get branch name using AdvancedMercurialManager, which we'll need later on as well. */
        AdvancedMercurialManager amm = new AdvancedMercurialManager(build, launcher, listener);
        String branchName = amm.getBranch();
        Map buildVariables = build.getBuildVariables();


        /* Get a ReleaseBranch compatible object to bump release branch versions with. */
        /* TODO: resolve user ReleaseBranchImpl of choice here, learn Java Generics first ;) */
        ReleaseBranch releaseBranch = new ReleaseBranchImpl(fbCase.getTargetBranch());
        List<MercurialBranch> branchList = amm.getBranches();

        ReleaseBranch nextBranch = releaseBranch.copy();
        nextBranch.next();

        /* Prepare points to push merge results to, so we can tell the dev what we upmerged */
        List<String> branchesToPush = new ArrayList<String>();
        branchesToPush.add(releaseBranch.getName());
        RedisProvider redisProvider = new RedisProvider();
        Jedis redis = redisProvider.getConnection();

        /*
         Do actual upmerging in this loop, until we can't upmerge no moar.
         Will not attempt to Upmerge to branches that were not in the 'hg branches' output.
        */

        int retries = 0;
        do {
            if (this.isInBranchList(releaseBranch.getName(), branchList)) {
                try {
                    amm.update(nextBranch.getName());
                    amm.mergeWorkspaceWith(releaseBranch.getName());
                    amm.commit("[Jenkins Upmerging] Merged " + nextBranch.getName() + " with " + releaseBranch.getName());
                    redis.rpush("topush_" + build.getExternalizableId(), nextBranch.getName());
                    branchesToPush.add(nextBranch.getName());
                    retries = 0;

                } catch (UnknownRevisionException ure) {
                    retries++;
                }
            } else {
                retries++;
            }

            // Bump releases
            releaseBranch.next();
            nextBranch.next();
        }  while(retries < 5);

        redisProvider.returnConnection(redis);
        return true;
    }

    private boolean isInBranchList(String branchName, List<MercurialBranch> list) {
        for (MercurialBranch b : list) {
            if (b.getBranchName().equals(branchName)) {
                return true;
            }
        }
        return false;
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link UpmergeBuilder}. Used as a singleton. Stores global UpmergePlugin settings.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() throws Exception {
            super();
            Plugin fbPlugin = Jenkins.getInstance().getPlugin("FogbugzPlugin");
            if (fbPlugin == null) {
                throw new Exception("You need the 'FogbugzPlugin' installed in order to use 'UpmergePlugin'");
            }

            Plugin hgPlugin = Jenkins.getInstance().getPlugin("mercurial");
            if (hgPlugin == null) {
                throw new Exception("You need the 'mercurial' plugin installed in order to use 'UpmergePlugin'");
            }
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Perform Upmerging of release branches.";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }
    }
}

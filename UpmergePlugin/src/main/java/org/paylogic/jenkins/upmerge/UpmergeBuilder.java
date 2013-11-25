package org.paylogic.jenkins.upmerge;
import hudson.Launcher;
import hudson.Extension;
import hudson.Plugin;
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
import org.paylogic.jenkins.executionhelper.ExecutionHelper;
import org.paylogic.jenkins.fogbugz.FogbugzNotifier;
import org.paylogic.jenkins.upmerge.releasebranch.ReleaseBranch;
import org.paylogic.jenkins.upmerge.releasebranch.ReleaseBranchImpl;
import org.paylogic.jenkins.upmerge.releasebranch.ReleaseBranchInvalidException;
import org.paylogic.redis.RedisProvider;
import redis.clients.jedis.Jedis;

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

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public UpmergeBuilder(boolean run_always) {
        this.run_always = run_always;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.
        if (build.getResult() != Result.SUCCESS && !this.run_always) {
            log.info("Not running build due to no successful tests.");
            return false;
        }

        // This also shows how you can consult the global configuration of the builder
        PrintStream l = listener.getLogger();

        /* Wheird generics logic which does not work at all.
        // Get the ReleaseBranch implementation
        Class<?> releaseBranchImpl = null;
        for (ReleaseBranch r: ReleaseBranch.all()) {
            // Loop trough the classes, assign one if found in list.
            // If more than one exists, our supplied implementation has less priority.
            if (releaseBranchImpl == null) {
                releaseBranchImpl = r.getClass();
                break;
            }
        }

        if (releaseBranchImpl == null) {
            log.info("NO RELAESE BRANCH IMPLEMENTATION FOUDN! DEFAULTING TO BUILTIN");
            releaseBranchImpl = ReleaseBranchImpl.class;
        }

        // END Get the releasebranch implementation
        */

        ExecutionHelper executor = new ExecutionHelper(build, launcher);
        RedisProvider redisProvider = new RedisProvider();
        Jedis redis = redisProvider.getConnection();

        Map buildVariables = build.getBuildVariables();

        FogbugzCaseManager caseManager = new FogbugzNotifier().getFogbugzCaseManager();
        // this is just a test :)
        FogbugzCase fbCase = caseManager.getCaseById(3);

        String branchName;

        try {
            branchName = executor.runCommandClean("hg branch");
        } catch (Exception e) {
            log.log(Level.SEVERE, "ERRRUR", e);
            return false;
        }

        ReleaseBranch releaseBranch = null;
        try {
            releaseBranch = new ReleaseBranchImpl(branchName);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Invalid releasebranch implementation found.", e);
            return false;
        }

        AdvancedMercurialManager amm = new AdvancedMercurialManager(build, launcher);
        List<MercurialBranch> branchList = amm.getBranches();
        l.print(AdvancedMercurialManager.prettyPrintBranchlist(branchList));

        ReleaseBranch nextBranch = null;
        try {
            nextBranch = releaseBranch.copy();
        } catch (ReleaseBranchInvalidException e) {
            log.info("NOW HOW DID THAT HAPPEN?");
            return false;
        }
        nextBranch.next();

        List<String> branchesToPush = new ArrayList<String>();
        branchesToPush.add(releaseBranch.getName());

        int retries = 0;
        do {
            if (this.isInBranchList(releaseBranch.getName(), branchList)) {
                amm.update(nextBranch.getName());
                amm.mergeWorkspaceWith(releaseBranch.getName());
                amm.commit("[Jenkins Upmerging] Merged " + releaseBranch.getName() + " with " + nextBranch.getName());

                redis.rpush("topush_" + build.getExternalizableId(), nextBranch.getName());
                branchesToPush.add(nextBranch.getName());

                retries = 0;
            } else {
                retries++;
            }

            // Bump releases
            releaseBranch.next();
            nextBranch.next();
        }  while(retries < 5);

        redis.disconnect();
        return true;

        /**
         * Here we should do upmerging. Luckily, we can access the command line,
         * and run stuff from there (on the agents even!). Better to use the mercurial plugin though.
         * (edit: this seems to be not possible :| , so lets create our own one!).
         *
         * So:
         * - Fetch case info using branch name.
         * - Create 'ReleaseBranch' object from case info and a nextBranch object which is releasebranch.copy().next();
         * - Initiate UpMerge sequence....
         *   - Try to pull new code from nextBranch.getNext();
         *   - Try to merge this new code with releaseBranch();
         *   - Commit this shiny new code.
         *   - Set a flag somewhere, indicating that this upmerge has been done.
         *   - Repeat UpMerge sequence for next releases until there are no moar releases.
         * - In some post-build thingy, push these new branches if all went well.
         * - We SHOULD not have to do any cleanup actions, because workspace is force-cleared every build.
         * - Rely on the FogbugzPlugin (dependency, see pom.xml) to do reporting of our upmerges.
         * - Trigger new builds on all branches that have been merged.
         *
         */
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
     * Descriptor for {@link UpmergeBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/UpmergeBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
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
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Does upmerging 'n stuff";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            save();
            return super.configure(req,formData);
        }
    }
}

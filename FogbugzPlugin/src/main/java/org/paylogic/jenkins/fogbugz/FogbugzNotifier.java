package org.paylogic.jenkins.fogbugz;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.scm.SCM;
import hudson.tasks.*;
import lombok.Getter;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.paylogic.fogbugz.FogbugzCase;
import org.paylogic.fogbugz.FogbugzCaseManager;
import org.paylogic.jenkins.advancedmercurial.AdvancedMercurialManager;
import org.paylogic.redis.RedisProvider;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static hudson.Util.*;


/**
 * Notifier in Jenkins system, reports status to Fogbugz.
 * Searches for branches compatible with caseID, and reports if that's found.
 */
@Log
public class FogbugzNotifier extends Notifier {

    private static final String FEATURE_BRANCH_REGEX = "c\\d+";
    private static final String RELEASE_BRANCH_REGEX = "r\\d{4}";

    @DataBoundConstructor
    public FogbugzNotifier() {
        super();
    }

    /*
    private void initialize(String url, String token) {
        if (url.endsWith("/"))
            this.url = url;
        else
            this.url = url + "/";

        this.token = token;
    }

    private void initialize() {
        this.initialize(DESCRIPTOR.getUrl(), DESCRIPTOR.getToken());
    }
    */

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
        return true;
	}

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        log.info("Now performing post-build action for Fogbugz reporting.");

        RedisProvider redisProvider = new RedisProvider();
        Jedis redis = redisProvider.getConnection();

        EnvVars envVars = null;
        try {
            envVars = build.getEnvironment(listener);
        } catch (Exception e) {
            log.log(Level.SEVERE, "ERRORRRRRRRRRRBBLBL", e);
        }

        listener.getLogger().print("Build uuid: " + build.getExternalizableId());

        String givenNodeId = replaceMacro("$NODE_ID", envVars);
        String givenCaseId = replaceMacro("$CASE_ID", envVars);
        log.info("Given case id = " + givenCaseId);

        AdvancedMercurialManager amm = null;
        try {
            amm = new AdvancedMercurialManager(build, launcher, listener);
        } catch (Exception e) {
            log.log(Level.SEVERE, "AdvancedMercurialManager could not be instantiated.", e);
            return false;
        }

        String output = amm.getBranch();
        String branchAccordingToRedis = redis.get("old_" + build.getExternalizableId());

        if (output.matches(FEATURE_BRANCH_REGEX)) {
            log.info("Current branch is case branch, using that to find case in FB");
        } else if (!givenCaseId.isEmpty() && !givenCaseId.equals("0")) {
            log.info("Using given case ID for reporting.");
        } else if (branchAccordingToRedis != null &&
                    !branchAccordingToRedis.isEmpty() &&
                    branchAccordingToRedis.matches(FEATURE_BRANCH_REGEX)) {
            log.info("Current branch is no case branch, but branchname found in Redis is!");
            output = branchAccordingToRedis;
        } else {
            log.info("No case branch found, currently not reporting to fogbugz.");
            return false;  // TODO: should we return true or false here?
                           // TODO: and does that even impact build status?
        }

        int usableCaseId = 0;
        if (!givenCaseId.isEmpty()) {
            usableCaseId = Integer.parseInt(givenCaseId);
        } else {
            // Strip the 'c' from branch name, then fetch case with that.
            usableCaseId = Integer.parseInt(output.substring(1, output.length()));
        }


        FogbugzCaseManager caseManager = this.getFogbugzCaseManager();
        FogbugzCase fbCase = caseManager.getCaseById(usableCaseId);
        if (fbCase == null) {
            log.log(Level.SEVERE, "Fetching case from fogbugz failed. Please check your settings.");
            return false;
        }
        fbCase.assignToOpener();
        //fbCase.addTag("merged");

        Map<String, String> templateContext = new HashMap();
        templateContext.put("url", build.getAbsoluteUrl());
        templateContext.put("buildNumber", Integer.toString(build.getNumber()));
        templateContext.put("buildResult", build.getResult().toString());

        //templateContext.put("buildHealth", build.getTestResultAction().getBuildHealth().getDescription());
        try {
            templateContext.put("tests_failed", Integer.toString(build.getTestResultAction().getFailCount()));
            templateContext.put("tests_skipped", Integer.toString(build.getTestResultAction().getSkipCount()));
            templateContext.put("tests_total", Integer.toString(build.getTestResultAction().getTotalCount()));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception during fetching of test results:", e);
            templateContext.put("tests_failed", "unknown");
            templateContext.put("tests_skipped", "unknown");
            templateContext.put("tests_total", "unknown");
        }

        String listOfBranchesMergedWith = "";
        for (String branchName: redis.lrange("topush_" + build.getExternalizableId(), 0, -1)) {
            listOfBranchesMergedWith += branchName + ", ";
        }

        redis.expire("topush_" + build.getExternalizableId(), 3600);
        redis.expire("old_" + build.getExternalizableId(), 3600);

        templateContext.put("mergedwith", listOfBranchesMergedWith);

        // Fetch&render templates, then save the template output together with the case.
        Template mustacheTemplate;
        if (build.getResult() == Result.SUCCESS) {
            mustacheTemplate = Mustache.compiler().compile(this.getDescriptor().getSuccessfulBuildTemplate());
        } else {
            mustacheTemplate = Mustache.compiler().compile(this.getDescriptor().getFailedBuildTemplate());
        }
        caseManager.saveCase(fbCase, mustacheTemplate.execute(templateContext));

        return true;
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    public FogbugzCaseManager getFogbugzCaseManager() {
        return this.getDescriptor().getFogbugzCaseManager();
    }


    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Getter private String url;
        @Getter private String token;

        private String failedBuildTemplate;
        private String successfulBuildTemplate;

        @Getter private String featureBranchFieldname;
        @Getter private String originalBranchFieldname;
        @Getter private String targetBranchFieldname;

        @Getter private String job_to_trigger;

        private int mergekeeperUserId;

        public int getMergekeeperUserId() {
            if (this.mergekeeperUserId == 0) {
                return 1;
            } else {
                return this.mergekeeperUserId;
            }
        }

        public String getFailedBuildTemplate() {
            if (this.failedBuildTemplate == null || this.failedBuildTemplate.isEmpty()) {
                return "Jenkins reports that the build has {{tests_failed}} failed tests :(" +
                        "\nView extended result here: {{url}}";
            } else {
                return this.failedBuildTemplate;
            }
        }

        public String getSuccessfulBuildTemplate() {
            if (this.successfulBuildTemplate == null || this.successfulBuildTemplate.isEmpty()) {
                return "Jenkins reports that the build was successful!" +
                        "{{#mergedwith}}\nUpmerged: {{mergedwith}}{{/mergedwith}}" +
                        "\nView extended result here: {{url}}";
            } else {
                return this.successfulBuildTemplate;
            }
        }

        public DescriptorImpl() {
            super();
            load();
        }

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> type) {
            return true;
		}

		@Override
		public String getDisplayName() {
            return "Fogbugz reporting plugin";
		}

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            String url = formData.getString("url");
            if (url.endsWith("/"))
                this.url = url;
            else
                this.url = url + "/";

            this.token = formData.getString("token");
            this.featureBranchFieldname = formData.getString("featureBranchFieldname");
            this.originalBranchFieldname = formData.getString("originalBranchFieldname");
            this.targetBranchFieldname = formData.getString("targetBranchFieldname");

            int mergekeeperid = formData.getInt("mergekeeperUserId");
            if (mergekeeperid == 0) {
                mergekeeperid = 1;
            }
            this.mergekeeperUserId = mergekeeperid;

            this.failedBuildTemplate = formData.getString("failedBuildTemplate");
            this.successfulBuildTemplate = formData.getString("successfulBuildTemplate");
            this.job_to_trigger = formData.getString("job_to_trigger");

            save();
            return super.configure(req, formData);
        }

        public FogbugzCaseManager getFogbugzCaseManager() {
            return new FogbugzCaseManager(this.url, this.token, this.featureBranchFieldname,
                                             this.originalBranchFieldname, this.targetBranchFieldname,
                                             this.mergekeeperUserId);
        }

	}
}

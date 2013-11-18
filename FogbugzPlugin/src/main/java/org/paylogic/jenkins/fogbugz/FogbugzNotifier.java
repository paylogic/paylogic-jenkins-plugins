package org.paylogic.jenkins.fogbugz;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import hudson.Extension;
import hudson.Launcher;
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

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


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
        log.info(build.getResult().toString());
        log.info(launcher.toString());
        log.info(listener.toString());

        SCM scm = build.getProject().getScm();
        log.info("SCM type: " + scm.getType());

        OutputStream os = new ByteArrayOutputStream();
        String[] command = {"hg", "branch"};
        try {
            launcher.launchChannel(command, os, build.getWorkspace(), build.getEnvVars());
        } catch (EOFException e) {
            log.info("End of output stream from hg branch command reached.");
        } catch (Exception e) {
            log.log(Level.INFO, "Exception while running command 'hg branch'", e);
        }

        String output = os.toString().replaceAll("\\s","");
        log.info("Response from command: " + output);

        if (output.matches(FEATURE_BRANCH_REGEX) || output.startsWith("c")) {
            log.info("Case branch found! Reporting to fogbugz now!");
            // Probably a case branch :)
            FogbugzCaseManager caseManager = this.getFogbugzCaseManager();
            // Strip the 'c' from branch name, then fetch case with that.
            FogbugzCase fbCase = caseManager.getCaseById(Integer.parseInt(output.substring(1, output.length())));
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

            // Fetch&render templates, then save the template output together with the case.
            Template mustacheTemplate;
            if (build.getResult() == Result.SUCCESS) {
                mustacheTemplate = Mustache.compiler().compile(this.getDescriptor().getSuccessfulBuildTemplate());
            } else {
                mustacheTemplate = Mustache.compiler().compile(this.getDescriptor().getFailedBuildTemplate());
            }
            caseManager.saveCase(fbCase, mustacheTemplate.execute(templateContext));
        } else {
            log.info("No case branch found, currently not reporting to fogbugz.");
        }

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
                return "Jenkins reports that the build has failed :(";
            } else {
                return this.failedBuildTemplate;
            }
        }

        public String getSuccessfulBuildTemplate() {
            if (this.successfulBuildTemplate == null || this.successfulBuildTemplate.isEmpty()) {
                return "Jenkins reports that the build was successful!";
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
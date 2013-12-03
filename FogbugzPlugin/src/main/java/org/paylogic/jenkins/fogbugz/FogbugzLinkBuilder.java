package org.paylogic.jenkins.fogbugz;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import lombok.extern.java.Log;
import org.kohsuke.stapler.DataBoundConstructor;
import org.paylogic.jenkins.advancedmercurial.AdvancedMercurialManager;

import java.util.logging.Level;

import static hudson.Util.replaceMacro;

/**
 * Adds a FogbugzLinkAction to the current build.
 * Gets CASE_ID from parameters or branch name.
 */
@Log
public class FogbugzLinkBuilder extends Recorder {
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @DataBoundConstructor
    public FogbugzLinkBuilder(){ }

    @Override
    public final boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) {
        int caseId = 0;
        try {
            String givenCaseId = replaceMacro("$CASE_ID", build.getEnvVars());
            caseId = Integer.parseInt(givenCaseId);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Could not resolve $CASE_ID", e);
        }

        // Unresolved caseId, try with AMM.
        if (caseId == 0) {
            try {
                AdvancedMercurialManager amm = new AdvancedMercurialManager(build, launcher, listener);
                String branchName = amm.getBranch();
                caseId = Integer.parseInt(branchName);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Exception while fetching branch information from AMM", e);
            }
        }

        // If we finally get a caseId, use that to create and attach a LinkAction.
        // Else: just bail out.
        if (caseId != 0) {
            // Attach an Action to the Build.
            build.getActions().add(new FogbugzLinkAction(caseId));
        }

        return true;
    }
}
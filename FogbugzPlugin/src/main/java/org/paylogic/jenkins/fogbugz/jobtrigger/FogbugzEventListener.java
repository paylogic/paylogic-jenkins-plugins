package org.paylogic.jenkins.fogbugz.jobtrigger;

import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import lombok.extern.java.Log;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.paylogic.fogbugz.FogbugzCase;
import org.paylogic.fogbugz.FogbugzCaseManager;
import org.paylogic.jenkins.fogbugz.FogbugzNotifier;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 * FogbugzEventListener: a HTTP endpoint that triggers builds.
 * Set the build to trigger in Jenkins' global settings page.
 */
@Log
@Extension
public class FogbugzEventListener implements UnprotectedRootAction {

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "Fogbugz Event Listener";
    }

    public String getUrlName() {
        return "fbTrigger";
    }

    public void doIndex(@QueryParameter(required = true) int caseid) {
        if (caseid == 0) {
            return;
        }
        log.info("WE GOTZ AN TRIGGUR!");

        FogbugzCaseManager caseManager = new FogbugzNotifier().getFogbugzCaseManager();
        FogbugzCase fbCase = caseManager.getCaseById(caseid);

        String featureBranch = "";
        try {
            featureBranch = fbCase.getFeatureBranch().split("#")[1];
        } catch (Exception e) {
            log.log(Level.SEVERE, "No feature branch found in correct format. Aborting trigger...");
            return;
        }

        for (Project<?, ?> p: Jenkins.getInstance().getItems(Project.class)) {
            if (p.getName().equals(new FogbugzNotifier().getDescriptor().getJob_to_trigger())) {

                // Fetch default Parameters
                ParametersDefinitionProperty property = p.getProperty(ParametersDefinitionProperty.class);
                final List<ParameterValue> parameters = new ArrayList<ParameterValue>();
                for (final ParameterDefinition pd : property.getParameterDefinitions()) {
                    final ParameterValue param = pd.getDefaultParameterValue();
                    if (pd.getName().equals("CASE_ID")) {
                        parameters.add(new StringParameterValue("CASE_ID", Integer.toString(fbCase.getId())));
                    } else if (param != null) {
                        parameters.add(param);
                    }
                }

                // Here, we actually schedule the build.
                p.scheduleBuild2(0, new FogbugzBuildCause(), new ParametersAction(parameters));
            }
        }
    }
}

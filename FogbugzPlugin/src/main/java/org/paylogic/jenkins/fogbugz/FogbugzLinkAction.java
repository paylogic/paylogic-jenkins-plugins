package org.paylogic.jenkins.fogbugz;

import hudson.model.Action;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * Container object for FogbugzLink, is used by Jenkins to render the caseURL on build page.
 */
@Log
public class FogbugzLinkAction implements Action {
    @Getter private int caseId;
    @Getter private String caseUrl;

    public FogbugzLinkAction(int caseId) {
        super();
        this.caseId = caseId;
        this.caseUrl = new FogbugzNotifier().getDescriptor().getUrl() + "default.asp?" + Integer.toString(caseId);
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "Display a link to the related FogBugz case on the build page.";
    }

    public String getUrlName() {
        return "fogbugzlink";
    }
}

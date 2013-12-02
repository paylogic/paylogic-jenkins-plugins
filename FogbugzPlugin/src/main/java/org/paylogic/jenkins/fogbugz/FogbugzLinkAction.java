package org.paylogic.jenkins.fogbugz;

import hudson.model.Action;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
public class FogbugzLinkAction implements Action {
    @Getter private int caseId;

    public FogbugzLinkAction(int caseId) {
        super();
        this.caseId = caseId;
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "Fogbugz Link to Case";
    }

    public String getUrlName() {
        return "fogbugzlink";
    }
}

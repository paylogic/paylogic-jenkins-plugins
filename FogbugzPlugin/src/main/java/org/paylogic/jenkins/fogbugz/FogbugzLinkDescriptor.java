package org.paylogic.jenkins.fogbugz;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import lombok.extern.java.Log;

@Log
@Extension
public class FogbugzLinkDescriptor extends BuildStepDescriptor<Publisher> {

    public FogbugzLinkDescriptor() {
        super(FogbugzLinkBuilder.class);
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Adds fogbugz link to case on build page";
    }
}

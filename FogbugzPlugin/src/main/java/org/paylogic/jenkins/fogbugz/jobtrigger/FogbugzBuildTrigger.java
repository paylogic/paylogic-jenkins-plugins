package org.paylogic.jenkins.fogbugz.jobtrigger;

/*
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public class FogbugzBuildTrigger extends Trigger<AbstractProject<?, ?>> {

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        private String job_to_trigger;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            this.job_to_trigger = formData.getString("job_to_trigger");
            save();
            return super.configure(req, formData);
        }

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Fogbugz Build Trigger";
        }


        @Extension
        public static class FogbugzItemListenerImpl extends ItemListener {
            @Override
            public void onLoaded() {
            }
        }

    }
}

*/

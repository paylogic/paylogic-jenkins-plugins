package org.paylogic.jenkins.upmerge;
import hudson.Launcher;
import hudson.Extension;
import hudson.Plugin;
import hudson.plugins.mercurial.MercurialSCM;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.paylogic.fogbugz.FogbugzCase;
import org.paylogic.fogbugz.FogbugzCaseManager;
import org.paylogic.jenkins.advancedmercurial.AdvancedMercurialManager;
import org.paylogic.jenkins.advancedmercurial.MercurialBranch;
import org.paylogic.jenkins.executionhelper.ExecutionHelper;
import org.paylogic.jenkins.fogbugz.FogbugzNotifier;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link UpmergeBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
@Log
public class UpmergeBuilder extends Builder {

    private final String name;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public UpmergeBuilder(String name) {
        this.name = name;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
        PrintStream l = listener.getLogger();

        if (getDescriptor().getUseFrench())
            l.println("Bonjour, "+name+"!");
        else
            l.println("Hello, "+name+"!");

        ExecutionHelper executor = new ExecutionHelper(build, launcher);

        Map buildVariables = build.getBuildVariables();

        FogbugzCaseManager caseManager = new FogbugzNotifier().getFogbugzCaseManager();
        // this is just a test :)
        FogbugzCase fbCase = caseManager.getCaseById(3);

        try {
            String branchName = executor.runCommand("hg branch");
        } catch (Exception e) {
            log.log(Level.SEVERE, "ERRRUR", e);
            return false;
        }

        AdvancedMercurialManager amm = new AdvancedMercurialManager(build, launcher);
        l.print(AdvancedMercurialManager.prettyPrintBranchlist(amm.getBranches()));

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
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;

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

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
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
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getUseFrench() {
            return useFrench;
        }
    }
}

package org.paylogic.jenkins.advancedmercurial;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.plugins.mercurial.MercurialSCM;
import org.paylogic.jenkins.executionhelper.ExecutionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom class for handling branches and merges with Mercurial repositories in Jenkins.
 * Maybe one day we should try to merge this back to the Jenkins plugin, as this is not really a plugin.
 */
public class AdvancedMercurialManager {

    private MercurialSCM repository;
    private String hgExe;
    private AbstractBuild build;
    private Launcher launcher;
    private ExecutionHelper executor;

    public AdvancedMercurialManager(AbstractBuild build, Launcher launcher) {
        this.repository = ((MercurialSCM) build.getProject().getScm());
        this.hgExe = repository.getDescriptor().getHgExe();
        this.executor = new ExecutionHelper(build, launcher);
        this.build = build;
        this.launcher = launcher;
    }

    /**
     * Get Mercurial branches from command line output,
     * and put them in a List  with MercurialBranches so it's nice to work with.
     * @return List of MercurialBranches
     */
    public List<MercurialBranch> getBranches() {
        String rawBranches = "";
        try {
            String[] command = {this.hgExe, "branches"};
            rawBranches = this.executor.runCommand(command);
        } catch (Exception e) {
            return null;
        }
        List<MercurialBranch> list = new ArrayList<MercurialBranch>();
        for (String line: rawBranches.split("\n")) {
            // line should contain: <branchName>                 <revision>:<hash>  (yes, with lots of whitespace)
            String[] seperatedByWhitespace = line.split("\\s+");
            String branchName = seperatedByWhitespace[0];
            String[] seperatedByColon = seperatedByWhitespace[1].split(":");
            int revision = Integer.parseInt(seperatedByColon[0]);
            String hash = seperatedByColon[1];

            list.add(new MercurialBranch(branchName, revision, hash));
        }
        return list;
    }

    public static String prettyPrintBranchlist(List<MercurialBranch> list) {
        String output = "";
        for (MercurialBranch branch: list) {
            output += branch.getBranchName() + "\t" +
                    Integer.toString(branch.getRevision()) + "\t" +
                    branch.getHash() + "\n";
        }
        return output;
    }
}

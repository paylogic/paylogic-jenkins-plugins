package org.paylogic.jenkins.advancedmercurial;


import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.plugins.mercurial.MercurialSCM;
import lombok.extern.java.Log;
import org.paylogic.jenkins.advancedmercurial.exceptions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Custom class
 * for handling branches and merges with Mercurial repositories in Jenkins.
 * Maybe one day we should try to merge this back to the Jenkins plugin, as this is not really a plugin.
 */
@Log
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

    /**
     * Pretty printable String with MercurialBranch objects.
     * @param list The list of MercurialBranch objects to prettyprint.
     * @return String which is pretty
     */
    public static String prettyPrintBranchlist(List<MercurialBranch> list) {
        String output = "";
        for (MercurialBranch branch: list) {
            output += branch.getBranchName() + "\t" +
                    Integer.toString(branch.getRevision()) + "\t" +
                    branch.getHash() + "\n";
        }
        return output;
    }

    /**
     * Get the current branch name in the workspace. Executes 'hg branch'
     * @return String with branch name in it.
     */
    public String getBranch() {
        String branchName = "";
        try {
            String[] command = {this.hgExe, "branch"};
            branchName = this.executor.runCommandClean(command);
        } catch (Exception e) {
            return "";
        }
        return branchName;
    }

    /**
     * Updates workspace to given revision/branch. Executes hg update <revision>.
     * @param revision : String with revision, hash of branchname to update to.
     */
    public void update(String revision) throws MercurialException {
        String output = "";
        try {
            String[] command = {this.hgExe, "update", revision};
            output = this.executor.runCommandClean(command);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception occured during update of workspace.", e);
        }
        log.info(output);

        if (output.contains("abort: unknown revision")) {
            throw new UnknownRevisionException(output);
        } else if (output.contains("abort:")) {
            throw new MercurialException(output);
        }
    }

    /**
     * Commit the workspace changes with the given message. Executes hg commit -m <message>.
     * @param message : String with message to give this commit.
     */
    public void commit(String message) throws MercurialException {
        String output = "";
        try {
            String[] command = {this.hgExe, "commit", "-m", message};
            output = this.executor.runCommand(command);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception occured while trying to commit workspace changes.");
        }
        log.info(output);

        if (output.contains("nothing changed")) {
            throw new NothingChangedException(output);
        } else if (output.contains("abort:")) {
            throw new MercurialException(output);
        }
    }

    /**
     * Merge current workspace with given revision. Executes hg merge <revision>.
     * Do not forget to commit merge afterwards manually.
     * @param revision : String with revision, hash or branchname to merge with.
     * @return String : Output of merge command (should be empty if all went well)
     */
    public String mergeWorkspaceWith(String revision) throws MercurialException {
        String output = "";
        try {
            String[] command = {this.hgExe, "merge", revision};
            output = this.executor.runCommand(command);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception occured during merge of workspace with " + revision + ".", e);
        }

        if (output.contains("abort: merging") && output.contains("has no effect")) {
            throw new MergeWontHaveEffectException(output);
        } else if (output.contains("abort:")) {
            throw new MercurialException(output);
        }

        return output;
    }

    /**
     * Update workspace to 'updateTo' and then merge that workspace with 'revision'.
     * Executes hg update <updateTo> && hg merge <revision>.
     * Do not forget to commit merge afterwards manually.
     * @param revision : String with revision, hash or branchname to merge with.
     * @param updateTo : String with revision, hash or branchname to update to before merge.
     * @return String : output of command run.
     */
    public String mergeWorkspaceWith(String revision, String updateTo) throws MercurialException {
        this.update(updateTo);
        return this.mergeWorkspaceWith(revision);
    }


    /**
     * Executes 'hg push'
     */
    public String push() throws MercurialException {
        String output = "";
        try {
            String[] command = {this.hgExe, "push"};
            output = this.executor.runCommand(command);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Execption during push :(", e);
        }

        if (output.contains("abort: push creates new remote head")) {
            throw new PushCreatesNewRemoteHeadException(output);
        } else if (output.contains("abort:")) {
            throw new MercurialException(output);
        }

        return output;
    }

    /**
     * Executes 'hg pull'
     * @throws MercurialException
     */
    public String pull() throws MercurialException {
        String output = "";
        try {
            String[] command = {this.hgExe, "pull"};
            output = this.executor.runCommand(command);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error durign mercuruial command exceution");
        }

        if (output.contains("abort:")) {
            throw new MercurialException(output);
        }
        return output;
    }

    @Deprecated
    public String push(String revision) throws MercurialException {
        return this.push();
    }

}

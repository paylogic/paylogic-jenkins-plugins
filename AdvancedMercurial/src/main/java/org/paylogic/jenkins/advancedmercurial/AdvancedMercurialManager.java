package org.paylogic.jenkins.advancedmercurial;


import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.plugins.mercurial.MercurialSCM;
import lombok.extern.java.Log;
import org.paylogic.jenkins.advancedmercurial.exceptions.*;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Custom class for handling branches and merges with Mercurial repositories in Jenkins.
 * Maybe one day we should try to merge this back to the Jenkins Mercurial plugin, as this is not really a plugin.
 */
@Log
public class AdvancedMercurialManager {

    private String hgExe;
    private AbstractBuild build;
    private Launcher launcher;
    private ExecutionHelper executor;
    private BuildListener buildListener;
    private PrintStream l;

    public AdvancedMercurialManager(AbstractBuild build, Launcher launcher, BuildListener listener) throws Exception {
        this.hgExe = new MercurialSCM.DescriptorImpl().getHgExe();
        this.build = build;
        this.launcher = launcher;
        this.buildListener = listener;
        this.l = listener.getLogger();

        String givenRepoSubdir = Util.replaceMacro("$REPO_SUBDIR", build.getEnvVars());
        if (givenRepoSubdir.equals("$REPO_SUBDIR")) {
            // var has not changed
            givenRepoSubdir = "";
        }
        this.executor = new ExecutionHelper(launcher, listener, this.build.getEnvVars(),
                new FilePath(new File(this.build.getWorkspace() + givenRepoSubdir)));
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
            rawBranches = this.executor.runCommand(command).stdout;
        } catch (Exception e) {
            l.append(e.toString());
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
            branchName = this.executor.runCommandClean(command).stdout;
        } catch (Exception e) {
            l.append(e.toString());
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
            output = this.executor.runCommand(command).stdout;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception occured during update of workspace.", e);
            l.append(e.toString());
        }
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
            output = this.executor.runCommand(command).stdout;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception occured while trying to commit workspace changes.");
            l.append(e.toString());
        }

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
        CommandResult output = null;
        try {
            String[] command = {this.hgExe, "merge", revision};
            output = this.executor.runCommand(command);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception occured during merge of workspace with " + revision + ".", e);
            l.append(e.toString());
        }

        if (output.stdout.contains("abort: merging") && output.stdout.contains("has no effect")) {
            throw new MergeWontHaveEffectException(output.stdout);
        } else if (output.stdout.contains("abort:")) {
            throw new MercurialException(output.stdout);
        }

        return output.stdout;
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
            output = this.executor.runCommand(command).stdout;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Execption during push :(", e);
            l.append(e.toString());
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
            output = this.executor.runCommand(command).stdout;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error during Mercurial command exceution");
            l.append(e.toString());
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

package org.paylogic.jenkins.upmerge.releasebranch;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Abstract class to represent a Release Branch.
 * Implement one for your use case by filling the ExtensionPoint.
 */
public abstract class ReleaseBranch implements ExtensionPoint {
    private final String startBranch;

    /**
     * Constructor of ReleaseBranch.
     * Split the name of the branch if needed and store locally.
     * @param startBranch Name of branch to start with (String).
     */
    public ReleaseBranch(String startBranch) throws ReleaseBranchInvalidException {
        this.startBranch = startBranch;
    }

    /**
     * Sets the object to the next release.
     * Does not return representation of release.
     */
    public abstract void next();

    /**
     * Sets the object to the previous release.
     * Does not return representation of release.
     */
    public abstract void previous();

    /**
     * Returns the current branch name as String
     * Output need to be able to be consumed by constructor of ReleaseBranch.
     * @return current branch name
     */
    public abstract String getName();

    /**
     * Create a new ReleaseBranch object with the current release branch.
     * @return new ReleaseBranch
     */
    public abstract ReleaseBranch copy() throws ReleaseBranchInvalidException;


    public static ExtensionList<ReleaseBranch> all() {
        return Jenkins.getInstance().getExtensionList(ReleaseBranch.class);
    }
}


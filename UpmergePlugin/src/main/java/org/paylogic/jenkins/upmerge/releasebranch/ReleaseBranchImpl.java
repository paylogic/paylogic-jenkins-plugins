package org.paylogic.jenkins.upmerge.releasebranch;

import hudson.Extension;
import org.paylogic.fogbugz.FogbugzConstants;

import java.text.DecimalFormat;

public class ReleaseBranchImpl extends ReleaseBranch {

    private final DecimalFormat df;

    private int year;
    private int week;

    /**
     * Constructor of ReleaseBranch.
     * Split the name of the branch if needed and store locally.
     *
     * @param startBranch Name of branch to start with (String).
     */
    public ReleaseBranchImpl(String startBranch) throws ReleaseBranchInvalidException {
        super(startBranch);
        if (!startBranch.matches(FogbugzConstants.RELEASEBRANCH_REGEX)) {
            throw new ReleaseBranchInvalidException("Release branch " + startBranch + " is invalid.");
        }
        this.df = new DecimalFormat("00");

        String sYear = startBranch.substring(1, 3);
        String sWeek = startBranch.substring(3, 5);

        this.year = Integer.parseInt(sYear);
        this.week = Integer.parseInt(sWeek);
    }

    /**
     * Sets the object to the next release.
     * Does not return representation of release.
     */
    @Override
    public void next() {
        this.week += 2;
        if (this.week > 52) {
            this.week = 0;
            this.year++;
        }
    }

    /**
     * Sets the object to the previous release.
     * Does not return representation of release.
     */
    @Override
    public void previous() {
        this.week -= 2;
        if (this.week < 0) {
            this.week = 52;
            this.year--;
        }
    }

    /**
     * Returns the current branch name as String
     *
     * @return current branch name
     */
    @Override
    public String getName() {
        return String.format("r%s%s", df.format(this.year), df.format(this.week));
    }

    /**
     * Create a new ReleaseBranch object with the current release branch.
     *
     * @return new ReleaseBranch
     */
    @Override
    public ReleaseBranch copy() throws ReleaseBranchInvalidException {
        return new ReleaseBranchImpl(this.getName());
    }
}

package org.paylogic.jenkins.advancedmercurial;

import lombok.Getter;

/**
 * Container to store command results which is:
 * - an integer, exitcode
 * - a String, the stdout of the command
 * - another String, the stderr of the command
 */
public class CommandResult {
    @Getter public final int exitcode;
    @Getter public final String stdout;
    @Getter public final String stderr;

    public CommandResult(int exitcode, String stdout) {
        this.exitcode = exitcode;
        this.stdout = stdout;
        this.stderr = "";
    }

    public CommandResult(int exitcode, String stdout, String stderr) {
        this.exitcode = exitcode;
        this.stdout = stdout;
        this.stderr = stderr;
    }
}

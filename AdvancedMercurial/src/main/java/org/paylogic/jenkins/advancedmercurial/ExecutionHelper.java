package org.paylogic.jenkins.advancedmercurial;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.BuildListener;
import lombok.extern.java.Log;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;

@Log
public class ExecutionHelper {
    private FilePath workingDirectory;
    private Launcher launcher;
    private BuildListener buildListener;
    private PrintStream l;
    private Map envVars;

    public ExecutionHelper(Launcher launcher, BuildListener listener, Map envVars, FilePath workingDirectory) {
        this.launcher = launcher;
        this.buildListener = listener;
        this.l = listener.getLogger();
        this.workingDirectory = workingDirectory;
        this.envVars = envVars;
    }

    /**
     * Runs a given command inside the build enviroment.
     * @param command A StringArray with the command and its parameters.
     * @return A String with the output of the command.
     * @throws IOException
     * @throws InterruptedException
     */
    public CommandResult runCommand(String[] command) throws IOException, InterruptedException {
        /* Prepare command */
        OutputStream os = new ByteArrayOutputStream();
        OutputStream es = new ByteArrayOutputStream();
        Launcher.ProcStarter p = this.launcher.launch().stdout(os).stderr(es);
        Proc pr = null;
        int returnCode = 0;
        log.info("Executing command '" + StringUtils.join(command, " ") + "'");

        /* Actually run command */
        try {
            returnCode = p.cmds(command).envs(this.envVars).pwd(this.workingDirectory).join();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Command terminated prematurely...", e);
            l.append(e.toString() + " : " + e.getMessage());
        }

        /* Parse, log and return output */
        String output = os.toString();
        String error = es.toString();
        os.close();
        es.close();
        log.info("Command exit code: " + Integer.toString(returnCode));
        log.info("Response from command is: " + output);
        log.info("StdErr from command is: " + error);
        l.append(output);

        return new CommandResult(returnCode, output, error);
    }

    /**
     * Runs the given command inside the build environment.
     * @param command String with command in it. Will be splitted by space characters,
     *                so use a String[] if your arguments contain spaces.
     * @return String with output of build.
     * @throws IOException
     * @throws InterruptedException
     */
    public CommandResult runCommand(String command) throws IOException, InterruptedException {
        String[] splittedCommand = command.split(" ");
        return this.runCommand(splittedCommand);
    }

    /**
     * Runs the given command, and strips all whitespace from the output.
     */
    public CommandResult runCommandClean(String[] command) throws IOException, InterruptedException {
        CommandResult output = this.runCommand(command);
        return new CommandResult(output.exitcode, this.clean(output.stdout), output.stderr);
    }

    /**
     * Runs the given command, and strips all whitespace from the output.
     * String version, splits by space characters.
     */
    public CommandResult runCommandClean(String command) throws IOException, InterruptedException {
        CommandResult output = this.runCommand(command);
        return new CommandResult(output.exitcode, this.clean(output.stdout), output.stderr);
    }

    /**
     * Helper function to clean output from commands.
     */
    private String clean(String input) {
        return input.toString().replaceAll("\\s", "");
    }
}

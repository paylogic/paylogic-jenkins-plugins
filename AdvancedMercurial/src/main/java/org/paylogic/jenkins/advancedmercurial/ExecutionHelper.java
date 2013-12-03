package org.paylogic.jenkins.advancedmercurial;

import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.Channel;
import lombok.extern.java.Log;
import org.apache.commons.lang.StringUtils;

import java.io.*;

@Log
public class ExecutionHelper {
    private AbstractBuild build;
    private Launcher launcher;
    private BuildListener buildListener;
    private PrintStream l;

    public ExecutionHelper(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.buildListener = listener;
        this.l = listener.getLogger();
    }

    /**
     * Runs a given command inside the build enviroment.
     * @param command A StringArray with the command and its parameters.
     * @return A String with the output of the command.
     * @throws IOException
     * @throws InterruptedException
     */
    public String runCommand(String[] command) throws IOException, InterruptedException {
        OutputStream os = new ByteArrayOutputStream();
        Launcher.ProcStarter p = this.launcher.launch();
        Proc pr = null;
        try {
            pr = p.cmds(command).stdout(os).envs(this.build.getEnvVars()).start();
        } catch (Exception e) {
            log.info("Command terminated prematurely...");
        }

        String output = os.toString();
        os.close();
        log.info("Response from command is: " + output);
        l.append(output);

        return output;
    }

    /**
     * Runs the given command inside the build environment.
     * @param command String with command in it. Will be splitted by space characters,
     *                so use a String[] if your arguments contain spaces.
     * @return String with output of build.
     * @throws IOException
     * @throws InterruptedException
     */
    public String runCommand(String command) throws IOException, InterruptedException {
        String[] splittedCommand = command.split(" ");
        return this.runCommand(splittedCommand);
    }

    /**
     * Runs the given command, and strips all whitespace from the output.
     */
    public String runCommandClean(String[] command) throws IOException, InterruptedException {
        String output = this.runCommand(command);
        return this.clean(output);
    }

    /**
     * Runs the given command, and strips all whitespace from the output.
     * String version, splits by space characters.
     */
    public String runCommandClean(String command) throws IOException, InterruptedException {
        String output = this.runCommand(command);
        return this.clean(output);
    }

    /**
     * Helper function to clean output from commands.
     */
    private String clean(String input) {
        return input.toString().replaceAll("\\s", "");
    }
}

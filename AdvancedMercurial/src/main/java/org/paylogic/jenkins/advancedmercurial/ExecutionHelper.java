package org.paylogic.jenkins.advancedmercurial;

import hudson.Launcher;
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

    public String runCommand(String[] command) throws IOException, InterruptedException {
        OutputStream os = new ByteArrayOutputStream();
        try {
            this.launcher.launchChannel(command, os, build.getWorkspace(), build.getEnvVars());
        } catch (Exception e) {
            log.info("Command terminated prematurely...");
        }

        String output = os.toString();
        log.info("Response from command is: " + output);
        l.append(output);

        return output;
    }

    public String runCommand(String command) throws IOException, InterruptedException {
        String[] splittedCommand = command.split(" ");
        return this.runCommand(splittedCommand);
    }

    public String runCommandClean(String[] command) throws IOException, InterruptedException {
        String output = this.runCommand(command);
        return this.clean(output);
    }

    public String runCommandClean(String command) throws IOException, InterruptedException {
        String output = this.runCommand(command);
        return this.clean(output);
    }

    private String clean(String input) {
        return input.toString().replaceAll("\\s", "");
    }
}

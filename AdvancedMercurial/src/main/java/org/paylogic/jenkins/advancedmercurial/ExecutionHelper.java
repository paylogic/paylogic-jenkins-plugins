package org.paylogic.jenkins.advancedmercurial;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import lombok.extern.java.Log;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

@Log
public class ExecutionHelper {
    private AbstractBuild build;
    private Launcher launcher;


    public ExecutionHelper(AbstractBuild build, Launcher launcher) {
        this.build = build;
        this.launcher = launcher;
    }

    public String runCommand(String[] command) throws IOException, InterruptedException {
        OutputStream os = new ByteArrayOutputStream();
        try {
            this.launcher.launchChannel(command, os, this.build.getWorkspace(), this.build.getEnvVars());
        } catch (EOFException e) {
            log.info("End of file reached, command is done.");
        }

        String output = os.toString();
        log.info("Response from command is: " + output);

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

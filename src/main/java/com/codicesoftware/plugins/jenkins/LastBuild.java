package com.codicesoftware.plugins.jenkins;

import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class LastBuild {

    private LastBuild() {
    }

    @CheckForNull
    public static Run<?, ?> get(@Nonnull final Item owner) {
        for (Job<?, ?> job : owner.getAllJobs()) {
            if (job == null) {
                continue;
            }

            Run<?, ?> run = job.getLastBuild();
            if (run != null) {
                return run;
            }
        }
        return null;
    }

}

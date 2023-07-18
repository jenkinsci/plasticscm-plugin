package com.codicesoftware.plugins.jenkins;

import hudson.AbortException;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

public class AbortExceptionBuilder {

    private AbortExceptionBuilder() {
    }

    public static AbortException build(
            @Nonnull final Logger logger, @Nonnull final TaskListener listener, @Nonnull final Exception e) {
        listener.fatalError(e.getMessage());
        logger.severe(e.getMessage());
        AbortException result = new AbortException(e.getMessage());
        result.initCause(e);
        return result;
    }
}

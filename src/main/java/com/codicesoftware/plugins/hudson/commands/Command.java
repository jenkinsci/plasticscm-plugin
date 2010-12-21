package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;

public interface Command {

    /**
     * Returns the arguments to be sent to the cm command line client
     * @return arguments for the cm tool
     */
    MaskedArgumentListBuilder getArguments();
}
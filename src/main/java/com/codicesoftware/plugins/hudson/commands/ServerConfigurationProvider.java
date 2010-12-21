package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import java.io.IOException;
import java.io.Reader;

public interface ServerConfigurationProvider {
    public Reader execute(MaskedArgumentListBuilder args) throws IOException, InterruptedException;
}

package com.codicesoftware.plugins.hudson.commands;

import com.codicesoftware.plugins.hudson.util.MaskedArgumentListBuilder;
import hudson.FilePath;

public class UndoCheckoutCommand implements Command {
    public UndoCheckoutCommand(FilePath wkPath) {
        mWkPath = wkPath;
    }

    @Override
    public MaskedArgumentListBuilder getArguments() {
        MaskedArgumentListBuilder arguments = new MaskedArgumentListBuilder();

        arguments.add("unco");
        arguments.add("--all");
        arguments.add(mWkPath.getRemote());

        return arguments;
    }

    FilePath mWkPath;
}

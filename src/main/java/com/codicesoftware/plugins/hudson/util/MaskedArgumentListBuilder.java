package com.codicesoftware.plugins.hudson.util;

import hudson.util.ArgumentListBuilder;
import java.util.Collection;
import java.util.HashSet;

/**
 * ArgumentListBuilder that supports marking arguments as masked.
 *
 * @author Erik Ramfelt
 * @author Dick Porter
 */
public class MaskedArgumentListBuilder extends ArgumentListBuilder {
    private static final long serialVersionUID = 1L;

    private Collection<Integer> maskedArgumentIndex;

    @Override
    public ArgumentListBuilder prepend(String... args) {
        if (maskedArgumentIndex != null) {
            Collection<Integer> newMaskedArgumentIndex = new HashSet<Integer>();

            for (Integer argIndex : maskedArgumentIndex) {
                newMaskedArgumentIndex.add(argIndex + args.length);
            }
            maskedArgumentIndex = newMaskedArgumentIndex;
        }

        return super.prepend(args);
    }

    @Override
    public boolean hasMaskedArguments() {
        return (maskedArgumentIndex != null);
    }

    @Override
    public boolean[] toMaskArray() {
        String[] commands = toCommandArray();
        boolean[] mask = new boolean[commands.length];
        if (maskedArgumentIndex != null) {
            for (Integer argIndex : maskedArgumentIndex) {
                mask[argIndex] = true;
            }
        }

        return mask;
    }

    @Override
    public void addMasked(String string) {
        if (maskedArgumentIndex == null) {
            maskedArgumentIndex = new HashSet<Integer>();
        }
        maskedArgumentIndex.add(toCommandArray().length);
        add(string);
    }
}
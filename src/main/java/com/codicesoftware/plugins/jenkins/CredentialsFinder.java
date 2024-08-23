package com.codicesoftware.plugins.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.security.ACL;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class CredentialsFinder {

    private CredentialsFinder() {
    }

    @CheckForNull
    public static StandardUsernamePasswordCredentials getFromId(
            @CheckForNull String credentialsId,
            @Nullable Item item) {

        if (Util.fixEmpty(credentialsId) == null) {
            return null;
        }

        return CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentialsInItem(
                StandardUsernamePasswordCredentials.class,
                item,
                item instanceof Queue.Task ? ((Queue.Task) item).getDefaultAuthentication2() : ACL.SYSTEM2,
                URIRequirementBuilder.create().build()),
            CredentialsMatchers.withId(credentialsId));
    }
}

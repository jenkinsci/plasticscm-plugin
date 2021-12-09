package com.codicesoftware.plugins.hudson.util;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public final class FormFiller {
    // hide default constructor
    private FormFiller() { }

    public static ListBoxModel doFillCredentialsIdItems(Item item, String credentialsId) {
        StandardListBoxModel result = new StandardListBoxModel();
        if (item == null) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return result.includeCurrentValue(credentialsId);
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ)
                && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return result.includeCurrentValue(credentialsId);
            }
        }
        return result
            .includeEmptyValue()
            .includeMatchingAs(
                item instanceof Queue.Task
                    ? Tasks.getAuthenticationOf((Queue.Task) item)
                    : ACL.SYSTEM,
                item,
                StandardUsernamePasswordCredentials.class,
                // No domain requirements
                URIRequirementBuilder.create().build(),
                CredentialsMatchers.always()
            )
            .includeCurrentValue(credentialsId);
    }
}

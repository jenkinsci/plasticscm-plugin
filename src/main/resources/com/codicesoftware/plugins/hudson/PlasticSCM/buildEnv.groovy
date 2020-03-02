package com.codicesoftware.plugins.hudson

def l = namespace(lib.JenkinsTagLib)

["PLASTICSCM_CHANGESET_ID","PLASTICSCM_CHANGESET_GUID","PLASTICSCM_BRANCH","PLASTICSCM_AUTHOR","PLASTICSCM_REPSPEC"].each { name ->
    l.buildEnvVar(name:name) {
        raw(_("${name}.blurb"))
    }
}

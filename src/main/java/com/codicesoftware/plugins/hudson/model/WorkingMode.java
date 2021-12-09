package com.codicesoftware.plugins.hudson.model;

public enum WorkingMode {
    NONE("Use system configuration", null),
    UP("User & password", "UPWorkingMode"),
    LDAP("LDAP / Cloud", "LDAPWorkingMode");

    private final String label;
    private final String plasticWorkingMode;

    WorkingMode(String label, String plasticWorkingMode) {
        this.label = label;
        this.plasticWorkingMode = plasticWorkingMode;
    }

    public String getLabel() {
        return label;
    }

    public String getPlasticWorkingMode() {
        return plasticWorkingMode;
    }

    @Override
    public String toString() {
        return label;
    }
}

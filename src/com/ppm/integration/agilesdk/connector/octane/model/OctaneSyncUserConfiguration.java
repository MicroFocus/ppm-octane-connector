package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.List;

/**
 * {@code OctaneSyncUserConfiguration} Main class of
 * "OctaneSyncUserConfiguration", used to sync users from Agile tool to PPM.
 * @since PPM 10.0.2
 */
public class OctaneSyncUserConfiguration {

    private List<UserSecurityConfiguration> security;

    private OctaneProject agileProjectValue;

    public List<UserSecurityConfiguration> getSecurity() {
        return security;
    }

    public void setSecurity(List<UserSecurityConfiguration> security) {
        this.security = security;
    }

    public OctaneProject getAgileProjectValue() {
        return agileProjectValue;
    }

    public void setAgileProjectValue(OctaneProject agileProjectValue) {
        this.agileProjectValue = agileProjectValue;
    }

}

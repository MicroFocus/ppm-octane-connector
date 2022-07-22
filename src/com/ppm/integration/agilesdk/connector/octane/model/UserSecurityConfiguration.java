package com.ppm.integration.agilesdk.connector.octane.model;

import java.util.List;

/**
 * {@code UserSecurityConfiguration} Main class of user integration which
 * contains information to add security groups and product license to users from
 * agile.
 * @since PPM 10.0.2
 */
public class UserSecurityConfiguration {
    private String role;
    private String permission;
    
    private List<String> securityGroups;
    
    private List<Long> productLicenses;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(List<String> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public List<Long> getProductLicenses() {
        return productLicenses;
    }

    public void setProductLicenses(List<Long> productLicenses) {
        this.productLicenses = productLicenses;
    }
    
    
    
    

}

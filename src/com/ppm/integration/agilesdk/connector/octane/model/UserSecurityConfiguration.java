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

    private List<String> licenseTypes;
    
    private List<String> securityGroups;
    
    private List<Long> productLicenses;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getLicenseTypes() {
        return licenseTypes;
    }

    public void setLicenseTypes(List<String> licenseTypes) {
        this.licenseTypes = licenseTypes;
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

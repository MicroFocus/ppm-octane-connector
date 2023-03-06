package com.ppm.integration.agilesdk.connector.octane.client;

public enum PermissionLogicName {
    STRATEGY("perm.change.scmusersassignmenttome");

    private String code;

    PermissionLogicName(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public static PermissionLogicName decode(final String code) {
        for (final PermissionLogicName type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}

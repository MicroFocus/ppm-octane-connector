package com.ppm.integration.agilesdk.connector.octane;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;


import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataLicense;
import com.ppm.integration.agilesdk.agiledata.AgileDataUser;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.LicenseType;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneSyncUserConfiguration;
import com.ppm.integration.agilesdk.connector.octane.model.Permission;
import com.ppm.integration.agilesdk.connector.octane.model.PermissionData;
import com.ppm.integration.agilesdk.connector.octane.model.RoleData;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpaceUser;
import com.ppm.integration.agilesdk.connector.octane.model.UserSecurityConfiguration;
import com.ppm.integration.agilesdk.connector.octane.model.WorkspaceRole;
import com.ppm.integration.agilesdk.user.UserIntegration;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class OctaneUserIntegration extends UserIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    private static final String DELETED = "-DELETED";

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * @param instanceConfigurationParameters
     * @param agileProjectValue
     *            {"security":[{"role":"role_logical_Name","securityGroups":["securityGroupReferenceCode1"],"productLicenses":[productId1]},{"licenseType":"licenseTypeId","securityGroups":["securityGroupReferenceCode2"],"productLicenses":[productId2]}],"agileProjectValue":{"workspaceId":1003,"sharedSpaceId":1003}}
     * @param queryParams
     * @return
     * @see com.ppm.integration.agilesdk.user.UserIntegration#getAgileDataUsers(com.ppm.integration.agilesdk.ValueSet,
     *      java.lang.String, java.util.Map)
     */
    @Override
    public List<AgileDataUser> getAgileDataUsers(final ValueSet instanceConfigurationParameters,
            final String agileProjectValue, Map<String, Object> queryParams)
    {
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        JSONObject agileProjectJson = (JSONObject)JSONSerializer.toJSON(agileProjectValue);

        Map<String, Class<?>> classMap = new HashMap<>();
        classMap.put("security", UserSecurityConfiguration.class);
        OctaneSyncUserConfiguration userConfiguration = (OctaneSyncUserConfiguration)agileProjectJson
                .toBean(agileProjectJson, OctaneSyncUserConfiguration.class, classMap);

        String sharedSpaceId = String.valueOf(userConfiguration.getAgileProjectValue().getSharedSpaceId());

        String filter = getUserQueryFilter(queryParams);
        Long offset = null;
        Long limit = null;
        try {
            if (queryParams.containsKey("offset") && queryParams.containsKey("limit")) {
                offset = Long.parseLong((String)queryParams.get("offset"));
                limit = Long.parseLong((String)queryParams.get("limit"));
            }

        } catch (NumberFormatException e) {
            logger.error("Exception when parsing query parameter", e);
        }


        List<SharedSpaceUser> usersList = client.getUsersWithSearchFilter(sharedSpaceId, limit, offset, filter);


        List<AgileDataUser> users = new ArrayList<>();

        for (SharedSpaceUser spaceUser : usersList) {

            AgileDataUser user = new AgileDataUser();
            user.setUserId(spaceUser.getId());
            int activityLevel = Integer.valueOf(spaceUser.getActivityLevel());

            if (activityLevel == OctaneConstants.USER_DELETED_STATUS_CODE) {
                user.setUserName(spaceUser.getId() + DELETED);
            } else {
                user.setUserName(spaceUser.getName());
            }

            user.setFirstName(spaceUser.getFirstName());
            user.setLastName(spaceUser.getLastName());
            user.setEmail(spaceUser.getEmail());
            WorkspaceRole role = spaceUser.getWorkspaceRoles();
            // US#673013 improve user sync performance - use license type
            // instead of permissions
            LicenseType licenseType = spaceUser.getLicenseType();

            List<String> roleList = getRoleListOfUser(role);

            if (activityLevel == OctaneConstants.USER_ACTIVITY_STATUS_CODE) {
                addSecurityGroupAndLicenseToUsers(user, roleList, licenseType,
                        userConfiguration.getSecurity());
                user.setEnabledFlag(true);
            } else {
                user.setEnabledFlag(false);
            }

            try {
                Date date = sdf.parse(spaceUser.getLastModified());
                user.setLastUpdateDate(date);

            } catch (final ParseException e) {
                logger.error(e.getMessage());
            }

            users.add(user);
        }

        return users;
    }

    private Date parseDateTimeString(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        ZonedDateTime date = ZonedDateTime.parse(dateString, formatter);
        return Date.from(date.toInstant());
    }

    @Override
    public List<AgileDataLicense> getTenantLicenses(final ValueSet instanceConfigurationParameters) {
        List<AgileDataLicense> datas = new ArrayList<>();
        ClientPublicAPI client = ClientPublicAPI.getClient(instanceConfigurationParameters);
        SharedSpace space = client.getActiveSharedSpace();
        if (space == null) {
            return datas;
        }
        List<JSONObject> licenseJSONObjs = client.getTenantLicenses(space.getId());
        for (JSONObject obj : licenseJSONObjs) {
            AgileDataLicense data = new AgileDataLicense();
            data.setType(obj.getString("type"));
            data.setId(obj.getString("id"));
            data.setLicenseId(obj.getString("license_id"));
            data.setExpirationDate(parseDateTimeString(obj.getString("expiration_date")));
            data.setStartDate(parseDateTimeString(obj.getString("start_date")));
            data.setEditions(obj.getString("editions"));
            data.setConsumed(obj.getLong("consumed"));
            data.setLicenseModel(obj.getString("license_model"));
            data.setLicenseType(obj.getString("license_type"));
            data.setCapacity(obj.getLong("capacity"));
            datas.add(data);
        }
        return datas;
    }

    private List<String> parsePermissions(Permission permission) {
        List<String> permissionLogicalNames = new ArrayList<>();
        if (permission != null) {
            for (PermissionData data : permission.getData()) {
                permissionLogicalNames.add(data.getLogicalName());
            }
        }

        return permissionLogicalNames;
    }



    private List<String> getRoleListOfUser(WorkspaceRole role) {
        List<String> roleList = new ArrayList<>();
        if (role != null) {
            for (RoleData data : role.getData()) {
                if (data.getWorkspace() == null) {
                    roleList.add(data.getRole().getLogicalName());
                }
            }
        }


        return roleList;
    }

    /**
     * @param user
     * @param roleList contain user role information.
     * @param agileProjectJson
     *            {"security":[{"role":"role_logical_Name","securityGroups":["securityGroupReferenceCode1"],"productLicenses":[productId1]},{"permission":"permission_logical_name","securityGroups":["securityGroupReferenceCode2"],"productLicenses":[productId2]}],"agileProjectValue":{"workspaceId":1003,"sharedSpaceId":1003}}
     */
    private void addSecurityGroupAndLicenseToUsers(AgileDataUser user, List<String> roleList,
            LicenseType userLicenseType,
            List<UserSecurityConfiguration> security)
    {

        for (UserSecurityConfiguration securityConf : security) {
            // configure in table ppm_int_agile_user_sync
            String role = securityConf.getRole();
            List<String> licenseTypes = securityConf.getLicenseTypes();

            if (role != null) {
                if (roleList.contains(role)) {
                    user.addAllSecurityGroupCodes(securityConf.getSecurityGroups());
                    user.addAllProductIds(securityConf.getProductLicenses());

                }
            }

            if (userLicenseType != null) {
                if (licenseTypes.contains(userLicenseType.getId())) {
                    user.addAllSecurityGroupCodes(securityConf.getSecurityGroups());
                    user.addAllProductIds(securityConf.getProductLicenses());
                }

            }

        }

    }

    private String getUserQueryFilter(Map<String, Object> queryParams) {
        String filter = "\"";
        Date lastUpdateTime = (Date)queryParams.get("last_modified");
        if (lastUpdateTime != null) {
            String formatDate = sdf.format(lastUpdateTime);

            filter += "last_modified > '" + formatDate + "' ; ";
        }

        // filter out api access
        filter += "is_api_key=false\"";

        try {
            filter = URLEncoder.encode(filter, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }

        return filter;
    }

}

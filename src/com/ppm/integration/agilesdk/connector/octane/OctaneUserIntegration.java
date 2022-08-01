package com.ppm.integration.agilesdk.connector.octane;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataUser;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneSyncUserConfiguration;
import com.ppm.integration.agilesdk.connector.octane.model.Permission;
import com.ppm.integration.agilesdk.connector.octane.model.PermissionData;
import com.ppm.integration.agilesdk.connector.octane.model.RoleData;
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


        JsonArray userArray = client.getUsersWithSearchFilter(sharedSpaceId, limit, offset, filter);


        List<AgileDataUser> users = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement userObj : userArray) {
            SharedSpaceUser spaceUser = gson.fromJson(userObj, SharedSpaceUser.class);

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
            Permission permission = spaceUser.getPermissions();

            List<String> roleList = getRoleListOfUser(role);
            List<String> permissionLogicalNames = parsePermissions(permission);
            if (activityLevel == OctaneConstants.USER_ACTIVITY_STATUS_CODE) {
                addSecurityGroupAndLicenseToUsers(user, roleList, permissionLogicalNames,
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
            List<String> permissionLogicalNames,
            List<UserSecurityConfiguration> security)
    {

        for (UserSecurityConfiguration securityConf : security) {
            String role = securityConf.getRole();
            String permission = securityConf.getPermission();
            if (role != null) {
                if (roleList.contains(role)) {
                    user.addAllSecurityGroupCodes(securityConf.getSecurityGroups());
                    user.addAllProductIds(securityConf.getProductLicenses());

                }
            }

            if (permission != null) {
                if (permissionLogicalNames.contains(permission)) {
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

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

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.AgileDataUser;
import com.ppm.integration.agilesdk.connector.octane.client.ClientPublicAPI;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneSyncUserConfiguration;
import com.ppm.integration.agilesdk.connector.octane.model.UserSecurityConfiguration;
import com.ppm.integration.agilesdk.user.UserIntegration;

import net.sf.json.JSONArray;
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

        String workSpaceId = userConfiguration.getAgileProjectValue().getWorkspaceId() == null ? null
                : String.valueOf(userConfiguration.getAgileProjectValue().getWorkspaceId());
        String sharedSpaceId = String.valueOf(userConfiguration.getAgileProjectValue().getSharedSpaceId());

        String filter = getUserQueryFilter(queryParams, null);
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


        JSONArray userArray = client.getUsersWithSearchFilter(sharedSpaceId, workSpaceId, limit, offset, filter, false);


        Map<String, String> licenseTypeMap = new HashMap<>();
        if (isGettingWorkspaceUsers(workSpaceId)) {
            // get license type of users from sharedSpace api, which can not be
            // got by workspace api
            licenseTypeMap = getUserLicenseType(client, sharedSpaceId, userArray);

            addMissingUsersForIncrementalSync(client, queryParams, sharedSpaceId, workSpaceId, limit,
                    userArray);

        }



        List<AgileDataUser> users = new ArrayList<>();

        for (int i = 0; i < userArray.size(); i++) {
            JSONObject userObj = userArray.getJSONObject(i);
            AgileDataUser user = new AgileDataUser();
            user.setUserId(userObj.getString("id"));

            int activityLevel = userObj.getInt("activity_level");

            if (activityLevel == OctaneConstants.USER_DELETED_STATUS_CODE) {
                user.setUserName(userObj.getString("id") + DELETED);
            } else {
                user.setUserName(userObj.getString("name"));
            }

            user.setFirstName(userObj.getString("first_name"));
            user.setLastName(userObj.getString("last_name"));
            user.setEmail(userObj.getString("email"));

            List<String> roleList = getRoleListOfUser(workSpaceId, userObj);

            JSONObject permissionsObj = userObj.getJSONObject("permissions");
            JSONArray permissionsData = permissionsObj.getJSONArray("data");
            List<String> permissionLogicalNames = parsePermissions(permissionsData);
            if (activityLevel == OctaneConstants.USER_ACTIVITY_STATUS_CODE) {
                addSecurityGroupAndLicenseToUsers(user, roleList, permissionLogicalNames,
                        userConfiguration.getSecurity());
                user.setEnabledFlag(true);
            } else {
                user.setEnabledFlag(false);
            }


            try {
                Date date = sdf.parse(userObj.getString("last_modified"));
                user.setLastUpdateDate(date);

            } catch (final ParseException e) {
                logger.error(e.getMessage());
            }


            users.add(user);
        }

        return users;
    }

    private List<String> parsePermissions(JSONArray permissionsData) {
        List<String> permissionLogicalNames = new ArrayList<>();
        for (int i = 0; i < permissionsData.size(); i++) {
            JSONObject permissionObj = permissionsData.getJSONObject(i);
            String logicalName = permissionObj.getString("logical_name");
            permissionLogicalNames.add(logicalName);
        }
        return permissionLogicalNames;
    }

    /**
     * For users which have been updated license_type in sharedSpace level,
     * their last_modified ONLY been updated in sharedSpace level, thus these
     * users won't been updated by workspace api with query "last_modified >
     * stored date". By getting users by sharedSpace api and add these missing
     * users at last in this incremental synchronization.
     */
    private void addMissingUsersForIncrementalSync(ClientPublicAPI client, Map<String, Object> queryParams,
            String sharedSpaceId, String workSpaceId, Long limit, JSONArray userArray)
    {
        Date lastUpdateTime = (Date)queryParams.get("last_modified");
        // at the last time of this incremental synchronization
        if (lastUpdateTime != null && userArray.size() < limit) {
            // get incremental users from workspace api
            JSONArray userArrayByWorkpaceApi =
                    client.getUsersWithSearchFilter(sharedSpaceId, workSpaceId, null, null,
                            getUserQueryFilter(queryParams, null), true);

            // get incremental users from sharedSpace api
            String appendFilter = "workspaces={id=" + workSpaceId + "};";
            JSONArray userArrayBySharedSpaceApi = client.getUsersBySharedSpaceApi(sharedSpaceId, null, null,
                    getUserQueryFilter(queryParams, appendFilter));

            // add mission users whose license_type is updated at sharedSpace
            // level
            addMissingUsersToUserArray(userArrayByWorkpaceApi, userArrayBySharedSpaceApi, userArray);
        }
    }


    /**
     * add mission users to userArray
     */
    private JSONArray addMissingUsersToUserArray(JSONArray userArrayByWorkpaceApi, JSONArray userArrayBySharedSpaceApi,
            JSONArray userArray)
    {
        Map<String, JSONObject> userInWorkspace = new HashMap<>();
        Map<String, JSONObject> userInSharedSpace = new HashMap<>();


        for (int i = 0; i < userArrayByWorkpaceApi.size(); i++) {
            JSONObject userObj = userArrayByWorkpaceApi.getJSONObject(i);
            userInWorkspace.put(userObj.getString("id"), userObj);
        }

        for (int j = 0; j < userArrayBySharedSpaceApi.size(); j++) {
            JSONObject userObj = userArrayBySharedSpaceApi.getJSONObject(j);
            userInSharedSpace.put(userObj.getString("id"), userObj);

        }

        if (userArrayByWorkpaceApi.isEmpty() && !userArrayBySharedSpaceApi.isEmpty()) {
            for (JSONObject userJson : userInSharedSpace.values()) {
                userArray.add(userJson);
            }

        } else {
            userInSharedSpace.keySet().removeAll(userInWorkspace.keySet());
            for (String userId : userInSharedSpace.keySet()) {
                userArray.add(userInSharedSpace.get(userId));
            }

        }

        return userArray;

    }

    /**
     * get license_type of users from sharedSpace level, because this field
     * can't be got by worksapce api
     * @param client
     * @param workSpaceId
     * @param sharedSpaceId
     * @param userArray
     * @return
     */
    private Map<String, String> getUserLicenseType(ClientPublicAPI client,
            String sharedSpaceId, JSONArray userArray)
    {
        Map<String, String> licenseTypeMap = new HashMap<>();

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < userArray.size(); i++) {
            JSONObject userObj = userArray.getJSONObject(i);
            ids.add(userObj.getString("id"));
        }
        JSONArray userLicenseTypeArray = client.getUsersLicenseType(sharedSpaceId, ids);

        for (int i = 0; i < userLicenseTypeArray.size(); i++) {
            JSONObject userObj = userLicenseTypeArray.getJSONObject(i);
            JSONObject licenseType = userObj.getJSONObject("license_type");
            licenseTypeMap.put(userObj.getString("id"), licenseType.getString("id"));
        }

        return licenseTypeMap;
    }

    private boolean isGettingWorkspaceUsers(String workSpaceId) {
        return workSpaceId != null;
    }

    private List<String> getRoleListOfUser(String workSpaceId, JSONObject userObj) {
        List<String> roleList = new ArrayList<>();
        if (userObj.containsKey("workspace_roles")) {
            parseSharedSpaceRoles(workSpaceId, userObj, roleList);
        } else if (userObj.containsKey("roles")) {
            parseWorkspaceRoles(userObj, roleList);
        }

        return roleList;
    }

    private void parseWorkspaceRoles(JSONObject userObj, List<String> roleList) {
        // workspace level
        JSONObject roles = userObj.getJSONObject("roles");
        JSONArray rolesArray = roles.getJSONArray("data");
        for (int j = 0; j < rolesArray.size(); j++) {
            JSONObject roleJson = rolesArray.getJSONObject(j);
            roleList.add(roleJson.getString("logical_name"));

        }
    }

    private void parseSharedSpaceRoles(String workSpaceId, JSONObject userObj, List<String> roleList) {
        // sharedSpace level
        JSONObject workspaceRoles = userObj.getJSONObject("workspace_roles");
        JSONArray rolesArray = workspaceRoles.getJSONArray("data");
        for (int j = 0; j < rolesArray.size(); j++) {
            JSONObject roleJson = rolesArray.getJSONObject(j);
            JSONObject workspace = roleJson.getJSONObject("workspace");
            // when workspace is null, this means that the role is applied to shared
            // space level
            if (workspace.isNullObject()) {
                JSONObject role = roleJson.getJSONObject("role");
                roleList.add(role.getString("logical_name"));
            }
        }
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

    private String getUserQueryFilter(Map<String, Object> queryParams, String appendFilter) {
        String filter = "\"";
        Date lastUpdateTime = (Date)queryParams.get("last_modified");
        if (lastUpdateTime != null) {
            String formatDate = sdf.format(lastUpdateTime);

            filter += "last_modified > '" + formatDate + "' ; ";
        }

        if (appendFilter != null) {
            filter += appendFilter;
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

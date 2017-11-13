package com.ppm.integration.agilesdk.connector.octane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.ppm.dm.model.AgileEntity;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.OctaneConstants;
import com.ppm.integration.agilesdk.connector.octane.model.*;
import com.ppm.integration.agilesdk.dm.FieldValue;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;


import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * This Octane client is using the ClientID/ClientSecret authentication, and should be used wherever possible,
 * i.e. whenever it's not mandatory to know who the end user is.
 *
 * workspace ID & Shared space ID will be read from the passed parameters and used whenever needed when doing REST calls ; you can also set them manually with the setters.
 */
public class ClientPublicAPI {

    private static final String JSON_TIME_SUFFIX = "T12:00:00Z";

    private final Logger logger = Logger.getLogger(this.getClass());

    private String baseURL = "";

    private Integer workSpaceId = null;

    private Integer sharedSpaceId = null;

    private String cookies;

    private Proxy proxy = null;

    private int retryNumber = 0;

    public ClientPublicAPI(String baseUrl) {
        this.baseURL = baseUrl.trim();
        if (this.baseURL.endsWith("/")) {
            this.baseURL = this.baseURL.substring(0, this.baseURL.length() - 1);
        }
    }

    public static ClientPublicAPI getClient(ValueSet values) {
        ClientPublicAPI client = OctaneClientHelper.setupClientPublicAPI(values);
        String clientId = values.get(OctaneConstants.APP_CLIENT_ID);
        String clientSecret = values.get(OctaneConstants.APP_CLIENT_SECRET);

        if (!client.getAccessTokenWithFormFormat(clientId, clientSecret)) {
            throw new OctaneClientException("AGM_APP", "error when retrieving access token.");
        }

        return client;
    }

    public boolean getAccessTokenWithFormFormat(String clientId, String clientSecret)
    {
        String url = String.format("%s/authentication/sign_in", baseURL);
        String data =
                String.format("{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"enable_csrf\": \"true\"}", clientId,
                        clientSecret);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", MediaType.APPLICATION_JSON);
        RestResponse response = sendRequest(url, HttpMethod.POST, data, headers);
        return verifyResult(HttpStatus.SC_OK, response.getStatusCode());
    }

    public void setProxy(String host, int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }


    private RestResponse sendGet(String url) {
        return sendRequest(url, HttpMethod.GET, null);
    }

    private RestResponse sendRequest(String url, String method, String jsonData) {

        Map<String, String> headers = new HashMap<>();

        headers.put("Cookie", this.cookies);
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

        if (jsonData != null ) {
            headers.put("Content-Type", MediaType.APPLICATION_JSON);
        }

        if (!HttpMethod.GET.equals(method)) {
            String csrf = this.getCSRF(this.cookies);
            if (csrf != null) {
                headers.put("HPSSO-HEADER-CSRF", csrf);
            }
        }

        return sendRequest(url, method, jsonData, headers);
    }

    /**
     * Use this method only when you need to have full control over the header sent, for example during authentication process.
     * For standard REST API usage, use {@link #sendRequest(String, String, String)}, it will take care of everything for you.
     */
    private RestResponse sendRequest(String url, String method, String data, Map<String, String> headers)
    {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = null;
            if (this.proxy != null) {
                con = (HttpURLConnection)obj.openConnection(this.proxy);
            } else {
                con = (HttpURLConnection)obj.openConnection();
            }

            con.setRequestMethod(method);

            //set headers
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    con.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            //set data

            if (data != null) {
                con.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
                wr.write(data);
                wr.flush();
                wr.close();
            }

            int responseCode = con.getResponseCode();
            BufferedReader in;
            if (responseCode == 200 ) {
                in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                if (cookies == null) {
                    this.cookies = getCookie(con);
                }
            } else {
                InputStream inputStream = con.getErrorStream();
                if (inputStream == null) {
                    inputStream = con.getInputStream();
                }
                in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            }

            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String output = response.toString();

            if (responseCode == 401) {
                retryNumber = 0;
                logger.error("OCTANE_API: HTTP 401 Error - URL:" + url + " Result: " + output);
                throw new OctaneClientException("OCTANE_API", "ERROR_AUTHENTICATION_FAILED");
            } else if (responseCode == 403) {
                retryNumber = 0;
                logger.error("OCTANE_API: HTTP 403 Error - URL:" + url + " Result: " + output);
                throw new OctaneClientException("OCTANE_API", "ERROR_ACCESS_FAILED");
            } else if (responseCode == 400) {
                //sometimes there is a network issue, so we retry it again. If the retry number is great than 3,
                //We will throw exception
                logger.error("OCTANE_API: HTTP 400 Error - URL:" + url + " Result: " + output);
                if (retryNumber < 3) {
                    retryNumber += 1;
                    logger.error("OCTANE_API: HTTP 400 Error - This is the " + retryNumber + " time to retry.");
                    return sendRequest(url, method, data, headers);
                } else {
                    retryNumber = 0;
                    throw new OctaneClientException("OCTANE_API", "ERROR_BAD_REQUEST");
                }
            }
            retryNumber = 0;
            return new RestResponse(responseCode, output);
        } catch (IOException e) {
            retryNumber = 0;
            logger.error("error in http connectivity:", e);
            throw new OctaneClientException("AGM_APP", "error in http connectivity: "+ e.getMessage());
        }
    }

    private boolean verifyResult(int expected, int result) {
        boolean isVerify = false;
        if (expected != result) {
            logger.error("error in access token retrieve.");
            throw new OctaneClientException("OCTANE_API", "error in access token retrieve");
        } else {
            isVerify = true;
        }
        return isVerify;
    }

    public List<TimesheetItem> getTimeSheetData(int shareSpace, String userName, String startDateStr, String endDateStr,
            int workspaceId)
    {

        String url = String.format(
                "%s/api/shared_spaces/%d/timesheet?login_names=%s&start_date=%s&end_date=%s&workspace_ids=%d&task_level=true",
                baseURL, shareSpace, userName, startDateStr, endDateStr, workspaceId);

        RestResponse response = sendGet(url);

        try {
            List<TimesheetItem> items = parseTimesheetItems(response.getData());
            return items;
        } catch (Exception e) {
            logger.error("error in timesheet retrieve:", e);
            throw new OctaneClientException("AGM_APP", "error in timesheet retrieve:", e.getMessage());
        }
    }

    private List<TimesheetItem> parseTimesheetItems(String json) {
        try {
            org.json.JSONObject obj = new org.json.JSONObject(json);

            org.json.JSONArray arr = obj.getJSONArray("data");
            List<TimesheetItem> items = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject rawItem = (org.json.JSONObject)arr.get(i);
                TimesheetItem item = new TimesheetItem();
                items.add(item);
                    /* see reports/timesheet topic
                    for full list of fields. */
                int entityId = 0;
                try {
                    entityId = Integer.parseInt(rawItem.getString("entity_id"));
                } catch (Exception ignore) {
                }
                item.setEntityId(entityId);

                item.setEntityName(rawItem.getString("entity_name"));
                item.setEntityType(rawItem.getString("entity_type"));
                item.setLoginName(rawItem.getString("login_name"));
                item.setFullName(rawItem.getString("user_full_name"));

                int investedHours = 0;
                try {
                    investedHours = Integer.parseInt(rawItem.getString("invested_hours"));
                } catch (Exception ignore) {
                }
                item.setInvested(investedHours);

                item.setDate(rawItem.getString("date"));

                int sprintId = 0;
                try {
                    sprintId = Integer.parseInt(rawItem.getString("sprint_id"));
                } catch (Exception ignore) {
                }
                item.setSprintId(sprintId);

                item.setSprintName(rawItem.getString("sprint_name"));

                int releaseId = 0;
                try {
                    releaseId = Integer.parseInt(rawItem.getString("release_id"));
                } catch (Exception ignore) {
                }
                item.setReleaseId(releaseId);

                item.setReleaseName(rawItem.getString("release_name"));
            }
            return items;
        } catch (Exception e) {
            throw new RuntimeException("Error when parsing JSON", e);
        }
    }

    private String getCookie(HttpURLConnection con)
    {
        String cookieVal = "";
        String key;
        for (int i = 1; (key = con.getHeaderFieldKey(i)) != null; i++) {
            if (key.equalsIgnoreCase("set-cookie")) {
                cookieVal = cookieVal + con.getHeaderField(i) + ";";
            }
        }
        return cookieVal;
    }

    public List<SharedSpace> getSharedSpaces() {

        String url = String.format("%s/api/shared_spaces", baseURL);

        RestResponse response = sendGet(url);

        SharedSpaces tempSharedSpace = new SharedSpaces();
        try {
            tempSharedSpace.SetCollection(response.getData());
        } catch (Exception e) {
            logger.error("error in get ShareSpaces:", e);
            throw new OctaneClientException("AGM_APP", "error in get ShareSpace:", e.getMessage());
        }

        return tempSharedSpace.getCollection();
    }

    public List<WorkSpace> getWorkSpaces(int sharedSpacesId) {

        String url = String.format("%s/api/shared_spaces/%d/workspaces", baseURL, sharedSpacesId);

        RestResponse response = sendGet(url);

        WorkSpaces tempWorkSpace = new WorkSpaces();
        try {
            tempWorkSpace.SetCollection(response.getData());
        } catch (Exception e) {
            logger.error("error in get WorkSpaces:", e);
            throw new OctaneClientException("AGM_APP", "error in get WorkSpace:", e.getMessage());
        }
        return tempWorkSpace.getCollection();
    }

    public Release getRelease(int sharedSpacesId, int workSpaceId, int releaseId) {

        String url =
                String.format("%s/api/shared_spaces/%d/workspaces/%d/releases/%d", baseURL, sharedSpacesId, workSpaceId,
                        releaseId);

        RestResponse response = sendGet(url);

        Release tempRelease = new Release();
        try {
            tempRelease.ParseData(response.getData());
        } catch (Exception e) {
            logger.error("error in get WorkSpaces:", e);
            throw new OctaneClientException("AGM_APP", "error in get WorkSpace:", e.getMessage());
        }
        return tempRelease;
    }

    public List<Release> getAllReleases() {
        List<Release> results = new LinkedList<>();
        boolean hasNext = true;
        int offset = 0;
        int limit = 100;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/releases?offset=%d&limit=%d", baseURL,
                    sharedSpaceId, workSpaceId, offset, limit);

            RestResponse response = sendGet(url);

            try {
                Releases tempReleases = new Releases();
                tempReleases.SetCollection(response.getData());
                results.addAll(tempReleases.getCollection());
                if (tempReleases.getCollection().size() == limit) {
                    offset += limit;
                } else {
                    hasNext = false;
                }
            } catch (Exception e) {
                logger.error("error in get Releases:", e);
                throw new OctaneClientException("AGM_APP", "error in get Releases:", e.getMessage());
            }
        } while (hasNext);

        OctaneUtils.sortReleases(results);

        return results;
    }

    public List<ReleaseTeam> getReleaseTeams() {
        List<ReleaseTeam> releaseTeams = new LinkedList<>();
        List<Release> releases = getAllReleases();

        Iterator<Release> iterator = releases.iterator();
        while (iterator.hasNext()) {
            String releaseId = iterator.next().id;
            boolean hasNext = true;
            int offset = 0;
            int limit = 100;
            do {
                String url =
                        String.format("%s/api/shared_spaces/%d/workspaces/%d/teams%s%s%s&offset=%d&limit=%d", baseURL,
                                sharedSpaceId, workSpaceId, "?query=%22releases%3D%7Bid%3D", releaseId, "%7D%22",
                                offset, limit);

                RestResponse response = sendGet(url);

                ReleaseTeams tempReleaseTeams = new ReleaseTeams();
                tempReleaseTeams.releaseId = releaseId;
                try {
                    tempReleaseTeams.SetCollection(response.getData());
                } catch (Exception e) {
                    logger.error("error in get ReleaseTeams:", e);
                    throw new OctaneClientException("AGM_APP", "error in get ReleaseTeams:", e.getMessage());
                }
                if (tempReleaseTeams.getCollection().size() != 0) {
                    releaseTeams.addAll(tempReleaseTeams.getCollection());
                }
                if (tempReleaseTeams.getCollection().size() == limit) {
                    offset += limit;
                } else {
                    hasNext = false;
                }
            } while (hasNext);
        }
        return releaseTeams;
    }

    public List<Team> getTeams(int sharedSpaceId, int workSpaceId) {
        List<Team> results = new LinkedList<>();
        boolean hasNext = true;
        int offset = 0;
        int limit = 100;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/teams?offset=%d&limit=%d", baseURL,
                    sharedSpaceId, workSpaceId, offset, limit);

            RestResponse response = sendGet(url);

            try {
                Teams tempTeams = new Teams();
                tempTeams.SetCollection(response.getData());
                List<Team> teams = tempTeams.getCollection();

                Iterator<Team> iterator = teams.iterator();
                while (iterator.hasNext()) {
                    Team team = iterator.next();
                    team.membersCapacity = getTeamMemberCapacity(sharedSpaceId, workSpaceId, Integer.parseInt(team.id));
                }
                if (teams.size() == limit) {
                    offset += limit;
                } else {
                    hasNext = false;
                }
                results.addAll(teams);

            } catch (Exception e) {
                logger.error("error in get Teams:", e);
                throw new OctaneClientException("AGM_APP", "error in get Teams:", e.getMessage());
            }
        } while (hasNext);
        return results;
    }

    private int getTeamMemberCapacity(int sharedSpaceId, int workSpaceId, int teamId) {
        boolean hasNext = true;
        int offset = 0;
        int limit = 100;
        int memberCapacity = 0;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/team_members%s%d%s&offset=%d&limit=%d",
                    baseURL, sharedSpaceId, workSpaceId, "?query=%22team%3D%7Bid%3D", teamId, "%7D%22", offset, limit);

            RestResponse response = sendGet(url);
            //get the sum of team member'capacity
            net.sf.json.JSONObject object = net.sf.json.JSONObject.fromObject(response.getData());
            if(object.get("data") == null){
            	return memberCapacity;
            }
            net.sf.json.JSONArray jsonarray = (net.sf.json.JSONArray)(object.get("data"));
            if (jsonarray.size() == limit) {
                offset += limit;
            } else {
                hasNext = false;
            }
            for (int i = 0, length = jsonarray.size(); i < length; i++) {
                net.sf.json.JSONObject tempObj = jsonarray.getJSONObject(i);
                memberCapacity += (int)tempObj.get("capacity");
            }
        } while (hasNext);

        return memberCapacity;
    }

    public List<Sprint> getAllSprints() {

        List<JSONObject> sprintsJson = new JsonPaginatedOctaneGetter().get(
                String.format("%s/api/shared_spaces/%d/workspaces/%d/sprints", baseURL,
                sharedSpaceId, workSpaceId));

        List<Sprint> results = new ArrayList<>(sprintsJson.size());

        for (JSONObject sprintJson : sprintsJson) {
            Sprint sprint = new Sprint();
            sprint.ParseJsonData(sprintJson);
            results.add(sprint);
        }

        return results;
    }

    public WorkItemRoot getWorkItemRoot(int sharedSpaceId, int workSpaceId) {
        WorkItemRoot tempWorkItemRoot = new WorkItemRoot();
        boolean hasNext = true;
        int offset = 0;
        int limit = 200;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/work_items?offset=%d&limit=%d", baseURL,
                    sharedSpaceId, workSpaceId, offset, limit);

            RestResponse response = sendGet(url);

            try {
                tempWorkItemRoot.GetTempParseData(response.getData(), true);
                if (tempWorkItemRoot.length == limit) {
                    offset += limit;
                } else {
                    hasNext = false;
                }
            } catch (Exception e) {
                logger.error("error in get WorkItemRoot:", e);
                throw new OctaneClientException("AGM_APP", "error in get WorkItemRoot:", e.getMessage());
            }
        } while (hasNext);
        tempWorkItemRoot.ParseDataIntoDetail();
        return tempWorkItemRoot;
    }

    /*
    example request:
    http://XXX.asiapacific.hpqcorp.net:8080/api/shared_spaces/2001/workspaces/1002/epics/2001?fields=path,actual_story_points
    example result:

    {
       "type":"epic",
       "path":"0000000001OT",
       "logical_name":"k9z2qy3jjw06yan223ev67x4n",
       "actual_story_points":null,
       "id":"2001"
    }
    * */
    public WorkItemEpic getEpicActualStoryPointsAndPath(int sharedSpaceId, int workSpaceId, String epicId) {

        String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/epics/%s?fields=name,path,actual_story_points",
                baseURL, sharedSpaceId, workSpaceId, epicId);

        RestResponse response = sendGet(url);

        WorkItemEpic epic = new WorkItemEpic();
        try {
            if(response.getStatusCode() != 200) {
                logger.error("error in getEpicActualStoryPointsAndPath:" + response.getData());
                throw new OctaneClientException("AGM_APP", "error of get epicActualStoryPoints and path: [http status code, error message]:" + response.getStatusCode() + "," + response.getData());
            }
            net.sf.json.JSONObject jsonStr = net.sf.json.JSONObject.fromObject(response.getData());
            if(jsonStr.containsKey("path")) {
                epic.path = jsonStr.getString("path");
            }
            //US#108005-Epic item name should be able to updated to reflect the latest changes in agile tools
            if(jsonStr.containsKey("name")) {
                epic.name = jsonStr.getString("name");
            }
            if(jsonStr.containsKey("actual_story_points")) {
                String val = (String)jsonStr.getString("actual_story_points");
                try{
                    if(val != null && !"null".equals(val)) {
                        epic.totalStoryPoints = Integer.parseInt(val);
                    }
                }catch(NumberFormatException e) {
                    logger.error("NumberFormatException of actual_story_points in getEpicActualStoryPointsAndPath, "
                            + "ignore error and use 0.", e);
                }
            } else {
                logger.error("Could not found key 'actual_story_points' in response of getEpicActualStoryPointsAndPath,"
                        + " maybe Octane 's version should be upgraded.");
            }
        } catch (Exception e) {
            logger.error("error in getEpicActualStoryPointsAndPath:", e);
            throw new OctaneClientException("AGM_APP", "error of get epicActualStoryPoints and path:", e.getMessage());
        }
        return epic;
    }

    /*
    example request:
    http://XXX.asiapacific.hpqcorp.net:8080/api/shared_spaces/2001/workspaces/1002/phases?query="(entity='defect'||
        entity='story'||entity='quality_story');metaphase={logical_name='metaphase.work_item.done'}"
    example result:
    {
       "total_count":5,
       "data":[
          {
             "type":"phase",
             "creation_time":"2017-05-02T07:32:39Z",
             "is_system":true,
             "logical_name":"phase.quality_story.done",
             "version_stamp":1,
             "is_start_phase":false,
             "is_hidden":false,
             "color_hex":"#00a982",
             "description":null,
             "index":2,
             "workspace_id":1002,
             "name":"Done",
             "id":"1033",
             "metaphase":{
                "type":"metaphase",
                "id":"1004"
             },
             "last_modified":"2017-05-02T07:32:39Z",
             "entity":"quality_story"
          },
          {
             "type":"phase",
             "creation_time":"2017-05-02T07:32:39Z",
             "is_system":true,
             "logical_name":"phase.story.done",
             "version_stamp":1,
             "is_start_phase":false,
             "is_hidden":false,
             "color_hex":"#00a982",
             "description":null,
             "index":3,
             "workspace_id":1002,
             "name":"Done",
             "id":"1030",
             "metaphase":{
                "type":"metaphase",
                "id":"1004"
             },
             "last_modified":"2017-05-02T07:32:39Z",
             "entity":"story"
          },
          {
             "type":"phase",
             "creation_time":"2017-05-02T07:32:38Z",
             "is_system":true,
             "logical_name":"phase.defect.closed",
             "version_stamp":1,
             "is_start_phase":false,
             "is_hidden":false,
             "color_hex":"#00a982",
             "description":null,
             "index":4,
             "workspace_id":1002,
             "name":"Closed",
             "id":"1004",
             "metaphase":{
                "type":"metaphase",
                "id":"1004"
             },
             "last_modified":"2017-05-02T07:32:38Z",
             "entity":"defect"
          },
          {
             "type":"phase",
             "creation_time":"2017-05-02T07:32:38Z",
             "is_system":true,
             "logical_name":"phase.defect.duplicate",
             "version_stamp":1,
             "is_start_phase":false,
             "is_hidden":false,
             "color_hex":"#d7c238",
             "description":null,
             "index":6,
             "workspace_id":1002,
             "name":"Duplicate",
             "id":"1007",
             "metaphase":{
                "type":"metaphase",
                "id":"1004"
             },
             "last_modified":"2017-05-02T07:32:38Z",
             "entity":"defect"
          },
          {
             "type":"phase",
             "creation_time":"2017-05-02T07:32:38Z",
             "is_system":true,
             "logical_name":"phase.defect.rejected",
             "version_stamp":1,
             "is_start_phase":false,
             "is_hidden":false,
             "color_hex":"#be665c",
             "description":null,
             "index":7,
             "workspace_id":1002,
             "name":"Rejected",
             "id":"1008",
             "metaphase":{
                "type":"metaphase",
                "id":"1004"
             },
             "last_modified":"2017-05-02T07:32:38Z",
             "entity":"defect"
          }
       ],
       "exceeds_total_count":false
    }
* */

    /**
     * @return The phase ID for work items that is considered as "DONE".
     */
    public String[] getDoneDefinitionOfUserStoryAndDefect(int sharedSpaceId, int workSpaceId) {
        String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/phases?query=\"(entity='defect'||entity='story'||"
                + "entity='quality_story');metaphase={logical_name='metaphase.work_item.done'}\"", baseURL, sharedSpaceId, workSpaceId);
        RestResponse response = sendGet(url);

        ArrayList<String> ids = new ArrayList();
        try {
            if(response.getStatusCode() != 200) {
                throw new OctaneClientException("AGM_APP", "error of get getDoneDefinitionOfUserStoryAndDefect and path: [http status code, error message]:" + response.getStatusCode() + "," + response.getData());
            }
            net.sf.json.JSONObject jsonResponse = net.sf.json.JSONObject.fromObject(response.getData());
            net.sf.json.JSONArray jsonData = jsonResponse.getJSONArray("data");
            if(jsonData != null && jsonData.size() > 0) {
                for(int i=0; i<jsonData.size(); i++) {
                    net.sf.json.JSONObject data = (net.sf.json.JSONObject)jsonData.get(i);
                    ids.add(data.getString("id"));
                }
            }
        } catch (Exception e) {
            logger.error("error in getDoneDefinitionOfUserStoryAndDefect:", e);
            throw new OctaneClientException("AGM_APP", "error in getDoneDefinitionOfUserStoryAndDefect:", e.getMessage());
        }
        return ids.toArray(new String[]{});
    }


    public WorkItemEpic getEpicDoneStoryPoints(int sharedSpaceId, int workSpaceId, String epicPath, String[] doneStatusIDs)  {

        //example:
        // (phase={id=1030}||phase={id=1033}||phase={id=1004})
        StringBuffer statusStr = new StringBuffer();

        if (doneStatusIDs.length > 0) {
            statusStr.append("(");
            boolean first = true;
            for (String x : doneStatusIDs) {

                // Old versions of Octane will return a number as phase ID.
                // Newer verions of Octane (12.55+ ?) will return the string like phase.defect.closed, and as such
                // should be enclosed in "^" in the query string.
                if (!StringUtils.isNumeric(x)) {
                    x = "^"+x+"^";
                }

                if (first) {
                    first = false;
                    statusStr.append("phase={id=" + x + "}");
                } else {
                    statusStr.append("||phase={id=" + x + "}");
                }
            }
            statusStr.append(")");
        } else {
            Exception e = new RuntimeException("error by get doneStatusIDs empty");
            logger.error("error by get doneStatusIDs empty", e);
            throw new OctaneClientException("AGM_APP", "error by get doneStatusIDs empty", e.getMessage());

        }
        String url = String.format(
                "%s/api/shared_spaces/%d/workspaces/%d/work_items/groups?group_data=sum(story_points)&group_by=phase&query=\""
                        + "path='%s*';(subtype='defect'||subtype='story'||subtype='quality_story');%s\"", baseURL,
                sharedSpaceId, workSpaceId, epicPath, statusStr);

        RestResponse response = sendGet(url);

        WorkItemEpic epic = new WorkItemEpic();
        try {
            if(response.getStatusCode() != 200) {
                throw new OctaneClientException("AGM_APP", "error of get getDoneDefinitionOfUserStoryAndDefect and path: [http status code, error message]:" + response.getStatusCode() + "," + response.getData());
            }
            net.sf.json.JSONObject jsonResponse = net.sf.json.JSONObject.fromObject(response.getData());
            net.sf.json.JSONArray jsonData = jsonResponse.getJSONArray("groups");
            if (jsonData != null && jsonData.size() > 0) {
                for (int i = 0; i < jsonData.size(); i++) {
                    net.sf.json.JSONObject data = (net.sf.json.JSONObject)jsonData.get(i);
                    if (data.containsKey("aggregatedData")) {
                        net.sf.json.JSONObject aggregatedData = data.getJSONObject("aggregatedData");
                        if (aggregatedData.containsKey("story_points")) {
                            epic.doneStoryPoints += aggregatedData.getInt("story_points");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("error in getEpicDoneStoryPoints:", e);
            throw new OctaneClientException("AGM_APP", "error in getEpicDoneStoryPoints:", e.getMessage());
        }
        return epic;
    }

    /**
     * @param sprintDays number of days per sprint (including week ends). If null, will use kanban release.
     */
    public Release createRelease(String releaseName, String releaseDescription, Date startDate, Date endDate, Integer sprintDays) {

        // First, let's retrieve the nodeList ids for release type: sprint or kanban
        String releaseTypeNodeListId = null;
        if (sprintDays == null || sprintDays.intValue() == 0) {
            // Kanban
            releaseTypeNodeListId = getListNodeIdForLogicalName("list_node.release_agile_type.kanban");
        } else {
            // Scrum
            releaseTypeNodeListId = getListNodeIdForLogicalName("list_node.release_agile_type.scrum");
        }

        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/releases", baseURL, sharedSpaceId, workSpaceId);

        JSONObject createReleasePayload = new JSONObject();
        JSONArray data = new JSONArray();
        JSONObject releaseData = new JSONObject();
        releaseData.put("description", releaseDescription);
        releaseData.put("name", releaseName);
        releaseData.put("sprint_duration", sprintDays == null ? 14 : (sprintDays));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        releaseData.put("start_date", sdf.format(startDate)+JSON_TIME_SUFFIX);
        releaseData.put("end_date", sdf.format(endDate)+JSON_TIME_SUFFIX);
        JSONObject agileType = new JSONObject();
        agileType.put("type", "list_node");
        agileType.put("id", releaseTypeNodeListId);
        releaseData.put("agile_type", agileType);

        data.add(releaseData);
        createReleasePayload.put("data", data);

        RestResponse response = sendRequest(url, HttpMethod.POST, createReleasePayload.toString());
        if (HttpStatus.SC_CREATED != response.getStatusCode()) {
            this.logger.error("Error occured when creating release in Octane. Response code = " + response.getStatusCode());
            throw new OctaneClientException("AGM_APP", "An error occurred when creating the release. Make sure a release with this name doesn't already exist.", new String[] { response.getData() });
        }

        Releases tempReleases = new Releases();
        tempReleases.SetCollection(response.getData());
        return tempReleases.getCollection().get(0);
    }

    private String getListNodeIdForLogicalName(String nodeListLogicalName) {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/list_nodes?fields=id&query=\"logical_name=^%s^\"",
                baseURL, sharedSpaceId, workSpaceId, nodeListLogicalName);
        RestResponse response = sendGet(url);

        Releases tempReleases = new Releases();
        tempReleases.SetCollection(response.getData());
        List<Release> releases = tempReleases.getCollection();

        if (releases.size() != 1) {
            return null;
        }

        return releases.get(0).getId();

    }

    public List<EpicEntity> createEpicInWorkspace(String sharedspaceId, String workspaceId, EpicCreateEntity epicCreateEntity)  {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/epics", baseURL, sharedspaceId, workspaceId);

        RestResponse response = sendRequest(url, HttpMethod.POST, this.getJsonStrFromObject(epicCreateEntity));
        if (HttpStatus.SC_CREATED != response.getStatusCode()) {
          this.logger.error("Error occurs when creating epic in Octane: Response code = " + response.getStatusCode());
          throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR", new String[] { response.getData() });
        }
        return (List<EpicEntity>) getDataContent(response.getData(), new TypeReference<List<EpicEntity>>(){});
    }

    public List<EpicAttr> getEpicPhase(final String sharedspaceId, final String workspaceId, final String phaseLogicName) {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/phases?fields=id&query=%s%s%s",
                baseURL, sharedspaceId, workspaceId, "%22logical_name%3D'", phaseLogicName, "'%22");
        RestResponse response = sendGet(url);

        return (List<EpicAttr>)getDataContent(response.getData(), new TypeReference<List<EpicAttr>>(){});
    }

    public List<EpicAttr> getAllEpics() {
        String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/epics?fields=id,name",
                baseURL, sharedSpaceId, workSpaceId);
        RestResponse response = sendGet(url);

        return (List<EpicAttr>)getDataContent(response.getData(), new TypeReference<List<EpicAttr>>(){});
    }
    
    public List<EpicAttr> getEpicParent(final String sharedspaceId, final String workspaceId, final String workitemSubtype) {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/work_items?fields=id&query=%s%s%s",
            baseURL, sharedspaceId, workspaceId, "%22subtype%3D'", workitemSubtype, "'%22");
        RestResponse response = sendGet(url);
    	
        return (List<EpicAttr>)getDataContent(response.getData(), new TypeReference<List<EpicAttr>>(){});
    }

    public List<FieldInfo> getEntityFields(final String sharedspaceId, final String workspaceId, final String entityName) {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/metadata/fields?query=%s%s%s",
                baseURL, sharedspaceId, workspaceId, "%22entity_name%20EQ%20'", entityName, "';visible_in_ui%20EQ%20true%22");
        List fieldsList = new ArrayList();
        RestResponse response = sendGet(url);
        JSONObject dataObj = JSONObject.fromObject(response.getData());
        if (dataObj != null) {
            JSONArray fieldsArray = dataObj.getJSONArray("data");
            for(int i = 0; i < fieldsArray.size(); i++) {
                JSONObject data = fieldsArray.getJSONObject(i);
                FieldInfo info = new FieldInfo(data);
                fieldsList.add(info);
            }
        }
        return fieldsList;
    }

    public List<FieldValue> getEntityFieldValueList(final String sharedspaceId, final String workspaceId, final String logicalName) {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/list_nodes?query=%s%s%s",
                baseURL, sharedspaceId, workspaceId, "%22logical_name%20EQ%20^", logicalName, ".*^%22");
        RestResponse response = sendGet(url);
        List valueList = new ArrayList();
        JSONObject dataObj = JSONObject.fromObject(response.getData());
        if (dataObj != null) {
            JSONArray fieldsArray = dataObj.getJSONArray("data");
            for(int i = 0; i < fieldsArray.size(); i++) {
                JSONObject data = fieldsArray.getJSONObject(i);
                FieldValue value = new FieldValue();
                value.setKey(data.getString(OctaneConstants.KEY_FIELD_ID));
                value.setValue(data.getString(OctaneConstants.KEY_FIELD_NAME));
                valueList.add(value);
            }
        }
        return valueList;
    }

    public List<FeatureEntity> createFeatureInWorkspace(final String sharedspaceId, final String workspaceId, final FeatureCreateEntity entity) {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/features", baseURL, sharedspaceId, workspaceId);

        RestResponse response = sendRequest(url, HttpMethod.POST, this.getJsonStrFromObject(entity));
        if (HttpStatus.SC_CREATED != response.getStatusCode()) {
            this.logger.error("Error occurs when creating feature in Octane: Response code = " + response.getStatusCode());
            throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR", new String[] { response.getData() });
        }
        return (List<FeatureEntity>) getDataContent(response.getData(), new TypeReference<List<FeatureEntity>>(){});
    }
    
    public List<StoryEntity> createStoryInWorkspace(final String sharedspaceId, final String workspaceId, final StoryCreateEntity entity) {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/stories", baseURL, sharedspaceId, workspaceId);

        RestResponse response = sendRequest(url, HttpMethod.POST, this.getJsonStrFromObject(entity));
        if (HttpStatus.SC_CREATED != response.getStatusCode()) {
            this.logger.error("Error occurs when creating story in Octane: Response code = " + response.getStatusCode());
            throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR", new String[] { response.getData() });
        }
        return (List<StoryEntity>) getDataContent(response.getData(), new TypeReference<List<StoryEntity>>(){});
    }

    
    private String getJsonStrFromObject(Object sourceObj)  {
    	ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(sourceObj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error when generating JSon String from object", e);
        }
    }
    
    private String getCSRF(final String cookies) {
        String csrf = null;
        int csrfStart = cookies.indexOf("HPSSO_COOKIE_CSRF=");
        if (csrfStart > -1) {
            int csrfEnd = cookies.indexOf(";", csrfStart);
            csrf = cookies.substring(csrfStart + 18, csrfEnd);
        }
        return csrf;
    }
    
    private List<?> getDataContent(String jsonData, TypeReference<?> typeRef) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            org.json.JSONObject obj = new org.json.JSONObject(jsonData);
            Object dataObj = obj.get("data");
            if (dataObj != null) {
                String arrayStr = dataObj.toString();
                if (arrayStr.length() >= 2) {
                    return mapper.readValue(arrayStr, typeRef);
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error when parsing JSon Data: " + jsonData, e);
            throw new OctaneClientException("AGM_APP", "Error when reading JSon data from Octane: "+ e.getMessage());
        }
    }

    public List<GenericWorkItem> getEpicWorkItems(int epicId, Set<String> itemTypes) {

        List<JSONObject> workItemsJson = getWorkItems(String.format("id=%d||parent={id=%d}||parent={parent={id=%d}};subtype%%20IN%%20%s", epicId, epicId, epicId, StringUtils.join(itemTypes, ",")));

        List<GenericWorkItem> results = new ArrayList<>(workItemsJson.size());

        for (JSONObject workItemJson : workItemsJson) {
            GenericWorkItem wi = new GenericWorkItem(workItemJson);
            results.add(wi);
        }

        return results;
    }

    public List<GenericWorkItem> getReleaseWorkItems(int releaseId, Set<String> itemTypes) {
        List<JSONObject> workItemsJson = getWorkItems(String.format("release={id=%d};subtype%%20IN%%20%s", releaseId, StringUtils.join(itemTypes, ",")));

        List<GenericWorkItem> results = new ArrayList<>(workItemsJson.size());

        for (JSONObject workItemJson : workItemsJson) {
            GenericWorkItem wi = new GenericWorkItem(workItemJson);
            results.add(wi);
        }

        return results;
    }

    private List<JSONObject> getWorkItems(String queryFilter) {

        String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/work_items?" +
                "fields=id,name,phase,estimated_hours,invested_hours,remaining_hours,subtype,release,sprint,creation_time,last_modified,owner,parent,story_points,actual_story_points"
                        , baseURL, sharedSpaceId, workSpaceId);
        if (!StringUtils.isBlank(queryFilter)) {
            url += "&query=\""+queryFilter+"\"";
        }

        return new JsonPaginatedOctaneGetter().get(url);
    }

    public void setWorkSpaceId(String workSpaceId) {
        if (!StringUtils.isBlank(workSpaceId)) {
            this.workSpaceId = Integer.parseInt(workSpaceId);
        }
    }

    public void setSharedSpaceId(String sharedSpaceId) {
        if (!StringUtils.isBlank(sharedSpaceId)) {
            this.sharedSpaceId = Integer.parseInt(sharedSpaceId);
        }
    }

    public Map<String, String> getAllPhases() {


        List<JSONObject> phasesJson = new JsonPaginatedOctaneGetter().get(
                String.format("%s/api/shared_spaces/%d/workspaces/%d/phases", baseURL,
                        sharedSpaceId, workSpaceId));

        Map<String, String> phases = new HashMap<>(phasesJson.size());

        for (JSONObject phaseJson : phasesJson) {
            phases.put(phaseJson.getString("id"), phaseJson.getString("name"));
        }

        return phases;
    }

    public Map<String,String> getAllWorkspaceUsers() {

        List<JSONObject> usersJson = new JsonPaginatedOctaneGetter().get(
                String.format("%s/api/shared_spaces/%d/workspaces/%d/workspace_users?fields=email,id", baseURL,
                        sharedSpaceId, workSpaceId));

        Map<String, String> users = new HashMap<>(usersJson.size());

        for (JSONObject userJson : usersJson) {
            users.put(userJson.getString("id"), userJson.getString("email"));
        }

        return users;

    }

    public List<GenericWorkItem> getWorkItemsByIds(Set<String> ids) {

        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>(0);
        }

        String query = "id%20IN%20" + StringUtils.join(ids, ",");
        List<JSONObject> workItemsJson = getWorkItems(query);

        List<GenericWorkItem> results = new ArrayList<>(workItemsJson.size());

        for (JSONObject workItemJson : workItemsJson) {
            GenericWorkItem wi = new GenericWorkItem(workItemJson);
            results.add(wi);
        }

        return results;
    }
    
    public Map<String, Map<String, List<FieldValue>>> getUserStories(String sharedspaceId, String workspaceId,Set<String> ids) {
    	Map<String, Map<String, List<FieldValue>>> storiesMap = new HashMap<>(); 

        if (ids == null || ids.isEmpty()) {
           return new HashMap<>();        }

        String query = "id%20IN%20" + StringUtils.join(ids, ",");
        List<JSONObject> workItemsJson = getUserStoriesJson(sharedspaceId, workspaceId,query);
        for (JSONObject workItemJson : workItemsJson) {
        	   	
        	Map<String, List<FieldValue>> storyMap = new HashMap<String, List<FieldValue>>();
        	storyMap = wapperStoryFieldsMap(workItemJson);
        	storiesMap.put(workItemJson.getString(OctaneConstants.KEY_FIELD_ID), storyMap);
        }

        return storiesMap;
    }
    
    public Map<String, Map<String, List<FieldValue>>> getFeatures(String sharedspaceId, String workspaceId,Set<String> ids) {
    	Map<String, Map<String, List<FieldValue>>> featuresMap = new HashMap<>(); 

        if (ids == null || ids.isEmpty()) {
           return new HashMap<>();        }

        String query = "id%20IN%20" + StringUtils.join(ids, ",");
        List<JSONObject> workItemsJson = getFeatureJson(sharedspaceId, workspaceId,query);
        for (JSONObject workItemJson : workItemsJson) {
        	   	
        	Map<String, List<FieldValue>> featureMap = new HashMap<String, List<FieldValue>>();
        	featureMap = wapperFeaturesMap(workItemJson);
        	featuresMap.put(workItemJson.getString(OctaneConstants.KEY_FIELD_ID), featureMap);
        }

        return featuresMap;
    }
    
    private Map<String, List<FieldValue>> wapperStoryFieldsMap(JSONObject item){
    	
    	Map<String, List<FieldValue>> agileFields = new HashMap<String, List<FieldValue>>();
    	FieldValue type =  new FieldValue(OctaneConstants.KEY_FIELD_TYPE, item.get(OctaneConstants.KEY_FIELD_TYPE).toString());
    	FieldValue description =  new FieldValue(OctaneConstants.KEY_FIELD_DESCRIPTION, item.get(OctaneConstants.KEY_FIELD_DESCRIPTION).toString());
    	FieldValue name =  new FieldValue(OctaneConstants.KEY_FIELD_NAME, item.get(OctaneConstants.KEY_FIELD_NAME).toString());
    	FieldValue creationTime =  new FieldValue(OctaneConstants.KEY_FIELD_CREATE_TIME, item.get(OctaneConstants.KEY_FIELD_CREATE_TIME).toString());
    	FieldValue lastModified = new FieldValue(OctaneConstants.KEY_FIELD_LAST_MODIFIED, item.get(OctaneConstants.KEY_FIELD_LAST_MODIFIED).toString());
    	FieldValue phase = new FieldValue(OctaneConstants.KEY_FIELD_PHASE, getString("id", getObj(OctaneConstants.KEY_FIELD_PHASE,item)));
    	FieldValue storyPoints = new FieldValue(OctaneConstants.KEY_FIELD_STORY_POINTS, item.get(OctaneConstants.KEY_FIELD_STORY_POINTS).toString());
    	FieldValue remainingHours = new FieldValue(OctaneConstants.KEY_FIELD_REMAINING_HOURS, item.get(OctaneConstants.KEY_FIELD_REMAINING_HOURS).toString());
    	FieldValue estimatedHours = new FieldValue(OctaneConstants.KEY_FIELD_ESTIMATED_HOURS, item.get(OctaneConstants.KEY_FIELD_ESTIMATED_HOURS).toString());
    	
    	agileFields.put(OctaneConstants.KEY_FIELD_TYPE,  Arrays.asList(type));
    	agileFields.put(OctaneConstants.KEY_FIELD_DESCRIPTION,  Arrays.asList(description));
    	agileFields.put(OctaneConstants.KEY_FIELD_NAME,  Arrays.asList(name));
    	agileFields.put(OctaneConstants.KEY_FIELD_CREATE_TIME,  Arrays.asList(creationTime));
    	agileFields.put(OctaneConstants.KEY_FIELD_LAST_MODIFIED,  Arrays.asList(lastModified));
    	agileFields.put(OctaneConstants.KEY_FIELD_PHASE,  Arrays.asList(phase));
    	agileFields.put(OctaneConstants.KEY_FIELD_STORY_POINTS,  Arrays.asList(storyPoints));
    	agileFields.put(OctaneConstants.KEY_FIELD_REMAINING_HOURS,  Arrays.asList(remainingHours));
    	agileFields.put(OctaneConstants.KEY_FIELD_REMAINING_HOURS,  Arrays.asList(estimatedHours));
    	
    	return agileFields;
    	
    }
    
    private Map<String, List<FieldValue>> wapperFeaturesMap(JSONObject item){
    	
    	Map<String, List<FieldValue>> agileFields = new HashMap<String, List<FieldValue>>();
    	FieldValue type =  new FieldValue(OctaneConstants.KEY_FIELD_TYPE, item.get(OctaneConstants.KEY_FIELD_TYPE).toString());
    	FieldValue description =  new FieldValue(OctaneConstants.KEY_FIELD_DESCRIPTION, item.get(OctaneConstants.KEY_FIELD_DESCRIPTION).toString());
    	FieldValue name =  new FieldValue(OctaneConstants.KEY_FIELD_NAME, item.get(OctaneConstants.KEY_FIELD_NAME).toString());
    	FieldValue creationTime =  new FieldValue(OctaneConstants.KEY_FIELD_CREATE_TIME, item.get(OctaneConstants.KEY_FIELD_CREATE_TIME).toString());
    	FieldValue version = new FieldValue(OctaneConstants.KEY_FIELD_VERSION_STAMP, item.get(OctaneConstants.KEY_FIELD_VERSION_STAMP).toString());
    	FieldValue lastModified = new FieldValue(OctaneConstants.KEY_FIELD_LAST_MODIFIED, item.get(OctaneConstants.KEY_FIELD_LAST_MODIFIED).toString());
    	FieldValue phase = new FieldValue(OctaneConstants.KEY_FIELD_PHASE, getString("id", getObj(OctaneConstants.KEY_FIELD_PHASE,item)));
    	FieldValue storyPoints = new FieldValue(OctaneConstants.KEY_FIELD_STORY_POINTS, item.get(OctaneConstants.KEY_FIELD_STORY_POINTS).toString());
    	
    	agileFields.put(OctaneConstants.KEY_FIELD_TYPE,  Arrays.asList(type));
    	agileFields.put(OctaneConstants.KEY_FIELD_DESCRIPTION,  Arrays.asList(description));
    	agileFields.put(OctaneConstants.KEY_FIELD_NAME,  Arrays.asList(name));
    	agileFields.put(OctaneConstants.KEY_FIELD_CREATE_TIME,  Arrays.asList(creationTime));
    	agileFields.put(OctaneConstants.KEY_FIELD_VERSION_STAMP,  Arrays.asList(version));
    	agileFields.put(OctaneConstants.KEY_FIELD_LAST_MODIFIED,  Arrays.asList(lastModified));
    	agileFields.put(OctaneConstants.KEY_FIELD_PHASE,  Arrays.asList(phase));
    	agileFields.put(OctaneConstants.KEY_FIELD_STORY_POINTS,  Arrays.asList(storyPoints));
    	
    	
    	return agileFields;
    	
    }
    
    private List<JSONObject> getUserStoriesJson(String sharedspaceId, String workspaceId, String queryFilter) {

        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/stories", baseURL, sharedspaceId, workspaceId);
        if (!StringUtils.isBlank(queryFilter)) {
            url += "?query=\""+queryFilter+"\"";
        }

        return new JsonPaginatedOctaneGetter().get(url);
    }
    
    private List<JSONObject> getFeatureJson(String sharedspaceId, String workspaceId, String queryFilter) {

        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/features", baseURL, sharedspaceId, workspaceId);
        if (!StringUtils.isBlank(queryFilter)) {
            url += "?query=\""+queryFilter+"\"";
        }

        return new JsonPaginatedOctaneGetter().get(url);
    }
    
    private String getString(String key, JSONObject obj) {

        if (obj == null) {
            return null;
        }

        try {
            return obj.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }
    
    private JSONObject getObj(String key, JSONObject obj) {
        if (obj == null) {
            return null;
        }

        try {
            return obj.getJSONObject(key);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * This class is in charge of retrieving data from Octane REST API in a paginated way.
     *
     * Default page size is 1000 records.
     *
     */
    private class JsonPaginatedOctaneGetter {

        private int limit = 1000;

        public JsonPaginatedOctaneGetter() {}

        public JsonPaginatedOctaneGetter(int pageSize) {
            if (pageSize > 0) {
                limit = pageSize;
            }
        }

        public List<JSONObject> get(String url) {

            List<JSONObject> results = null;

            int offset = 0 ;
            int totalCount = 0;

            boolean first = true;

            do {
                if (first) {
                    first = false;
                } else {
                    offset += limit;
                }

                String paginatedUrl = url;
                if (!url.contains("?")) {
                    paginatedUrl += "?";
                } else {
                    paginatedUrl += "&";
                }

                paginatedUrl += "offset=" +offset + "&limit="+limit;

                String responseString = sendGet(paginatedUrl).getData();

                JSONObject response = JSONObject.fromObject(responseString);

                totalCount = response.getInt("total_count");

                JSONArray data = (JSONArray)(response.get("data"));

                if (results == null) {
                    results = new ArrayList<>(totalCount);
                }

                // Add results
                for (int i = 0 ; i < data.size() ; i++) {
                    results.add(data.getJSONObject(i));
                }

            } while (offset + limit < totalCount);

            return results;
        }
    }

    // https://mqast010pngx.saas.hpe.com/api/shared_spaces/22001/workspaces/1002/work_items?fields=sprint,owner,name,invested_hours,phase,release,remaining_hours,estimated_hours,story_points,subtype&query=%22id%20%3D%2059004%20%7C%7C%20parent%20%3D%20%7Bid%20%3D%2059004%7D%20%7C%7C%20parent%20%3D%20%7Bparent%20%3D%20%7Bid%20%3D%2059004%7D%7D%22
}
package com.ppm.integration.agilesdk.connector.octane.client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kintana.core.server.execution.ParseException;
import com.ppm.integration.agilesdk.connector.octane.model.EpicAttr;
import com.ppm.integration.agilesdk.connector.octane.model.EpicCreateEntity;
import com.ppm.integration.agilesdk.connector.octane.model.EpicEntity;
import com.ppm.integration.agilesdk.connector.octane.model.Release;
import com.ppm.integration.agilesdk.connector.octane.model.ReleaseTeam;
import com.ppm.integration.agilesdk.connector.octane.model.ReleaseTeams;
import com.ppm.integration.agilesdk.connector.octane.model.Releases;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpaces;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.connector.octane.model.Sprints;
import com.ppm.integration.agilesdk.connector.octane.model.Team;
import com.ppm.integration.agilesdk.connector.octane.model.Teams;
import com.ppm.integration.agilesdk.connector.octane.model.TimesheetItem;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemEpic;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpaces;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ClientPublicAPI {

    private final Logger logger = Logger.getLogger(this.getClass());

    protected String baseURL = "";

    private String cookies;

    private Proxy proxy = null;

    private int retryNumber = 0;

    public ClientPublicAPI(String baseUrl) {
        this.baseURL = baseUrl.trim();
        if (this.baseURL.endsWith("/")) {
            this.baseURL = this.baseURL.substring(0, this.baseURL.length() - 1);
        }
    }

    public boolean getAccessTokenWithFormFormat(String clientId, String clientSecret)
            throws IOException, JSONException
    {
        //http(s)://<server>:<port>/agm/oauth/token
        String url = String.format("%s/authentication/sign_in", baseURL);
        String data =
                String.format("{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"enable_csrf\": \"true\"}", clientId,
                        clientSecret);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", MediaType.APPLICATION_JSON);
        RestResponse response = sendRequest(url, HttpMethod.POST, data, headers);
        return verifyResult(HttpStatus.SC_OK, response.getStatusCode());

        //return AccessToken.createFromJson(response.getData());
    }

    public void setProxy(String host, int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    private RestResponse sendRequest(String url, String method, String data, Map<String, String> headers)
            throws IOException
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
            int workspaceId) throws IOException
    {
        String method = "GET";
        String url = String.format(
                "%s/api/shared_spaces/%d/timesheet?login_names=%s&start_date=%s&end_date=%s&workspace_ids=%d&task_level=true",
                baseURL, shareSpace, userName, startDateStr, endDateStr, workspaceId);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookies);
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        RestResponse response = sendRequest(url, method, null, headers);

        try {
            List<TimesheetItem> items = parseTimesheetItems(response.getData());
            return items;
        } catch (Exception e) {
            logger.error("error in timesheet retrieve:", e);
            throw new OctaneClientException("AGM_APP", "error in timesheet retrieve:", e.getMessage());
        }
    }

    public List<TimesheetItem> parseTimesheetItems(String json) throws IOException, JSONException, ParseException {
        JSONObject obj = new JSONObject(json);
        JSONArray arr = obj.getJSONArray("data");
        List<TimesheetItem> items = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject rawItem = (JSONObject)arr.get(i);
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

    public List<SharedSpace> getSharedSpaces() throws IOException {

        String method = "GET";
        String url = String.format("%s/api/shared_spaces", baseURL);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookies);
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        RestResponse response = sendRequest(url, method, null, headers);

        SharedSpaces tempSharedSpace = new SharedSpaces();
        try {
            tempSharedSpace.SetCollection(response.getData());
        } catch (Exception e) {
            logger.error("error in get ShareSpaces:", e);
            throw new OctaneClientException("AGM_APP", "error in get ShareSpace:", e.getMessage());
        }

        return tempSharedSpace.getCollection();
    }

    public List<WorkSpace> getWorkSpaces(int sharedSpacesId) throws IOException {

        String method = "GET";
        String url = String.format("%s/api/shared_spaces/%d/workspaces", baseURL, sharedSpacesId);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookies);
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        RestResponse response = sendRequest(url, method, null, headers);

        WorkSpaces tempWorkSpace = new WorkSpaces();
        try {
            tempWorkSpace.SetCollection(response.getData());
        } catch (Exception e) {
            logger.error("error in get WorkSpaces:", e);
            throw new OctaneClientException("AGM_APP", "error in get WorkSpace:", e.getMessage());
        }
        return tempWorkSpace.getCollection();
    }

    public Release getRelease(int sharedSpacesId, int workSpaceId, int releaseId) throws IOException {

        String method = "GET";
        String url =
                String.format("%s/api/shared_spaces/%d/workspaces/%d/releases/%d", baseURL, sharedSpacesId, workSpaceId,
                        releaseId);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookies);
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        RestResponse response = sendRequest(url, method, null, headers);

        Release tempRelease = new Release();
        try {
            tempRelease.ParseData(response.getData());
        } catch (Exception e) {
            logger.error("error in get WorkSpaces:", e);
            throw new OctaneClientException("AGM_APP", "error in get WorkSpace:", e.getMessage());
        }
        return tempRelease;
    }

    public List<Release> getReleases(int sharedSpaceId, int workSpaceId) throws IOException {
        String method = "GET";
        List<Release> results = new LinkedList<>();
        boolean hasNext = true;
        int offset = 0;
        int limit = 100;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/releases?offset=%d&limit=%d", baseURL,
                    sharedSpaceId, workSpaceId, offset, limit);
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", this.cookies);
            headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

            RestResponse response = sendRequest(url, method, null, headers);

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
        return results;
    }

    public List<ReleaseTeam> getReleaseTeams(int sharedSpaceId, int workSpaceId) throws IOException {
        List<ReleaseTeam> releaseTeams = new LinkedList<>();
        List<Release> releases = getReleases(sharedSpaceId, workSpaceId);

        Iterator<Release> iterator = releases.iterator();
        while (iterator.hasNext()) {
            String releaseId = iterator.next().id;
            String method = "GET";
            boolean hasNext = true;
            int offset = 0;
            int limit = 100;
            do {
                String url =
                        String.format("%s/api/shared_spaces/%d/workspaces/%d/teams%s%s%s&offset=%d&limit=%d", baseURL,
                                sharedSpaceId, workSpaceId, "?query=%22releases%3D%7Bid%3D", releaseId, "%7D%22",
                                offset, limit);

                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", this.cookies);
                headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

                RestResponse response = sendRequest(url, method, null, headers);

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

    public List<Team> getTeams(int sharedSpaceId, int workSpaceId) throws IOException {
        String method = "GET";
        List<Team> results = new LinkedList<>();
        boolean hasNext = true;
        int offset = 0;
        int limit = 100;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/teams?offset=%d&limit=%d", baseURL,
                    sharedSpaceId, workSpaceId, offset, limit);

            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", this.cookies);
            headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

            RestResponse response = sendRequest(url, method, null, headers);

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

    private int getTeamMemberCapacity(int sharedSpaceId, int workSpaceId, int teamId) throws IOException {
        String method = "GET";
        boolean hasNext = true;
        int offset = 0;
        int limit = 100;
        int memberCapacity = 0;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/team_members%s%d%s&offset=%d&limit=%d",
                    baseURL, sharedSpaceId, workSpaceId, "?query=%22team%3D%7Bid%3D", teamId, "%7D%22", offset, limit);

            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", this.cookies);
            headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

            RestResponse response = sendRequest(url, method, null, headers);
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

    public List<Sprint> getSprints(int sharedSpaceId, int workSpaceId) throws IOException {
        String method = "GET";
        List<Sprint> results = new LinkedList<>();
        boolean hasNext = true;
        int offset = 0;
        int limit = 100;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/sprints?offset=%d&limit=%d", baseURL,
                    sharedSpaceId, workSpaceId, offset, limit);

            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", this.cookies);
            headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

            RestResponse response = sendRequest(url, method, null, headers);
            try {
                Sprints tempSprints = new Sprints();
                tempSprints.SetCollection(response.getData());
                if (tempSprints.getCollection().size() == limit) {
                    offset += limit;
                } else {
                    hasNext = false;
                }
                results.addAll(tempSprints.getCollection());
            } catch (Exception e) {
                logger.error("error in get Sprints:", e);
                throw new OctaneClientException("AGM_APP", "error in get Sprints:", e.getMessage());
            }
        } while (hasNext);
        return results;
    }

    public WorkItemRoot getWorkItemRoot(int sharedSpaceId, int workSpaceId) throws IOException {
        String method = "GET";
        WorkItemRoot tempWorkItemRoot = new WorkItemRoot();
        boolean hasNext = true;
        int offset = 0;
        int limit = 200;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/work_items?offset=%d&limit=%d", baseURL,
                    sharedSpaceId, workSpaceId, offset, limit);

            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", this.cookies);
            headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

            RestResponse response = sendRequest(url, method, null, headers);

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

    public WorkItemRoot getWorkItemRoot(int sharedSpaceId, int workSpaceId, int releaseId) throws IOException {
        String method = "GET";
        WorkItemRoot tempWorkItemRoot = new WorkItemRoot();
        boolean hasNext = true;
        int offset = 0;
        int limit = 200;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/work_items?offset=%d&limit=%d", baseURL,
                    sharedSpaceId, workSpaceId, offset, limit);

            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", this.cookies);
            headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

            RestResponse response = sendRequest(url, method, null, headers);

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
        tempWorkItemRoot.ParseDataIntoDetail(String.valueOf(releaseId));
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
    public WorkItemEpic getEpicActualStoryPointsAndPath(int sharedSpaceId, int workSpaceId, String epicId) throws IOException {

        String method = "GET";
        String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/epics/%s?fields=name,path,actual_story_points",
                baseURL, sharedSpaceId, workSpaceId, epicId);

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookies);
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        RestResponse response = sendRequest(url, method, null, headers);

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

    public String[] getDoneDefinationOfUserStoryAndDefect(int sharedSpaceId, int workSpaceId) throws IOException {
        String method = "GET";
        String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/phases?query=\"(entity='defect'||entity='story'||"
                + "entity='quality_story');metaphase={logical_name='metaphase.work_item.done'}\"", baseURL, sharedSpaceId, workSpaceId);
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookies);
//        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        RestResponse response = sendRequest(url, method, null, headers);

        ArrayList<String> ids = new ArrayList();
        try {
            if(response.getStatusCode() != 200) {
                throw new OctaneClientException("AGM_APP", "error of get getDoneDefinationOfUserStoryAndDefect and path: [http status code, error message]:" + response.getStatusCode() + "," + response.getData());
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
            logger.error("error in getDoneDefinationOfUserStoryAndDefect:", e);
            throw new OctaneClientException("AGM_APP", "error in getDoneDefinationOfUserStoryAndDefect:", e.getMessage());
        }
        return ids.toArray(new String[]{});
    }

/*
    example request:
    http://XXX.asiapacific.hpqcorp.net:8080/api/shared_spaces/2001/workspaces/1002/work_items/groups?group_by=phase&query="path='0000000001OT*';
        (subtype='defect'||subtype='story'||subtype='quality_story');(phase={id=1030}||phase={id=1033}||phase={id=1004})"
    example result 1:

    {"groups":[],"groupsTotalCount":0}

    example result 2:
    {
       "groups":[
          {
             "count":1,
             "value":{
                "type":"phase",
                "logical_name":"phase.defect.closed",
                "name":"Closed",
                "index":3,
                "id":"1004"
             },
             "aggregatedData":{
                "story_points":99
             },
             "groups":null
          },
          {
             "count":1,
             "value":{
                "type":"phase",
                "logical_name":"phase.story.done",
                "name":"Done",
                "index":3,
                "id":"1030"
             },
             "aggregatedData":{
                "story_points":12
             },
             "groups":null
          }
       ],
       "groupsTotalCount":2
    }
    * */
    public WorkItemEpic getEpicDoneStoryPoints(int sharedSpaceId, int workSpaceId, String epicPath, String[] doneStatusIDs) throws IOException {

        String method = "GET";
        //example:
        // (phase={id=1030}||phase={id=1033}||phase={id=1004})
        StringBuffer statusStr = new StringBuffer();

        if (doneStatusIDs.length > 0) {
            statusStr.append("(");
            boolean first = true;
            for (String x : doneStatusIDs) {
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

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookies);
        //headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        RestResponse response = sendRequest(url, method, null, headers);

        WorkItemEpic epic = new WorkItemEpic();
        try {
            if(response.getStatusCode() != 200) {
                throw new OctaneClientException("AGM_APP", "error of get getDoneDefinationOfUserStoryAndDefect and path: [http status code, error message]:" + response.getStatusCode() + "," + response.getData());
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

    public List<EpicEntity> createEpicInWorkspace(String sharedspaceId, String workspaceId, EpicCreateEntity epicCreateEntity) throws JsonProcessingException, IOException, JSONException {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/epics", baseURL, sharedspaceId, workspaceId);
        Map<String, String> headers = new HashMap<>();
        
        headers.put("Cookie", this.cookies);
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
  //    	headers.put("HPECLIENTTYPE", "HPE_SWAGGER_API");
  //    	headers.put("Cookie", this.cookies.replace("OCTANE_USER", "S_OCTANE_USER"));
        headers.put("Content-Type", MediaType.APPLICATION_JSON);
        String csrf = this.getCSRF(this.cookies);
        if (csrf != null) {
          headers.put("HPSSO-HEADER-CSRF", csrf);
        }
        
        RestResponse response = sendRequest(url, HttpMethod.POST, this.getJsonStrFromObject(epicCreateEntity), headers);
        if (HttpStatus.SC_CREATED != response.getStatusCode()) {
          this.logger.error("Error occurs when creating epic in Octane: Response code = " + response.getStatusCode());
          throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR", new String[] { response.getData() });
        }
        return (List<EpicEntity>) getDataContent(response.getData(), new TypeReference<List<EpicEntity>>(){});
    }

    public List<EpicAttr> getEpicPhase(final String sharedspaceId, final String workspaceId, final String phaseLogicName) throws JsonProcessingException, IOException, JSONException {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/phases?fields=id&query=%s%s%s",
            baseURL, sharedspaceId, workspaceId, "%22logical_name%3D'", phaseLogicName, "'%22");
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookies);
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        RestResponse response = sendRequest(url, HttpMethod.GET, null, headers);
        
        return (List<EpicAttr>)getDataContent(response.getData(), new TypeReference<List<EpicAttr>>(){});
    }
    
    public List<EpicAttr> getEpicParent(final String sharedspaceId, final String workspaceId, final String workitemSubtype) throws JsonProcessingException, IOException, JSONException {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/work_items?fields=id&query=%s%s%s",
            baseURL, sharedspaceId, workspaceId, "%22subtype%3D'", workitemSubtype, "'%22");
        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", this.cookies);
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        RestResponse response = sendRequest(url, HttpMethod.GET, null, headers);
    	
        return (List<EpicAttr>)getDataContent(response.getData(), new TypeReference<List<EpicAttr>>(){});
    }
    
    public String getJsonStrFromObject(Object sourceObj) throws JsonProcessingException {
    	ObjectMapper objectMapper = new ObjectMapper();
    	return objectMapper.writeValueAsString(sourceObj);
    }
    
    public String getCSRF(final String cookies) {
        String csrf = null;
        int csrfStart = cookies.indexOf("HPSSO_COOKIE_CSRF=");
        if (csrfStart > -1) {
            int csrfEnd = cookies.indexOf(";", csrfStart);
            csrf = cookies.substring(csrfStart + 18, csrfEnd);
        }
        return csrf;
    }
    
    private List<?> getDataContent(String jsonData, TypeReference<?> typeRef)
        throws JSONException, JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JSONObject obj = new JSONObject(jsonData);
        Object dataObj = obj.get("data");
        if(dataObj != null) {
            String arrayStr = dataObj.toString();			
            if(arrayStr.length() > 2){
                return mapper.readValue(arrayStr, typeRef);
            }
        }
        return null;

    }
}
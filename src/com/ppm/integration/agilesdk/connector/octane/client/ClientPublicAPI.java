package com.ppm.integration.agilesdk.connector.octane.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.ppm.integration.model.AgileEntityFieldValue;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.octane.OctaneConstants;
import com.ppm.integration.agilesdk.connector.octane.model.EpicAttr;
import com.ppm.integration.agilesdk.connector.octane.model.EpicCreateEntity;
import com.ppm.integration.agilesdk.connector.octane.model.EpicEntity;
import com.ppm.integration.agilesdk.connector.octane.model.FieldInfo;
import com.ppm.integration.agilesdk.connector.octane.model.GenericWorkItem;
import com.ppm.integration.agilesdk.connector.octane.model.OctaneUtils;
import com.ppm.integration.agilesdk.connector.octane.model.Release;
import com.ppm.integration.agilesdk.connector.octane.model.ReleaseTeam;
import com.ppm.integration.agilesdk.connector.octane.model.ReleaseTeams;
import com.ppm.integration.agilesdk.connector.octane.model.Releases;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpaces;
import com.ppm.integration.agilesdk.connector.octane.model.SimpleEntity;
import com.ppm.integration.agilesdk.connector.octane.model.Sprint;
import com.ppm.integration.agilesdk.connector.octane.model.Team;
import com.ppm.integration.agilesdk.connector.octane.model.Teams;
import com.ppm.integration.agilesdk.connector.octane.model.TimesheetItem;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemEpic;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpaces;
import com.ppm.integration.agilesdk.tm.AuthenticationInfo;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;


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

    private Integer currentUserName = null;

    private String CURRENT_USER_URL = "/api/current_user";

    private String cookies;

    private Proxy proxy = null;

    private int retryNumber = 0;

    public static final String DEFAULT_ENTITY_ITEM_URL =
            "%s/ui/entity-navigation?p=%s/%s&entityType=work_item&id=%s";

    private String SHART_SSO_GRANT_TOOL_TOKEN = "/authentication/grant_tool_token";
    
    private String STORE_TOOL_TOKEN = "/authentication/store_tool_token";

    public static final String KEY_LAST_UPDATE_DATE = "last_modified";

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

    public String getBaseURL() {
        return this.baseURL;
    }

    public boolean getAccessTokenWithFormFormat(String clientId, String clientSecret)
    {
        String url = String.format("%s/authentication/sign_in", baseURL);
        String data =
                String.format("{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"enable_csrf\": \"false\"}", clientId,
                        clientSecret);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", MediaType.APPLICATION_JSON);
        RestResponse response = sendRequest(url, HttpMethod.POST, data, headers);
        return verifyResult(HttpStatus.SC_OK, response.getStatusCode());
    }
    
    public boolean signOut(ValueSet values)
    {
      String url = String.format("%s/authentication/sign_out", new Object[] { this.baseURL });
      String clientId = (String)values.get("clientId");
      String clientSecret = (String)values.get("clientSecret");

      String data = String.format("{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"enable_csrf\": \"false\"}", new Object[] { clientId, clientSecret });

      Map headers = new HashMap();
      headers.put("Content-Type", "application/json");
      RestResponse response = sendRequest(url, "POST", data, headers);
      return verifyResult(200, response.getStatusCode());
    }

    public void setProxy(String host, int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }


    private RestResponse sendGet(String url) {
        return sendRequest(url, HttpMethod.GET, null);
    }

    private RestResponse sendRequest(String url, String method, String jsonData) {

        Map<String, String> headers = new HashMap<>();

        if (this.cookies != null) {
        headers.put("Cookie", this.cookies);
        }
        // headers.put("HPECLIENTTYPE", "HPE_PPM");
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

        if (jsonData != null) {
            headers.put("Content-Type", MediaType.APPLICATION_JSON);
        }

        return sendRequest(url, method, jsonData, headers);
    }

    private RestResponse sendSSORequest(String url, String method, String jsonData) {

        Map<String, String> headers = new HashMap<>();

        if (this.cookies != null) {
            headers.put("Cookie", this.cookies);
        }
        headers.put("HPECLIENTTYPE", "HPE_MQM_UI");
        headers.put("ALM_OCTANE_TECH_PREVIEW", "true");

        if (jsonData != null) {
            headers.put("Content-Type", MediaType.APPLICATION_JSON);
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

            // Some HTTPS servers (like Octane on AWS) do not negotiate if you start with TLS1. So let's only use TLS1.2
            if (con instanceof HttpsURLConnection) {
                // Only allowing secure protocols to connect
                System.setProperty("https.protocols", "TLSv1.2,SSLv3");
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
            throw new OctaneClientException("AGM_APP", "ERROR_IN_HTTP_CONNECTIVITY", new String[] {e.getMessage()});
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

        RestResponse response = null;

        try {
            response = sendGet(url);
        } catch (OctaneClientException oce) {
            if ("ERROR_ACCESS_FAILED".equals(oce.getMsgKey())) {
                // Client API user does not have acces to this work space; no big deal, it likely means we don't expect any data from there.
                return new ArrayList<TimesheetItem>(0);
            } else {
                throw oce;
            }
        }

        try {
            List<TimesheetItem> items = parseTimesheetItems(response.getData());
            return items;
        } catch (Exception e) {
            logger.error("error in timesheet retrieve:", e);
            throw new OctaneClientException("AGM_APP", "error when retrieving timesheet information:", e.getMessage());
        }
    }

    private List<TimesheetItem> parseTimesheetItems(String json) {
        try {
            org.json.JSONObject obj = new org.json.JSONObject(json);

            if (!obj.has("data")) {
                // Likely an error on Octane side, authentication or something else.
                if (obj.has("error_code") && obj.has("description") && obj.has("stack_trace")) {
                    // Octane "proper" Error
                    throw new RuntimeException("Octane error "+obj.getString("error_code")+ ": '"+obj.getString("description")+"'.\nOctane StackTrace:\n" + obj.getString("stack_trace"));
                } else {
                    // Unknown error
                    throw new RuntimeException("Unknown error, cannot parse Octane Response; retrieved payload from Octane: "+json);
                }
            }

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
                List<HttpCookie> cookie = HttpCookie.parse(con.getHeaderField(i));
                if (cookie.size() > 0) {
                    cookieVal = cookieVal + cookie.get(0).getName() + "=" + cookie.get(0).getValue() + ";";
                }
            }

        }
        return cookieVal;
    }

    public List<SharedSpace> getSharedSpaces() {

        String url = String.format("%s/api/shared_spaces?fields=id,name", baseURL);

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

        String url = String.format("%s/api/shared_spaces/%d/workspaces?fields=id,name", baseURL, sharedSpacesId);

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
                String.format("%s/api/shared_spaces/%d/workspaces/%d/releases/%d?fields=id,name,start_date,end_date", baseURL, sharedSpacesId, workSpaceId,
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
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/releases?fields=id,name,start_date,end_date&offset=%d&limit=%d", baseURL,
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
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/teams?fields=id,name,team_lead,number_of_members,estimated_velocity&offset=%d&limit=%d", baseURL,
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
                    baseURL, sharedSpaceId, workSpaceId, "?fields=capacity&query=%22team%3D%7Bid%3D", teamId, "%7D%22", offset, limit);

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
                String.format("%s/api/shared_spaces/%d/workspaces/%d/sprints?fields=id,name,release,start_date,end_date,creation_time,last_modified", baseURL,
                sharedSpaceId, workSpaceId));

        List<Sprint> results = new ArrayList<>(sprintsJson.size());

        for (JSONObject sprintJson : sprintsJson) {
            Sprint sprint = new Sprint();
            sprint.ParseJsonData(sprintJson);
            results.add(sprint);
        }

        return results;
    }

    public WorkItemRoot getWorkItems(int sharedSpaceId, int workSpaceId) {
        WorkItemRoot tempWorkItemRoot = new WorkItemRoot();
        boolean hasNext = true;
        int offset = 0;
        int limit = 200;
        do {
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/work_items?fields=id,name,subtype,parent&offset=%d&limit=%d", baseURL,
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

    public SimpleEntity getWorkItemRoot(int sharedSpaceId, int workSpaceId) {
        SimpleEntity tempWorkItemRoot = null;

        String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/work_item_roots?fields=id,name", baseURL, sharedSpaceId,
                workSpaceId);

        RestResponse response = sendGet(url);
        JSONObject obj = JSONObject.fromObject(response.getData());
        JSONArray roots = JSONArray.fromObject(obj.get("data"));
        if (roots.size() > 0) {
            tempWorkItemRoot = wrapperWorkItemRootFromData(roots.get(0));
        }

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

        String url =
                String.format("%s/api/shared_spaces/%d/workspaces/%d/epics/%s?fields=name,path,actual_story_points",
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

        JSONObject obj = JSONObject.fromObject(response.getData());
        JSONArray releases = (JSONArray)obj.get("data");
        JSONObject oneRelease = releases.getJSONObject(0);
        Release tempRelease = new Release();
        if (oneRelease != null) {
            tempRelease.ParseData(oneRelease.toString());
            tempRelease.setName(releaseName);
        }

        return tempRelease;
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
                baseURL, sharedspaceId, workspaceId, "%22entity_name%20EQ%20'", entityName,
                "';field_type%20IN%20'string','reference','memo','integer'%22");
        // "';visible_in_ui%20EQ%20true;editable%20EQ%20true;field_type%20IN%20'string','reference','memo'%22");
        List fieldsList = new ArrayList();
        RestResponse response = sendGet(url);
        JSONObject dataObj = JSONObject.fromObject(response.getData());
        if (dataObj != null) {
            JSONArray fieldsArray = dataObj.getJSONArray("data");
            for(int i = 0; i < fieldsArray.size(); i++) {
                JSONObject data = fieldsArray.getJSONObject(i);
                FieldInfo info = new FieldInfo(data);
                if (info.getFieldType() != null
                        && (filterFieldTypes(info)))
                {
                    fieldsList.add(info);
                }
            }
        }
        return fieldsList;
    }

    private boolean filterFieldTypes(FieldInfo field){
        String fieldType = field.getFieldType();
        switch (fieldType){
            case OctaneConstants.KEY_FIELD_STRING:
            case OctaneConstants.KEY_FIELD_USER_LIST:
            case OctaneConstants.KEY_SUB_TYPE_LIST_NODE:
            case OctaneConstants.KEY_FIELD_MEMO:
            case OctaneConstants.KEY_FIELD_INTEGER:
                return true;
            //hide some reference fields(eg: sprint, team) whose real field
            //type is actually auto complete list field.
            case OctaneConstants.KEY_AUTO_COMPLETE_LIST:
                String fieldName = field.getName();
                switch (fieldName){
                    //open phase, release
                    case OctaneConstants.KEY_FIELD_PHASE:
                    case OctaneConstants.KEY_FIELD_RELEASE:
                        return true;
                }
            default:
                return false;
        }
    }

    public List<AgileEntityFieldValue> getEntityFieldListNode(final String sharedSpaceId, final String workSpaceId,
            final String logicalName)
    {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/list_nodes?fields=id,name&query=%s%s%s", baseURL,
                sharedSpaceId, workSpaceId, "%22list_root={logical_name%20EQ%20^", logicalName, "^}%22");
        RestResponse response = sendGet(url);
        JSONObject dataObj = JSONObject.fromObject(response.getData());
        List<AgileEntityFieldValue> valueList = parseValueJson(dataObj);
        return valueList;
    }

    public List<AgileEntityFieldValue> getEntityFieldValueList(final String sharedSpaceId, final String workSpaceId,
            final String entityName, final String fieldName)
    {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/%s?fields=id,name", baseURL, sharedSpaceId,
                workSpaceId, fieldName);
        if(OctaneConstants.KEY_FIELD_PHASE_API_NAME.equals(fieldName)){
            url = String.format("%s&query=%s%s%s", url, "%22entity%20EQ%20'", entityName, "'%22");
        }
        RestResponse response = sendGet(url);
        JSONObject dataObj = JSONObject.fromObject(response.getData());
        List<AgileEntityFieldValue> valueList = parseValueJson(dataObj);

        return valueList;
    }
    
    public JSONArray getCommentsJsonFromWorkItems(final String sharedSpaceId, final String workSpaceId,
            final List<String> workItemIds)
    {
    	JSONArray dataList = new JSONArray();
    	if(!workItemIds.isEmpty()) {
    		StringBuilder idStr = new StringBuilder();
    		idStr.append("id%3D").append(workItemIds.get(0));
    		for(int i=1;i<workItemIds.size();i++) {
    			idStr.append("%7C%7C").append("id%3D").append(workItemIds.get(i));
    		}
    		String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/comments", baseURL, sharedSpaceId,
                    workSpaceId);
            url = String.format("%s?fields=id,owner_work_item,text&query=%s%s%s&order_by=last_modified", url, "%22(owner_work_item%3D%7B", idStr, "%7D)%22");
            RestResponse response = sendGet(url);
            JSONObject dataObj = JSONObject.fromObject(response.getData());
            dataList = dataObj.getJSONArray("data");
    	}
        
        return dataList;
    }

    public JSONArray getCommentsJsonFromWorkItem(final String sharedSpaceId, final String workSpaceId,
            final String workItemId)
    {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/comments", baseURL, sharedSpaceId,
                workSpaceId);
        url = String.format("%s?fields=id,owner_work_item,text&query=%s%s%s&order_by=last_modified", url, "%22(owner_work_item%3D%7Bid%3D", workItemId, "%7D)%22");
        RestResponse response = sendGet(url);
        JSONObject dataObj = JSONObject.fromObject(response.getData());
        JSONArray data = dataObj.getJSONArray("data");
        return data;
    }

    public List<String> getCommentsPlainTxtForWorkItem(final String sharedSpaceId, final String workSpaceId, final String workItemId){
        JSONArray data = getCommentsJsonFromWorkItem(sharedSpaceId, workSpaceId, workItemId);
        List result = new ArrayList<>();
        if(null == data || data.isEmpty()){
            return result;
        }else {
            //filter html tag, newline break
            String[] filterTag = {"<html>", "</html>", "<body>", "</body>", "<p>", "</p>", "\n", "&nbsp;"};
            for (int i = 0; i < data.size(); i++) {
                JSONObject comment = data.getJSONObject(i);
                String commentStr = comment.getString("text");
                for (String tag : filterTag) {
                    commentStr = commentStr.replaceAll(tag, "");
                }
                result.add(commentStr);
            }
        }
        return result;
    }

    private List<AgileEntityFieldValue> parseValueJson(JSONObject dataObj){
        List<AgileEntityFieldValue> valueList = new ArrayList<AgileEntityFieldValue>();
        if (dataObj != null) {
            JSONArray fieldsArray = dataObj.getJSONArray("data");
            for (int i = 0; i < fieldsArray.size(); i++) {
                JSONObject data = fieldsArray.getJSONObject(i);
                Map<String, String> map = new HashMap<String, String>();
                AgileEntityFieldValue fieldValue = new AgileEntityFieldValue();
                fieldValue.setId(data.getString(OctaneConstants.KEY_FIELD_ID));
                fieldValue.setName(data.getString(OctaneConstants.KEY_FIELD_NAME));
                valueList.add(fieldValue);
            }
        }
        return valueList;
    }

    //check if entity contains <comments>, if true, add comment and remove <comments> from entity
    public String addComment(final String sharedSpaceId, final String workspaceId, final String entity){

        JSONArray entityList = JSONArray.fromObject(entity);
        JSONObject entityObj = entityList.getJSONObject(0);
        if(entityObj.containsKey(OctaneConstants.KEY_FIELD_COMMENTS)){
            // add comments
            JSONObject commentJson = entityObj.getJSONObject(OctaneConstants.KEY_FIELD_COMMENTS);
            String url =
                    String.format("%s/api/shared_spaces/%s/workspaces/%s/comments", baseURL, sharedSpaceId, workspaceId);

            RestResponse response = sendRequest(url, HttpMethod.POST, commentJson.toString());
            if (HttpStatus.SC_CREATED != response.getStatusCode() && HttpStatus.SC_OK != response.getStatusCode()) {
                this.logger
                        .error("Error occurs when creating feature in Octane: Response code = " + response.getStatusCode());
                this.logger.error(response.getData());
                throw new OctaneClientException("AGM_APP", "ERROR_AGILE_ENTITY_SAVE_ERROR",
                        new String[] {getError(response.getData())});
            }
            entityObj.remove(OctaneConstants.KEY_FIELD_COMMENTS);
            return entityList.toString();
        }else{
            return entity;
        }

    }

    public JSONObject saveFeatureInWorkspace(final String sharedspaceId, final String workspaceId,
            final String entity, final String method)
    {
        // comments is a special field which can only add value but can not be updated/removed,
        // so execute add comment action before save  feature/story workspace
        String entityStr = addComment(sharedspaceId, workspaceId, entity);


        String url =
                String.format("%s/api/shared_spaces/%s/workspaces/%s/features", baseURL, sharedspaceId, workspaceId);

        RestResponse response = sendRequest(url, method, this.getJsonStrForPOSTData(entityStr));
        if (HttpStatus.SC_CREATED != response.getStatusCode() && HttpStatus.SC_OK != response.getStatusCode()) {
            this.logger
                    .error("Error occurs when creating feature in Octane: Response code = " + response.getStatusCode());
            this.logger.error(response.getData());
            throw new OctaneClientException("AGM_APP", "ERROR_AGILE_ENTITY_SAVE_ERROR",
                    new String[] {getError(response.getData())});
        }

        JSONObject obj = getCreateEntityFromResponse(response.getData());

        return this.getFeature(sharedspaceId, workspaceId, obj.getString("id")).get(0);
    }

    public JSONObject saveStoryInWorkspace(final String sharedspaceId, final String workspaceId,
            final String entity, final String method)
    {
        // comments is a special field which can only add value but can not be updated/removed,
        // so execute add comment action before save  feature/story workspace
        String entityStr = addComment(sharedspaceId, workspaceId, entity);

        String url =
                String.format("%s/api/shared_spaces/%s/workspaces/%s/stories", baseURL, sharedspaceId, workspaceId);

        RestResponse response = sendRequest(url, method, this.getJsonStrForPOSTData(entityStr));
        if (HttpStatus.SC_CREATED != response.getStatusCode() && HttpStatus.SC_OK != response.getStatusCode()) {
            this.logger.error("Error occurs when saving story in Octane: Response code = " + response.getStatusCode());
            this.logger.error(response.getData());
            throw new OctaneClientException("AGM_APP", "ERROR_AGILE_ENTITY_SAVE_ERROR",
                    new String[] {getError(response.getData())});
        }
        JSONObject obj = getCreateEntityFromResponse(response.getData());

        return this.getUserStory(sharedspaceId, workspaceId, obj.getString("id")).get(0);
    }
    
    public JSONObject saveEpicInWorkspace(final String sharedspaceId, final String workspaceId,
            final String entity, final String method)
    {
        // comments is a special field which can only add value but can not be updated/removed,
        // so execute add comment action before save  feature/story/epic workspace
        String entityStr = addComment(sharedspaceId, workspaceId, entity);

        String url =
                String.format("%s/api/shared_spaces/%s/workspaces/%s/epics", baseURL, sharedspaceId, workspaceId);

        RestResponse response = sendRequest(url, method, this.getJsonStrForPOSTData(entityStr));
        if (HttpStatus.SC_CREATED != response.getStatusCode() && HttpStatus.SC_OK != response.getStatusCode()) {
            this.logger.error("Error occurs when saving epic in Octane: Response code = " + response.getStatusCode());
            this.logger.error(response.getData());
            throw new OctaneClientException("AGM_APP", "ERROR_AGILE_ENTITY_SAVE_ERROR",
                    new String[] {getError(response.getData())});
        }
        JSONObject obj = getCreateEntityFromResponse(response.getData());

        return this.getEpic(sharedspaceId, workspaceId, obj.getString("id")).get(0);
    }

    private JSONObject getCreateEntityFromResponse(String jsonData) {
        JSONObject obj = null;
        try {
            JSONArray data = JSONObject.fromObject(jsonData).getJSONArray("data");
            if (data.size() > 0)
                obj = data.getJSONObject(0);
        } catch (JSONException e) {
            throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR",
                    "Error occurs when parse response data:" + jsonData);
        }
        return obj;

    }

    private String getError(String jsonData) {
        JSONObject obj;
        String description = "";
        obj = JSONObject.fromObject(jsonData);
        JSONArray errors = (JSONArray)(obj.get("errors"));
        if (errors.size() > 0) {
            JSONObject error = errors.getJSONObject(0);
            if (error.containsKey("description")) {
                description = error.getString("description");
            }
        }
        return description;
    }

    private String getJsonStrFromObject(Object sourceObj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(sourceObj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error when generating JSon String from object", e);
        }
    }

    private String getJsonStrForPOSTData(Object sourceObj) {
        JSONObject entityObj = new JSONObject();
        entityObj.put("data", sourceObj);
        return entityObj.toString();
    }

    private List getDataContent(String jsonData, TypeReference<?> typeRef) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            org.json.JSONObject obj = new org.json.JSONObject(jsonData);
            Object dataObj = obj.get("data");
            if (dataObj != null) {
                String arrayStr = dataObj.toString();
                if (arrayStr.length() >= 2) {
                    return (List) mapper.readValue(arrayStr, typeRef);
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
                String.format("%s/api/shared_spaces/%d/workspaces/%d/phases?fields=id,name", baseURL,
                        sharedSpaceId, workSpaceId));

        Map<String, String> phases = new HashMap<>(phasesJson.size());

        for (JSONObject phaseJson : phasesJson) {
            phases.put(phaseJson.getString("id"), phaseJson.getString("name"));
        }

        return phases;
    }

    public Map<String,String> getAllWorkspaceUsers() {

        List<JSONObject> usersJson = new JsonPaginatedOctaneGetter().get(
                String.format("%s/api/shared_spaces/%d/workspaces/%d/workspace_users?fields=email,id,full_name,name",
                        baseURL,
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
    
    public List<JSONObject> getEpic(String sharedspaceId, String workspaceId, String id) {
        if (id == null || "".equals(id)) {
            return null;
        }

        String query = "id=" + id;
        List<JSONObject> workItemsJson = getEpicJson(sharedspaceId, workspaceId, query);
        return workItemsJson;
    }
    
    public List<JSONObject> getEpics(String sharedspaceId, String workspaceId, Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        String query = "id%20IN%20" + StringUtils.join(ids, ",");
        List<JSONObject> workItemsJson = getEpicJson(sharedspaceId, workspaceId, query);

        return workItemsJson;
    }

    public List<JSONObject> getUserStory(String sharedspaceId, String workspaceId, String id) {
        if (id == null || "".equals(id)) {
            return null;
        }

        String query = "id=" + id;
        List<JSONObject> workItemsJson = getUserStoriesJson(sharedspaceId, workspaceId, query);
        return workItemsJson;
    }

    public List<JSONObject> getUserStories(String sharedspaceId, String workspaceId, Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        String query = "id%20IN%20" + StringUtils.join(ids, ",");
        List<JSONObject> workItemsJson = getUserStoriesJson(sharedspaceId, workspaceId, query);

        return workItemsJson;
    }

    public List<JSONObject> getFeature(String sharedspaceId, String workspaceId, String id) {
        if (id == null || "".equals(id)) {
            return null;
        }

        String query = "id=" + id;
        List<JSONObject> workItemsJson = getFeatureJson(sharedspaceId, workspaceId, query);
        return workItemsJson;
    }

    public List<JSONObject> getFeatures(String sharedspaceId, String workspaceId, Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        String query = "id%20IN%20" + StringUtils.join(ids, ",");
        List<JSONObject> workItemsJson = getFeatureJson(sharedspaceId, workspaceId, query);

       return workItemsJson;
    }

    public void deleteEntity(String sharedspaceId, String workspaceId, String entityType, String entityId) {
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/%s/%s", baseURL, sharedspaceId, workspaceId,
                entityType, entityId);

        RestResponse response = sendRequest(url, HttpMethod.DELETE, null);
        if (HttpStatus.SC_OK != response.getStatusCode() && HttpStatus.SC_NOT_FOUND != response.getStatusCode()) {
            this.logger.error("Error occurs when saving story in Octane: Response code = " + response.getStatusCode());
            throw new OctaneClientException("AGM_APP", "ERROR_HTTP_CONNECTIVITY_ERROR",
                    new String[] {getError(response.getData())});
        }

    }
    
    public List<JSONObject> getEpicsAfterDate(String sharedspaceId, String workspaceId, Set<String> ids,
            Date updateDate)
    {
        if (null == updateDate) {
            return getEpics(sharedspaceId, workspaceId, ids);
        }

        String query = "";
        if (null != ids && !ids.isEmpty()) {
            query += "id%20IN%20" + StringUtils.join(ids, ",") + "%20;%20";
        }
        query += "last_modified%20GT%20^" + transformDateFormat(updateDate) + "^";

        List<JSONObject> workItemsJson = getEpicJson(sharedspaceId, workspaceId, query);

        return workItemsJson;
    }

    public List<JSONObject> getUserStoriesAfterDate(String sharedspaceId, String workspaceId, Set<String> ids,
            Date updateDate)
    {
        if (null == updateDate) {
            return getUserStories(sharedspaceId, workspaceId, ids);
        }

        String query = "";
        if (null != ids && !ids.isEmpty()) {
            query += "id%20IN%20" + StringUtils.join(ids, ",") + "%20;%20";
        }
        query += "last_modified%20GT%20^" + transformDateFormat(updateDate) + "^";

        List<JSONObject> workItemsJson = getUserStoriesJson(sharedspaceId, workspaceId, query);

        return workItemsJson;
    }

    public List<JSONObject> getFeaturesAfterDate(String sharedspaceId, String workspaceId, Set<String> ids,
            Date updateDate)
    {
        if (null == updateDate) {
            return getFeatures(sharedspaceId, workspaceId, ids);
        }

        String query = "";
        if (null != ids && !ids.isEmpty()) {
            query += "id%20IN%20" + StringUtils.join(ids, ",") + "%20;%20";
        }
        query += "last_modified%20GT%20^" + transformDateFormat(updateDate) + "^";

        List<JSONObject> workItemsJson = getFeatureJson(sharedspaceId, workspaceId, query);

        return workItemsJson;
    }


    public JSONArray getUsersByIds(String sharedspaceId, String workspaceId, String[] ids) {
        String query = "";
        if (null != ids && ids.length > 0) {
            query += "\"id IN '" + StringUtils.join(ids, "','")  + "'\"";
        } else {
            return null;
        }
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = String.format(
                "%s/api/shared_spaces/%s/workspaces/%s/workspace_users?fields=email,id,full_name,name&query=%s",
                baseURL,
                sharedspaceId, workspaceId, query);
        RestResponse response = sendGet(url);
        JSONObject dataObj = JSONObject.fromObject(response.getData());
        JSONArray userList = JSONArray.fromObject(dataObj.get("data"));
        return userList;
    }

    public JSONArray getUsersByEmails(String sharedspaceId, String workSpaceId, String[] emails) {
        String query = "";
        if (null != emails && emails.length > 0) {
            query += "\"email IN '" + StringUtils.join(emails, "','") + "'\"";
        } else {
            return null;
        }
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/workspace_users?fields=email,id,full_name,name&query=%s", baseURL,
                sharedspaceId, workSpaceId, query);
        RestResponse response = sendGet(url);
        JSONObject dataObj = JSONObject.fromObject(response.getData());
        JSONArray userList = JSONArray.fromObject(dataObj.get("data"));
        return userList;
    }

    private String transformDateFormat(Date dateStr) {
        String dateString;
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateString = sdf.format(dateStr);

        return dateString;
    }
    
    private List<JSONObject> getEpicJson(String sharedspaceId, String workspaceId, String queryFilter) {

        List<FieldInfo> fieldsInfos = getEntityFields(sharedspaceId, workspaceId, "epic");
        List fieldNames = new ArrayList();
        for (FieldInfo field : fieldsInfos) {
            fieldNames.add(field.getName());
        }
        fieldNames.add(KEY_LAST_UPDATE_DATE);
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/epics?fields=%s", baseURL, sharedspaceId, workspaceId,StringUtils.join(fieldNames, ","));
        if (!StringUtils.isBlank(queryFilter)) {
            url += "&query=\"" + queryFilter + "\"";
        }

        List<JSONObject> resultJsonList = new JsonPaginatedOctaneGetter().get(url);
        resetComments(resultJsonList, sharedspaceId, workspaceId);
        return resultJsonList;
    }

    private List<JSONObject> getUserStoriesJson(String sharedspaceId, String workspaceId, String queryFilter) {

        List<FieldInfo> fieldsInfos = getEntityFields(sharedspaceId, workspaceId, "story");
    	List fieldNames = new ArrayList();
        for (FieldInfo field : fieldsInfos) {
    		fieldNames.add(field.getName());
    	}
        fieldNames.add(KEY_LAST_UPDATE_DATE);
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/stories?fields=%s", baseURL, sharedspaceId, workspaceId,StringUtils.join(fieldNames, ","));
        if (!StringUtils.isBlank(queryFilter)) {
            url += "&query=\"" + queryFilter + "\"";
        }

        List<JSONObject> resultJsonList = new JsonPaginatedOctaneGetter().get(url);
        resetComments(resultJsonList, sharedspaceId, workspaceId);
        return resultJsonList;
    }

    private List<JSONObject> getFeatureJson(String sharedspaceId, String workspaceId, String queryFilter) {

        List<FieldInfo> fieldsInfos = getEntityFields(sharedspaceId, workspaceId, "feature");
    	List fieldNames = new ArrayList();
        for (FieldInfo field : fieldsInfos) {
    		fieldNames.add(field.getName());
    	}
        fieldNames.add(KEY_LAST_UPDATE_DATE);
        String url = String.format("%s/api/shared_spaces/%s/workspaces/%s/features?fields=%s", baseURL, sharedspaceId, workspaceId, StringUtils.join(fieldNames, ","));
        if (!StringUtils.isBlank(queryFilter)) {
            url += "&query=\"" + queryFilter + "\"";
        }
        List<JSONObject> resultJsonList = new JsonPaginatedOctaneGetter().get(url);
        resetComments(resultJsonList, sharedspaceId, workspaceId);
        return resultJsonList;
    }

    private void resetComments(List<JSONObject> resultJsonList, String sharedspaceId, String workspaceId){
    	List<String> workIteamIds = new ArrayList<>();
        for (JSONObject resultJson : resultJsonList) {
            if(resultJson.has(OctaneConstants.KEY_FIELD_COMMENTS)){
            	JSONObject comments = resultJson.getJSONObject(OctaneConstants.KEY_FIELD_COMMENTS);
            	int commentsSize = comments.getInt("total_count");
            	if(commentsSize>=0) {
            		workIteamIds.add(resultJson.getString("id"));
            	}
                
            }
        }
        
        // it is a entity id-comments Map
        JSONArray commentsJson = getCommentsJsonFromWorkItems(sharedspaceId, workspaceId, workIteamIds);
        Map<Long,StringBuilder> entityComments = new HashMap<Long,StringBuilder>();
        for (int i = 0; i < commentsJson.size(); i++) {
        	JSONObject comment = commentsJson.getJSONObject(i);
        	Long commentEntityId = comment.getJSONObject("owner_work_item").getLong("id");
        	StringBuilder entityComment = entityComments.get(commentEntityId);
        	if(entityComment==null) {
        		 entityComment = new StringBuilder("");
        		 entityComment.append(comment.getString("text"));
        		 entityComments.put(commentEntityId, entityComment);
        	} else {
        		entityComment.append(OctaneConstants.COMMENTS_SEPARATOR);
        		entityComment.append(comment.getString("text"));
        	}
        }
		for (JSONObject resultJson : resultJsonList) {
			if (resultJson.has(OctaneConstants.KEY_FIELD_COMMENTS)) {
				if (entityComments.get(resultJson.getLong("id")) != null) {
					resultJson.put(OctaneConstants.KEY_FIELD_COMMENTS,
							entityComments.get(resultJson.getLong("id")).toString());
				} else {
					resultJson.put(OctaneConstants.KEY_FIELD_COMMENTS,"");
				}
			}
		}
    }

    private SimpleEntity wrapperWorkItemRootFromData(Object data) {
        SimpleEntity root = new SimpleEntity();
        JSONObject obj = JSONObject.fromObject(data);
        root.id = obj.getString("id");
        root.name = obj.getString("name");
        root.type = obj.getString("type");
        return root;
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

    public String getSSOURL() {
        String url = baseURL + SHART_SSO_GRANT_TOOL_TOKEN;
        RestResponse response = sendGet(url);
        if (HttpStatus.SC_OK == response.getStatusCode()) {
            return response.getData();
        } else {
            throw new OctaneClientException("OCTANE_APP", "FAIL_TO_RETRIEVE_SSO_URL");
        }
    }
    
    public AuthenticationInfo getSSOAuthentication(String identifier) {
        AuthenticationInfo userInfo = new AuthenticationInfo();
        String url = baseURL + SHART_SSO_GRANT_TOOL_TOKEN;
        JSONObject identifierObj = new JSONObject();
        identifierObj.put("identifier", identifier);
        RestResponse response = sendSSORequest(url, HttpMethod.POST, identifierObj.toString());
        String result = null;
        if (HttpStatus.SC_OK == response.getStatusCode()) {
            result = response.getData();
            JSONObject obj = JSONObject.fromObject(result);
            String cookie = obj.getString("access_token");
            String cookieKey = obj.getString("cookie_name");
            this.cookies = cookieKey + "=" + cookie;
            RestResponse currentUser = sendSSORequest(baseURL + CURRENT_USER_URL, HttpMethod.GET, null);
            if (HttpStatus.SC_OK == currentUser.getStatusCode()) {
                JSONObject user = JSONObject.fromObject(currentUser.getData());
                userInfo.setLoginName(user.get("name").toString());
                userInfo.setEmail(user.get("email").toString());
                userInfo.setFullName(user.get("full_name").toString());
            } else {
                throw new OctaneClientException("OCTANE_APP", "FAIL_TO_RETRIEVE_USER_INFO");
            }
        } else {
            throw new OctaneClientException("OCTANE_APP", "TIP_TO_AUTHENTICATION");
        }

        return userInfo;
    }
}
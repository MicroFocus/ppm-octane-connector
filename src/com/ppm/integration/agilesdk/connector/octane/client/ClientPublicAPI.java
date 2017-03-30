package com.ppm.integration.agilesdk.connector.octane.client;

import com.kintana.core.server.execution.ParseException;
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
                String.format("{  \"client_id\":\"%s\",\"client_secret\":\"%s\",\"enable_csrf\": \"true\"}", clientId,
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
            } else if (responseCode == 403) {
                throw new OctaneClientException("OCTANE_API", "ERROR_AUTHENTICATION_FAILED");
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

            return new RestResponse(responseCode, output);
        } catch (IOException e) {
            logger.error("error in http connectivity:", e);
            throw new OctaneClientException("AGM_APP", "error in http connectivity:", e.getMessage());
        }
    }

    private boolean verifyResult(int expected, int result) {
        boolean isVerify = false;
        if (expected != result) {
            logger.error("error in access token retrieve.");
            throw new OctaneClientException("AGM_APP", "error in access token retrieve.");
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
                        String.format("%s/api/shared_spaces/%d/workspaces/%d/teams%s%s%s?offset=%d&limit=%d", baseURL,
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
            String url = String.format("%s/api/shared_spaces/%d/workspaces/%d/team_members%s%d%s?offset=%d&limit=%d",
                    baseURL, sharedSpaceId, workSpaceId, "?query=%22team%3D%7Bid%3D", teamId, "%7D%22", offset, limit);

            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", this.cookies);
            headers.put("HPECLIENTTYPE", "HPE_MQM_UI");

            RestResponse response = sendRequest(url, method, null, headers);
            //get the sum of team member'capacity
            net.sf.json.JSONObject object = net.sf.json.JSONObject.fromObject(response.getData());
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

}
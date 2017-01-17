package com.ppm.integration.agilesdk.connector.octane.client;

import com.kintana.core.logging.LogManager;
import com.kintana.core.logging.Logger;
import com.mercury.itg.core.ContextFactory;
import com.mercury.itg.core.model.Context;
import com.mercury.itg.core.user.model.User;
import com.ppm.integration.agilesdk.connector.octane.model.Release;
import com.ppm.integration.agilesdk.connector.octane.model.Releases;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpaces;
import com.ppm.integration.agilesdk.connector.octane.model.WorkItemRoot;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpaces;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientRequest;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.client.handlers.ClientHandler;
import org.apache.wink.client.handlers.HandlerContext;

public class Client {
    public static final String AUTHORIZATION_SIGN_IN_URL = "/authentication/sign_in";

    public static final String AUTHORIZATION_SIGN_OUT_URL = "/authentication/sign_out";

    protected static final Logger logger = LogManager.getLogger(Client.class);

    public boolean isGetDefect = false;

    protected String baseURL = "";

    private User _currentUser = null;

    private List<String> cookie = null;

    private ClientConfig config;

    public Client(String baseURL) {
        try {
            Context ctx = ContextFactory.getThreadContext();
            _currentUser = (User)ctx.get(Context.USER);
        } catch (Exception ex) {
            // if error, do nothing
        }

        this.baseURL = baseURL.trim();
        if (this.baseURL.endsWith("/")) {
            this.baseURL = this.baseURL.substring(0, this.baseURL.length() - 1);
        }

        this.config = new ClientConfig().handlers(new ClientHandler() {

            @Override public ClientResponse handle(ClientRequest req, HandlerContext context) throws Exception {

                req.getHeaders().add("Content-Type", "application/json");
                //req.getHeaders().add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

                URI origURI = req.getURI();
                if (!isAuthURI(origURI)) {
                    req.getHeaders().put("Cookie", cookie);
                    req.getHeaders().add("HPECLIENTTYPE", "HPE_MQM_UI");
                }

                debug(req.getURI().toString());

                return context.doChain(req);
            }
        }).applications(new Application() {
            @Override public Set<Class<?>> getClasses() {
                Set<Class<?>> clazz = new HashSet<Class<?>>();
                clazz.add(JAXBProvider.class);
                return clazz;
            }
        });
    }

    public void setCurrentUser(User user) {
        _currentUser = user;
    }

    protected void debug(String agmURL) {
        StringBuffer sb = new StringBuffer();
        if (_currentUser != null) {
            sb.append("(user ID=");
            sb.append(_currentUser.getUserId());
            sb.append("; user name=");
            sb.append(_currentUser.getUsername());
            sb.append(")");
        }

        sb.append(agmURL);
        logger.error(sb.toString());
    }

    public Client proxy(String host, int port) {
        this.config.proxyHost(host).proxyPort(port);
        return this;
    }

    public Client auth(String username, String password) {

        JSONObject json = CredentialJson.toJSONObject(username, password);

        ClientResponse resp = null;
        Resource rsc = this.oneResource(AUTHORIZATION_SIGN_IN_URL);
        resp = rsc.post(json.toString());

        if (resp.getStatusCode() != 200) {
            throw new OctaneClientException("AGM_API", "ERROR_AUTHENTICATION_FAILED");
        }

        this.cookie = resp.getHeaders().get("Set-Cookie");
        return this;
    }

    public Client auth(String username, String password, boolean enable_csrf) {

        JSONObject json = CredentialJson.toJSONObject(username, password, enable_csrf);

        ClientResponse resp = null;
        Resource rsc = this.oneResource(AUTHORIZATION_SIGN_IN_URL);
        resp = rsc.post(json.toString());

        if (resp.getStatusCode() != 200) {
            throw new OctaneClientException("AGM_API", "ERROR_AUTHENTICATION_FAILED");
        }

        this.cookie = resp.getHeaders().get("Set-Cookie");
        return this;
    }

    public Client auth(List<String> cookies) {
        this.cookie = cookies;
        return this;
    }

    public List<String> getCookies() {
        return this.cookie;
    }

    protected String getURLParamNameForFieldQuery() {
        return "query";
    }

    protected String getURLParamNameForVisibleFields() {
        return "fields";
    }

    public Resource oneResource(String url, FieldQuery... queries) {
        return oneResource(url, null, queries);
    }

    public Resource oneResource(String url, String[] fields, FieldQuery... queries) {

        Resource rsc = new RestClient(this.config).resource(baseURL + url);

        if (fields != null && fields.length > 0) {
            rsc.queryParam(getURLParamNameForVisibleFields(), StringUtils.join(fields, ','));
        }

        if (queries.length > 0) {
            StringBuffer sb = new StringBuffer();
            sb.append("%7B");
            boolean isFirst = true;
            for (FieldQuery q : queries) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(';');
                }

                sb.append(q.toQueryString());
            }
            sb.append("%7D");

            rsc.queryParam(getURLParamNameForFieldQuery(), sb.toString());
        }

        return rsc;
    }

    protected boolean isAuthURI(URI uri) {
        //        return false;
        String path = uri.getPath();
        return AUTHORIZATION_SIGN_IN_URL.equals(path) || AUTHORIZATION_SIGN_OUT_URL.equals(path);
    }

    public List<SharedSpace> getSharedSpaces() {

        ClientResponse response =
                oneResource("/api/shared_spaces").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        SharedSpaces tempSharedSpace = new SharedSpaces();
        tempSharedSpace.SetCollection(response.getEntity(String.class));

        return tempSharedSpace.getCollection();
    }

    public List<WorkSpace> getWorkSpaces(String sharedSpacesId) {

        ClientResponse response = oneResource(String.format("/api/shared_spaces/%s/workspaces", sharedSpacesId))
                .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        WorkSpaces tempWorkSpace = new WorkSpaces();
        tempWorkSpace.SetCollection(response.getEntity(String.class));
        return tempWorkSpace.getCollection();
    }

    public List<Release> getReleases(String sharedSpaceId, String workSpaceId) {
        List<Release> results = new ArrayList<>();
        boolean hasNext = true;
        int offset = 0;
        int limit = 100;
        do {
            ClientResponse response = oneResource(
                    String.format("/api/shared_spaces/%s/workspaces/%s/releases?offset=%d&limit=%d", sharedSpaceId,
                            workSpaceId, offset, limit)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            Releases tempReleases = new Releases();
            tempReleases.SetCollection(response.getEntity(String.class));
            results.addAll(tempReleases.getCollection());
            if (tempReleases.getCollection().size() == limit) {
                offset += limit;
            } else {
                hasNext = false;
            }
        } while (hasNext);
        return results;
    }

    public Release getRelease(String sharedSpaceId, String workSpaceId, String releaseId) {
        ClientResponse response = oneResource(
                String.format("/api/shared_spaces/%s/workspaces/%s/releases/%s", sharedSpaceId, workSpaceId, releaseId))
                .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        Release tempRelease = new Release();
        tempRelease.ParseData(response.getEntity(String.class));
        return tempRelease;
    }

    public WorkItemRoot getWorkItems(String sharedSpaceId, String workSpaceId, String releaseId) {
        WorkItemRoot tempWorkItemRoot = new WorkItemRoot();
        boolean hasNext = true;
        int offset = 0;
        int limit = 200;
        isGetDefect = false;
        do {
            ClientResponse response = oneResource(
                    String.format("/api/shared_spaces/%s/workspaces/%s/work_items?offset=%d&limit=%d", sharedSpaceId,
                            workSpaceId, offset, limit)).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            tempWorkItemRoot.GetTempParseData(response.getEntity(String.class), isGetDefect);
            if (tempWorkItemRoot.length == limit) {
                offset += limit;
            } else {
                hasNext = false;
            }
        } while (hasNext);
        tempWorkItemRoot.ParseDataIntoDetail(releaseId);
        return tempWorkItemRoot;
    }

}

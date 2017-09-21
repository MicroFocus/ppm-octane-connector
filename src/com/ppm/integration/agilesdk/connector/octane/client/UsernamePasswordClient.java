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
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpaces;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

/**
 * This Octane client will use Username + Password for authentication. You should NOT use it except if you have a good reason.<br>
 *     Current uses are:
 *     <ul>
 *         <li>Authenticate user in Timesheet Integration page to ensure they are who they claim they are.</li>
 *         <li>Authenticate user in Work plan task mapping page to only retrieve the list of workspaces they have access to.</li>
 *     </ul>
 */
public class UsernamePasswordClient {
    public static final String AUTHORIZATION_SIGN_IN_URL = "/authentication/sign_in";

    public static final String AUTHORIZATION_SIGN_OUT_URL = "/authentication/sign_out";

    protected static final Logger logger = LogManager.getLogger(UsernamePasswordClient.class);

    protected String baseURL = "";

    private List<String> cookie = null;

    private ClientConfig config;

    public UsernamePasswordClient(String baseURL) {

        this.baseURL = baseURL.trim();
        if (this.baseURL.endsWith("/")) {
            this.baseURL = this.baseURL.substring(0, this.baseURL.length() - 1);
        }

        this.config = new ClientConfig().handlers(new ClientHandler() {

            @Override public ClientResponse handle(ClientRequest req, HandlerContext context) throws Exception {

                req.getHeaders().add("Content-Type", "application/json");

                URI origURI = req.getURI();
                if (!isAuthURI(origURI)) {
                    req.getHeaders().put("Cookie", cookie);
                    req.getHeaders().add("HPECLIENTTYPE", "HPE_MQM_UI");
                }

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

    public UsernamePasswordClient proxy(String host, int port) {
        this.config.proxyHost(host).proxyPort(port);
        return this;
    }

    public UsernamePasswordClient auth(String username, String password) {

        return auth(username, password, false);
    }

    public UsernamePasswordClient auth(String username, String password, boolean enable_csrf) {

        JSONObject json = enable_csrf ? CredentialJson.toJSONObject(username, password, enable_csrf) : CredentialJson.toJSONObject(username, password);

        ClientResponse resp;
        Resource rsc = this.oneResource(AUTHORIZATION_SIGN_IN_URL);
        resp = rsc.post(json.toString());

        if (resp.getStatusCode() != 200) {
            throw new OctaneClientException("AGM_API", "ERROR_AUTHENTICATION_FAILED");
        }

        this.cookie = resp.getHeaders().get("Set-Cookie");
        return this;
    }

    public UsernamePasswordClient auth(List<String> cookies) {
        this.cookie = cookies;
        return this;
    }

    public List<String> getCookies() {
        return this.cookie;
    }

    private String getURLParamNameForFieldQuery() {
        return "query";
    }

    private String getURLParamNameForVisibleFields() {
        return "fields";
    }

    private Resource oneResource(String url, FieldQuery... queries) {
        return oneResource(url, null, queries);
    }

    private Resource oneResource(String url, String[] fields, FieldQuery... queries) {

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

    /**
     * @return true if the URL is the url of sign in or sign out.
     */
    private boolean isAuthURI(URI uri) {
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


}

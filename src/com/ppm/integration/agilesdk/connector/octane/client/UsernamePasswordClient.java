package com.ppm.integration.agilesdk.connector.octane.client;

import com.kintana.core.logging.LogManager;
import com.kintana.core.logging.Logger;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpace;
import com.ppm.integration.agilesdk.connector.octane.model.SharedSpaces;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpace;
import com.ppm.integration.agilesdk.connector.octane.model.WorkSpaces;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

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

    protected String baseURL;

    private List<String> cookie = null;

    private Proxy proxy;

    public UsernamePasswordClient(String baseURL) {

        this.baseURL = baseURL.trim();
        if (this.baseURL.endsWith("/")) {
            this.baseURL = this.baseURL.substring(0, this.baseURL.length() - 1);
        }
    }

    public UsernamePasswordClient proxy(String host, int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        return this;
    }

    public UsernamePasswordClient auth(String username, String password) {

        return auth(username, password, false);
    }

    public UsernamePasswordClient auth(String username, String password, boolean enable_csrf) {
        String url = this.oneResource(AUTHORIZATION_SIGN_IN_URL);
        String payload = enable_csrf ? CredentialJson.toJSONObject(username, password, enable_csrf).toString()
                : CredentialJson.toJSONObject(username, password).toString();
        RestResponse response = executeTextRequest(url, HttpMethod.POST, createHeaders(URI.create(url), null), payload);

        if (response.getStatusCode() != 200) {
            throw new OctaneClientException("AGM_API", "ERROR_AUTHENTICATION_FAILED");
        }

        this.cookie = response.getHeaders().get(HttpHeaders.SET_COOKIE);
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

    private String oneResource(String url, FieldQuery... queries) {
        return oneResource(url, null, queries);
    }

    private String oneResource(String url, String[] fields, FieldQuery... queries) {
        prepareSecureProtocols();

        StringBuilder resourceUrl = new StringBuilder(baseURL).append(url);

        if (fields != null && fields.length > 0) {
            appendQueryParam(resourceUrl, getURLParamNameForVisibleFields(), String.join(",", fields));
        }

        if (queries.length > 0) {
            StringBuilder sb = new StringBuilder();
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

            appendQueryParam(resourceUrl, getURLParamNameForFieldQuery(), sb.toString());
        }

        return resourceUrl.toString();
    }

    /**
     * @return true if the URL is the url of sign in or sign out.
     */
    private boolean isAuthURI(URI uri) {
        String path = uri.getPath();
        return AUTHORIZATION_SIGN_IN_URL.equals(path) || AUTHORIZATION_SIGN_OUT_URL.equals(path);
    }

    public List<SharedSpace> getSharedSpaces() {
        String url = oneResource("/api/shared_spaces");
        RestResponse response = executeTextRequest(url, HttpMethod.GET,
                createHeaders(URI.create(url), MediaType.APPLICATION_JSON_VALUE), null);
        SharedSpaces tempSharedSpace = new SharedSpaces();
        tempSharedSpace.SetCollection(response.getData());

        return tempSharedSpace.getCollection();
    }

    public List<WorkSpace> getWorkSpaces(String sharedSpacesId) {
        String url = oneResource(String.format("/api/shared_spaces/%s/workspaces", sharedSpacesId));
        RestResponse response = executeTextRequest(url, HttpMethod.GET,
                createHeaders(URI.create(url), MediaType.APPLICATION_JSON_VALUE), null);
        WorkSpaces tempWorkSpace = new WorkSpaces();
        tempWorkSpace.SetCollection(response.getData());
        return tempWorkSpace.getCollection();
    }

    private HttpHeaders createHeaders(URI uri, String acceptType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (acceptType != null) {
            headers.set(HttpHeaders.ACCEPT, acceptType);
        }
        if (!isAuthURI(uri)) {
            if (cookie != null) {
                headers.put(HttpHeaders.COOKIE, cookie);
            }
            headers.set("HPECLIENTTYPE", "HPE_MQM_UI");
        }
        return headers;
    }

    private RestResponse executeTextRequest(String url, HttpMethod method, HttpHeaders headers, String data) {
        try {
            ClientPublicAPI clientPublicAPI = new ClientPublicAPI(this.baseURL);
            clientPublicAPI.setProxy(this.proxy);
            return clientPublicAPI.executeTextRequest(url, method.name(), toHeadersMap(headers), data);
        } catch (IOException e) {
            logger.error("error in http connectivity:", e);
            throw new OctaneClientException("AGM_API", "ERROR_IN_HTTP_CONNECTIVITY", e.getMessage());
        }
    }

    private Map<String, String> toHeadersMap(HttpHeaders headers) {
        Map<String, String> requestHeaders = new LinkedHashMap<String, String>();
        for (String headerName : headers.keySet()) {
            List<String> values = headers.get(headerName);
            if (values == null || values.isEmpty()) {
                continue;
            }
            requestHeaders.put(headerName, HttpHeaders.COOKIE.equalsIgnoreCase(headerName)
                    ? String.join("; ", values)
                    : String.join(", ", values));
        }
        return requestHeaders;
    }

    private void prepareSecureProtocols() {
        System.setProperty("https.protocols", "TLSv1.2,SSLv3");
    }

    private void appendQueryParam(StringBuilder urlBuilder, String name, String value) {
        urlBuilder.append(urlBuilder.indexOf("?") >= 0 ? '&' : '?').append(name).append('=').append(value);
    }



}

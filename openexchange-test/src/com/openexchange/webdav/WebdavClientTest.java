
package com.openexchange.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.webdav.lib.WebdavResource;
import org.apache.webdav.lib.WebdavResources;
import org.junit.After;
import org.junit.Before;
import com.meterware.httpunit.Base64;
import com.meterware.httpunit.WebRequest;
import com.openexchange.groupware.configuration.AbstractConfigWrapper;
import com.openexchange.test.WebdavInit;

public class WebdavClientTest {

    protected Properties webdavProps;
    protected String login;
    protected String password;
    protected String hostname;

    protected List<String> clean = new ArrayList<String>();
    private String path;

    @Before
    public void setUp() throws Exception {
        // Copied from AbstractWebdavTest
        webdavProps = WebdavInit.getWebdavProperties();
        login = AbstractConfigWrapper.parseProperty(webdavProps, "login", "");
        password = AbstractConfigWrapper.parseProperty(webdavProps, "password", "");
        hostname = AbstractConfigWrapper.parseProperty(webdavProps, "hostname", "localhost");
        path = AbstractConfigWrapper.parseProperty(webdavProps, "infostore_subpath", "");

    }

    @After
    public void tearDown() throws Exception {
        for (final String url : clean) {
            getResource(url).deleteMethod();
        }
    }

    protected WebdavResource getResource(final String url, final String login, final String password) throws HttpException, IOException {
        final HttpURL httpUrl = new HttpURL(getUrl(url));
        httpUrl.setUserinfo(login, password);
        final WebdavResource res = new WebdavResource(httpUrl, 0, WebdavResource.NOACTION);
        return res;
    }

    protected String getUrl(String url) {
        return "http://" + hostname + "/servlet/webdav.infostore/" + path + "/" + url;
    }

    protected WebdavResource getResource(final String url) throws HttpException, IOException {
        return getResource(url, login, password);
    }

    public void mkdir(final String path) throws HttpException, IOException {
        getResource(path).mkcolMethod();
    }

    public void save(final String path, final String data) throws HttpException, IOException {
        getResource(path).putMethod(data);
    }

    public void cp(final String from, final String to) throws HttpException, IOException {
        getResource(from).copyMethod(getUrl(to));
    }

    public void mv(final String from, final String to) throws HttpException, IOException {
        getResource(from).moveMethod(getUrl(to));
    }

    public void lock(final String resource) throws HttpException, IOException {
        getResource(resource).lockMethod();
    }

    public String lock(final String resource, int seconds) throws HttpException, IOException {
        WebdavResource res = getResource(resource);
        boolean locked = res.lockMethod(getClass().getName(), seconds);
        assertTrue("Lock failed", locked);
        return res.getLockDiscovery().getActiveLocks()[0].getLockToken();
    }

    public void assertContent(final String path, final String... names) throws HttpException, IOException {
        final WebdavResource res = getResource(path);
        final WebdavResources resources = res.getChildResources();
        final Set<String> expected = new HashSet<String>(Arrays.asList(names));

        final Enumeration enumeration = resources.getResourceNames();
        while (enumeration.hasMoreElements()) {
            final String name = (String) enumeration.nextElement();
            assertTrue(name + " not expected", expected.remove(name));
        }
        assertTrue(expected.toString(), expected.isEmpty());
    }

    public void assertBody(final String path, final InputStream body) throws HttpException, IOException {
        final InputStream is = getResource(path).getMethodData();
        assertEqualContent(is, body);
    }

    public void assertEqualContent(final InputStream is, final InputStream body) throws IOException {
        int i = 0;
        int j = 0;
        while ((i = is.read()) != -1) {
            j = body.read();
            assertEquals(j, i);
        }
        assertEquals(-1, body.read());
    }

    public void assertBody(final String path, final String content) throws HttpException, IOException {
        assertEquals(content, getResource(path).getMethodDataAsString());
    }

    // Many thanks to offspring for this snippet
    public void setAuth(final WebRequest req) {
        if (password == null) {
            password = "";
        }

        final String authData = new String(Base64.encode(login + ":" + password));
        req.setHeaderField("authorization", "Basic " + authData);
    }

    public void setAuth(HttpClient client) {
        if (password == null) {
            password = "";
        }
        client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(login, password));
    }
}

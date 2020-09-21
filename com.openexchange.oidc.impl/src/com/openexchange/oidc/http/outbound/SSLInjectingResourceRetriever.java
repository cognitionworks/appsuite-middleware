/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the OX Software GmbH group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2016-2020 OX Software GmbH
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.oidc.http.outbound;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import javax.net.ssl.HttpsURLConnection;
import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;
import com.openexchange.oidc.osgi.Services;

/**
 * {@link SSLInjectingResourceRetriever}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.4
 */
public class SSLInjectingResourceRetriever implements ResourceRetriever {

    /**
     * Creates a new instance using currently active HTTP configuration.
     *
     * @return The new instance
     */
    public static SSLInjectingResourceRetriever newInstance() {
        HttpConfig httpConfig = HttpConfig.getInstance();
        return new SSLInjectingResourceRetriever(httpConfig.getConnectTimeout(), httpConfig.getReadTimeout());
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final int connectTimeout;
    private final int readTimeout;

    /**
     * Initializes a new {@link SSLInjectingResourceRetriever}.
     *
     * @param connectTimeout The HTTP connects timeout, in milliseconds, zero for infinite. Must not be negative.
     * @param readTimeout The HTTP read timeout, in milliseconds, zero for infinite. Must not be negative.
     */
    public SSLInjectingResourceRetriever(int connectTimeout, int readTimeout) {
        super();
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    @Override
    public Resource retrieveResource(URL url) throws IOException {
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (ClassCastException e) {
            throw new IOException("Couldn't open HTTP(S) connection: " + e.getMessage(), e);
        }

        con.setConnectTimeout(connectTimeout);
        con.setReadTimeout(readTimeout);

        if (con instanceof HttpsURLConnection) {
            SSLSocketFactoryProvider factoryProvider = Services.getOptionalService(SSLSocketFactoryProvider.class);
            if (factoryProvider != null) {
                ((HttpsURLConnection) con).setSSLSocketFactory(factoryProvider.getDefault());
            }
        }

        String content;
        {
            InputStream inputStream = con.getInputStream();
            try {
                content = IOUtils.readInputStreamToString(inputStream, Charset.forName("UTF-8"));
            } finally {
                inputStream.close();
            }
        }

        // Check HTTP code + message
        final int statusCode = con.getResponseCode();
        final String statusMessage = con.getResponseMessage();

        // Ensure 2xx status code
        if (statusCode > 299 || statusCode < 200) {
            throw new IOException("HTTP " + statusCode + ": " + statusMessage);
        }

        return new Resource(content, con.getContentType());
    }

}

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

package com.openexchange.soap.cxf.interceptor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.cxf.frontend.WSDLGetInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import com.openexchange.config.ConfigurationService;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.soap.cxf.osgi.Services;


/**
 * {@link HttpsAwareWSDLGetInterceptor}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class HttpsAwareWSDLGetInterceptor extends WSDLGetInterceptor {

    private static final String HTTPS = "https";

    /**
     * Initializes a new {@link HttpsAwareWSDLGetInterceptor}.
     */
    public HttpsAwareWSDLGetInterceptor() {
        super();
    }

    /**
     * Initializes a new {@link HttpsAwareWSDLGetInterceptor}.
     * @param outInterceptor
     */
    public HttpsAwareWSDLGetInterceptor(Interceptor<Message> outInterceptor) {
        super(outInterceptor);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        try {
            String baseUri = (String) message.get(Message.REQUEST_URL);

            URI uri = new URI(baseUri);
            ConfigurationService configService = Services.optService(ConfigurationService.class);
            boolean forceHTTPS = configService != null && configService.getBoolProperty(ServerConfig.Property.FORCE_HTTPS.getPropertyName(), Boolean.valueOf(ServerConfig.Property.FORCE_HTTPS.getDefaultValue()).booleanValue());
            TreeMap<String, List<String>> headers = (TreeMap<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
            Optional<List<String>> optProto = headers != null ? Optional.ofNullable(headers.get("X-Forwarded-Proto")) : Optional.empty();
            if (forceHTTPS || (optProto.isPresent() && optProto.get().size() > 0 && optProto.get().get(0).equalsIgnoreCase(HTTPS))) {
                Optional<List<String>> optForwardPort = headers != null ? Optional.ofNullable(headers.get("X-Forwarded-Port")) : Optional.empty();
                int port = 443;
                try {
                    if (optForwardPort.isPresent() && optForwardPort.get().isEmpty() == false) {
                        port = Integer.valueOf(optForwardPort.get().get(0)).intValue();
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
                baseUri = new URI(HTTPS, uri.getUserInfo(), uri.getHost(), port, uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
                message.put(Message.REQUEST_URL, baseUri);
            }

            super.handleMessage(message);
        } catch (URISyntaxException e) {
            throw new Fault(e);
        }
    }

}

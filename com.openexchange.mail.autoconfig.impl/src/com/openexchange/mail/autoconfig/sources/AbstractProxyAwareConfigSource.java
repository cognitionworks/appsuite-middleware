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

package com.openexchange.mail.autoconfig.sources;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.autoconfig.tools.Utils.OX_CONTEXT_ID;
import static com.openexchange.mail.autoconfig.tools.Utils.OX_USER_ID;
import static com.openexchange.rest.client.httpclient.util.HttpContextUtils.addCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import com.openexchange.server.ServiceLookup;

/**
 * Connects to the Mozilla ISPDB. For more information see <a
 * href="https://developer.mozilla.org/en/Thunderbird/Autoconfiguration">https://developer.mozilla.org/en/Thunderbird/Autoconfiguration</a>
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a> Added google-common cache
 */
public abstract class AbstractProxyAwareConfigSource extends AbstractConfigSource {

    /** The OSGi service look-up */
    protected final ServiceLookup services;

    /**
     * Initializes a new {@link AbstractProxyAwareConfigSource}.
     *
     * @param services The service look-up
     */
    protected AbstractProxyAwareConfigSource(ServiceLookup services) {
        super();
        this.services = services;
    }

    /**
     * Generated a {@link HttpContext} in which user and context identifiers
     * are set.
     *
     * @param context The context to set with identifier {@link #OX_CONTEXT_ID}
     * @param user The user to set with identifier {@link #OX_USER_ID}
     * @return A {@link HttpContext}
     */
    protected HttpContext httpContextFor(int context, int user) {
        BasicHttpContext httpContext = new BasicHttpContext();
        httpContext.setAttribute(OX_CONTEXT_ID, I(context));
        httpContext.setAttribute(OX_USER_ID, I(user));
        addCookieStore(httpContext, context, user, getAccountId());
        return httpContext;
    }

    /**
     * Gets the account identifier used in the HTTP context
     *
     * @return The identifier
     */
    protected abstract String getAccountId();
}

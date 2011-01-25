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
 *    trademarks of the Open-Xchange, Inc. group of companies.
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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.ajax.framework;

import java.io.IOException;
import org.json.JSONException;
import com.openexchange.ajax.session.LoginTools;
import com.openexchange.ajax.session.actions.LoginRequest;
import com.openexchange.ajax.session.actions.LogoutRequest;
import com.openexchange.configuration.AJAXConfig;
import com.openexchange.configuration.AJAXConfig.Property;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.configuration.ConfigurationException.Code;
import com.openexchange.tools.servlet.AjaxException;

/**
 * This class implements the temporary memory of an AJAX client and provides some convenience methods to determine user specific values for
 * running some tests more easily.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class AJAXClient {

    private final AJAXSession session;

    private final UserValues values = new UserValues(this);

    private boolean mustLogout;

    private String hostname = null;

    private String protocol = null;

    public AJAXClient(final AJAXSession session) {
        this.session = session;
        this.mustLogout = session.mustLogout();
    }

    public AJAXClient(final User user) throws ConfigurationException, AjaxException, IOException, JSONException {
        AJAXConfig.init();
        String login = AJAXConfig.getProperty(user.getLogin());
        if (null == login) {
            throw new ConfigurationException(Code.PROPERTY_MISSING, user.getLogin().getPropertyName());
        }
        if(!login.contains("@")){
            final String context = AJAXConfig.getProperty(Property.CONTEXTNAME);
            if (null == context) {
                throw new ConfigurationException(Code.PROPERTY_MISSING, Property.CONTEXTNAME.getPropertyName());
            }
            login += "@"+context;
        }
        final String password = AJAXConfig.getProperty(user.getPassword());
        if (null == password) {
            throw new ConfigurationException(Code.PROPERTY_MISSING, user.getPassword().getPropertyName());
        }
        session = new AJAXSession();
        session.setId(execute(new LoginRequest(login, password, LoginTools.generateAuthId(), AJAXClient.class.getName(), "6.15.0")).getSessionId());
    }

    public enum User {
        User1(Property.LOGIN, Property.PASSWORD),
        User2(Property.SECONDUSER, Property.PASSWORD),
        User3(Property.THIRDLOGIN, Property.PASSWORD),
        OXAdmin(Property.OXADMIN, Property.PASSWORD);

        private Property login;

        private Property password;

        private User(final Property login, final Property password) {
            this.login = login;
            this.password = password;
        }

        public Property getLogin() {
            return login;
        }

        public Property getPassword() {
            return password;
        }
    }

    /**
     * @return the session
     */
    public AJAXSession getSession() {
        return session;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mustLogout)
                logout();
        } finally {
            super.finalize();
        }
    }

    public void logout() throws AjaxException, IOException, JSONException {
        if (null != session.getId()) {
            execute(new LogoutRequest(session.getId()));
            session.setId(null);
        }
        session.getConversation().clearContents();
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the values
     */
    public UserValues getValues() {
        return values;
    }

    public <T extends AbstractAJAXResponse> T execute(final AJAXRequest<T> request) throws AjaxException, IOException, JSONException {
        if (hostname != null && protocol != null) {
            // TODO: Maybe assume http as default protocol
            return Executor.execute(getSession(), request, getProtocol(), getHostname());
        }
        return Executor.execute(this, request);
    }
}

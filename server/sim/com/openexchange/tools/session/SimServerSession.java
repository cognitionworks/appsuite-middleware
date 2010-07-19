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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.tools.session;

import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.SimContext;
import com.openexchange.groupware.ldap.MockUser;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.mail.usersetting.UserSettingMail;


/**
 * {@link SimServerSession}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class SimServerSession implements ServerSession {

    private Context context;
    private User user;
    private UserConfiguration userConfig;
    
    public SimServerSession(Context context, User user, UserConfiguration userConfig) {
        super();
        this.context = context;
        this.user = user;
        this.userConfig = userConfig;
    }
    
    
    public SimServerSession(int ctxId, int uid) {
        this(new SimContext(ctxId), null, null);
        this.user = new MockUser(uid);
    }
    
    public Context getContext() {
        return context;
    }

    public User getUser() {
        return user;
    }

    public UserConfiguration getUserConfiguration() {
        return userConfig;
    }

    public UserSettingMail getUserSettingMail() {
        return null;
    }

    public boolean containsParameter(String name) {
        return false;
    }

    public int getContextId() {
        return context.getContextId();
    }

    public String getLocalIp() {
        return null;
    }

    public String getLogin() {
        return null;
    }

    public String getLoginName() {
        return null;
    }

    public Object getParameter(String name) {
        return null;
    }

    public String getPassword() {
        return null;
    }

    public String getRandomToken() {
        return null;
    }

    public String getSecret() {
        return null;
    }

    public String getSessionID() {
        return null;
    }

    public int getUserId() {
        return user.getId();
    }

    public String getUserlogin() {
        return null;
    }

    public void removeRandomToken() {
        throw new UnsupportedOperationException();
    }

    public void setParameter(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    public String getAuthId() {
        throw new UnsupportedOperationException();
    }


    /* (non-Javadoc)
     * @see com.openexchange.session.Session#getHash()
     */
    public String getHash() {
        // TODO Auto-generated method stub
        return null;
    }


    /* (non-Javadoc)
     * @see com.openexchange.session.Session#setLocalIp(java.lang.String)
     */
    public void setLocalIp(String ip) {
        // TODO Auto-generated method stub
        
    }
}

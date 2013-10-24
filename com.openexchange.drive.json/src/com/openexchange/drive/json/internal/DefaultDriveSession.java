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
 *     Copyright (C) 2004-2013 Open-Xchange, Inc.
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

package com.openexchange.drive.json.internal;

import java.util.List;
import java.util.Locale;
import com.openexchange.drive.DriveFileField;
import com.openexchange.drive.DriveSession;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link DefaultDriveSession}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DefaultDriveSession implements DriveSession {

    private final String rootFolderID;
    private final ServerSession session;
    private final int apiVersion;
    private String deviceName;
    private Boolean diagnostics;
    private List<DriveFileField> fields;
    private final HostData hostData;

    /**
     * Initializes a new {@link DefaultDriveSession}.
     *
     * @param session The session
     * @param rootFolderID The root folder ID
     */
    public DefaultDriveSession(ServerSession session, String rootFolderID, HostData hostData, int apiVersion) {
        super();
        this.session = session;
        this.rootFolderID = rootFolderID;
        this.hostData = hostData;
        this.apiVersion = apiVersion;
    }

    /**
     * Sets the diagnostics
     *
     * @param diagnostics The diagnostics to set
     */
    public void setDiagnostics(Boolean diagnostics) {
        this.diagnostics = diagnostics;
    }

    /**
     * Sets the deviceName
     *
     * @param deviceName The deviceName to set
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * Sets the fields
     *
     * @param fields The fields to set
     */
    public void setFields(List<DriveFileField> fields) {
        this.fields = fields;
    }

    @Override
    public String getRootFolderID() {
        return rootFolderID;
    }

    @Override
    public ServerSession getServerSession() {
        return session;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public Boolean isDiagnostics() {
        return diagnostics;
    }

    @Override
    public Locale getLocale() {
        Locale locale = null;
        if (null != session) {
            User user = session.getUser();
            if (null != user) {
                locale = user.getLocale();
            }
        }
        return null != locale ? locale : Locale.US;
    }

    @Override
    public HostData getHostData() {
        return hostData;
    }

    @Override
    public List<DriveFileField> getFields() {
        return fields;
    }

    @Override
    public int getApiVersion() {
        return apiVersion;
    }

    @Override
    public String toString() {
        return "DriveSession [sessionID=" + session.getSessionID() + ", rootFolderID=" + rootFolderID + ", contextID=" +
            session.getContextId() + ", deviceName=" + deviceName + ", diagnostics=" + diagnostics + "]";
    }

}

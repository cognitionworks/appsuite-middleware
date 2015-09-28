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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.ajax;

import com.openexchange.config.ConfigurationService;
import com.openexchange.configuration.ServerConfig.Property;
import com.openexchange.tools.images.osgi.Services;

/**
 * {@link Client} - An enumeration for known clients accessing the AJAX HTTP-API.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public enum Client {

    /**
     * The client for OX6 UI: <code>"com.openexchange.ox.gui.dhtml"</code>
     */
    OX6_UI("com.openexchange.ox.gui.dhtml", "/ox6/index.html"),
    /**
     * The client for App Suite UI: <code>"open-xchange-appsuite"</code>
     */
    APPSUITE_UI("open-xchange-appsuite", "/appsuite/"),
    /**
     * The client for Mobile Mail App: <code>"open-xchange-mailapp"</code>
     */
    MOBILE_APP("open-xchange-mailapp", null),
    /**
     * The client for USM/EAS: <code>"USM-EAS"</code>
     */
    USM_EAS("USM-EAS", null),
    /**
     * The client for USM/JSON (OLOX): <code>"USM-JSON"</code>
     */
    USM_JSON("USM-JSON", null),
    /**
     * The client for Outlook OXtender2 AddIn: <code>"OpenXchange.HTTPClient.OXAddIn"</code>
     */
    OUTLOOK_OXTENDER2_ADDIN("OpenXchange.HTTPClient.OXAddIn", null),
    /**
     * The client for OX Notifier: <code>"OpenXchange.HTTPClient.OXNotifier"</code>
     */
    OXNOTIFIER("OpenXchange.HTTPClient.OXNotifier", null),
    /**
     * The client for Outlook Update 1: <code>"com.open-xchange.updater.olox1"</code>
     */
    OUTLOOK_UPDATER1("com.open-xchange.updater.olox1", null),
    /**
     * The client for Outlook Update 2: <code>"com.open-xchange.updater.olox2"</code>
     */
    OUTLOOK_UPDATER2("com.open-xchange.updater.olox2", null),
    /**
     * The client for CardDAV: <code>"CARDDAV"</code>
     */
    CARDDAV("CARDDAV", null),
    /**
     * The client for CalDAV: <code>"CALDAV"</code>
     */
    CALDAV("CALDAV", null),
    /**
     * The client for WebDAV iCal: <code>"WEBDAV_ICAL"</code>
     */
    WEBDAV_ICAL("WEBDAV_ICAL", null),
    /**
     * The client for WebDav InfoStore: <code>"WEBDAV_INFOSTORE"</code>
     */
    WEBDAV_INFOSTORE("WEBDAV_INFOSTORE", null),
    /**
     * The client for WebDav vCard: <code>"WEBDAV_VCARD"</code>
     */
    WEBDAV_VCARD("WEBDAV_VCARD", null);

    private final String clientId;
    private final String uiWebPath;
    private final String DEFAULT_UIWebPath = "client-defined";

    /**
     * Initializes a new {@link Client}.
     */
    private Client(String clientId, String uiWebPath) {
        this.clientId = clientId;
        this.uiWebPath = uiWebPath;
    }

    /**
     * Gets the client identifier
     *
     * @return The client identifier
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the UIWebPath for this client.
     * 
     * @return the UIWebPath
     */
    public String getUIWebPath() {
        ConfigurationService confService = Services.getService(ConfigurationService.class);
        if (confService == null)
        {
            return uiWebPath;
        }
        String prop = confService.getProperty(Property.UI_WEB_PATH.getPropertyName());
        if (prop.contentEquals(DEFAULT_UIWebPath)) {
            return uiWebPath;
        }
        else
        {
            return prop;
        }
    }

    @Override
    public String toString() {
        return getClientId();
    }

    /**
     * Gets the Client with the given clientID
     * 
     * @param clientID
     * @return the Client
     */
    public static Client getClientByID(String clientID) {
        Client[] clients = Client.values();
        for (Client clt : clients)
        {
            if (clt.clientId.contentEquals(clientID))
            {
                return clt;
            }
        }
        return null;
    }

}

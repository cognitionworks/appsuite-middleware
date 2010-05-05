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

package com.openexchange.ajax.framework;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;
import org.xml.sax.SAXException;

import com.openexchange.ajax.config.actions.GetRequest;
import com.openexchange.ajax.config.actions.SetRequest;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.tools.servlet.AjaxException;

/**
 * Help and caching class for values of the preferences tree.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class UserValues {

    private final AJAXClient client;

    private String inboxFolder;
    private String sentFolder;
    private String trashFolder;
    private String draftsFolder;
    private String sendAddress;
    private Integer privateInfostoreFolder;
    private Locale locale;
    private int privateAppointmentFolder = -1;
    private int privateContactFolder = -1;
    private int privateTaskFolder = -1;
    private TimeZone timeZone;
    private int userId = -1;

    private String defaultAddress;

    public UserValues(final AJAXClient client) {
        super();
        this.client = client;
    }

    public String getInboxFolder() throws AjaxException, IOException, SAXException, JSONException {
        if (null == inboxFolder) {
            inboxFolder = client.execute(new GetRequest(Tree.InboxFolder)).getString();
        }
        return inboxFolder;
    }

    public String getSentFolder() throws AjaxException, IOException, SAXException, JSONException {
        if (null == sentFolder) {
            sentFolder = client.execute(new GetRequest(Tree.SentFolder)).getString();
        }
        return sentFolder;
    }

    public String getTrashFolder() throws AjaxException, IOException, SAXException, JSONException {
        if (null == trashFolder) {
            trashFolder = client.execute(new GetRequest(Tree.TrashFolder)).getString();
        }
        return trashFolder;
    }
    
    public String getDraftsFolder() throws AjaxException, IOException, SAXException, JSONException {
        if (null == draftsFolder) {
            draftsFolder = client.execute(new GetRequest(Tree.DraftsFolder)).getString();
        }
        return draftsFolder;
    }
    
    public int getPrivateInfostoreFolder() throws AjaxException, IOException, SAXException, JSONException {
        if (null == privateInfostoreFolder) {
            privateInfostoreFolder = I(client.execute(new GetRequest(Tree.PrivateInfostoreFolder)).getInteger());
        }
        return privateInfostoreFolder.intValue();
    }
    
    public String getSendAddress() throws AjaxException, IOException, SAXException, JSONException {
        if (null == sendAddress) {
            sendAddress = client.execute(new GetRequest(Tree.SendAddress)).getString();
        }
        return sendAddress;
    }

    public Locale getLocale() throws AjaxException, IOException, SAXException, JSONException {
        if (null == locale) {
            final String localeId = client.execute(new GetRequest(Tree.Language)).getString();
            locale = LocaleTools.getLocale(localeId);
        }
        return locale;
    }

    public int getPrivateAppointmentFolder() throws AjaxException, IOException, SAXException, JSONException {
        if (-1 == privateAppointmentFolder) {
            privateAppointmentFolder = client.execute(new GetRequest(Tree.PrivateAppointmentFolder)).getInteger();
        }
        return privateAppointmentFolder;
    }

    public int getPrivateContactFolder() throws AjaxException, IOException,
        SAXException, JSONException {
        if (-1 == privateContactFolder) {
            privateContactFolder = client.execute(new GetRequest(Tree.PrivateContactFolder)).getInteger();
        }
        return privateContactFolder;
    }

    public int getPrivateTaskFolder() throws AjaxException, IOException, SAXException, JSONException {
        if (-1 == privateTaskFolder) {
            privateTaskFolder = client.execute(new GetRequest(Tree.PrivateTaskFolder)).getInteger();
        }
        return privateTaskFolder;
    }

    public Date getServerTime() throws AjaxException, IOException, SAXException, JSONException {
        long serverTime = client.execute(new GetRequest(Tree.CurrentTime)).getLong();
        serverTime -= getTimeZone().getOffset(serverTime);
        return new Date(serverTime);
    }

    public TimeZone getTimeZone() throws AjaxException, IOException, SAXException, JSONException {
        if (null == timeZone) {
            final String tzId = client.execute(new GetRequest(Tree.TimeZone)).getString();
            timeZone = TimeZone.getTimeZone(tzId);
        }
        return timeZone;
    }
    
    public void setTimeZone(final TimeZone timeZone) throws AjaxException, IOException, SAXException, JSONException {
        client.execute(new SetRequest(Tree.TimeZone, timeZone.getID()));
    }

    public int getUserId() throws AjaxException, IOException, SAXException, JSONException {
        if (-1 == userId) {
            userId = client.execute(new GetRequest(Tree.Identifier)).getInteger();
        }
        return userId;
    }

    public String getDefaultAddress() throws AjaxException, IOException, SAXException, JSONException {
        if (null == defaultAddress) {
            defaultAddress = client.execute(new GetRequest(Tree.DefaultAddress)).getString();
        }
        return defaultAddress;
    }
}

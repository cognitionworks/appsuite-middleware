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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.ajax.folder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.xml.sax.SAXException;

import com.openexchange.ajax.folder.actions.DeleteRequest;
import com.openexchange.ajax.folder.actions.InsertRequest;
import com.openexchange.ajax.folder.actions.ListRequest;
import com.openexchange.ajax.folder.actions.ListResponse;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.CommonDeleteResponse;
import com.openexchange.ajax.framework.CommonInsertResponse;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.api2.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.tools.servlet.AjaxException;

/**
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class FolderTools {

    /**
     * Prevent instantiation
     */
    private FolderTools() {
        super();
    }

    public static CommonInsertResponse insert(final AJAXClient client,
        final InsertRequest request) throws AjaxException, IOException,
        SAXException, JSONException {
        return Executor.execute(client, request);
    }

    public static CommonDeleteResponse delete(final AJAXClient client,
        final DeleteRequest request) throws AjaxException, IOException,
        SAXException, JSONException {
        return Executor.execute(client, request);
    }

    public static ListResponse list(final AJAXClient client,
        final ListRequest request) throws AjaxException, IOException,
        SAXException, JSONException {
        return Executor.execute(client, request);
    }

    /**
     * @deprecated use {@link #getSubFolders(AJAXClient, String, boolean)}.
     */
    @Deprecated
    public static List<FolderObject> getSubFolders(final AJAXSession session,
        final String protocol, final String hostname, final String parent,
        final boolean ignoreMailFolder) throws AjaxException, IOException,
        SAXException, JSONException, OXException {
        final ListRequest request = new ListRequest(parent, ignoreMailFolder);
        final ListResponse response = Executor.execute(session, request,
            protocol, hostname);
        final List<FolderObject> retval = new ArrayList<FolderObject>();
        final Iterator<FolderObject> iter = response.getFolder();
        while (iter.hasNext()) {
            retval.add(iter.next());
        }
        return retval;
    }
    
    public static List<FolderObject> getSubFolders(final AJAXClient client,
        final String parent, final boolean ignoreMailFolder)
        throws AjaxException, IOException, SAXException, JSONException,
        OXException {
        final ListRequest request = new ListRequest(parent, ignoreMailFolder);
        final ListResponse response = client.execute(request);
        final List<FolderObject> retval = new ArrayList<FolderObject>();
        final Iterator<FolderObject> iter = response.getFolder();
        while (iter.hasNext()) {
            retval.add(iter.next());
        }
        return retval;
    }
}

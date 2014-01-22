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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.ajax.folder.actions;

import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.framework.CommonDeleteParser;
import com.openexchange.ajax.framework.CommonDeleteResponse;
import com.openexchange.groupware.container.FolderObject;

/**
 * Stores the parameters to delete a folder.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a> - working with mail folders
 */
public class DeleteRequest extends AbstractFolderRequest<CommonDeleteResponse> {

    private final String[] folderIds;
    private final Date lastModified;
    private final boolean failOnError;

    public DeleteRequest(final API api, final String[] folderIds, final Date lastModified, final boolean failOnError) {
        super(api);
        this.folderIds = folderIds;
        this.lastModified = lastModified;
        this.failOnError = failOnError;
    }

    public DeleteRequest(final API api, final String[] folderIds, final Date lastModified) {
        this(api, folderIds, lastModified, true);
    }

    public DeleteRequest(final API api, final String folderId, final Date lastModified) {
        this(api, new String[] { folderId }, lastModified);
    }

    public DeleteRequest(final API api, final int[] folderIds, final Date lastModified) {
        this(api, i2s(folderIds), lastModified);
    }

    public DeleteRequest(final API api, final int folderId, final Date lastModified) {
        this(api, new int[] { folderId }, lastModified);
    }

    public DeleteRequest(final API api, final boolean failOnError, final FolderObject... folder) {
        super(api);
        folderIds = new String[folder.length];
        Date maxLastModified = new Date(Long.MIN_VALUE);
        for (int i = 0; i < folder.length; i++) {
            if (folder[i].containsObjectID()) { // task, appointment or contact folder
                folderIds[i] = Integer.valueOf(folder[i].getObjectID()).toString();
            } else { // mail folder
                folderIds[i] = folder[i].getFullName();
            }
            if (maxLastModified.before(folder[i].getLastModified())) {
                maxLastModified = folder[i].getLastModified();
            }
        }
        lastModified = maxLastModified;
        this.failOnError = failOnError;
    }

    public DeleteRequest(final API api, final FolderObject... folder) {
        this(api, true, folder);
    }

    @Override
    public Object getBody() {
        final JSONArray array = new JSONArray();
        for (final String folderId : folderIds) {
            array.put(folderId);
        }
        return array;
    }

    @Override
    public Method getMethod() {
        return Method.PUT;
    }

    @Override
    protected void addParameters(final List<Parameter> params) {
        params.add(new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_DELETE));
        params.add(new Parameter(AJAXServlet.PARAMETER_TIMESTAMP, lastModified.getTime()));
    }

    @Override
    public CommonDeleteParser getParser() {
        return new CommonDeleteParser(failOnError);
    }
}

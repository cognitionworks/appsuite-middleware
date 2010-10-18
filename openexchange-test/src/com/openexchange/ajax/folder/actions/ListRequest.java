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

package com.openexchange.ajax.folder.actions;

import java.util.List;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.Folder;
import com.openexchange.groupware.container.FolderObject;

/**
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class ListRequest extends AbstractFolderRequest<ListResponse> {

    private static final int[] DEFAULT_COLUMNS = {
        FolderObject.OBJECT_ID, FolderObject.MODULE, FolderObject.FOLDER_NAME, FolderObject.SUBFOLDERS, FolderObject.STANDARD_FOLDER,
        FolderObject.CREATED_BY };

    private final String parentFolder;

    private final int[] columns;

    private final boolean ignoreMail;
    
    private final boolean failOnError;

    public ListRequest(API api, String parentFolder, int[] columns, boolean ignoreMail, boolean failOnError) {
        super(api);
        this.parentFolder = parentFolder;
        this.columns = columns;
        this.ignoreMail = ignoreMail;
        this.failOnError = failOnError;
    }
    
    public ListRequest(API api, String parentFolder, int[] columns, boolean ignoreMail) {
        super(api);
        this.parentFolder = parentFolder;
        this.columns = columns;
        this.ignoreMail = ignoreMail;
        this.failOnError = true;
    }

    public ListRequest(API api, String parentFolder) {
        this(api, parentFolder, DEFAULT_COLUMNS, false);
    }

    public ListRequest(API api, int parentFolder) {
        this(api, Integer.toString(parentFolder), false);
    }

    public ListRequest(API api, String parentFolder, boolean ignoreMail) {
        this(api, parentFolder, DEFAULT_COLUMNS, ignoreMail);
    }

    public Object getBody() {
        return null;
    }

    public Method getMethod() {
        return Method.GET;
    }

    @Override
    protected void addParameters(List<Parameter> params) {
        params.add(new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_LIST));
        params.add(new Parameter(Folder.PARAMETER_PARENT, parentFolder));
        params.add(new Parameter(AJAXServlet.PARAMETER_COLUMNS, columns));
        if (ignoreMail) {
            params.add(new Parameter(AJAXServlet.PARAMETER_IGNORE, "mailfolder"));
        }
    }

    public ListParser getParser() {
        return new ListParser(columns, failOnError);
    }
}

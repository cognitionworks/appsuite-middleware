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
 * {@link VisibleFoldersRequest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class VisibleFoldersRequest extends AbstractFolderRequest<VisibleFoldersResponse> {

    private static final int[] DEFAULT_COLUMNS = {
        FolderObject.OBJECT_ID, FolderObject.MODULE, FolderObject.TYPE, FolderObject.FOLDER_NAME, FolderObject.SUBFOLDERS, FolderObject.STANDARD_FOLDER,
        FolderObject.CREATED_BY };

    private final String contentType;

    private final int[] columns;

    public VisibleFoldersRequest(API api, String contentType, int[] columns) {
        super(api);
        this.contentType = contentType;
        this.columns = columns;
    }

    public VisibleFoldersRequest(API api, String contentType) {
        this(api, contentType, DEFAULT_COLUMNS);
    }

    public Object getBody() {
        return null;
    }

    public Method getMethod() {
        return Method.GET;
    }

    @Override
    protected void addParameters(List<Parameter> params) {
        params.add(new Parameter(AJAXServlet.PARAMETER_ACTION, "allVisible"));
        params.add(new Parameter(Folder.PARAMETER_CONTENT_TYPE, contentType));
        params.add(new Parameter(AJAXServlet.PARAMETER_COLUMNS, columns));
    }

    public VisibleFoldersParser getParser() {
        return new VisibleFoldersParser(columns, true);
    }
}

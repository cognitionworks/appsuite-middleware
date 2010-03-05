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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.groupware.container.FolderObject;

/**
 * Request to get a folder from the server.
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public final class GetRequest extends AbstractFolderRequest<GetResponse> {

    class GetParser extends AbstractAJAXParser<GetResponse> {

        /**
         * Default constructor.
         */
        GetParser(final boolean failOnError) {
            super(failOnError);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected GetResponse createResponse(final Response response) throws JSONException {
            return new GetResponse(response);
        }
    }

    private final boolean failOnError;

    private final String folderIdentifier;

    private final int[] columns;

    private int tree = 0;

    /**
     * Initializes a new {@link GetRequest} for specified columns
     */
    public GetRequest(final String folderIdentifier, final int[] columns, final boolean failOnError) {
        super();
        this.folderIdentifier = folderIdentifier;
        this.columns = columns;
        this.failOnError = failOnError;
    }

    /**
     * Initializes a new {@link GetRequest} for all columns
     */
    public GetRequest(final String folderIdentifier, final boolean failOnError) {
        super();
        this.folderIdentifier = folderIdentifier;
        this.columns = FolderObject.ALL_COLUMNS;
        this.failOnError = failOnError;
    }

    public GetRequest(final int folderId, final int[] columns) {
        this(String.valueOf(folderId), columns, true);
    }

    public GetRequest(final String folderId, final int[] columns) {
        this(folderId, columns, true);
    }

    public GetRequest setTree(final int tree) {
        this.tree = tree;
        return this;
    }

    public Object getBody() throws JSONException {
        return null;
    }

    public Method getMethod() {
        return Method.GET;
    }

    /**
     * {@inheritDoc}
     */
    public Parameter[] getParameters() {
        final Parameter[] params = getParams();
        final List<Parameter> l = new ArrayList<Parameter>(Arrays.asList(params));
        if (tree > 0) {
            l.add(new Parameter("tree", String.valueOf(tree)));
        }
        return l.toArray(new Parameter[l.size()]);
    }

    private Parameter[] getParams() {
        return new Parameter[] {
            new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_GET),
            new Parameter(AJAXServlet.PARAMETER_ID, folderIdentifier),
            new Parameter(AJAXServlet.PARAMETER_COLUMNS, columns)
        };
    }

    public GetParser getParser() {
        return new GetParser(failOnError);
    }
}

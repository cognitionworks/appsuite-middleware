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

package com.openexchange.ajax.infostore.actions;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.ajax.framework.Params;

/**
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class DeleteInfostoreRequest extends AbstractInfostoreRequest<DeleteInfostoreResponse> {

    private List<Integer> ids, folders;

    private Date timestamp;

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setFolders(List<Integer> folders) {
        this.folders = folders;
    }

    public List<Integer> getFolders() {
        return folders;
    }

    public void setTimestamp(Date timestamps) {
        this.timestamp = timestamps;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public DeleteInfostoreRequest(List<Integer> ids, List<Integer> folders,Date timestamp) {
        this();
        setIds(ids);
        setFolders(folders);
        setTimestamp(timestamp);
    }

    public DeleteInfostoreRequest() {
        super();
        setIds(new LinkedList<Integer>());
        setFolders(new LinkedList<Integer>());
    }

    public DeleteInfostoreRequest(int id, int folder, Date timestamp) {
        this();
        setIds(Arrays.asList(Integer.valueOf(id)));
        setFolders(Arrays.asList(Integer.valueOf(folder)));
        setTimestamp(timestamp);
    }

    public Object getBody() throws JSONException {
        return writeFolderAndIDList(getIds(), getFolders());
    }

    public com.openexchange.ajax.framework.AJAXRequest.Method getMethod() {
        return Method.PUT;
    }

    public com.openexchange.ajax.framework.AJAXRequest.Parameter[] getParameters() {
        return new Params(
            AJAXServlet.PARAMETER_ACTION,
            AJAXServlet.ACTION_DELETE,
            AJAXServlet.PARAMETER_TIMESTAMP,
            String.valueOf(getTimestamp().getTime())).toArray();
    }

    public AbstractAJAXParser<? extends DeleteInfostoreResponse> getParser() {
        return new AbstractAJAXParser<DeleteInfostoreResponse>(getFailOnError()) {

            @Override
            protected DeleteInfostoreResponse createResponse(final Response response) throws JSONException {
                return new DeleteInfostoreResponse(response);
            }
        };
    }

}

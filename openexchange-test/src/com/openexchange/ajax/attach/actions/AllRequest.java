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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.ajax.attach.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.attach.AttachmentTools;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.search.Order;


/**
 * {@link AllRequest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class AllRequest extends AbstractAttachmentRequest<AllResponse> {

    private final CommonObject obj;
    private final int[] columns;
    private final int sort;
    private final Order order;
    private final boolean failOnError;

    public AllRequest(final CommonObject obj, final int[] columns, final int sort, final Order order) {
        this(obj, columns, sort, order, true);
    }

    public AllRequest(final CommonObject obj, final int[] columns, final int sort, final Order order, boolean failOnError) {
        super();
        this.obj = obj;
        this.columns = columns;
        this.sort = sort;
        this.order = order;
        this.failOnError = failOnError;
    }

    public AllRequest(final CommonObject obj, final int[] columns) {
        this(obj, columns, true);
    }

    public AllRequest(final CommonObject obj, final int[] columns, boolean failOnError) {
        this(obj, columns, -1, null, failOnError);
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Method getMethod() {
        return Method.GET;
    }

    @Override
    public com.openexchange.ajax.framework.AJAXRequest.Parameter[] getParameters() throws IOException, JSONException {
        List<Parameter> parameters = new ArrayList<Parameter>();
        parameters.add(new URLParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_ALL));
        parameters.add(new Parameter(AJAXServlet.PARAMETER_ATTACHEDID, obj.getObjectID()));
        parameters.add(new Parameter(AJAXServlet.PARAMETER_FOLDERID, obj.getParentFolderID()));
        parameters.add(new Parameter(AJAXServlet.PARAMETER_MODULE, AttachmentTools.determineModule(obj)));
        parameters.add(new Parameter(AJAXServlet.PARAMETER_COLUMNS, columns));

        if (sort > 0) {
            parameters.add(new Parameter(AJAXServlet.PARAMETER_SORT, sort));
            parameters.add(new Parameter(AJAXServlet.PARAMETER_ORDER, order.toString()));
        }

        return parameters.toArray(new Parameter[parameters.size()]);
    }

    @Override
    public AbstractAJAXParser<? extends AllResponse> getParser() {
        return new AllParser(failOnError);
    }

    @Override
    public Object getBody() throws IOException, JSONException {
        return null;
    }

}

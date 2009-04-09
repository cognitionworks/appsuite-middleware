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

package com.openexchange.ajax.framework;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONException;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.fields.OrderFields;
import com.openexchange.groupware.search.Order;

/**
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class CommonUpdatesRequest<T extends CommonUpdatesResponse> implements AJAXRequest<T> {

    private final String servletPath;

    private final int folderId;

    private final int[] columns;

    private final int sort;

    private final Order order;

    private final Date lastModified;

    private final Ignore ignore;

    private final boolean failOnError;

    /**
     * Default constructor.
     */
    public CommonUpdatesRequest(final String servletPath, final int folderId,
        final int[] columns, final int sort, final Order order,
        final Date lastModified, final boolean failOnError) {
        this(servletPath, folderId, columns, sort, order, lastModified,
            Ignore.DELETED, failOnError);
    }

    /**
     * Full constructor.
     */
    public CommonUpdatesRequest(final String servletPath, final int folderId,
        final int[] columns, final int sort, final Order order,
        final Date lastModified, final Ignore ignore, final boolean failOnError) {
        super();
        this.servletPath = servletPath;
        this.folderId = folderId;
        this.columns = columns;
        this.sort = sort;
        this.order = order;
        this.lastModified = lastModified;
        this.ignore = ignore;
        this.failOnError = failOnError;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getServletPath() {
        return servletPath;
    }

    /**
     * {@inheritDoc}
     */
    public Object getBody() throws JSONException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Method getMethod() {
        return Method.GET;
    }

    /**
     * {@inheritDoc}
     */
    public Parameter[] getParameters() {
        final List<Parameter> params = new ArrayList<Parameter>();
        params.add(new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet
            .ACTION_UPDATES));
        params.add(new Parameter(AJAXServlet.PARAMETER_FOLDERID, folderId));
        params.add(new Parameter(AJAXServlet.PARAMETER_COLUMNS, getColumns()));
        if (null != order) {
            params.add(new Parameter(AJAXServlet.PARAMETER_SORT, sort));
            params.add(new Parameter(AJAXServlet.PARAMETER_ORDER, OrderFields
                .write(order)));
        }
        params.add(new Parameter(AJAXServlet.PARAMETER_TIMESTAMP, lastModified));
        params.add(new Parameter(AJAXServlet.PARAMETER_IGNORE, ignore.value));
        return params.toArray(new Parameter[params.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public CommonUpdatesParser<T> getParser() {
        return new CommonUpdatesParser<T>(isFailOnError(), getColumns());
    }

    public int[] getColumns() {
        return columns;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public enum Ignore {
        DELETED("deleted"),
        NONE("none");
        private final String value;
        private Ignore(final String value) {
            this.value = value;
        }
    }
}

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

package com.openexchange.ajax.user.actions;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.contact.action.ContactSearchJSONWriter;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.DataObject;
import com.openexchange.groupware.search.ContactSearchObject;

/**
 * This class stores the values for searching users.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class SearchRequest extends AbstractUserRequest<SearchResponse> {

    public final static int[] DEFAULT_COLUMNS = { DataObject.OBJECT_ID, Contact.INTERNAL_USERID, Contact.EMAIL1 };

    private final ContactSearchObject search;

    private final int[] columns;

    private final boolean failOnError;

    private final List<Parameter> parameters;

    /**
     * @param search
     * @param columns
     */
    public SearchRequest(final ContactSearchObject search, final int[] columns) {
        this(search, columns, true);
    }

    /**
     * Default constructor.
     * 
     * @param search Object with search information. Currently only the pattern
     *            is supported.
     */
    public SearchRequest(final ContactSearchObject search, final int[] columns, final boolean failOnError) {
        this(search, columns, failOnError, null);
    }

    public SearchRequest(ContactSearchObject search, int[] columns, boolean failOnError, List<Parameter> parameters) {
        super();
        this.search = search;
        this.failOnError = failOnError;
        this.columns = columns;
        this.parameters = new ArrayList<Parameter>();
        this.parameters.add(new Parameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_SEARCH));
        this.parameters.add(new Parameter(AJAXServlet.PARAMETER_COLUMNS, columns));
        if (parameters != null) {
            this.parameters.addAll(parameters);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getBody() throws JSONException {
        return ContactSearchJSONWriter.write(search);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Method getMethod() {
        return Method.PUT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter[] getParameters() {
        return this.parameters.toArray(new Parameter[parameters.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchParser getParser() {
        return new SearchParser(failOnError, columns);
    }
}

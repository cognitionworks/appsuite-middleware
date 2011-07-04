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

package com.openexchange.mailfilter.ajax.actions;

import com.openexchange.mailfilter.ajax.Action;
import com.openexchange.mailfilter.ajax.Credentials;
import com.openexchange.mailfilter.ajax.Parameter;
import com.openexchange.mailfilter.ajax.exceptions.OXMailfilterException;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessiondException;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public abstract class AbstractRequest {

    private Session session;

    private Parameters parameters;

    /**
     * The body of a PUT request.
     */
    private String body;

    /**
     * Default constructor.
     */
    protected AbstractRequest() {
        super();
    }

    /**
     * @return the body
     */
    public String getBody() {
        return body;
    }

    /**
     * @param body
     *                the body to set
     */
    public void setBody(final String body) {
        this.body = body;
    }

    /**
     * @return the parameters
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * @param parameters
     *                the parameters to set
     */
    public void setParameters(final Parameters parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * @param session
     *                the session to set
     */
    public void setSession(final Session session) {
        this.session = session;
    }

    public Credentials getCredentials() throws SessiondException, OXMailfilterException {
        final String loginName = session.getLoginName();
        final String password = session.getPassword();
        final int userId = session.getUserId();
        final int contextId = session.getContextId();
        
        try {
            final String username = getUsername();
            return new Credentials(loginName, password, userId, contextId, username);
        } catch (AjaxException e) {
            return new Credentials(loginName, password, userId, contextId);
        }        
    }
    
    /**
     * @return the action
     * @throws AjaxException
     */
    public Action getAction() throws AjaxException {
        final Parameter action = Parameter.ACTION;
        if (null == parameters) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create( action.getName());
        }
        final String value = parameters.getParameter(Parameter.ACTION);
        if (null == value) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create( action.getName());
        }
        final Action retval = Action.byName(value);
        if (null == retval) {
            throw AjaxExceptionCodes.UnknownAction.create( value);
        }
        return retval;
    }
    
    private String getUsername() throws AjaxException {
        final Parameter pUsername = Parameter.USERNAME;        
        final String username = parameters.getParameter(pUsername);
        if (username == null) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create( pUsername);
        }
        
        return username;
    }

    public interface Parameters {
        String getParameter(Parameter param) throws AjaxException;
    }
}

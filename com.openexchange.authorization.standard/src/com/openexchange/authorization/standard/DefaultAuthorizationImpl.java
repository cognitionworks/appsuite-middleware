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

package com.openexchange.authorization.standard;

import java.lang.reflect.UndeclaredThrowableException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.authorization.AuthorizationException;
import com.openexchange.authorization.AuthorizationExceptionCodes;
import com.openexchange.authorization.AuthorizationService;
import com.openexchange.context.ContextExceptionCodes;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.ldap.User;


/**
 * {@link DefaultAuthorizationImpl}
 * 
 */
public final class DefaultAuthorizationImpl implements AuthorizationService {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(DefaultAuthorizationImpl.class));

    private static final DefaultAuthorizationImpl INSTANCE = new DefaultAuthorizationImpl();

    /**
     * Gets the instance.
     * 
     * @return The instance
     */
    public static DefaultAuthorizationImpl getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes a new {@link DefaultAuthorizationImpl}.
     */
    private DefaultAuthorizationImpl() {
        super();
    }

    /**
     * @param ctx
     * @param user
     * @throws AuthorizationException
     */
    public void authorizeUser(final Context ctx, final User user) throws AuthorizationException {
        try {
            if (!ctx.isEnabled()) {
                final ContextException e = ContextExceptionCodes.CONTEXT_DISABLED.create();
                LOG.debug(e.getMessage(), e);
                throw AuthorizationExceptionCodes.USER_DISABLED.create(e);
            }
        } catch (final UndeclaredThrowableException e) {
            throw AuthorizationExceptionCodes.UNKNOWN.create(e);
        }
        if (!user.isMailEnabled()) {
            throw AuthorizationExceptionCodes.USER_DISABLED.create();
        }
        if (user.getShadowLastChange() == 0) {
            throw AuthorizationExceptionCodes.PASSWORD_EXPIRED.create();
        }
    }

}

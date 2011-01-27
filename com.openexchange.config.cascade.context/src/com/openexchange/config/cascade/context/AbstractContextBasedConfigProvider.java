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

package com.openexchange.config.cascade.context;

import java.util.Collection;
import java.util.Collections;
import com.openexchange.config.cascade.BasicProperty;
import com.openexchange.config.cascade.ConfigCascadeException;
import com.openexchange.config.cascade.ConfigProviderService;
import com.openexchange.context.ContextService;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;

/**
 * {@link AbstractContextBasedConfigProvider}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public abstract class AbstractContextBasedConfigProvider implements ConfigProviderService {

    protected ContextService contexts;

    public AbstractContextBasedConfigProvider(ContextService contexts) {
        this.contexts = contexts;
    }

    public BasicProperty get(String property, int context, int user) throws ConfigCascadeException {
        if (context == NO_CONTEXT) {
            return NO_PROPERTY;
        }
        try {
            return get(property, contexts.getContext(context));
        } catch (ContextException e) {
            throw new ConfigCascadeException(e);
        }
    }

    public Collection<String> getAllPropertyNames(int context, int user) throws ConfigCascadeException {
        if (context == NO_CONTEXT) {
            return Collections.emptyList();
        }
        try {
            return getAllPropertyNames(contexts.getContext(context));
        } catch (ContextException e) {
            throw new ConfigCascadeException(e);
        }
    }

    protected abstract Collection<String> getAllPropertyNames(Context context);

    protected abstract BasicProperty get(String property, Context context);

}

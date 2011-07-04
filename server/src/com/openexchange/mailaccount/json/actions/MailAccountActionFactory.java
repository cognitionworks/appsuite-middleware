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

package com.openexchange.mailaccount.json.actions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXActionServiceFactory;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link MailAccountActionFactory}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailAccountActionFactory implements AJAXActionServiceFactory {

    private static final MailAccountActionFactory SINGLETON = new MailAccountActionFactory();

    private final Map<String, AJAXActionService> actions;

    private MailAccountActionFactory() {
        super();
        actions = initActions();
    }

    /**
     * Gets the {@link MailAccountActionFactory} instance.
     * 
     * @return The {@link MailAccountActionFactory} instance
     */
    public static final MailAccountActionFactory getInstance() {
        return SINGLETON;
    }

    public AJAXActionService createActionService(final String action) throws AjaxException {
        final AJAXActionService retval = actions.get(action);
        if (null == retval) {
            throw new AjaxException(AjaxExceptionCodes.UnknownAction, action);
        }
        return retval;
    }

    private Map<String, AJAXActionService> initActions() {
        final Map<String, AJAXActionService> tmp = new HashMap<String, AJAXActionService>();
        tmp.put(AllAction.ACTION, new AllAction());
        tmp.put(ListAction.ACTION, new ListAction());
        tmp.put(GetAction.ACTION, new GetAction());
        tmp.put(ValidateAction.ACTION, new ValidateAction());
        tmp.put(DeleteAction.ACTION, new DeleteAction());
        tmp.put(UpdateAction.ACTION, new UpdateAction());
        tmp.put(GetTreeAction.ACTION, new GetTreeAction());
        tmp.put(NewAction.ACTION, new NewAction());
        return Collections.unmodifiableMap(tmp);
    }

}

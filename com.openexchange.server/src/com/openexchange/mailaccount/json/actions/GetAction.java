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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import org.slf4j.Logger;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.meta.MetaContributor;
import com.openexchange.ajax.meta.MetaContributorRegistry;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.tools.JSONCoercion;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.jslob.JSlob;
import com.openexchange.jslob.JSlobId;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.json.fields.MailAccountFields;
import com.openexchange.mailaccount.json.writer.MailAccountWriter;
import com.openexchange.server.services.MetaContributors;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link GetAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@Action(method = RequestMethod.GET, name = "get", description = "Get a mail account", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "id", description = "The ID of the account to return.")
}, responseDescription = "A JSON object representing the desired mail account. See mail account data.")
public final class GetAction extends AbstractMailAccountAction implements MailAccountFields {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(GetAction.class);

    public static final String ACTION = AJAXServlet.ACTION_GET;

    /**
     * Initializes a new {@link GetAction}.
     */
    public GetAction() {
        super();
    }

    @Override
    protected AJAXRequestResult innerPerform(final AJAXRequestData requestData, final ServerSession session, final JSONValue jVoid) throws OXException {
        final int id = parseIntParameter(AJAXServlet.PARAMETER_ID, requestData);

        try {
            final MailAccountStorageService storageService =
                ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);

            final MailAccount mailAccount = storageService.getMailAccount(id, session.getUserId(), session.getContextId());

            if (isUnifiedINBOXAccount(mailAccount)) {
                // Treat as no hit
                throw MailAccountExceptionCodes.NOT_FOUND.create(
                    Integer.valueOf(id),
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(session.getContextId()));
            }

            if (!session.getUserPermissionBits().isMultipleMailAccounts() && !isDefaultMailAccount(mailAccount)) {
                throw MailAccountExceptionCodes.NOT_ENABLED.create(
                    Integer.valueOf(session.getUserId()),
                    Integer.valueOf(session.getContextId()));
            }

            final JSONObject jsonAccount = MailAccountWriter.write(mailAccount);
            // final JSONObject jsonAccount = MailAccountWriter.write(checkFullNames(mailAccount, storageService, session));

            {
                final JSlobId jSlobId = new JSlobId(JSLOB_SERVICE_ID, Integer.toString(id), session.getUserId(), session.getContextId());
                final JSlob jSlob = getStorage().opt(jSlobId);
                Map<String, Object> map = null == jSlob ? null : (Map<String, Object>) JSONCoercion.coerceToNative(jSlob.getJsonObject());
                map = contributeTo(map, id, session);

                if (map != null && !map.isEmpty()) {
                    jsonAccount.put(META, JSONCoercion.coerceToJSON(map));
                }
            }

            return new AJAXRequestResult(jsonAccount);
        } catch (final JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private static Map<String, Object> contributeTo(final Map<String, Object> map, final int accountId, final Session session) {
        final MetaContributorRegistry registry = MetaContributors.getRegistry();
        if (null == registry) {
            return map;
        }
        final Set<MetaContributor> contributors = registry.getMetaContributors("ox/mail/account");
        if (null != contributors && !contributors.isEmpty()) {
            final Map<String, Object> mapp = null == map ? new LinkedHashMap<String, Object>(2) : map;
            final String id = Integer.toString(accountId);
            for (final MetaContributor contributor : contributors) {
                try {
                    contributor.contributeTo(mapp, id, session);
                } catch (final Exception e) {
                    LOG.warn("Cannot contribute to entity (contributor={}, entity={})", contributor.getClass().getName(), Integer.valueOf(accountId), e);
                }
            }
            return mapp;
        }
        return map;
    }

}

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

package com.openexchange.quota.json.actions;

import static com.openexchange.mail.utils.StorageUtility.UNLIMITED_QUOTA;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.quota.json.QuotaAJAXRequest;
import com.openexchange.server.ServiceLookup;


/**
 * {@link MailAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailAction extends AbstractQuotaAction {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MailAction.class));

    /**
     * Initializes a new {@link MailAction}.
     * @param services
     */
    public MailAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final QuotaAJAXRequest req) throws OXException, JSONException {
        MailServletInterface mi = null;
        try {
            long[][] quotaInfo = null;
            try {
                mi = MailServletInterface.getInstance(req.getSession());
                quotaInfo = mi.getQuotas(new int[] {
                    MailServletInterface.QUOTA_RESOURCE_STORAGE, MailServletInterface.QUOTA_RESOURCE_MESSAGE });
            } catch (final OXException e) {
                if (MailExceptionCode.ACCOUNT_DOES_NOT_EXIST.equals(e)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(e.getMessage(), e);
                    }
                } else {
                    LOG.error(e.getMessage(), e);
                }
                quotaInfo = new long[][] { { UNLIMITED_QUOTA, UNLIMITED_QUOTA }, { UNLIMITED_QUOTA, UNLIMITED_QUOTA } };
            }
            final JSONObject data = new JSONObject();
            // STORAGE
            data.put("quota", quotaInfo[0][0] << 10);
            data.put("use", quotaInfo[0][1] << 10);
            // MESSAGE
            data.put("countquota", quotaInfo[1][0]);
            data.put("countuse", quotaInfo[1][1]);
            /*
             * Write JSON object into writer as data content of a response object
             */
            return new AJAXRequestResult(data, "json");
        } finally {
            try {
                if (mi != null) {
                    mi.close(false);
                }
            } catch (final OXException e) {
                LOG.error(e);
            }
        }
    }

}

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

package com.openexchange.voipnow.json.actions;

import java.util.Date;
import javax.mail.internet.MailDateFormat;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.voipnow.json.Utility;

/**
 * {@link FaxAuthAction} - Maps the action to a <tt>faxauth</tt> action.
 * <p>
 * A new call is initiated using VoipNow's HTTP API.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FaxAuthAction extends AbstractVoipNowAction {

    /**
     * The <tt>faxauth</tt> action string.
     */
    public static final String ACTION = "faxauth";

    /**
     * Initializes a new {@link FaxAuthAction}.
     */
    public FaxAuthAction() {
        super();
    }

    public AJAXRequestResult perform(final AJAXRequestData request, final ServerSession session) throws AbstractOXException {
        try {
            /*
             * Parse parameters
             */
            final String subject = checkStringParameter(request, "subject");
            final int validity;
            {
                final String tmp = request.getParameter("validity");
                int val = 3600;
                if (null != tmp) {
                    try {
                        val = Integer.parseInt(tmp.trim());
                    } catch (final NumberFormatException e) {
                        val = 3600;
                    }
                }
                validity = val;
            }
            /*
             * Get VoipNow server setting
             */
            final VoipNowServerSetting setting = getVoipNowServerSetting(session, true);
            /*
             * Get user's main extension
             */
            final User sessionUser = session.getUser();
            final String mainExtension = getMainExtensionNumberOfSessionUser(sessionUser, session.getContextId());
            /*
             * RFC822 format created date
             */
            final String createdString = generateRFC822CreatedDate(System.currentTimeMillis(), sessionUser.getTimeZone());
            /*
             * Compose JSON object
             */
            final JSONObject retval = new JSONObject();
            retval.put("X-fax-extension", mainExtension);
            retval.put("X-fax-created", createdString);
            if (validity > 0) {
                retval.put("X-fax-validity", validity);
            }
            retval.put("X-fax-account", setting.getLogin());
            retval.put("X-fax-validation", calculateHash(subject, validity, setting, mainExtension, createdString));
            /*
             * Return JSON object
             */
            return new AJAXRequestResult(retval);
        } catch (final JSONException e) {
            throw new AjaxException(AjaxException.Code.JSONError, e, e.getMessage());
        }
    }

    private String generateRFC822CreatedDate(final long now, final String timeZone) {
        final MailDateFormat mdf = new MailDateFormat();
        mdf.setTimeZone(Utility.getTimeZone(timeZone));
        return mdf.format(new Date(now));
    }

    private static final String TRANS_ENC = "hex";

    private String calculateHash(final String subject, final int validity, final VoipNowServerSetting setting, final String mainExtension, final String createdString) {
        final StringBuilder hashMe = new StringBuilder(512);
        hashMe.append(mainExtension).append(setting.getLogin()).append(createdString);
        if (validity > 0) {
            hashMe.append(validity);
        }
        hashMe.append(subject.trim());
        hashMe.append(Utility.getSha256(setting.getPassword(), TRANS_ENC));
        return Utility.getSha256(hashMe.toString(), TRANS_ENC);
    }

}

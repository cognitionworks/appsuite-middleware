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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.halo.linkedin;

import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.AddressException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.contact.ContactService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.halo.HaloContactDataSource;
import com.openexchange.halo.HaloContactImageSource;
import com.openexchange.halo.HaloContactQuery;
import com.openexchange.halo.Picture;
import com.openexchange.halo.linkedin.helpers.ContactEMailCompletor;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.linkedin.LinkedInService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;


/**
 * {@link LoFiLinkedinProfileDataSource}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class LoFiLinkedinProfileDataSource extends AbstractLinkedinDataSource implements HaloContactDataSource, HaloContactImageSource {

    public LoFiLinkedinProfileDataSource(final ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    public String getId() {
        return "com.openexchange.halo.linkedIn.fullProfile";
    }

    @Override
    public AJAXRequestResult investigate(final HaloContactQuery query, final AJAXRequestData req, final ServerSession session) throws OXException {
        final int uid = session.getUserId();
        final int cid = session.getContextId();

        final Contact contact = query.getContact();
        final ContactService contactService = serviceLookup.getService(ContactService.class);
        final UserService userService = serviceLookup.getService(UserService.class);
        final ContactEMailCompletor cc = new ContactEMailCompletor(session, contactService, userService);
        cc.complete(contact);

        final List<OAuthAccount> accounts = getOauthService().getAccounts(LinkedInService.SERVICE_ID, session, uid, cid);
        if(accounts.size() == 0) {
            throw LinkedinHaloExceptionCodes.NO_ACCOUNT.create();
        }

        String firstName = contact.getGivenName();
        String lastName = contact.getSurName();

        if (firstName == null || lastName == null) {
            List<String> eMail = getEMail(contact);
            for (String string : eMail) {
                if (!Strings.isEmpty(string)) {
                    try {
                        final String personal = new QuotedInternetAddress(string, false).getPersonal();
                        if (!Strings.isEmpty(personal)) {
                            String[] pSplit = personal.replace(",", " ").split("\\s+");
                            if (pSplit.length == 2) {
                                firstName = pSplit[0];
                                lastName = pSplit[1];
                                break;
                            }
                        }
                    } catch (final AddressException e) {
                        // Ignore
                    }
                }
            }
        }
        if (firstName == null || lastName == null) {
            final AJAXRequestResult result = new AJAXRequestResult();
            result.setResultObject(new JSONObject(), "json");
            return result;
        }
        final OAuthAccount linkedinAccount = accounts.get(0);
        final JSONObject json = getLinkedinService().getFullProfileByFirstAndLastName(firstName, lastName, session, uid, cid, linkedinAccount.getId());
        final AJAXRequestResult result = new AJAXRequestResult();
        result.setResultObject(json, "json");
        return result;
    }

    private List<String> getEMail(final Contact queryContact) {
        final List<String> emails = new ArrayList<String>(3);
        if(queryContact.containsEmail1()) {
            emails.add(queryContact.getEmail1());
        }
        if(queryContact.containsEmail2()) {
            emails.add(queryContact.getEmail2());
        }
        if(queryContact.containsEmail3()) {
            emails.add(queryContact.getEmail3());
        }
        return emails;
    }

    /* (non-Javadoc)
     * @see com.openexchange.halo.HaloContactImageSource#getPriority()
     */
    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.openexchange.halo.HaloContactImageSource#getPicture(com.openexchange.halo.HaloContactQuery, com.openexchange.tools.session.ServerSession)
     */
    @Override
    public Picture getPicture(HaloContactQuery contactQuery, ServerSession session) throws OXException {
        // TODO Auto-generated method stub
        return null;
    }

}

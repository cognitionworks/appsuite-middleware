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
package com.openexchange.halo.mail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.halo.AbstractContactHalo;
import com.openexchange.halo.HaloContactDataSource;
import com.openexchange.halo.HaloContactQuery;
import com.openexchange.mail.IndexRange;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.search.CcTerm;
import com.openexchange.mail.search.FromTerm;
import com.openexchange.mail.search.ORTerm;
import com.openexchange.mail.search.SearchTerm;
import com.openexchange.mail.search.ToTerm;
import com.openexchange.mail.service.MailService;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;

public class EmailContactHalo extends AbstractContactHalo implements HaloContactDataSource {

	private final ServiceLookup services;
	
	public EmailContactHalo(ServiceLookup services) {
		this.services = services;
	}

	@Override
	public String getId() {
		return "com.openexchange.halo.mail";
	}

	@Override
	public AJAXRequestResult investigate(HaloContactQuery query,
			AJAXRequestData req, ServerSession session) throws OXException {

		int[] params = req.checkIntArray(AJAXServlet.PARAMETER_COLUMNS);
		int limit = req.getIntParameter(AJAXServlet.PARAMETER_LIMIT);
		limit = limit < 0 ? 10 : limit;
		
		List<String> addresses = getEMailAddresses(query.getContact());
		if(isUserThemselves(session.getUser(), addresses)){
			return new AJAXRequestResult(new LinkedList<MailMessage>(), "mail");
		}
		
		MailField[] requestedFields = MailField.getFields(params);
		
		MailService mailService = services.getService(MailService.class);
		MailAccountStorageService mailAccountService = services.getService(MailAccountStorageService.class);
		
		MailAccount[] userMailAccounts;
		if(searchingExternalMailboxesIsFast()){
			userMailAccounts = mailAccountService.getUserMailAccounts(session.getUserId(), session.getContextId());
		} else {
			userMailAccounts = new MailAccount[]{mailAccountService.getDefaultMailAccount(session.getUserId(), session.getContextId())};
		}


		List<MailMessage> messages = new LinkedList<MailMessage>();
		for (MailAccount mailAccount : userMailAccounts) {
			MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
			try {
			mailAccess = mailService.getMailAccess(session, mailAccount.getId());
			mailAccess.connect();
			List<MailMessage> moreMessages = Arrays.asList(mailAccess.getMessageStorage().searchMessages(
				"INBOX",
				IndexRange.NULL, 
				MailSortField.RECEIVED_DATE, 
				OrderDirection.DESC, 
				generateSenderSearch(addresses), 
				requestedFields ));
			messages.addAll(moreMessages);
			
			String sentFullName = mailAccess.getFolderStorage().getSentFolder();
			moreMessages = Arrays.asList(mailAccess.getMessageStorage().searchMessages(
					sentFullName, 
					IndexRange.NULL, 
					MailSortField.RECEIVED_DATE, 
					OrderDirection.DESC, 
					generateRecipientSearch(addresses), 
					requestedFields ));
			messages.addAll(moreMessages);
			} finally {
				if (mailAccess != null) {
					mailAccess.close(true);
				}
			}
		}

		Collections.sort(messages, new Comparator<MailMessage>(){

			@Override
			public int compare(final MailMessage arg0, final MailMessage arg1) {
				final Date sentDate1 = arg1.getSentDate();
                final Date sentDate0 = arg0.getSentDate();
                if (sentDate1 == null) {
                    return null == sentDate0 ? 0 : -1;
                }
                return null == sentDate0 ? 1 : sentDate1.compareTo(sentDate0);
			}});
		
		messages = messages.subList(0, Math.min(limit, messages.size()));
		return new AJAXRequestResult(messages, "mail");
	}

	protected SearchTerm<?> generateSenderSearch(List<String> addresses) {
		List<FromTerm> queries = new LinkedList<FromTerm>();
		for (String addr : addresses) {
			queries.add(new FromTerm(addr));
		}
		final int size = queries.size();
        if (size == 3) {
            return new ORTerm(new ORTerm(queries.get(0), queries.get(1)), queries.get(2));
        }
		if (size == 2) {
            return new ORTerm(queries.get(0), queries.get(1));
        }
		return queries.get(0);
	}

	protected SearchTerm<?> generateRecipientSearch(List<String> addresses) {
		List<ORTerm> queries = new LinkedList<ORTerm>();
		for (String addr : addresses) {
			queries.add(new ORTerm(new CcTerm(addr), new ToTerm(addr)));
		}
		if (queries.size() == 3) {
            return new ORTerm(new ORTerm(queries.get(0), queries.get(1)), queries.get(2));
        }
		if (queries.size() == 2) {
            return new ORTerm(queries.get(0), queries.get(1));
        }
		return queries.get(0);
	}
	
	protected boolean searchingExternalMailboxesIsFast(){
		return false; //TODO: once indexing is implemented, this should check whether it is turned on. 
	}
}

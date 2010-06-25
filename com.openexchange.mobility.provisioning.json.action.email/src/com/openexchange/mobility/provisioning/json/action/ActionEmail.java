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

package com.openexchange.mobility.provisioning.json.action;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.mail.MailException;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.TextBodyMailPart;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mobility.provisioning.json.container.ProvisioningInformation;
import com.openexchange.mobility.provisioning.json.container.ProvisioningResponse;
import com.openexchange.mobility.provisioning.json.servlet.MobilityProvisioningServlet;

/**
 * 
 * @author <a href="mailto:benjamin.otterbach@open-xchange.com">Benjamin Otterbach</a>
 * 
 */
public class ActionEmail implements ActionService {

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MobilityProvisioningServlet.class);
	
    public ProvisioningResponse handleAction(ProvisioningInformation provisioningInformation) throws ActionException {
    	ProvisioningResponse provisioningResponse = new ProvisioningResponse();
    	
    	try {
			Context ctx = ContextStorage.getStorageContext(provisioningInformation.getSession());
			User user = UserStorage.getInstance().getUser(provisioningInformation.getSession().getUserId(), ctx);
			
			InternetAddress fromAddress = new InternetAddress(user.getMail(), true);
			if (!provisioningInformation.getMailFrom().trim().toUpperCase().equals("USER")) {
				fromAddress = new InternetAddress(provisioningInformation.getMailFrom(), true);
			}

			final com.openexchange.mail.transport.TransportProvider provider =
				com.openexchange.mail.transport.TransportProviderRegistry.getTransportProviderBySession(provisioningInformation.getSession(), 0);

			ComposedMailMessage msg = provider.getNewComposedMailMessage(provisioningInformation.getSession(), ctx);
			msg.addFrom(fromAddress);
			msg.addTo(new InternetAddress(provisioningInformation.getTarget()));
			msg.setSubject(provisioningInformation.getMailSubject());
			
			final TextBodyMailPart textPart = provider.getNewTextBodyPart(provisioningInformation.getUrl());
			msg.setBodyPart(textPart);
			msg.setContentType("text/plain");

			final MailTransport transport = MailTransport.getInstance(provisioningInformation.getSession());
			try {
				transport.sendMailMessage(msg, com.openexchange.mail.dataobjects.compose.ComposeType.NEW, new Address[] { new InternetAddress(provisioningInformation.getTarget()) });
			} finally {
				transport.close();
			}
			
			provisioningResponse.setMessage("Provisioning mail has been send to " + provisioningInformation.getTarget());
			provisioningResponse.setSuccess(true);
		} catch (MailException e) {
			logError("Couldn't send provisioning mail", e, provisioningResponse);
		} catch (ContextException e) {
			logError("Cannot find context for user", e, provisioningResponse);
		} catch (LdapException e) {
			logError("Cannot get user object", e, provisioningResponse);
		} catch (AddressException e) {
			logError("Target Spam email address cannot be parsed", e, provisioningResponse);
		}
    	
    	return provisioningResponse;
    }
    
    private void logError(String message, Exception e, ProvisioningResponse provisioningResponse) {
    	LOG.error(message, e);
    	provisioningResponse.setMessage(message);
    	provisioningResponse.setSuccess(false);
    }
	
}

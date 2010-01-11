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
package com.openexchange.admin.tools;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.admin.exceptions.OXMailToolException;
import com.sun.mail.smtp.SMTPMessage;

/**
 * This class is used for mail handling, especially for sending notification mails to
 * the admin user
 * TODO no mails are sent with admin. Maybe this class can be removed.
 * @author d7
 *
 */
public class MailTool {

    private final static Log log = LogFactory.getLog(MailTool.class);
    
    final static String MAILHOST = "localhost";
    
    /**
     * @param recipient
     * @param sender Can be left out by setting null here
     * @param subject
     * @param message
     * @throws OXMailToolException
     */
    public final static void sendMail(final String recipient, final String sender, final String subject, final String message) throws OXMailToolException {
        final Properties props = new Properties();
        props.put("mail.smtp.host", MAILHOST);
        final Session session = Session.getDefaultInstance(props);
        final SMTPMessage msg = new SMTPMessage(session);
        try {
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            if (null != sender) {
                msg.setFrom(new InternetAddress(sender));
            }
            msg.setSubject(subject);
            msg.setText(message);
            if (msg.getSentDate() == null) {
                msg.setSentDate(new Date());
            }
            
            msg.saveChanges();      // don't forget this
            Transport.send(msg);
        } catch (final AddressException e) {
            log.error(e.getMessage(), e);
            throw new OXMailToolException(e.toString());
        } catch (final MessagingException e) {
            log.error(e.getMessage(), e);
            throw new OXMailToolException(e.toString());
        }
    }
}

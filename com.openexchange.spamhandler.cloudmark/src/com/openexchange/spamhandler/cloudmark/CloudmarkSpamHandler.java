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
 *     Copyright (C) 2004-2013 Open-Xchange, Inc.
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

package com.openexchange.spamhandler.cloudmark;

import static com.openexchange.spamhandler.cloudmark.osgi.CloudmarkSpamHandlerServiceRegistry.getServiceRegistry;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.mail.MailField;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.transport.MailTransport.SendRawProperties;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.session.Session;
import com.openexchange.spamhandler.SpamHandler;

/**
 * Cloudmark spam handler
 *
 * @author <a href="mailto:benjamin.otterbach@open-xchange.com">Benjamin Otterbach</a>
 */
public final class CloudmarkSpamHandler extends SpamHandler {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CloudmarkSpamHandler.class);

    private static final String NAME = "CloudmarkSpamHandler";

    private static final CloudmarkSpamHandler SINGLETON = new CloudmarkSpamHandler();

    public static CloudmarkSpamHandler getInstance() {
        return SINGLETON;
    }

    private CloudmarkSpamHandler() {
        super();
    }

    @Override
    public String getSpamHandlerName() {
        return NAME;
    }

    @Override
    public void handleSpam(final int accountId, final String fullName, final String[] mailIDs, final boolean move, final Session session) throws OXException {
        final ConfigurationService configuration = getServiceRegistry().getService(ConfigurationService.class);
        final String targetSpamEmailAddress = configuration.getProperty("com.openexchange.spamhandler.cloudmark.targetSpamEmailAddress", "").trim();

        MailAccess<?, ?> mailAccess = null;
        try {
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect();
            final MailMessage[] mailMessage = mailAccess.getMessageStorage().getMessages(fullName, mailIDs, new MailField[]{MailField.FULL});
            for (int i = 0; i < mailMessage.length; i++) {
                final MailTransport transport = MailTransport.getInstance(session);
                try {
                    if (isEmpty(targetSpamEmailAddress)) {
                        LOG.debug("There is no value configured for 'com.openexchange.spamhandler.cloudmark.targetSpamEmailAddress', cannot process spam reporting to server.");
                    } else {
                        SendRawProperties sendRawProperties = MailTransport.SendRawProperties.newInstance()
                            .addRecipient(new InternetAddress(targetSpamEmailAddress, true))
                            .setSender(getSenderAddress(session))
                            .setValidateAddressHeaders(false)
                            .setSanitizeHeaders(false);
                        transport.sendRawMessage(mailMessage[i].getSourceBytes(), sendRawProperties);
                    }
                } catch (final AddressException e) {
                    LOG.error("The configured target eMail address is not valid", e);
                } finally {
                    transport.close();
                }
            }

            if (move) {
                final String targetSpamFolder = configuration.getProperty("com.openexchange.spamhandler.cloudmark.targetSpamFolder", "1").trim();
                if (targetSpamFolder.equals("1")) {
                    mailAccess.getMessageStorage().moveMessages(fullName, mailAccess.getFolderStorage().getTrashFolder(), mailIDs, true);
                } else if (targetSpamFolder.equals("2")) {
                    mailAccess.getMessageStorage().moveMessages(fullName, mailAccess.getFolderStorage().getSpamFolder(), mailIDs, true);
                } else if (targetSpamFolder.equals("3")) {
                    mailAccess.getMessageStorage().moveMessages(fullName, mailAccess.getFolderStorage().getConfirmedSpamFolder(), mailIDs, true);
                } else if (targetSpamFolder.equals("0")) {
                	// no move at all
                } else {
                    mailAccess.getMessageStorage().moveMessages(fullName, mailAccess.getFolderStorage().getTrashFolder(), mailIDs, true);
                    LOG.error("There is no valid 'com.openexchange.spamhandler.cloudmark.targetSpamFolder' configured. Moving spam to trash.");
                }
            }
        } finally {
            if (null != mailAccess) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public void handleHam(final int accountId, final String fullname, final String[] mailIDs, final boolean move, final Session session) throws OXException {
        final ConfigurationService configuration = getServiceRegistry().getService(ConfigurationService.class);
        final String targetHamEmailAddress = configuration.getProperty("com.openexchange.spamhandler.cloudmark.targetHamEmailAddress", "").trim();

        MailAccess<?, ?> mailAccess = null;
        try {
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect();
            final MailMessage[] mailMessage = mailAccess.getMessageStorage().getMessages(fullname, mailIDs, new MailField[]{MailField.FULL});
            for (int i = 0; i < mailMessage.length; i++) {
                final MailTransport transport = MailTransport.getInstance(session);
                try {
                    if (isEmpty(targetHamEmailAddress)) {
                        LOG.debug("There is no value configured for 'com.openexchange.spamhandler.cloudmark.targetHamEmailAddress', cannot process ham reporting to server.");
                    } else {
                        SendRawProperties sendRawProperties = MailTransport.SendRawProperties.newInstance()
                            .addRecipient(new InternetAddress(targetHamEmailAddress, true))
                            .setSender(getSenderAddress(session))
                            .setValidateAddressHeaders(false)
                            .setSanitizeHeaders(false);
                        transport.sendRawMessage(mailMessage[i].getSourceBytes(), sendRawProperties);
                    }
                } catch (final AddressException e) {
                    LOG.error("The configured target eMail address is not valid", e);
                } finally {
                    transport.close();
                }
            }

            if (move) {
                final String targetSpamFolder = configuration.getProperty("com.openexchange.spamhandler.cloudmark.targetSpamFolder", "1").trim();
                if (!targetSpamFolder.equals("0")) {
                    try {
                        mailAccess.getMessageStorage().moveMessages(fullname, "INBOX", mailIDs, true);
                    } finally {
                        if (null != mailAccess) {
                            mailAccess.close(true);
                        }
                    }
                }
            }
        } finally {
            if (null != mailAccess) {
                mailAccess.close(true);
            }
        }
    }

    @Override
    public boolean isCreateConfirmedSpam() {
        final ConfigurationService configuration = getServiceRegistry().getService(ConfigurationService.class);
        final String targetSpamFolder = configuration.getProperty("com.openexchange.spamhandler.cloudmark.targetSpamFolder", "1").trim();
        return targetSpamFolder.equals("3");
    }

    @Override
    public boolean isCreateConfirmedHam() {
        return false;
    }

    private static boolean isEmpty(final String string) {
        if (null == string) {
            return true;
        }
        final int len = string.length();
        boolean isWhitespace = true;
        for (int i = 0; isWhitespace && i < len; i++) {
            isWhitespace = com.openexchange.java.Strings.isWhitespace(string.charAt(i));
        }
        return isWhitespace;
    }

    /**
     * Gets the session users sender address.
     *
     * @return The address or <code>null</code> if not configured
     */
    private static InternetAddress getSenderAddress(Session session) throws OXException {
        UserSettingMail usm = UserSettingMailStorage.getInstance().getUserSettingMail(session);
        if (usm == null) {
            return null;
        }

        String sendAddr = usm.getSendAddr();
        if (sendAddr == null) {
            return null;
        }

        try {
            return new InternetAddress(sendAddr, true);
        } catch (AddressException e) {
            return null;
        }
    }
}

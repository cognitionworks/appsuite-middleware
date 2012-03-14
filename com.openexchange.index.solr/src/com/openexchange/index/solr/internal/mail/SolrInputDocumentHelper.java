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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.index.solr.internal.mail;

import static com.openexchange.mail.mime.QuotedInternetAddress.toIDN;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.mail.internet.InternetAddress;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import com.openexchange.index.solr.internal.Services;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.mime.utils.MimeMessageUtility;

/**
 * {@link SolrInputDocumentHelper} - Helper for <code>SolrInputDocument</code> to <code>MailMessage</code> conversion and vice versa.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SolrInputDocumentHelper implements SolrMailConstants {

    private static final SolrInputDocumentHelper INSTANCE = new SolrInputDocumentHelper();

    /**
     * Gets the instance
     * 
     * @return The instance
     */
    public static SolrInputDocumentHelper getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes a new {@link SolrInputDocumentHelper}.
     */
    private SolrInputDocumentHelper() {
        super();
    }

    /**
     * Gets the {@link SolrInputDocument} for specified {@link MailMessage} instance.
     * 
     * @param mail The mail message
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The newly created {@link SolrInputDocument} for specified {@link MailMessage} instance
     */
    public SolrInputDocument inputDocumentFor(final MailMessage mail, final int userId, final int contextId) {
        final int accountId = mail.getAccountId();
        final MailUUID uuid = new MailUUID(contextId, userId, accountId, mail.getFolder(), mail.getMailId());
        return createDocument(uuid.getUUID(), mail, accountId, userId, contextId, System.currentTimeMillis());
    }

    private static SolrInputDocument createDocument(final String uuid, final MailMessage mail, final int accountId, final int userId, final int contextId, final long stamp) {
        final SolrInputDocument inputDocument = new SolrInputDocument();
        /*
         * Un-analyzed fields
         */
        {
            final SolrInputField field = new SolrInputField(FIELD_TIMESTAMP);
            field.setValue(Long.valueOf(stamp), 1.0f);
            inputDocument.put(FIELD_TIMESTAMP, field);
        }
        {
            final SolrInputField field = new SolrInputField(FIELD_UUID);
            field.setValue(uuid, 1.0f);
            inputDocument.put(FIELD_UUID, field);
        }
        {
            final SolrInputField field = new SolrInputField(FIELD_CONTEXT);
            field.setValue(Long.valueOf(contextId), 1.0f);
            inputDocument.put(FIELD_CONTEXT, field);
        }
        {
            final SolrInputField field = new SolrInputField(FIELD_USER);
            field.setValue(Long.valueOf(userId), 1.0f);
            inputDocument.put(FIELD_USER, field);
        }
        {
            final SolrInputField field = new SolrInputField(FIELD_ACCOUNT);
            field.setValue(Integer.valueOf(accountId), 1.0f);
            inputDocument.put(FIELD_ACCOUNT, field);
        }
        {
            final SolrInputField field = new SolrInputField(FIELD_FULL_NAME);
            field.setValue(mail.getFolder(), 1.0f);
            inputDocument.put(FIELD_FULL_NAME, field);
        }
        {
            final SolrInputField field = new SolrInputField(FIELD_ID);
            field.setValue(mail.getMailId(), 1.0f);
            inputDocument.put(FIELD_ID, field);
        }
        /*-
         * Address fields

            "From"
            "To"
            "Cc"
            "Bcc"
            "Reply-To"
            "Sender"
            "Errors-To"
            "Resent-Bcc"
            "Resent-Cc"
            "Resent-From"
            "Resent-To"
            "Resent-Sender"
            "Disposition-Notification-To"

         */
        {
            AddressesPreparation preparation = new AddressesPreparation(mail.getFrom());
            SolrInputField field = new SolrInputField(FIELD_FROM_PERSONAL);
            field.setValue(preparation.personalList, 1.0f);
            inputDocument.put(FIELD_FROM_PERSONAL, field);
            field = new SolrInputField(FIELD_FROM_ADDR);
            field.setValue(preparation.addressList, 1.0f);
            inputDocument.put(FIELD_FROM_ADDR, field);
            field = new SolrInputField(FIELD_FROM_PLAIN);
            field.setValue(preparation.line, 1.0f);
            inputDocument.put(FIELD_FROM_PLAIN, field);

            preparation = new AddressesPreparation(mail.getTo());
            field = new SolrInputField(FIELD_TO_PERSONAL);
            field.setValue(preparation.personalList, 1.0f);
            inputDocument.put(FIELD_TO_PERSONAL, field);
            field = new SolrInputField(FIELD_TO_ADDR);
            field.setValue(preparation.addressList, 1.0f);
            inputDocument.put(FIELD_TO_ADDR, field);
            field = new SolrInputField(FIELD_TO_PLAIN);
            field.setValue(preparation.line, 1.0f);
            inputDocument.put(FIELD_TO_PLAIN, field);

            preparation = new AddressesPreparation(mail.getCc());
            field = new SolrInputField(FIELD_CC_PERSONAL);
            field.setValue(preparation.personalList, 1.0f);
            inputDocument.put(FIELD_CC_PERSONAL, field);
            field = new SolrInputField(FIELD_CC_ADDR);
            field.setValue(preparation.addressList, 1.0f);
            inputDocument.put(FIELD_CC_ADDR, field);
            field = new SolrInputField(FIELD_CC_PLAIN);
            field.setValue(preparation.line, 1.0f);
            inputDocument.put(FIELD_CC_PLAIN, field);

            preparation = new AddressesPreparation(mail.getBcc());
            field = new SolrInputField(FIELD_BCC_PERSONAL);
            field.setValue(preparation.personalList, 1.0f);
            inputDocument.put(FIELD_BCC_PERSONAL, field);
            field = new SolrInputField(FIELD_BCC_ADDR);
            field.setValue(preparation.addressList, 1.0f);
            inputDocument.put(FIELD_BCC_ADDR, field);
            field = new SolrInputField(FIELD_BCC_PLAIN);
            field.setValue(preparation.line, 1.0f);
            inputDocument.put(FIELD_BCC_PLAIN, field);
        }
        /*
         * Attachment flag
         */
        {
            final SolrInputField field = new SolrInputField(FIELD_ATTACHMENT);
            field.setValue(Boolean.valueOf(mail.hasAttachment()), 1.0f);
            inputDocument.put(FIELD_ATTACHMENT, field);
        }
        /*
         * Write color label
         */
        {
            final SolrInputField field = new SolrInputField(FIELD_COLOR_LABEL);
            field.setValue(Integer.valueOf(mail.getColorLabel()), 1.0f);
            inputDocument.put(FIELD_COLOR_LABEL, field);
        }
        /*
         * Write size
         */
        {
            final SolrInputField field = new SolrInputField(FIELD_SIZE);
            field.setValue(Long.valueOf(mail.getSize()), 1.0f);
            inputDocument.put(FIELD_SIZE, field);
        }
        /*
         * Write date fields
         */
        {
            java.util.Date d = mail.getReceivedDate();
            if (null != d) {
                final SolrInputField field = new SolrInputField(FIELD_RECEIVED_DATE);
                field.setValue(Long.valueOf(d.getTime()), 1.0f);
                inputDocument.put(FIELD_RECEIVED_DATE, field);
            }
            d = mail.getSentDate();
            if (null != d) {
                final SolrInputField field = new SolrInputField(FIELD_SENT_DATE);
                field.setValue(Long.valueOf(d.getTime()), 1.0f);
                inputDocument.put(FIELD_SENT_DATE, field);
            }
        }
        /*
         * Write flags
         */
        {
            final int flags = mail.getFlags();

            SolrInputField field = new SolrInputField(FIELD_FLAG_ANSWERED);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_ANSWERED) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_ANSWERED, field);

            field = new SolrInputField(FIELD_FLAG_DELETED);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_DELETED) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_DELETED, field);

            field = new SolrInputField(FIELD_FLAG_DRAFT);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_DRAFT) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_DRAFT, field);

            field = new SolrInputField(FIELD_FLAG_FLAGGED);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_FLAGGED) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_FLAGGED, field);

            field = new SolrInputField(FIELD_FLAG_RECENT);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_RECENT) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_RECENT, field);

            field = new SolrInputField(FIELD_FLAG_SEEN);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_SEEN) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_SEEN, field);

            field = new SolrInputField(FIELD_FLAG_USER);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_USER) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_USER, field);

            field = new SolrInputField(FIELD_FLAG_SPAM);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_SPAM) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_SPAM, field);

            field = new SolrInputField(FIELD_FLAG_FORWARDED);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_FORWARDED) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_FORWARDED, field);

            field = new SolrInputField(FIELD_FLAG_READ_ACK);
            field.setValue(Boolean.valueOf((flags & MailMessage.FLAG_READ_ACK) > 0), 1.0f);
            inputDocument.put(FIELD_FLAG_READ_ACK, field);
        }
        /*
         * User flags
         */
        {
            final String[] userFlags = mail.getUserFlags();
            if (null != userFlags && userFlags.length > 0) {
                final SolrInputField field = new SolrInputField(FIELD_USER_FLAGS);
                field.setValue(Arrays.asList(userFlags), 1.0f);
                inputDocument.put(FIELD_USER_FLAGS, field);
            }
        }
        /*
         * Subject
         */
        {
            final String subject = mail.getSubject();
            SolrInputField field = new SolrInputField(FIELD_SUBJECT_PLAIN);
            field.setValue(subject, 1.0f);
            inputDocument.put(FIELD_SUBJECT_PLAIN, field);

            String language;
            try {
                final Locale detectedLocale = Services.detectLocale(subject);
                if (null == detectedLocale || !Services.isSupportedLocale(detectedLocale)) {
                    language = "en";
                } else {
                    language = detectedLocale.getLanguage();
                }
            } catch (final Exception e) {
                language = "en";
            }
            final String name = FIELD_SUBJECT_PREFIX + language;
            field = new SolrInputField(name);
            field.setValue(subject, 1.0f);
            inputDocument.put(name, field);
        }
        return inputDocument;
    }

    private static final class AddressesPreparation {

        protected final List<String> personalList;

        protected final List<String> addressList;

        protected final String line;

        protected AddressesPreparation(final InternetAddress[] addrs) {
            super();
            if (addrs == null || addrs.length <= 0) {
                personalList = Collections.emptyList();
                addressList = Collections.emptyList();
                line = null;
            } else {
                final List<String> pl = new LinkedList<String>();
                final List<String> al = new LinkedList<String>();
                final StringBuilder lineBuilder = new StringBuilder(256);
                for (int i = 0; i < addrs.length; i++) {
                    final InternetAddress address = addrs[i];
                    final String personal = address.getPersonal();
                    if (!isEmpty(personal)) {
                        pl.add(preparePersonal(personal));
                    }
                    al.add(prepareAddress(address.getAddress()));
                    lineBuilder.append(", ").append(address.toString());
                }
                personalList = pl;
                addressList = al;
                lineBuilder.delete(0, 2);
                line = lineBuilder.toString();
            }
        }

        private static String preparePersonal(final String personal) {
            return MimeMessageUtility.quotePhrase(MimeMessageUtility.decodeMultiEncodedHeader(personal), false);
        }

        private static final String DUMMY_DOMAIN = "@unspecified-domain";

        private static String prepareAddress(final String address) {
            final String decoded = MimeMessageUtility.decodeMultiEncodedHeader(address);
            final int pos = decoded.indexOf(DUMMY_DOMAIN);
            if (pos >= 0) {
                return toIDN(decoded.substring(0, pos));
            }
            return toIDN(decoded);
        }

    }

    /**
     * Checks for an empty string.
     */
    protected static boolean isEmpty(final String string) {
        if (null == string) {
            return true;
        }
        final int len = string.length();
        boolean isWhitespace = true;
        for (int i = 0; isWhitespace && i < len; i++) {
            isWhitespace = Character.isWhitespace(string.charAt(i));
        }
        return isWhitespace;
    }

}

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

package com.openexchange.mail.json.writer;

import static com.openexchange.mail.mime.QuotedInternetAddress.toIDN;
import static com.openexchange.mail.utils.MailFolderUtility.prepareFullname;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.TimeZone;
import javax.mail.internet.InternetAddress;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.fields.FolderChildFields;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.mail.parser.MailMessageParser;
import com.openexchange.mail.parser.handlers.JSONMessageHandler;
import com.openexchange.mail.parser.handlers.RawJSONMessageHandler;
import com.openexchange.mail.structure.StructureMailMessageParser;
import com.openexchange.mail.structure.handler.MIMEStructureHandler;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.mail.utils.DisplayMode;
import com.openexchange.session.Session;
import com.openexchange.tools.TimeZoneUtils;

/**
 * {@link MessageWriter} - Writes {@link MailMessage} instances as JSON strings
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MessageWriter {

    // private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MessageWriter.class));

    /**
     * No instantiation
     */
    private MessageWriter() {
        super();
    }

    /**
     * Writes specified mail's structure as a JSON object.
     * <p>
     * Optionally a prepared version can be returned, this includes following actions:
     * <ol>
     * <li>Header names are inserted to JSON lower-case</li>
     * <li>Mail-safe encoded header values as per RFC 2047 are decoded;<br>
     * e.g.&nbsp;<code><i>To:&nbsp;=?iso-8859-1?q?Keld_J=F8rn?=&nbsp;&lt;keld@xyz.dk&gt;</i></code></li>
     * <li>Address headers are delivered as JSON objects with a <code>"personal"</code> and an <code>"address"</code> field</li>
     * <li>Parameterized headers are delivered as JSON objects with a <code>"type"</code> and a <code>"params"</code> field</li>
     * </ol>
     *
     * @param accountId The mail's account ID
     * @param mail The mail to write
     * @param maxSize The allowed max. size
     * @return The structure as a JSON object
     * @throws OXException If writing structure fails
     */
    public static JSONObject writeStructure(final int accountId, final MailMessage mail, final long maxSize) throws OXException {
        final MIMEStructureHandler handler = new MIMEStructureHandler(maxSize);
        new StructureMailMessageParser().setParseTNEFParts(true).parseMailMessage(mail, handler);
        return handler.getJSONMailObject();
    }

    /**
     * Writes whole mail as a JSON object.
     *
     * @param accountId The account ID
     * @param mail The mail to write
     * @param displayMode The display mode
     * @param session The session
     * @param settings The user's mail settings used for writing message; if <code>null</code> the settings are going to be fetched from
     *            storage, thus no request-specific preparations will take place.
     * @param warnings A container for possible warnings
     * @return The written JSON object
     * @throws OXException If writing message fails
     */
    public static JSONObject writeMailMessage(final int accountId, final MailMessage mail, final DisplayMode displayMode, final Session session, final UserSettingMail settings) throws OXException {
        return writeMailMessage(accountId, mail, displayMode, session, settings, null, false, -1);
    }

    /**
     * Writes whole mail as a JSON object.
     *
     * @param accountId The account ID
     * @param mail The mail to write
     * @param displayMode The display mode
     * @param session The session
     * @param settings The user's mail settings used for writing message; if <code>null</code> the settings are going to be fetched from
     *            storage, thus no request-specific preparations will take place.
     * @param warnings A container for possible warnings
     * @param tokenTimeout
     * @token <code>true</code> to add attachment tokens
     * @return The written JSON object
     * @throws OXException If writing message fails
     */
    public static JSONObject writeMailMessage(final int accountId, final MailMessage mail, final DisplayMode displayMode, final Session session, final UserSettingMail settings, final Collection<OXException> warnings, final boolean token, final int tokenTimeout) throws OXException {
        return writeMailMessage(accountId, mail, displayMode, session, settings, warnings, token, tokenTimeout, Collections.<String> emptyList());
    }

    /**
     * Writes whole mail as a JSON object.
     *
     * @param accountId The account ID
     * @param mail The mail to write
     * @param displayMode The display mode
     * @param session The session
     * @param settings The user's mail settings used for writing message; if <code>null</code> the settings are going to be fetched from
     *            storage, thus no request-specific preparations will take place.
     * @param warnings A container for possible warnings
     * @param tokenTimeout
     * @param ignorableContentTypes The Content-Types to ignore
     * @token <code>true</code> to add attachment tokens
     * @return The written JSON object
     * @throws OXException If writing message fails
     */
    public static JSONObject writeMailMessage(final int accountId, final MailMessage mail, final DisplayMode displayMode, final Session session, final UserSettingMail settings, final Collection<OXException> warnings, final boolean token, final int tokenTimeout, final List<String> ignorableContentTypes) throws OXException {
        final MailPath mailPath;
        if (mail.getFolder() != null && mail.getMailId() != null) {
            mailPath = new MailPath(accountId, mail.getFolder(), mail.getMailId());
        } else if (mail.getMsgref() != null) {
            mailPath = mail.getMsgref();
        } else {
            mailPath = MailPath.NULL;
        }
        final UserSettingMail usm;
        try {
            usm =
                null == settings ? UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), session.getContextId()) : settings;
        } catch (final OXException e) {
            throw new OXException(e);
        }
        final JSONMessageHandler handler = new JSONMessageHandler(accountId, mailPath, mail, displayMode, session, usm, token, tokenTimeout);
        final MailMessageParser parser = new MailMessageParser().addIgnorableContentTypes(null == ignorableContentTypes ? Collections.<String> emptyList() : ignorableContentTypes);
        parser.parseMailMessage(mail, handler);
        if (null != warnings) {
            final List<OXException> list = parser.getWarnings();
            if (!list.isEmpty()) {
                warnings.addAll(list);
            }
        }
        if (!mail.isDraft()) {
            return handler.getJSONObject();
        }
        /*
         * Ensure "msgref" is present in draft mail
         */
        final JSONObject jsonObject = handler.getJSONObject();
        final String key = MailJSONField.MSGREF.getKey();
        if (!jsonObject.has(key) && null != mailPath) {
            try {
                jsonObject.put(key, mailPath.toString());
            } catch (final JSONException e) {
                throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
            }
        }
        return jsonObject;
    }

    /**
     * Writes raw mail as a JSON object.
     *
     * @param accountId The account ID
     * @param mail The mail to write
     * @return The written JSON object or <code>null</code> if message's text body parts exceed max. size
     * @throws OXException If writing message fails
     */
    public static JSONObject writeRawMailMessage(final int accountId, final MailMessage mail) throws OXException {
        final MailPath mailPath;
        if (mail.getFolder() != null && mail.getMailId() != null) {
            mailPath = new MailPath(accountId, mail.getFolder(), mail.getMailId());
        } else if (mail.getMsgref() != null) {
            mailPath = mail.getMsgref();
        } else {
            mailPath = MailPath.NULL;
        }
        final RawJSONMessageHandler handler = new RawJSONMessageHandler(accountId, mailPath, mail);
        new MailMessageParser().parseMailMessage(mail, handler);
        return handler.getJSONObject();
    }

    public static interface MailFieldWriter {

        public void writeField(JSONValue jsonContainer, MailMessage mail, int level, boolean withKey, int accountId, int user, int cid) throws OXException;
    }

    private static final class HeaderFieldWriter implements MailFieldWriter {

        private final String headerName;

        HeaderFieldWriter(final String headerName) {
            super();
            this.headerName = headerName;
        }

        @Override
        public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
            final Object value = getHeaderValue(mail);
            if (withKey) {
                if (null != value) {
                    try {
                        ((JSONObject) jsonContainer).put(headerName, value);
                    } catch (final JSONException e) {
                        throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                    }
                }
            } else {
                ((JSONArray) jsonContainer).put(null == value ? JSONObject.NULL : value);
            }
        }

        private Object getHeaderValue(final MailMessage mail) {
            final String[] headerValues = mail.getHeader(headerName);
            if (null == headerValues || 0 == headerValues.length) {
                return null;
            }
            if (1 == headerValues.length) {
                return headerValues[0];
            }
            final JSONArray ja = new JSONArray();
            for (int j = 0; j < headerValues.length; j++) {
                ja.put(headerValues[j]);
            }
            return ja;
        }

    }

    private static final EnumMap<MailListField, MailFieldWriter> WRITERS = new EnumMap<MailListField, MailFieldWriter>(MailListField.class);

    static {
        WRITERS.put(MailListField.ID, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(DataFields.ID, mail.getMailId());
                    } else {
                        ((JSONArray) jsonContainer).put(mail.getMailId());
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.FOLDER_ID, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(FolderChildFields.FOLDER_ID, prepareFullname(accountId, mail.getFolder()));
                    } else {
                        ((JSONArray) jsonContainer).put(prepareFullname(accountId, mail.getFolder()));
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.ATTACHMENT, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.HAS_ATTACHMENTS.getKey(), mail.hasAttachment());
                    } else {
                        ((JSONArray) jsonContainer).put(mail.hasAttachment());
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.FROM, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.FROM.getKey(), getAddressesAsArray(mail.getFrom()));
                    } else {
                        ((JSONArray) jsonContainer).put(getAddressesAsArray(mail.getFrom()));
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.TO, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.RECIPIENT_TO.getKey(), getAddressesAsArray(mail.getTo()));
                    } else {
                        ((JSONArray) jsonContainer).put(getAddressesAsArray(mail.getTo()));
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.CC, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.RECIPIENT_CC.getKey(), getAddressesAsArray(mail.getCc()));
                    } else {
                        ((JSONArray) jsonContainer).put(getAddressesAsArray(mail.getCc()));
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.BCC, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.RECIPIENT_BCC.getKey(), getAddressesAsArray(mail.getBcc()));
                    } else {
                        ((JSONArray) jsonContainer).put(getAddressesAsArray(mail.getBcc()));
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.SUBJECT, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    final String subject = mail.getSubject();
                    if (withKey) {
                        if (subject != null) {
                            ((JSONObject) jsonContainer).put(
                                MailJSONField.SUBJECT.getKey(),
                                MimeMessageUtility.decodeMultiEncodedHeader(subject.trim()));
                        }
                    } else {
                        ((JSONArray) jsonContainer).put(subject == null ? JSONObject.NULL : MimeMessageUtility.decodeMultiEncodedHeader(subject.trim()));
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.SIZE, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.SIZE.getKey(), mail.getSize());
                    } else {
                        ((JSONArray) jsonContainer).put(mail.getSize());
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.SENT_DATE, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        if (mail.containsSentDate() && mail.getSentDate() != null) {
                            ((JSONObject) jsonContainer).put(
                                MailJSONField.SENT_DATE.getKey(),
                                addUserTimezone(mail.getSentDate().getTime(), TimeZoneUtils.getTimeZone(UserStorage.getStorageUser(
                                    user,
                                    ContextStorage.getStorageContext(cid)).getTimeZone())));
                        }
                    } else {
                        if (mail.containsSentDate() && mail.getSentDate() != null) {
                            ((JSONArray) jsonContainer).put(addUserTimezone(
                                mail.getSentDate().getTime(),
                                TimeZoneUtils.getTimeZone(UserStorage.getStorageUser(user, ContextStorage.getStorageContext(cid)).getTimeZone())));
                        } else {
                            ((JSONArray) jsonContainer).put(JSONObject.NULL);
                        }
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                } catch (final OXException e) {
                    throw new OXException(e);
                }
            }
        });
        WRITERS.put(MailListField.RECEIVED_DATE, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        if (mail.containsReceivedDate() && mail.getReceivedDate() != null) {
                            ((JSONObject) jsonContainer).put(
                                MailJSONField.RECEIVED_DATE.getKey(),
                                addUserTimezone(mail.getReceivedDate().getTime(), TimeZoneUtils.getTimeZone(UserStorage.getStorageUser(
                                    user,
                                    ContextStorage.getStorageContext(cid)).getTimeZone())));
                        }
                    } else {
                        if (mail.containsReceivedDate() && mail.getReceivedDate() != null) {
                            ((JSONArray) jsonContainer).put(addUserTimezone(
                                mail.getReceivedDate().getTime(),
                                TimeZoneUtils.getTimeZone(UserStorage.getStorageUser(user, ContextStorage.getStorageContext(cid)).getTimeZone())));
                        } else {
                            ((JSONArray) jsonContainer).put(JSONObject.NULL);
                        }
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                } catch (final OXException e) {
                    throw new OXException(e);
                }
            }
        });
        WRITERS.put(MailListField.FLAGS, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.FLAGS.getKey(), mail.getFlags());
                    } else {
                        ((JSONArray) jsonContainer).put(mail.getFlags());
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.THREAD_LEVEL, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.THREAD_LEVEL.getKey(), mail.getThreadLevel());
                    } else {
                        ((JSONArray) jsonContainer).put(mail.getThreadLevel());
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.DISPOSITION_NOTIFICATION_TO, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    final Object value;
                    if ((mail.containsPrevSeen() ? mail.isPrevSeen() : mail.isSeen())) {
                        value = JSONObject.NULL;
                    } else {
                        value =
                            mail.getDispositionNotification() == null ? JSONObject.NULL : mail.getDispositionNotification().toUnicodeString();
                    }
                    if (withKey) {
                        if (!JSONObject.NULL.equals(value)) {
                            ((JSONObject) jsonContainer).put(MailJSONField.DISPOSITION_NOTIFICATION_TO.getKey(), value);
                        }
                    } else {
                        ((JSONArray) jsonContainer).put(value);
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.PRIORITY, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.PRIORITY.getKey(), mail.getPriority());
                    } else {
                        ((JSONArray) jsonContainer).put(mail.getPriority());
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.MSG_REF, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        if (mail.containsMsgref()) {
                            ((JSONObject) jsonContainer).put(MailJSONField.MSGREF.getKey(), mail.getMsgref());
                        }
                    } else {
                        ((JSONArray) jsonContainer).put(mail.containsMsgref() ? mail.getMsgref() : JSONObject.NULL);
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.COLOR_LABEL, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    final int colorLabel;
                    if (MailProperties.getInstance().isUserFlagsEnabled() && mail.containsColorLabel()) {
                        colorLabel = mail.getColorLabel();
                    } else {
                        colorLabel = 0;
                    }
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.COLOR_LABEL.getKey(), colorLabel);
                    } else {
                        ((JSONArray) jsonContainer).put(colorLabel);
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.TOTAL, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.TOTAL.getKey(), JSONObject.NULL);
                    } else {
                        ((JSONArray) jsonContainer).put(JSONObject.NULL);
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.NEW, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.NEW.getKey(), JSONObject.NULL);
                    } else {
                        ((JSONArray) jsonContainer).put(JSONObject.NULL);
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.UNREAD, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.UNREAD.getKey(), mail.getUnreadMessages());
                    } else {
                        ((JSONArray) jsonContainer).put(mail.getUnreadMessages());
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.DELETED, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.DELETED.getKey(), JSONObject.NULL);
                    } else {
                        ((JSONArray) jsonContainer).put(JSONObject.NULL);
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
        WRITERS.put(MailListField.ACCOUNT_NAME, new MailFieldWriter() {

            @Override
            public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
                try {
                    if (withKey) {
                        ((JSONObject) jsonContainer).put(MailJSONField.ACCOUNT_NAME.getKey(), mail.getAccountName());
                    } else {
                        ((JSONArray) jsonContainer).put(mail.getAccountName());
                    }
                } catch (final JSONException e) {
                    throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
                }
            }
        });
    }

    private static final MailFieldWriter UNKNOWN = new MailFieldWriter() {

        @Override
        public void writeField(final JSONValue jsonContainer, final MailMessage mail, final int level, final boolean withKey, final int accountId, final int user, final int cid) throws OXException {
            try {
                if (withKey) {
                    ((JSONObject) jsonContainer).put("Unknown column", JSONObject.NULL);
                } else {
                    ((JSONArray) jsonContainer).put(JSONObject.NULL);
                }
            } catch (final JSONException e) {
                throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
            }
        }
    };

    /**
     * Generates appropriate field writers for given mail fields
     *
     * @param fields The mail fields to write
     * @return Appropriate field writers as an array of {@link MailFieldWriter}
     */
    public static MailFieldWriter[] getMailFieldWriter(final MailListField[] fields) {
        final MailFieldWriter[] retval = new MailFieldWriter[fields.length];
        for (int i = 0; i < fields.length; i++) {
            final MailFieldWriter mfw = WRITERS.get(fields[i]);
            if (mfw == null) {
                retval[i] = UNKNOWN;
            } else {
                retval[i] = mfw;
            }
        }
        return retval;
    }

    /**
     * Gets writers for specified header names.
     *
     * @param headers The header names
     * @return The writers for specified header names
     */
    public static MailFieldWriter[] getHeaderFieldWriter(final String[] headers) {
        if (null == headers) {
            return new MailFieldWriter[0];
        }
        final MailFieldWriter[] retval = new MailFieldWriter[headers.length];
        for (int i = 0; i < headers.length; i++) {
            retval[i] = new HeaderFieldWriter(headers[i]);
        }
        return retval;
    }

    /**
     * Adds the user time zone offset to given date time
     *
     * @param time The date time
     * @param timeZone The time zone
     * @return The time with added time zone offset
     */
    public static long addUserTimezone(final long time, final TimeZone timeZone) {
        return (time + timeZone.getOffset(time));
    }

    private static final JSONArray EMPTY_JSON_ARR = new JSONArray();

    /**
     * Convert an array of <code>InternetAddress</code> instances into a JSON-Array conforming to:
     *
     * <pre>
     * [[&quot;The Personal&quot;, &quot;someone@somewhere.com&quot;], ...]
     * </pre>
     */
    public static JSONArray getAddressesAsArray(final InternetAddress[] addrs) {
        if (addrs == null || addrs.length == 0) {
            return EMPTY_JSON_ARR;
        }
        final JSONArray jsonArr = new JSONArray();
        for (final InternetAddress address : addrs) {
            jsonArr.put(getAddressAsArray(address));
        }
        return jsonArr;
    }

    /**
     * Convert an <code>InternetAddress</code> instance into a JSON-Array conforming to: ["The Personal", "someone@somewhere.com"]
     */
    private static JSONArray getAddressAsArray(final InternetAddress addr) {
        final JSONArray retval = new JSONArray();
        // Personal
        final String personal = addr.getPersonal();
        retval.put(personal == null || personal.length() == 0 ? JSONObject.NULL : preparePersonal(personal));
        // Address
        final String address = addr.getAddress();
        retval.put(address == null || address.length() == 0 ? JSONObject.NULL : prepareAddress(toIDN(address)));

        return retval;
    }

    // private static final Pattern PATTERN_QUOTE = Pattern.compile("[.,:;<>\"]");

    private static String preparePersonal(final String personal) {
        return MimeMessageUtility.quotePhrase(MimeMessageUtility.decodeMultiEncodedHeader(personal), false);
    }

    private static final String DUMMY_DOMAIN = "@unspecified-domain";

    private static String prepareAddress(final String address) {
        final String decoded = MimeMessageUtility.decodeMultiEncodedHeader(address);
        final int pos = decoded.indexOf(DUMMY_DOMAIN);
        if (pos >= 0) {
            return decoded.substring(0, pos);
        }
        return decoded;
    }

}

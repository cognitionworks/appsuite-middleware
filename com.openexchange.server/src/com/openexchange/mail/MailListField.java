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
 *    trademarks of the OX Software GmbH. group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.mail;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import com.openexchange.ajax.fields.CommonFields;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.fields.FolderChildFields;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.container.FolderObject;

/**
 * {@link MailListField} - An enumeration of mail list fields as defined in <a href=
 * "http://www.open-xchange.com/wiki/index.php?title=HTTP_API#Module_.22mail.22" >HTTP API's mail section</a>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public enum MailListField {

    /**
     * The mail ID
     */
    ID(600, DataFields.ID),
    /**
     * The folder ID
     */
    FOLDER_ID(601, FolderChildFields.FOLDER_ID),
    /**
     * Whether message contains attachments
     */
    ATTACHMENT(602, MailJSONField.HAS_ATTACHMENTS.getKey()),
    /**
     * From
     */
    FROM(603, MailJSONField.FROM.getKey()),
    /**
     * To
     */
    TO(604, MailJSONField.RECIPIENT_TO.getKey()),
    /**
     * Cc
     */
    CC(605, MailJSONField.RECIPIENT_CC.getKey()),
    /**
     * Bcc
     */
    BCC(606, MailJSONField.RECIPIENT_BCC.getKey()),
    /**
     * Subject
     */
    SUBJECT(607, MailJSONField.SUBJECT.getKey()),
    /**
     * Size
     */
    SIZE(608, MailJSONField.SIZE.getKey()),
    /**
     * Sent date
     */
    SENT_DATE(609, MailJSONField.SENT_DATE.getKey()),
    /**
     * Received date
     */
    RECEIVED_DATE(610, MailJSONField.RECEIVED_DATE.getKey()),
    /**
     * Flags
     */
    FLAGS(611, MailJSONField.FLAGS.getKey()),
    /**
     * Thread level
     */
    THREAD_LEVEL(612, MailJSONField.THREAD_LEVEL.getKey()),
    /**
     * <code>Disposition-Notification-To</code>
     */
    DISPOSITION_NOTIFICATION_TO(613, MailJSONField.DISPOSITION_NOTIFICATION_TO.getKey()),
    /**
     * Priority
     */
    PRIORITY(614, MailJSONField.PRIORITY.getKey()),
    /**
     * Message reference
     */
    MSG_REF(615, MailJSONField.MSGREF.getKey()),
    /**
     * Color Label
     */
    COLOR_LABEL(CommonObject.COLOR_LABEL, CommonFields.COLORLABEL),
    /**
     * Folder
     */
    FOLDER(650, MailJSONField.FOLDER.getKey()),
    /**
     * Flag \SEEN
     */
    FLAG_SEEN(651, MailJSONField.SEEN.getKey()),
    /**
     * Total count
     */
    TOTAL(FolderObject.TOTAL, MailJSONField.TOTAL.getKey()),
    /**
     * New count
     */
    NEW(FolderObject.NEW, MailJSONField.NEW.getKey()),
    /**
     * Unread count
     */
    UNREAD(FolderObject.UNREAD, MailJSONField.UNREAD.getKey()),
    /**
     * Deleted count
     */
    DELETED(FolderObject.DELETED, MailJSONField.DELETED.getKey()),
    /**
     * Account name
     */
    ACCOUNT_NAME(652, MailJSONField.ACCOUNT_NAME.getKey()),
    /**
     * Account identifier
     */
    ACCOUNT_ID(653, MailJSONField.ACCOUNT_ID.getKey()),
    /**
     * The original mail ID.
     * @since v7.8.0
     */
    ORIGINAL_ID(654, MailJSONField.ORIGINAL_ID.getKey()),
    /**
     * The original folder ID
     * @since v7.8.0
     */
    ORIGINAL_FOLDER_ID(655, MailJSONField.ORIGINAL_FOLDER_ID.getKey()),
    /**
     * The MIME type information
     * @since v7.8.0
     */
    MIME_TYPE(656, MailJSONField.CONTENT_TYPE.getKey()),
    ;

    private final int field;

    private final String key;

    private MailListField(final int field, final String jsonKey) {
        this.field = field;
        key = jsonKey;
    }

    /**
     * @return The <code>int</code> field value
     */
    public int getField() {
        return field;
    }

    /**
     * @return The JSON key
     */
    public String getKey() {
        return key;
    }

    private static final MailListField[] EMPTY_FIELDS = new MailListField[0];

    private static final TIntObjectMap<MailListField> FIELDS_MAP = new TIntObjectHashMap<MailListField>(25);

    static {
        final MailListField[] fields = MailListField.values();
        for (final MailListField listField : fields) {
            FIELDS_MAP.put(listField.field, listField);
        }
    }

    /**
     * Creates an array of {@link MailListField} corresponding to given <code>int</code> values
     *
     * @param fields The <code>int</code> values
     * @return The array of {@link MailListField} corresponding to given <code>int</code> values
     */
    public static final MailListField[] getFields(final int[] fields) {
        if ((fields == null) || (fields.length == 0)) {
            return EMPTY_FIELDS;
        }
        final MailListField[] retval = new MailListField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            retval[i] = getField(fields[i]);
        }
        return retval;
    }

    /**
     * Determines the corresponding {@link MailListField} constant to given <code>int</code> value
     *
     * @param field The <code>int</code> value
     * @return The corresponding {@link MailListField} constant
     */
    public static final MailListField getField(final int field) {
        return FIELDS_MAP.get(field);
    }

    /**
     * Returns all field values as an array of integers.
     *
     * @return
     */
    public static final int[] getAllFields() {
        final MailListField[] values = values();
        final int[] all = new int[values.length];
        for (int i = 0; i < all.length; i++) {
            all[i] = values[i].getField();
        }
        return all;
    }

    /**
     * Gets a field by the JSON name
     *
     * @param jsonName identifier
     * @return MailListField identified by jsonName, null if not found
     */
    public static final MailListField getBy(final String jsonName) {
        final MailListField[] values = values();
        for (final MailListField field : values) {
            if (jsonName.equals(field.getKey())) {
                return field;
            }
        }
        return null;
    }
}

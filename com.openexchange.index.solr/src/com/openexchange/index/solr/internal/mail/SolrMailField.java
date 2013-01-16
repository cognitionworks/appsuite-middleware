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

package com.openexchange.index.solr.internal.mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import com.openexchange.index.solr.internal.SolrField;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.index.MailIndexField;

/**
 * {@link SolrMailField}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public enum SolrMailField implements SolrField {

    UUID("uuid", MailIndexField.UUID, "param1"),
    TIMESTAMP("timestamp", MailIndexField.TIMESTAMP, "param2"),
    ACCOUNT("account", MailIndexField.ACCOUNT, "param3"),
    FULL_NAME("full_name", MailIndexField.FULL_NAME, "param4"),
    ID("id", MailIndexField.ID, "param5"),
    COLOR_LABEL("color_label", MailIndexField.COLOR_LABEL, "param6"),
    ATTACHMENT("attachment", MailIndexField.ATTACHMENT, "param7"),
    RECEIVED_DATE("received_date", MailIndexField.RECEIVED_DATE, "param8"),
    SENT_DATE("sent_date", MailIndexField.SENT_DATE, "param9"),
    SIZE("size", MailIndexField.SIZE, "param10"),
    FLAG_ANSWERED("flag_answered", MailIndexField.FLAG_ANSWERED, "param11"),
    FLAG_DELETED("flag_deleted", MailIndexField.FLAG_DELETED, "param12"),
    FLAG_DRAFT("flag_draft", MailIndexField.FLAG_DRAFT, "param13"),
    FLAG_FLAGGED("flag_flagged", MailIndexField.FLAG_FLAGGED, "param14"),
    FLAG_RECENT("flag_recent", MailIndexField.FLAG_RECENT, "param15"),
    FLAG_SEEN("flag_seen", MailIndexField.FLAG_SEEN, "param16"),
    FLAG_USER("flag_user", MailIndexField.FLAG_USER, "param17"),
    FLAG_SPAM("flag_spam", MailIndexField.FLAG_SPAM, "param18"),
    FLAG_FORWARDED("flag_forwarded", MailIndexField.FLAG_FORWARDED, "param19"),
    FLAG_READ_ACK("flag_read_ack", MailIndexField.FLAG_READ_ACK, "param20"),
    USER_FLAGS("user_flags", MailIndexField.USER_FLAGS, "param21"),
    FROM("from", MailIndexField.FROM, "param22"),
    TO("to", MailIndexField.TO, "param23"),
    CC("cc", MailIndexField.CC, "param24"),
    BCC("bcc", MailIndexField.BCC, "param25"),
    SUBJECT("subject", MailIndexField.SUBJECT, "param26"),
    CONTENT_FLAG("content_flag", MailIndexField.CONTENT_FLAG, "param27"),
    CONTENT("content", MailIndexField.CONTENT, "param28");


    private static final Set<MailIndexField> indexedFields;

    private final String solrName;

    private final MailIndexField indexField;

    private final String customParameter;

    static {
        final Set<MailIndexField> set = EnumSet.noneOf(MailIndexField.class);
        for (final SolrMailField field : values()) {
            if (field.solrName() != null) {
                set.add(field.indexField);
            }
        }
        indexedFields = Collections.unmodifiableSet(set);
    }

    private SolrMailField(final String solrName, final MailIndexField indexField, final String customParameter) {
        this.solrName = solrName;
        this.indexField = indexField;
        this.customParameter = customParameter;
    }

    @Override
    public String solrName() {
        return solrName;
    }

    @Override
    public String parameterName() {
        return customParameter;
    }

    @Override
    public MailIndexField indexField() {
        return indexField;
    }

    public boolean isIndexed() {
        return !StringUtils.isEmpty(solrName);
    }

    /**
     * Gets the appropriate index field for specified Solr name.
     *
     * @param solrName The Solr name
     * @return The index field or <code>null</code>
     */
    public static MailIndexField fieldFor(final String solrName) {
        if (null == solrName) {
            return null;
        }
        for (final SolrMailField field : values()) {
            if (solrName.equals(field.solrName)) {
                return field.indexField;
            }
        }
        return null;
    }


    public static Set<MailIndexField> getIndexedFields() {
        return indexedFields;
    }

    public static String[] solrNamesFor(final Set<SolrMailField> fields) {
        final List<String> names = new ArrayList<String>();
        for (final SolrMailField field : fields) {
            final String solrName = field.solrName();
            if (solrName != null) {
                names.add(solrName);
            }
        }
        return names.toArray(new String[names.size()]);
    }

    public Object getValueFromMail(final MailMessage mail) {
        switch (this) {
            case COLOR_LABEL:
                return mail.getColorLabel();

            case FLAG_ANSWERED:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case FLAG_DELETED:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case FLAG_DRAFT:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case FLAG_FLAGGED:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case FLAG_RECENT:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case FLAG_SEEN:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case FLAG_USER:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case FLAG_SPAM:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case FLAG_FORWARDED:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case FLAG_READ_ACK:
                return Boolean.valueOf((mail.getFlags() & MailMessage.FLAG_FORWARDED) > 0);

            case USER_FLAGS:
                final String[] userFlags = mail.getUserFlags();
                if (null != userFlags && userFlags.length > 0) {
                    return Arrays.asList(userFlags);
                } else {
                    return null;
                }

            default:
                return null;
        }
    }
}

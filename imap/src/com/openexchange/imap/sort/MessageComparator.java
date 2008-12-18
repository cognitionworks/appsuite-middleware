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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.imap.sort;

import java.text.Collator;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import com.openexchange.mail.MailSortField;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.mime.PlainTextAddress;

/**
 * {@link MessageComparator} - A {@link Comparator comparator} for
 * {@link Message messages}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
final class MessageComparator implements Comparator<Message> {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
            .getLog(MessageComparator.class);

    private static final String STR_EMPTY = "";

    private final boolean descendingDir;

    private static interface FieldComparer {

        public abstract int compareFields(final Message msg1, final Message msg2) throws MessagingException;
    }

    private static abstract class LocalizedFieldComparer implements FieldComparer {

        public final Locale locale;

        public final Collator collator;

        public LocalizedFieldComparer(final Locale locale) {
            super();
            this.locale = locale;
            collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
        }

    }

    private final FieldComparer fieldComparer;

    /**
     * Initializes a new {@link MessageComparator} sorting by header
     * <code>Date</code> (a.k.a. sent date).
     * 
     * @param descendingDirection <code>true</code> for descending order;
     *            otherwise <code>false</code>
     * @param locale The locale
     */
    public MessageComparator(final boolean descendingDirection, final Locale locale) {
        this(MailSortField.SENT_DATE, descendingDirection, locale);
    }

    /**
     * Initializes a new {@link MessageComparator}.
     * 
     * @param sortField The sort field
     * @param descendingDirection <code>true</code> for descending order;
     *            otherwise <code>false</code>
     * @param locale The locale
     */
    public MessageComparator(final MailSortField sortField, final boolean descendingDirection, final Locale locale) {
        super();
        this.descendingDir = descendingDirection;
        if (MailSortField.COLOR_LABEL.equals(sortField) && !MailProperties.getInstance().isUserFlagsEnabled()) {
            fieldComparer = DUMMY_COMPARER;
        } else {
            FieldComparer tmp = COMPARERS.get(sortField);
            if (null == tmp) {
                tmp = createFieldComparer(sortField, locale);
            }
            fieldComparer = tmp;
        }
    }

    static int compareAddrs(final Address[] addrs1, final Address[] addrs2, final Locale locale, final Collator collator) {
        if (isEmptyAddrArray(addrs1) && !isEmptyAddrArray(addrs2)) {
            return -1;
        } else if (!isEmptyAddrArray(addrs1) && isEmptyAddrArray(addrs2)) {
            return 1;
        } else if (isEmptyAddrArray(addrs1) && isEmptyAddrArray(addrs2)) {
            return 0;
        }
        return collator.compare(getCompareStringFromAddress(addrs1[0], locale), getCompareStringFromAddress(addrs2[0],
                locale));
    }

    private static boolean isEmptyAddrArray(final Address[] addrs) {
        return ((addrs == null) || (addrs.length == 0));
    }

    private static String getCompareStringFromAddress(final Address addr, final Locale locale) {
        if (addr instanceof PlainTextAddress) {
            final PlainTextAddress da1 = (PlainTextAddress) addr;
            return da1.getAddress().toLowerCase(Locale.ENGLISH);
        } else if (addr instanceof InternetAddress) {
            final InternetAddress ia1 = (InternetAddress) addr;
            final String personal = ia1.getPersonal();
            if ((personal != null) && (personal.length() > 0)) {
                /*
                 * Personal is present. Skip leading quotes.
                 */
                return (personal.charAt(0) == '\'') || (personal.charAt(0) == '"') ? personal.substring(1).toLowerCase(
                        locale) : personal.toLowerCase(locale);
            }
            return ia1.getAddress().toLowerCase(Locale.ENGLISH);
        } else {
            return STR_EMPTY;
        }
    }

    static Integer compareReferences(final Object o1, final Object o2) {
        if ((o1 == null) && (o2 != null)) {
            return Integer.valueOf(-1);
        } else if ((o1 != null) && (o2 == null)) {
            return Integer.valueOf(1);
        } else if ((o1 == null) && (o2 == null)) {
            return Integer.valueOf(0);
        }
        /*
         * Both references are not null
         */
        return null;
    }

    private static final EnumMap<MailSortField, FieldComparer> COMPARERS;

    static {
        COMPARERS = new EnumMap<MailSortField, FieldComparer>(MailSortField.class);
        COMPARERS.put(MailSortField.SENT_DATE, new FieldComparer() {
            public int compareFields(final Message msg1, final Message msg2) throws MessagingException {
                final Date d1 = msg1.getSentDate();
                final Date d2 = msg2.getSentDate();
                final Integer refComp = compareReferences(d1, d2);
                return refComp == null ? d1.compareTo(d2) : refComp.intValue();
            }
        });
        COMPARERS.put(MailSortField.RECEIVED_DATE, new FieldComparer() {
            public int compareFields(final Message msg1, final Message msg2) throws MessagingException {
                final Date d1 = msg1.getReceivedDate();
                final Date d2 = msg2.getReceivedDate();
                final Integer refComp = compareReferences(d1, d2);
                return refComp == null ? d1.compareTo(d2) : refComp.intValue();
            }
        });
        COMPARERS.put(MailSortField.FLAG_SEEN, new FieldComparer() {
            public int compareFields(final Message msg1, final Message msg2) throws MessagingException {
                final boolean isSeen1 = msg1.isSet(Flags.Flag.SEEN);
                final boolean isSeen2 = msg2.isSet(Flags.Flag.SEEN);
                if (isSeen1 && isSeen2) {
                    return 0;
                } else if (!isSeen1 && !isSeen2) {
                    final boolean isRecent1 = msg1.isSet(Flags.Flag.RECENT);
                    final boolean isRecent2 = msg2.isSet(Flags.Flag.RECENT);
                    if ((isRecent1 && isRecent2) || (!isRecent1 && !isRecent2)) {
                        return 0;
                    } else if (isRecent1 && !isRecent2) {
                        return 1;
                    } else if (!isRecent1 && isRecent2) {
                        return -1;
                    }
                } else if (isSeen1 && !isSeen2) {
                    return 1;
                } else if (!isSeen1 && isSeen2) {
                    return -1;
                }
                return 0;
            }
        });
        COMPARERS.put(MailSortField.SIZE, new FieldComparer() {
            public int compareFields(final Message msg1, final Message msg2) throws MessagingException {
                return Integer.valueOf(msg1.getSize()).compareTo(Integer.valueOf(msg2.getSize()));
            }
        });
        COMPARERS.put(MailSortField.COLOR_LABEL, new FieldComparer() {
            public int compareFields(final Message msg1, final Message msg2) throws MessagingException {
                final Integer cl1 = getColorFlag(msg1.getFlags().getUserFlags());
                final Integer cl2 = getColorFlag(msg2.getFlags().getUserFlags());
                return cl1.compareTo(cl2);
            }
        });
    }

    private static FieldComparer DUMMY_COMPARER = new FieldComparer() {
        public int compareFields(final Message msg1, final Message msg2) {
            return 0;
        }
    };

    private static FieldComparer createFieldComparer(final MailSortField sortCol, final Locale locale) {
        switch (sortCol) {
        case FROM:
            return new LocalizedFieldComparer(locale) {
                public int compareFields(final Message msg1, final Message msg2) throws MessagingException {
                    return compareAddrs(msg1.getFrom(), msg2.getFrom(), this.locale, collator);
                }
            };
        case TO:
            return new LocalizedFieldComparer(locale) {
                public int compareFields(final Message msg1, final Message msg2) throws MessagingException {
                    return compareAddrs(msg1.getRecipients(Message.RecipientType.TO), msg2
                            .getRecipients(Message.RecipientType.TO), this.locale, collator);
                }
            };
        case CC:
            return new LocalizedFieldComparer(locale) {
                public int compareFields(final Message msg1, final Message msg2) throws MessagingException {
                    return compareAddrs(msg1.getRecipients(Message.RecipientType.CC), msg2
                            .getRecipients(Message.RecipientType.CC), this.locale, collator);
                }
            };
        case SUBJECT:
            return new LocalizedFieldComparer(locale) {
                public int compareFields(final Message msg1, final Message msg2) throws MessagingException {
                    final String sub1 = msg1.getSubject() == null ? STR_EMPTY : msg1.getSubject();
                    final String sub2 = msg2.getSubject() == null ? STR_EMPTY : msg2.getSubject();
                    return collator.compare(sub1, sub2);
                }
            };
        default:
            throw new UnsupportedOperationException("Unknown sort column value " + sortCol);
        }
    }

    private static final Integer COLOR_FLAG_MIN = Integer.valueOf(-99);

    static Integer getColorFlag(final String[] userFlags) {
        for (int i = 0; i < userFlags.length; i++) {
            final String userFlag = userFlags[i];
            if (MailMessage.isColorLabel(userFlag)) {
                // A color flag; parse its integer value
                final int cf = MailMessage.parseColorLabel(userFlag, COLOR_FLAG_MIN.intValue());
                return MailMessage.COLOR_LABEL_NONE == cf ? COLOR_FLAG_MIN : Integer.valueOf(cf * -1);
            }
        }
        return COLOR_FLAG_MIN;
    }

    public int compare(final Message msg1, final Message msg2) {
        try {
            int comparedTo = fieldComparer.compareFields(msg1, msg2);
            if (descendingDir) {
                comparedTo *= (-1);
            }
            return comparedTo;
        } catch (final MessagingException e) {
            LOG.error(e.getMessage(), e);
            return 0;
        }
    }

}

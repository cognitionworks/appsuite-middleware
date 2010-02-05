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

package com.openexchange.messaging.facebook.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.IFacebookRestClient;
import com.google.code.facebookapi.schema.FqlQueryResponse;
import com.openexchange.messaging.MessagingContent;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.MessagingExceptionCodes;
import com.openexchange.messaging.MessagingField;
import com.openexchange.messaging.MessagingHeader;
import com.openexchange.messaging.MessagingMessage;
import com.openexchange.messaging.MessagingPart;
import com.openexchange.messaging.OrderDirection;
import com.openexchange.messaging.facebook.FacebookMessagingException;
import com.openexchange.messaging.facebook.FacebookMessagingMessageAccess;
import com.openexchange.messaging.generic.internet.MimeAddressMessagingHeader;
import com.openexchange.messaging.generic.internet.MimeStringMessagingHeader;

/**
 * {@link FacebookMessagingUtility}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FacebookMessagingUtility {

    /**
     * Initializes a new {@link FacebookMessagingUtility}.
     */
    private FacebookMessagingUtility() {
        super();
    }

    public interface StaticFiller {

        void fill(FacebookMessagingMessage message) throws MessagingException;
    }

    private static final class AccountNameFiller implements StaticFiller {

        private static final String NAME = MessagingHeader.KnownHeader.ACCOUNT_NAME.toString();

        private final String accountName;

        public AccountNameFiller(final String accountName) {
            super();
            this.accountName = accountName;
        }

        public void fill(final FacebookMessagingMessage message) throws MessagingException {
            message.addHeader(new MimeStringMessagingHeader(NAME, accountName));
        }

    }

    private static final class ToFiller implements StaticFiller {

        private static final String TO = MessagingHeader.KnownHeader.TO.toString();

        private final long userId;

        private final String userName;

        public ToFiller(final long userId, final String userName) {
            super();
            this.userId = userId;
            this.userName = userName;
        }

        public void fill(final FacebookMessagingMessage message) throws MessagingException {
            /*
             * Add "To"
             */
            message.setHeader(MimeAddressMessagingHeader.valueOfPlain(TO, userName, String.valueOf(userId)));
        }
    }

    private interface QueryAdder {

        void add2Query(Set<String> fieldNames);

        String getOrderBy();

    }

    private static final Map<MessagingField, QueryAdder> FH;

    static {
        {
            final EnumMap<MessagingField, QueryAdder> m = new EnumMap<MessagingField, QueryAdder>(MessagingField.class);

            m.put(MessagingField.BODY, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    fieldNames.add("message");
                    fieldNames.add("attachment");
                }

                public String getOrderBy() {
                    return null;
                }
            });

            m.put(MessagingField.CONTENT_TYPE, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    /*
                     * We need attachment info to determine Content-Type
                     */
                    fieldNames.add("attachment");
                }

                public String getOrderBy() {
                    return null;
                }
            });

            m.put(MessagingField.FROM, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    fieldNames.add("actor_id");
                }

                public String getOrderBy() {
                    return null;
                }
            });

            m.put(MessagingField.FULL, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    fieldNames.add("post_id");
                    fieldNames.add("actor_id");
                    fieldNames.add("message");
                    fieldNames.add("updated_time");
                    fieldNames.add("created_time");
                    fieldNames.add("filter_key");
                    fieldNames.add("attachment");
                }

                public String getOrderBy() {
                    return null;
                }
            });

            m.put(MessagingField.HEADERS, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    fieldNames.add("post_id");
                    fieldNames.add("actor_id");
                    fieldNames.add("updated_time");
                    fieldNames.add("created_time");
                    fieldNames.add("filter_key");
                    /*
                     * We need attachment info to determine Content-Type
                     */
                    fieldNames.add("attachment");
                }

                public String getOrderBy() {
                    return null;
                }
            });

            m.put(MessagingField.ID, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    fieldNames.add("post_id");
                }

                public String getOrderBy() {
                    return "post_id";
                }
            });

            m.put(MessagingField.RECEIVED_DATE, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    fieldNames.add("updated_time");
                    fieldNames.add("created_time");
                }

                public String getOrderBy() {
                    return "created_time";
                }
            });

            m.put(MessagingField.SENT_DATE, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    fieldNames.add("updated_time");
                    fieldNames.add("created_time");
                }

                public String getOrderBy() {
                    return "created_time";
                }
            });

            m.put(MessagingField.SUBJECT, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    fieldNames.add("message");
                }

                public String getOrderBy() {
                    return "message";
                }
            });

            m.put(MessagingField.SIZE, new QueryAdder() {

                public void add2Query(final Set<String> fieldNames) {
                    fieldNames.add("message");
                }

                public String getOrderBy() {
                    return null;
                }
            });

            FH = Collections.unmodifiableMap(m);
        }
    }

    /**
     * Gets the query-able fields.
     * 
     * @return The query-able fields
     */
    public static EnumSet<MessagingField> getQueryableFields() {
        return EnumSet.copyOf(FH.keySet());
    }

    /**
     * Gets the static fillers for given fields.
     * 
     * @param fields The fields
     * @param access The messaging access
     * @return The fillers
     * @throws MessagingException If a messaging error occurs
     */
    public static List<StaticFiller> getStaticFillers(final MessagingField[] fields, final FacebookMessagingMessageAccess access) throws MessagingException {
        return getStaticFillers(EnumSet.copyOf(Arrays.asList(fields)), access);
    }

    /**
     * Gets the static fillers for given fields.
     * 
     * @param fields The fields
     * @param access The messaging access
     * @return The fillers
     * @throws MessagingException If a messaging error occurs
     */
    public static List<StaticFiller> getStaticFillers(final EnumSet<MessagingField> fieldSet, final FacebookMessagingMessageAccess access) throws MessagingException {
        final List<StaticFiller> ret = new ArrayList<StaticFiller>(fieldSet.size());
        if (fieldSet.contains(MessagingField.ACCOUNT_NAME) || fieldSet.contains(MessagingField.FULL)) {
            ret.add(new AccountNameFiller(access.getMessagingAccount().getDisplayName()));
        }
        if (fieldSet.contains(MessagingField.TO) || fieldSet.contains(MessagingField.FULL)) {
            ret.add(new ToFiller(access.getFacebookUserId(), access.getFacebookUserName()));
        }
        return ret;
    }

    /**
     * Checks specified message's content to be of given type.
     * 
     * @param message The message
     * @return The typed content
     * @throws MessagingException If message's content is of given type
     */
    public static <C extends MessagingContent> C checkContent(final Class<C> clazz, final MessagingMessage message) throws MessagingException {
        final MessagingContent content = message.getContent();
        if (!(clazz.isInstance(content))) {
            throw MessagingExceptionCodes.UNKNOWN_MESSAGING_CONTENT.create(content.toString());
        }
        return clazz.cast(content);
    }

    private static final long DEFAULT = -1L;

    private static final int RADIX = 10;

    /**
     * Parses as an unsigned <code>long</code>.
     * 
     * @param s The string to parse
     * @return An unsigned <code>long</code> or <code>-1</code>.
     */
    public static long parseUnsignedLong(final String s) {
        if (s == null) {
            return DEFAULT;
        }
        final int max = s.length();
        if (max <= 0) {
            return -1;
        }
        if (s.charAt(0) == '-') {
            return -1;
        }

        long result = 0;
        int i = 0;

        final long limit = -Long.MAX_VALUE;
        final long multmin = limit / RADIX;
        int digit;

        if (i < max) {
            digit = Character.digit(s.charAt(i++), RADIX);
            if (digit < 0) {
                return DEFAULT;
            }
            result = -digit;
        }
        while (i < max) {
            /*
             * Accumulating negatively avoids surprises near MAX_VALUE
             */
            digit = Character.digit(s.charAt(i++), RADIX);
            if (digit < 0) {
                return DEFAULT;
            }
            if (result < multmin) {
                return DEFAULT;
            }
            result *= RADIX;
            if (result < limit + digit) {
                return DEFAULT;
            }
            result -= digit;
        }
        return -result;
    }

    /**
     * Fires given FQL query using specified facebook REST client.
     * 
     * @param query The FQL query to fire
     * @param facebookRestClient The facebook REST client
     * @return The FQL query's results
     * @throws FacebookMessagingException If query cannot be fired
     */
    public static List<Object> fireFQLQuery(final CharSequence query, final IFacebookRestClient<Object> facebookRestClient) throws FacebookMessagingException {
        try {
            return ((FqlQueryResponse) facebookRestClient.fql_query(query)).getResults();
        } catch (final FacebookException e) {
            throw FacebookMessagingException.create(e);
        }
    }

    /**
     * A FQL query.
     */
    public static final class Query {

        private final CharSequence charSequence;

        private final boolean orderBy;

        /**
         * Initializes a new {@link Query}.
         * 
         * @param query The query character sequence
         * @param orderBy <code>true</code> if query contains <code>ORDER BY</code> clause; otherwise <code>false</code>
         */
        public Query(final CharSequence charSequence, final boolean orderBy) {
            super();
            this.charSequence = charSequence;
            this.orderBy = orderBy;
        }

        /**
         * Gets the query character sequence
         * 
         * @return The query character sequence
         */
        public CharSequence getCharSequence() {
            return charSequence;
        }

        /**
         * Checks if query contains <code>ORDER BY</code> clause
         * 
         * @return <code>true</code> if query contains <code>ORDER BY</code> clause; otherwise <code>false</code>
         */
        public boolean containsOrderBy() {
            return orderBy;
        }

    }

    /**
     * Composes the FQL stream query for given fields.
     * 
     * @param fields The fields
     * @param postIds The post identifiers
     * @param facebookUserId The facebook user identifier
     * @return The FQL stream query or <code>null</code> if fields require no query
     */
    public static Query composeFQLStreamQueryFor(final MessagingField[] fields, final String[] postIds, final long facebookUserId) {
        return composeFQLStreamQueryFor0(fields, null, null, postIds, facebookUserId);
    }

    /**
     * Composes the FQL stream query for given fields.
     * 
     * @param fields The fields
     * @param sortField The sort field; may be <code>null</code>
     * @param order The order direction
     * @param postIds The post identifiers
     * @param facebookUserId The facebook user identifier
     * @return The FQL stream query or <code>null</code> if fields require no query
     */
    public static Query composeFQLStreamQueryFor(final MessagingField[] fields, final MessagingField sortField, final OrderDirection order, final String[] postIds, final long facebookUserId) {
        return composeFQLStreamQueryFor0(fields, sortField, order, postIds, facebookUserId);
    }

    /**
     * Composes the FQL stream query for given fields.
     * 
     * @param fields The fields
     * @param facebookUserId The facebook user identifier
     * @return The FQL stream query or <code>null</code> if fields require no query
     */
    public static Query composeFQLStreamQueryFor(final MessagingField[] fields, final long facebookUserId) {
        return composeFQLStreamQueryFor0(fields, null, null, null, facebookUserId);
    }

    /**
     * Composes the FQL stream query for given fields.
     * 
     * @param fields The fields
     * @param sortField The sort field; may be <code>null</code>
     * @param order The order direction
     * @param facebookUserId The facebook user identifier
     * @return The FQL stream query or <code>null</code> if fields require no query
     */
    public static Query composeFQLStreamQueryFor(final MessagingField[] fields, final MessagingField sortField, final OrderDirection order, final long facebookUserId) {
        return composeFQLStreamQueryFor0(fields, sortField, order, null, facebookUserId);
    }

    private static Query composeFQLStreamQueryFor0(final MessagingField[] fields, final MessagingField sortField, final OrderDirection order, final String[] postIds, final long facebookUserId) {
        final Set<String> fieldNames = new HashSet<String>(fields.length);
        for (int i = 0; i < fields.length; i++) {
            final QueryAdder queryAdder = FH.get(fields[i]);
            if (null != queryAdder) {
                queryAdder.add2Query(fieldNames);
            }
        }
        if (fieldNames.isEmpty()) {
            return null;
        }
        final int size = fieldNames.size();
        final StringBuilder query = new StringBuilder(size << 5).append("SELECT ");
        final Iterator<String> iter = fieldNames.iterator();
        query.append(iter.next());
        for (int i = 1; i < size; i++) {
            query.append(", ").append(iter.next());
        }
        query.append(" FROM stream WHERE source_id = ").append(facebookUserId);
        if (null != postIds && 0 < postIds.length) {
            if (1 == postIds.length) {
                query.append(" AND post_id = '").append(postIds[0]).append('\'');
            } else {
                query.append(" AND post_id IN ");
                query.append('(');
                {
                    query.append('\'').append(postIds[0]).append('\'');
                    for (int i = 1; i < postIds.length; i++) {
                        query.append(',').append('\'').append(postIds[i]).append('\'');
                    }
                }
                query.append(')');
            }
        }
        /*
         * Check sort field
         */
        if (null == sortField) {
            return new Query(query, false);
        }
        boolean containsOrderBy = false;
        final QueryAdder adder = FH.get(sortField);
        if (null != adder) {
            final String orderBy = adder.getOrderBy();
            if (null != orderBy) {
                query.append(" ORDER BY ").append(orderBy).append(OrderDirection.DESC.equals(order) ? " DESC" : " ASC");
                containsOrderBy = true;
            }
        }
        return new Query(query, containsOrderBy);
    }

    /**
     * Abbreviates a text using ellipses. This will turn "Now is the time for all good men" into "Now is the time for..."
     * 
     * @param text The text to check, may be null
     * @param maxWidth The maximum length of result String, must be at least 4
     * @return The abbreviated text or <code>null</code>
     * @throws IllegalArgumentException if the width is too small
     */
    public static String abbreviate(final String text, final int maxWidth) {
        return abbreviate(text, 0, maxWidth);
    }

    /**
     * Abbreviates a text using ellipses. This will turn "Now is the time for all good men" into "...is the time for..."
     * 
     * @param text The text to check, may be null
     * @param offset The left edge of source String
     * @param maxWidth The maximum length of result String, must be at least 4
     * @return The abbreviated text or <code>null</code>
     * @throws IllegalArgumentException if the width is too small
     */
    public static String abbreviate(final String text, final int offset, final int maxWidth) {
        if (text == null) {
            return null;
        }
        if (maxWidth < 4) {
            throw new IllegalArgumentException("Minimum abbreviation width is 4");
        }
        if (text.length() <= maxWidth) {
            return text;
        }
        int off = offset;
        if (off > text.length()) {
            off = text.length();
        }
        if ((text.length() - off) < (maxWidth - 3)) {
            off = text.length() - (maxWidth - 3);
        }
        if (off <= 4) {
            return new StringBuilder(text.substring(0, maxWidth - 3)).append("...").toString();
        }
        if (maxWidth < 7) {
            throw new IllegalArgumentException("Minimum abbreviation width with offset is 7");
        }
        if ((off + (maxWidth - 3)) < text.length()) {
            return new StringBuilder("...").append(abbreviate(text.substring(off), maxWidth - 3)).toString();
        }
        return new StringBuilder("...").append(text.substring(text.length() - (maxWidth - 3))).toString();
    }

    /**
     * Checks specified message's content to be of given type.
     * 
     * @param part The part
     * @return The typed content
     * @throws MessagingException If message's content is of given type
     */
    public static <C extends MessagingContent> C checkContent(final Class<C> clazz, final MessagingPart part) throws MessagingException {
        final MessagingContent content = part.getContent();
        if (!(clazz.isInstance(content))) {
            throw MessagingExceptionCodes.UNKNOWN_MESSAGING_CONTENT.create(content.toString());
        }
        return clazz.cast(content);
    }

}

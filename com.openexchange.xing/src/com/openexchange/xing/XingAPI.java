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

package com.openexchange.xing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.java.StringAllocator;
import com.openexchange.xing.RESTUtility.Method;
import com.openexchange.xing.exception.XingException;
import com.openexchange.xing.exception.XingUnlinkedException;
import com.openexchange.xing.session.Session;

/**
 * {@link XingAPI} - The XING API.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class XingAPI<S extends Session> {

    private static final int MAX_LIMIT = 100;

    private static final int DEFAULT_LIMIT = 10;

    private static final int MAX_WITH_LATEST_MESSAGES = 100;

    private static final int DEFAULT_WITH_LATEST_MESSAGES = 0;

    /**
     * The version of the API that this code uses.
     */
    public static final int VERSION = 1;

    /** The session */
    private final S session;

    /**
     * Initializes a new {@link XingAPI}.
     * 
     * @param session The associated session
     */
    public XingAPI(final S session) {
        super();
        this.session = session;
    }

    /**
     * Throws a {@link XingUnlinkedException} if the session in this instance is not linked.
     */
    protected void assertAuthenticated() throws XingUnlinkedException {
        if (!session.isLinked()) {
            throw new XingUnlinkedException();
        }
    }

    /**
     * Returns the {@link User} associated with the current {@link Session}.
     * 
     * @return the current session's {@link User}.
     * @throws XingUnlinkedException If you have not set an access token pair on the session, or if the user has revoked access.
     * @throws XingServerException If the server responds with an error code. See the constants in {@link XingServerException} for the
     *             meaning of each error code.
     * @throws XingIOException If any network-related error occurs.
     * @throws XingException For any other unknown errors. This is also a superclass of all other XING exceptions, so you may want to only
     *             catch this exception which signals that some kind of error occurred.
     */
    public User userInfo() throws XingException {
        assertAuthenticated();
        try {
            final JSONObject responseInformation = (JSONObject) RESTUtility.request(
                Method.GET,
                session.getAPIServer(),
                "/users/me",
                VERSION,
                session);
            return new User(responseInformation.getJSONArray("users").getJSONObject(0));
        } catch (final JSONException e) {
            throw new XingException(e);
        } catch (final RuntimeException e) {
            throw new XingException(e);
        }
    }

    /**
     * Returns the {@link User} associated with given user identifier.
     * 
     * @param userId The user identifier
     * @return The specified user.
     * @throws XingUnlinkedException If you have not set an access token pair on the session, or if the user has revoked access.
     * @throws XingServerException If the server responds with an error code. See the constants in {@link XingServerException} for the
     *             meaning of each error code.
     * @throws XingIOException If any network-related error occurs.
     * @throws XingException For any other unknown errors. This is also a superclass of all other XING exceptions, so you may want to only
     *             catch this exception which signals that some kind of error occurred.
     */
    public User userInfo(final String userId) throws XingException {
        assertAuthenticated();
        try {
            final JSONObject responseInformation = (JSONObject) RESTUtility.request(
                Method.GET,
                session.getAPIServer(),
                "/users/" + userId,
                VERSION,
                session);
            return new User(responseInformation.getJSONArray("users").getJSONObject(0));
        } catch (final JSONException e) {
            throw new XingException(e);
        } catch (final RuntimeException e) {
            throw new XingException(e);
        }
    }

    private static final Set<UserField> SUPPORTED_SORT_FIELDS = EnumSet.of(UserField.ID, UserField.LAST_NAME);

    /**
     * Gets the requested user's contacts.
     * 
     * @param userId The user identifier
     * @param limit The number of contacts to be returned. Must be zero or a positive number. Default: <code>10</code>, Maximum:
     *            <code>100</code>. If its value is equal to zero, default limit is passed to request
     * @param offset The offset. Must be zero or a positive number. Default: <code>0</code>
     * @param orderBy Determines the ascending order of the returned list. Currently only supports <code>"last_name"</code>. Defaults to
     *            <code>"id"</code>
     * @param userFields List of user attributes to return. If this parameter is not used, only the ID will be returned.
     * @return The user's contacts
     * @throws XingUnlinkedException If you have not set an access token pair on the session, or if the user has revoked access.
     * @throws XingServerException If the server responds with an error code. See the constants in {@link XingServerException} for the
     *             meaning of each error code.
     * @throws XingIOException If any network-related error occurs.
     * @throws XingException For any other unknown errors. This is also a superclass of all other XING exceptions, so you may want to only
     *             catch this exception which signals that some kind of error occurred.
     */
    public Contacts getContactsFrom(final String userId, final int limit, final int offset, final UserField orderBy, final Collection<UserField> userFields) throws XingException {
        if (limit < 0 || limit > 100) {
            throw new XingException("Invalid limit: " + limit + ". Must be zero OR less than or equal to 100.");
        }
        if (offset < 0) {
            throw new XingException("Invalid offset: " + offset + ". Must be greater than or equal to zero.");
        }
        assertAuthenticated();
        try {
            // Add parameters limit & offset
            final List<String> params = new ArrayList<String>(Arrays.asList(
                "limit",
                Integer.toString(limit == 0 ? DEFAULT_LIMIT : limit),
                "offset",
                Integer.toString(offset)));
            // Add order-by
            final boolean serverSort = (null == orderBy || SUPPORTED_SORT_FIELDS.contains(orderBy));
            if (serverSort) {
                if (null != orderBy) {
                    params.add("orderBy");
                    params.add(orderBy.getFieldName());
                }
            }
            // Add user fields
            if (null != userFields && !userFields.isEmpty()) {
                params.add("user_fields");
                final Iterator<UserField> iter = userFields.iterator();
                final StringAllocator fields = new StringAllocator(userFields.size() << 4);
                fields.append(iter.next().getFieldName());
                while (iter.hasNext()) {
                    fields.append(',').append(iter.next().getFieldName());
                }
                params.add(fields.toString());
            }

            final JSONObject responseInformation = (JSONObject) RESTUtility.request(
                Method.GET,
                session.getAPIServer(),
                "/users/" + userId + "/contacts",
                VERSION,
                params.toArray(new String[0]),
                session);

            if (serverSort) {
                return new Contacts(responseInformation.getJSONObject("contacts"));
            }
            // Manually sort contacts
            final Contacts contacts = new Contacts(responseInformation.getJSONObject("contacts"));
            Collections.sort(contacts.getUsers(), (null == orderBy ? UserField.ID : orderBy).getComparator(false));
            return contacts;
        } catch (final JSONException e) {
            throw new XingException(e);
        } catch (final RuntimeException e) {
            throw new XingException(e);
        }
    }

    /**
     * Gets all of the requested user's contacts.
     * 
     * @param userId The user identifier
     * @param orderBy Determines the ascending order of the returned list. Currently only supports <code>"last_name"</code>. Defaults to
     *            <code>"id"</code>
     * @param userFields List of user attributes to return. If this parameter is not used, only the ID will be returned.
     * @return The user's contacts
     * @throws XingUnlinkedException If you have not set an access token pair on the session, or if the user has revoked access.
     * @throws XingServerException If the server responds with an error code. See the constants in {@link XingServerException} for the
     *             meaning of each error code.
     * @throws XingIOException If any network-related error occurs.
     * @throws XingException For any other unknown errors. This is also a superclass of all other XING exceptions, so you may want to only
     *             catch this exception which signals that some kind of error occurred.
     */
    public Contacts getContactsFrom(final String userId, final UserField orderBy, final Collection<UserField> userFields) throws XingException {
        assertAuthenticated();
        try {
            final List<User> users;
            final int maxLimit = MAX_LIMIT;
            final int total;
            int offset = 0;
            // Request first chunk to determine total number of contacts
            {
                final Contacts contacts = getContactsFrom(userId, maxLimit, offset, null, userFields);
                final List<User> chunk = contacts.getUsers();
                final int chunkSize = chunk.size();
                if (chunkSize < maxLimit) {
                    // Obtained less than requested; no more contacts available then
                    return contacts;
                }
                total = contacts.getTotal();
                users = new ArrayList<User>(total);
                users.addAll(chunk);
                offset += chunkSize;
            }
            // Request remaining chunks
            while (offset < total) {
                final int remain = total - offset;
                final List<User> chunk = getContactsFrom(userId, remain > maxLimit ? maxLimit : remain, offset, null, userFields).getUsers();
                users.addAll(chunk);
                offset += chunk.size();
            }
            // Sort users
            Collections.sort(users, (null == orderBy ? UserField.ID : orderBy).getComparator(false));
            return new Contacts(total, users);
        } catch (final RuntimeException e) {
            throw new XingException(e);
        }
    }

    /**
     * Gets the conversations for specified user.
     * 
     * @param userId The user identifier
     * @param limit The number of conversations to be returned. Must be zero or a positive number. Default: <code>10</code>, Maximum:
     *            <code>100</code>. If its value is equal to zero, default limit is passed to request
     * @param offset The offset. Must be zero or a positive number. Default: <code>0</code>
     * @param userFields List of user attributes to return. If this parameter is not used, only the ID will be returned.
     * @param withLatestMessages The number of latest messages to be returned. Must be zero or a positive number. Default: <code>0</code>,
     *            Maximum: <code>100</code>. If its value is equal to zero, default limit is passed to request
     * @return The user's conversations
     * @throws XingException
     */
    public Conversations getConversationsFrom(final String userId, final int limit, final int offset, final Collection<UserField> userFields, final int withLatestMessages) throws XingException {
        if (limit < 0 || limit > 100) {
            throw new XingException("Invalid limit: " + limit + ". Must be zero OR less than or equal to 100.");
        }
        if (offset < 0) {
            throw new XingException("Invalid offset: " + offset + ". Must be greater than or equal to zero.");
        }
        if (withLatestMessages < 0 || withLatestMessages > 100) {
            throw new XingException("Invalid withLatestMessages: " + withLatestMessages + ". Must be zero OR less than or equal to 100.");
        }
        assertAuthenticated();
        try {
            // Add parameters limit & offset
            final List<String> params = new ArrayList<String>(Arrays.asList(
                "limit",
                Integer.toString(limit == 0 ? DEFAULT_LIMIT : limit),
                "offset",
                Integer.toString(offset),
                "with_latest_messages",
                Integer.toString(withLatestMessages == 0 ? DEFAULT_WITH_LATEST_MESSAGES : withLatestMessages)));
            // Add user fields
            if (null != userFields && !userFields.isEmpty()) {
                params.add("user_fields");
                final Iterator<UserField> iter = userFields.iterator();
                final StringAllocator fields = new StringAllocator(userFields.size() << 4);
                fields.append(iter.next().getFieldName());
                while (iter.hasNext()) {
                    fields.append(',').append(iter.next().getFieldName());
                }
                params.add(fields.toString());
            }

            final JSONObject responseInformation = (JSONObject) RESTUtility.request(
                Method.GET,
                session.getAPIServer(),
                "/users/" + userId + "/conversations",
                VERSION,
                params.toArray(new String[0]),
                session);
            return new Conversations(responseInformation.getJSONObject("conversations"));
        } catch (final JSONException e) {
            throw new XingException(e);
        } catch (final RuntimeException e) {
            throw new XingException(e);
        }
    }

    /**
     * Gets all of the requested user's conversations.
     * 
     * @param userId The user identifier
     * @param userFields List of user attributes to return. If this parameter is not used, only the ID will be returned.
     * @param withLatestMessages The number of latest messages to be returned. Must be zero or a positive number. Default: <code>0</code>,
     *            Maximum: <code>100</code>. If its value is equal to zero, default limit is passed to request
     * @return The user's conversations
     * @return The user's conversations
     * @throws XingUnlinkedException If you have not set an access token pair on the session, or if the user has revoked access.
     * @throws XingServerException If the server responds with an error code. See the constants in {@link XingServerException} for the
     *             meaning of each error code.
     * @throws XingIOException If any network-related error occurs.
     * @throws XingException For any other unknown errors. This is also a superclass of all other XING exceptions, so you may want to only
     *             catch this exception which signals that some kind of error occurred.
     */
    public Conversations getConversationsFrom(final String userId, final Collection<UserField> userFields, final int withLatestMessages) throws XingException {
        assertAuthenticated();
        try {
            final List<Conversation> items = new LinkedList<Conversation>();
            final int maxLimit = MAX_LIMIT;
            final int total;
            int offset = 0;
            // Request first chunk to determine total number of conversations
            {
                final Conversations conversations = getConversationsFrom(userId, maxLimit, offset, userFields, withLatestMessages);
                final List<Conversation> chunk = conversations.getItems();
                final int chunkSize = chunk.size();
                if (chunkSize < maxLimit) {
                    // Obtained less than requested; no more conversations available then
                    return conversations;
                }
                total = conversations.getTotal();
                items.addAll(chunk);
                offset += chunkSize;
            }
            // Request remaining chunks
            while (offset < total) {
                final int remain = total - offset;
                final List<Conversation> chunk = getConversationsFrom(
                    userId,
                    remain > maxLimit ? maxLimit : remain,
                    offset,
                    userFields,
                    withLatestMessages).getItems();
                items.addAll(chunk);
                offset += chunk.size();
            }
            return new Conversations(total, items);
        } catch (final RuntimeException e) {
            throw new XingException(e);
        }
    }

    /**
     * Gets the denoted conversation for specified user.
     * 
     * @param id The conversation identifier
     * @param userId The user identifier
     * @param userFields List of user attributes to return. If this parameter is not used, only the ID will be returned.
     * @param withLatestMessages The number of latest messages to be returned. Must be zero or a positive number. Default: <code>0</code>,
     *            Maximum: <code>100</code>. If its value is equal to zero, default limit is passed to request
     * @return The conversation
     * @throws XingException
     */
    public Conversation getConversationFrom(final String id, final String userId, final Collection<UserField> userFields, final int withLatestMessages) throws XingException {
        if (withLatestMessages < 0 || withLatestMessages > 100) {
            throw new XingException("Invalid withLatestMessages: " + withLatestMessages + ". Must be zero OR less than or equal to 100.");
        }
        assertAuthenticated();
        try {
            // Add parameters limit & offset
            final List<String> params = new ArrayList<String>(Arrays.asList(
                "with_latest_messages",
                Integer.toString(withLatestMessages == 0 ? DEFAULT_WITH_LATEST_MESSAGES : withLatestMessages)));
            // Add user fields
            if (null != userFields && !userFields.isEmpty()) {
                params.add("user_fields");
                final Iterator<UserField> iter = userFields.iterator();
                final StringAllocator fields = new StringAllocator(userFields.size() << 4);
                fields.append(iter.next().getFieldName());
                while (iter.hasNext()) {
                    fields.append(',').append(iter.next().getFieldName());
                }
                params.add(fields.toString());
            }

            final JSONObject responseInformation = (JSONObject) RESTUtility.request(
                Method.GET,
                session.getAPIServer(),
                "/users/" + userId + "/conversations/" + id,
                VERSION,
                params.toArray(new String[0]),
                session);
            return new Conversation(responseInformation.getJSONObject("conversation"));
        } catch (final JSONException e) {
            throw new XingException(e);
        } catch (final RuntimeException e) {
            throw new XingException(e);
        }
    }

    /**
     * Gets the associated session.
     */
    public S getSession() {
        return session;
    }

}

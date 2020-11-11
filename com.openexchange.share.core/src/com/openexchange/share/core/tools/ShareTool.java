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
 *    trademarks of the OX Software GmbH group of companies.
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

package com.openexchange.share.core.tools;

import static com.openexchange.share.core.ShareConstants.SHARE_SERVLET;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.ldap.UserImpl;
import com.openexchange.java.Enums;
import com.openexchange.java.Strings;
import com.openexchange.share.AuthenticationMode;
import com.openexchange.share.ShareExceptionCodes;
import com.openexchange.share.ShareInfo;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.ShareTargetPath;
import com.openexchange.share.recipient.AnonymousRecipient;
import com.openexchange.share.recipient.GuestRecipient;
import com.openexchange.share.recipient.InternalRecipient;
import com.openexchange.share.recipient.RecipientType;
import com.openexchange.share.recipient.ShareRecipient;
import com.openexchange.user.User;

/**
 * {@link ShareTool}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class ShareTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareTool.class);

    /**
     * The user attribute key for a link target
     */
    public static final String LINK_TARGET_USER_ATTRIBUTE = "com.openexchange.share.LinkTarget";

    /**
     * The user attribute key for the expiry date of a link target
     */
    public static final String EXPIRY_DATE_USER_ATTRIBUTE = "com.openexchange.share.expiryDate";

    /**
     * Extracts the first value of a specific attribute from a user.
     *
     * @param user The user to get the attribute value for
     * @param name The name of the attribute to get
     * @return The first found attribute value, or <code>null</code> if not found
     */
    public static String getUserAttribute(User user, String name) {
        Map<String, String> attributes = user.getAttributes();
        return attributes == null ? null : attributes.get(name);
    }

    /**
     * Adds an attribute to the given user while preserving existing ones.
     *
     * @param user The user impl
     * @param name The attribute name
     * @param value The attribute value
     */
    public static void assignUserAttribute(UserImpl user, String name, String value) {
        Map<String, String> existingAttributes = user.getAttributes();
        Map<String, String> attributes = null == existingAttributes ? new HashMap<String, String>() : new HashMap<String, String>(existingAttributes);
        attributes.put(name, value);
        user.setAttributes(attributes);
    }

    /**
     * Filters out all expired shares from the supplied list.
     *
     * @param shares The shares to filter
     * @return The expired shares that were removed from the supplied list, or <code>null</code> if no shares were expired
     */
    public static List<ShareInfo> filterExpiredShares(List<ShareInfo> shares) {
        List<ShareInfo> expiredShares = null;
        if (null != shares && 0 < shares.size()) {
            Iterator<ShareInfo> iterator = shares.iterator();
            while (iterator.hasNext()) {
                ShareInfo share = iterator.next();
                Date expiryDate = share.getGuest().getExpiryDate();
                if (null != expiryDate && expiryDate.before(new Date())) {
                    if (null == expiredShares) {
                        expiredShares = new ArrayList<ShareInfo>();
                    }
                    iterator.remove();
                    expiredShares.add(share);
                }
            }
        }
        return expiredShares;
    }

    /**
     * Converts the passed share target to a JSON representation, e.g.:
     * <code>
     * <pre>
     * {
     * "m":8,
     * "f":"247689",
     * "i":"247689/6592"
     * }
     * </pre>
     * </code>
     * 
     * @param target The target
     * @return The JSON object
     */
    public static JSONObject targetToJSON(ShareTarget target) throws OXException {
        try {
            JSONObject jTarget = new JSONObject();
            jTarget.put("m", target.getModule());
            jTarget.put("f", target.getFolder());
            jTarget.put("i", target.getItem());
            return jTarget;
        } catch (JSONException e) {
            throw ShareExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Parses a JSON target into a {@link ShareTarget} instance. JSON targets must be in the form of:
     * <code>
     * <pre>
     * {
     * "m":8,
     * "f":"247689",
     * "i":"247689/6592"
     * }
     * </pre>
     * </code>
     * 
     * @param jTarget The JSON object
     * @return The target
     * @throws JSONException
     */
    public static ShareTarget jsonToTarget(JSONObject jTarget) throws JSONException {
        int module = jTarget.getInt("m");
        String folder = jTarget.getString("f");
        String item = jTarget.optString("i", null);
        return new ShareTarget(module, folder, item);
    }

    /**
     * Gets all identifiers specified in the supplied shares.
     *
     * @param shares The shares to get the guest users for
     * @return The guest user identifiers in a set
     */
    public static Set<Integer> getGuestIDs(List<ShareInfo> shares) {
        if (null == shares || 0 == shares.size()) {
            return Collections.emptySet();
        }
        Set<Integer> guestIDs = new HashSet<Integer>();
        for (ShareInfo share : shares) {
            guestIDs.add(Integer.valueOf(share.getGuest().getGuestID()));
        }
        return guestIDs;
    }

    /**
     * Maps each different target of the supplied shares to one or more referenced guest user identifiers.
     *
     * @param shares The shares to perform the mapping for
     * @return All different share targets, mapped to the referenced guest user identifiers.
     */
    public static Map<ShareTarget, Set<Integer>> mapGuestsByTarget(List<ShareInfo> shares) {
        if (null == shares || 0 == shares.size()) {
            return Collections.emptyMap();
        }
        Map<ShareTarget, Set<Integer>> guestsByTarget = new HashMap<ShareTarget, Set<Integer>>();
        for (ShareInfo share : shares) {
            Set<Integer> guestIDs = guestsByTarget.get(share.getTarget());
            if (null == guestIDs) {
                guestIDs = new HashSet<Integer>();
                guestsByTarget.put(share.getTarget(), guestIDs);
            }
            guestIDs.add(Integer.valueOf(share.getGuest().getGuestID()));
        }
        return guestsByTarget;
    }

    /**
     * Checks a share target for validity before saving or updating it, throwing an exception if validation fails.
     *
     * @param target The target to validate
     * @throws OXException
     */
    public static void validateTarget(ShareTarget target) throws OXException {
        if (null == target.getItem() && null == target.getFolder()) {
            throw ShareExceptionCodes.UNEXPECTED_ERROR.create("No folder or item specified in share target");
        }
    }

    /**
     * Checks a share target for validity before saving or updating it, throwing an exception if validation fails.
     *
     * @param targets The targets to validate
     * @throws OXException
     */
    public static void validateTargets(Collection<ShareTarget> targets) throws OXException {
        for (ShareTarget target : targets) {
            validateTarget(target);
        }
    }

    /**
     * Gets the authentication mode applicable for the supplied guest user.
     *
     * @param guest The guest user
     * @return The authentication mode
     */
    public static AuthenticationMode getAuthenticationMode(User guest) {
        if (Strings.isEmpty(guest.getMail())) {
            if (guest.getUserPassword() == null) {
                return AuthenticationMode.ANONYMOUS;
            }
            return AuthenticationMode.ANONYMOUS_PASSWORD;
        }
        if (guest.getUserPassword() == null) {
            return AuthenticationMode.GUEST;
        }
        return AuthenticationMode.GUEST_PASSWORD;
    }

    /**
     * Gets the authentication mode applicable for the supplied share recipient.
     *
     * @param guest The guest user
     * @return The authentication mode, or <code>null</code> if the recipient does not denote guest authentication
     */
    public static AuthenticationMode getAuthenticationMode(ShareRecipient recipient) {
        switch (recipient.getType()) {
            case ANONYMOUS:
                return Strings.isEmpty(((AnonymousRecipient) recipient).getPassword()) ? AuthenticationMode.ANONYMOUS : AuthenticationMode.ANONYMOUS_PASSWORD;
            case GUEST:
                if (((GuestRecipient) recipient).getPassword() == null) {
                    return AuthenticationMode.GUEST;
                }
                return AuthenticationMode.GUEST_PASSWORD;
            default:
                return null;
        }
    }

    /**
     * Checks whether the passed user is a guest user and its authentication mode is either
     * {@link AuthenticationMode#ANONYMOUS} or {@link AuthenticationMode#ANONYMOUS_PASSWORD}.
     *
     * @param user The user to check
     * @return <code>true</code> if the user is an anonymous guest
     */
    public static boolean isAnonymousGuest(User user) {
        if (user.isGuest() && Strings.isEmpty(user.getMail())) {
            return true;
        }

        return false;
    }

    /**
     * Parses a share recipient from JSON, as used in the (object) permissions field sent by clients during file- and folder updates.
     *
     * @param jsonObject The JSON object to parse
     * @param timeZone The timezone to use, or <code>null</code> to not apply timezone offsets to parsed timestamps
     * @return The parsed share recipient
     */
    public static ShareRecipient parseRecipient(JSONObject jsonObject, TimeZone timeZone) throws OXException, JSONException {
        ShareRecipient recipient;
        RecipientType type = Enums.parse(RecipientType.class, jsonObject.optString("type"), null);
        if (RecipientType.ANONYMOUS == type) {
            /*
             * anonymous recipient for links
             */
            AnonymousRecipient anonymousRecipient = new AnonymousRecipient();
            anonymousRecipient.setPassword(jsonObject.optString("password", null));
            if (jsonObject.hasAndNotNull("expiry_date")) {
                long date = jsonObject.getLong("expiry_date");
                if (null != timeZone) {
                    date -= timeZone.getOffset(date);
                }
                anonymousRecipient.setExpiryDate(new Date(date));
            }
            recipient = anonymousRecipient;
        } else if (RecipientType.GUEST == type) {
            /*
             * guest recipient for invitations
             */
            GuestRecipient guestRecipient = new GuestRecipient();
            if (false == jsonObject.hasAndNotNull("email_address")) {
                throw OXException.mandatoryField("email_address");
            }
            guestRecipient.setEmailAddress(jsonObject.getString("email_address"));
            guestRecipient.setPassword(jsonObject.optString("password", null));
            guestRecipient.setDisplayName(jsonObject.optString("display_name", null));
            guestRecipient.setContactID(jsonObject.optString("contact_id", null));
            guestRecipient.setContactFolder(jsonObject.optString("contact_folder", null));
            recipient = guestRecipient;
        } else {
            /*
             * internal recipient pointing to an existing user/group entity
             */
            InternalRecipient internalRecipient = new InternalRecipient();
            if (false == jsonObject.has("entity")) {
                throw OXException.mandatoryField("entity");
            }
            internalRecipient.setEntity(jsonObject.getInt("entity"));
            boolean group;
            if (jsonObject.has("group")) {
                group = jsonObject.getBoolean("group");
            } else if (null != type) {
                group = RecipientType.GROUP == type;
            } else {
                throw OXException.mandatoryField("group");
            }
            internalRecipient.setGroup(group);
            recipient = internalRecipient;
        }
        if (false == jsonObject.hasAndNotNull("bits")) {
            throw OXException.mandatoryField("bits");
        }
        recipient.setBits(jsonObject.getInt("bits"));
        return recipient;
    }

    /**
     * Checks if the URL looks like an OX share link
     *
     * @param url The URL to check
     * @return <code>true</code> if an OX instance generated the string, <code>false</code> otherwise
     */
    public static boolean isShare(String url) {
        return null != getShareToken(url);
    }

    /**
     * Parses the given URL for a base token.
     *
     * @param url The URL
     * @return The token or <code>null</code> if not applicable
     */
    public static String getBaseToken(String url) {
        ShareToken shareToken = getShareToken(url);
        if (null == shareToken) {
            return null;
        }
        return shareToken.getToken();
    }

    /**
     * Get a share token from a path
     *
     * @param url The URL containing the share token
     * @return The token or <code>null</code> if not applicable
     */
    private static ShareToken getShareToken(String url) {
        String token = extractBaseToken(url);
        if (null == token) {
            return null;
        }
        try {
            ShareToken shareToken = new ShareToken(token);
            if (shareToken.getContextID() < 0 || shareToken.getUserID() < 0) {
                /*
                 * No context ID mean no OX share
                 */
                return null;
            }
            return shareToken;
        } catch (OXException e) {
            LOGGER.debug("Error while parsing: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extracts the share base token from a URL.
     *
     * @param url The path to extract the token from
     * @return The token or <code>null</code> if no token is embedded in the path
     */
    public static String extractBaseToken(String url) {
        if (Strings.isEmpty(url)) {
            return null;
        }
        String prefix = SHARE_SERVLET + '/';
        int beginIndex = url.lastIndexOf(prefix);
        if (-1 == beginIndex) {
            return null;
        }
        beginIndex += prefix.length();
        int endIndex = url.indexOf('/', beginIndex);
        return -1 == endIndex ? url.substring(beginIndex) : url.substring(beginIndex, endIndex);
    }

    /**
     * Extracts the folder identifier from a share link
     *
     * @param shareLink The share link
     * @return The {@link ShareTargetPath} or <code>null</code> if not applicable
     */
    public static ShareTargetPath getShareTarget(String shareLink) {
        String baseToken = extractBaseToken(shareLink);
        if (Strings.isEmpty(baseToken)) {
            return null;
        }
        int beginIndex = shareLink.indexOf(baseToken) + baseToken.length();
        if (shareLink.length() < beginIndex) {
            return null;
        }
        String targetPath = shareLink.substring(beginIndex);
        if (Strings.isEmpty(targetPath)) {
            return null;
        }
        return ShareTargetPath.parse(targetPath);
    }

}

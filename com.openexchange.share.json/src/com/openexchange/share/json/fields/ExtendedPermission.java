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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.share.json.fields;

import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.fields.ContactFields;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.tools.JSONCoercion;
import com.openexchange.group.Group;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.ldap.User;
import com.openexchange.share.ShareInfo;
import com.openexchange.share.core.DefaultRequestContext;

/**
 * {@link ExtendedPermission}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public abstract class ExtendedPermission {

    protected final PermissionResolver resolver;

    /**
     * Initializes a new {@link ExtendedPermission}.
     *
     * @param permissionResolver The permission resolver
     */
    protected ExtendedPermission(PermissionResolver permissionResolver) {
        super();
        this.resolver = permissionResolver;
    }

    protected void addGroupInfo(AJAXRequestData requestData, JSONObject jsonObject, Group group) throws JSONException {
        if (null != group) {
            jsonObject.put(ContactFields.DISPLAY_NAME, group.getDisplayName());
        }
    }

    protected void addUserInfo(AJAXRequestData requestData, JSONObject jsonObject, User user) throws JSONException {
        if (null != user) {
            Contact userContact = resolver.getUserContact(user.getId());
            if (null != userContact) {
                addContactInfo(requestData, jsonObject, userContact);
            } else {
                addContactInfo(requestData, jsonObject, user);
            }
        }
    }

    protected void addContactInfo(AJAXRequestData requestData, JSONObject jsonObject, Contact userContact) throws JSONException {
        if (null != userContact) {
            jsonObject.putOpt(ContactFields.DISPLAY_NAME, userContact.getDisplayName());
            JSONObject jsonContact = new JSONObject();
            jsonContact.putOpt(ContactFields.EMAIL1, userContact.getEmail1());
            jsonContact.putOpt(ContactFields.TITLE, userContact.getTitle());
            jsonContact.putOpt(ContactFields.LAST_NAME, userContact.getSurName());
            jsonContact.putOpt(ContactFields.FIRST_NAME, userContact.getGivenName());
            jsonContact.putOpt(ContactFields.IMAGE1_URL, resolver.getImageURL(userContact.getInternalUserId()));
            jsonObject.put("contact", jsonContact);
        }
    }

    protected void addContactInfo(AJAXRequestData requestData, JSONObject jsonObject, User user) throws JSONException {
        if (null != user) {
            jsonObject.putOpt(ContactFields.DISPLAY_NAME, user.getDisplayName());
            JSONObject jsonContact = new JSONObject();
            jsonContact.putOpt(ContactFields.EMAIL1, user.getMail());
            jsonContact.putOpt(ContactFields.LAST_NAME, user.getSurname());
            jsonContact.putOpt(ContactFields.FIRST_NAME, user.getGivenName());
            jsonContact.putOpt(ContactFields.IMAGE1_URL, resolver.getImageURL(user.getId()));
            jsonObject.put("contact", jsonContact);
        }
    }

    protected void addShareInfo(AJAXRequestData requestData, JSONObject jsonObject, ShareInfo share) throws JSONException {
        if (null != share) {
            if (null != requestData) {
                jsonObject.putOpt("share_url", share.getShareURL(DefaultRequestContext.newInstance(requestData)));
            }
            Date expiryDate = share.getShare().getExpiryDate();
            if (null != expiryDate) {
                long time = null != requestData ? addTimeZoneOffset(expiryDate.getTime(), getTimeZone(requestData)) : expiryDate.getTime();
                jsonObject.put("expiry_date", time);
            }
            Map<String, Object> meta = share.getShare().getMeta();
            if (null != meta) {
                jsonObject.put("meta", JSONCoercion.coerceToJSON(meta));
            }
            jsonObject.putOpt("password", share.getGuest().getPassword());
        }
    }

    private static long addTimeZoneOffset(long date, TimeZone timeZone) {
        return null == timeZone ? date : date + timeZone.getOffset(date);
    }

    private static TimeZone getTimeZone(AJAXRequestData requestData) {
        String timeZoneID = requestData.getParameter("timezone");
        if (null == timeZoneID) {
            timeZoneID = requestData.getSession().getUser().getTimeZone();
        }
        return TimeZone.getTimeZone(timeZoneID);
    }

}

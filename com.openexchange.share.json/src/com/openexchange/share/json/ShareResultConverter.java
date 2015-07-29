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

package com.openexchange.share.json;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.ajax.tools.JSONCoercion;
import com.openexchange.exception.OXException;
import com.openexchange.share.ShareInfo;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.groupware.ModuleSupport;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ShareResultConverter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ShareResultConverter implements ResultConverter {

    public static final String INPUT_FORMAT = "shareinfo";

    private final ModuleSupport service;

    /**
     * Initializes a new {@link ShareResultConverter}.
     *
     * @param service A reference to the module support service
     */
    public ShareResultConverter(ModuleSupport service) {
        super();
        this.service = service;
    }

    @Override
    public String getInputFormat() {
        return INPUT_FORMAT;
    }

    @Override
    public String getOutputFormat() {
        return "json";
    }

    @Override
    public Quality getQuality() {
        return Quality.GOOD;
    }

    @Override
    public void convert(AJAXRequestData requestData, AJAXRequestResult result, ServerSession session, Converter converter) throws OXException {
        /*
         * determine timezone, protocol, hostname
         */
        String timeZoneID = requestData.getParameter("timezone");
        if (null == timeZoneID) {
            timeZoneID = session.getUser().getTimeZone();
        }
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneID);
        /*
         * convert result object
         */
        Object resultObject = result.getResultObject();
        try {
            if (ShareLink.class.isInstance(resultObject)) {
                resultObject = convert((ShareLink) resultObject, timeZone, requestData);
            } else if (ShareInfo.class.isInstance(resultObject)) {
                resultObject = convert((ShareInfo) resultObject, timeZone, requestData);
            } else {
                resultObject = convert((List<ShareInfo>) resultObject, timeZone, requestData);
            }
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e);
        }
        result.setResultObject(resultObject, "json");
    }

    /**
     * Serializes a share link to JSON.
     *
     * @param link The share link to serialize
     * @param timeZone The client timezone
     * @param requestData The underlying ajax request data
     * @return The serialized guest share
     */
    private JSONObject convert(ShareLink link, TimeZone timeZone, AJAXRequestData requestData) throws OXException, JSONException {
        JSONObject json = new JSONObject();
        ShareInfo shareInfo = link.getShareInfo();
        json.put("url", shareInfo.getShareURL(requestData.getHostData()));
        json.put("is_new", link.isNew());
        Date expiryDate = shareInfo.getShare().getExpiryDate();
        if (null != expiryDate) {
            json.put("expiry_date", addTimeZoneOffset(expiryDate.getTime(), timeZone));
        }
        json.putOpt("password", shareInfo.getGuest().getPassword());
        Map<String, Object> meta = shareInfo.getShare().getMeta();
        if (null != meta) {
            json.put("meta", JSONCoercion.coerceToJSON(meta));
        }
        return json;
    }

    /**
     * Serializes multiple shares to JSON.
     *
     * @param shares The shares to serialize
     * @param timeZone The client timezone
     * @param requestData The underlying ajax request data
     * @return The serialized guest shares
     */
    private JSONArray convert(List<ShareInfo> shares, TimeZone timeZone, AJAXRequestData requestData) throws OXException {
        JSONArray jsonArray = new JSONArray(shares.size());
        for (ShareInfo share : shares) {
            jsonArray.put(convert(share, timeZone, requestData));
        }
        return jsonArray;
    }

    /**
     * Serializes a guest share to JSON.
     *
     * @param share The share to serialize
     * @param timeZone The client timezone
     * @param requestData The underlying ajax request data
     * @return The serialized guest share
     */
    private JSONObject convert(ShareInfo share, TimeZone timeZone, AJAXRequestData requestData) throws OXException {
        try {
            JSONObject json = new JSONObject();
            /*
             * common share properties
             */
            json.putOpt("share_url", share.getShareURL(requestData.getHostData()));
            json.put("token", share.getToken());
            json.putOpt("authentication", null != share.getGuest().getAuthentication() ? share.getGuest().getAuthentication().toString().toLowerCase() : null);
            json.putOpt("created", null != share.getShare().getCreated() ? addTimeZoneOffset(share.getShare().getCreated().getTime(), timeZone) : null);
            json.put("created_by", share.getShare().getCreatedBy());
            json.putOpt("last_modified", null != share.getShare().getModified() ? addTimeZoneOffset(share.getShare().getModified().getTime(), timeZone) : null);
            json.put("modified_by", share.getShare().getModifiedBy());
            Date expiryDate = share.getShare().getExpiryDate();
            if (null != expiryDate) {
                json.put("expiry_date", addTimeZoneOffset(expiryDate.getTime(), timeZone));
            }
            Map<String, Object> meta = share.getShare().getMeta();
            if (null != meta) {
                json.put("meta", JSONCoercion.coerceToJSON(meta));
            }
            /*
             * share targets & recipient
             */
            json.putOpt("target", serializeShareTarget(share.getShare().getTarget(), timeZone));
            json.put("recipient", serializeShareRecipient(share, timeZone));
            return json;
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e);
        }
    }

    /**
     * Serializes a share target to JSON.
     *
     * @param target The share target to serialize
     * @param timeZone The client timezone
     * @return The serialized share target
     * @throws JSONException
     */
    private JSONObject serializeShareTarget(ShareTarget target, TimeZone timeZone) throws JSONException {
        JSONObject jsonTarget = new JSONObject(8);
        String module = service.getShareModule(target.getModule());
        jsonTarget.put("module", module.isEmpty() ? String.valueOf(target.getModule()) : module);
        jsonTarget.putOpt("folder", target.getFolder());
        jsonTarget.putOpt("item", target.getItem());
        return jsonTarget;
    }

    /**
     * Extracts the share recipient from a guest share and serializes it to JSON.
     *
     * @param share The share to serialize the share recipient for
     * @param timeZone The client timezone
     * @return The serialized share recipient
     * @throws OXException
     */
    private JSONObject serializeShareRecipient(ShareInfo share, TimeZone timeZone) throws JSONException, OXException {
        JSONObject jsonRecipient = new JSONObject(8);
        jsonRecipient.put("type", share.getGuest().getRecipientType().toString().toLowerCase());
        jsonRecipient.put("base_token", share.getGuest().getBaseToken());
        jsonRecipient.putOpt("password", share.getGuest().getPassword());
        jsonRecipient.putOpt("email_address", share.getGuest().getEmailAddress());
        jsonRecipient.put("entity", String.valueOf(share.getShare().getGuest()));
        return jsonRecipient;
    }

    private static long addTimeZoneOffset(final long date, final TimeZone timeZone) {
        return null == timeZone ? date : date + timeZone.getOffset(date);
    }

}

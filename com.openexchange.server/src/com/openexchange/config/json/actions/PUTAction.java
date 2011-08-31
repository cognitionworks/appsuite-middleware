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

package com.openexchange.config.json.actions;

import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.config.json.ConfigAJAXRequest;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.settings.Setting;
import com.openexchange.groupware.settings.impl.ConfigTree;
import com.openexchange.groupware.settings.impl.SettingStorage;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link PUTAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class PUTAction extends AbstractConfigAction {

    /**
     * Initializes a new {@link PUTAction}.
     */
    public PUTAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final ConfigAJAXRequest req) throws OXException, JSONException {
        final ServerSession session = req.getSession();
        String value = req.getData().toString(); // Unparse
        if (value.length() > 0 && value.charAt(0) == '"') {
            value = value.substring(1);
        }
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        String path = req.getRequest().getSerlvetRequestURI();
        if (path.length() > 0 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        final SettingStorage stor = SettingStorage.getInstance(session);
        {
            final Setting setting = ConfigTree.getSettingByPath(path);
            setting.setSingleValue(value);
            saveSettingWithSubs(stor, setting);
        }
        return getJSONNullResult();
    }

    /**
     * Splits a value for a not leaf setting into its subsettings and stores them.
     *
     * @param storage setting storage.
     * @param setting actual setting.
     * @throws OXException if an error occurs.
     * @throws JSONException if the json object can't be parsed.
     */
    private void saveSettingWithSubs(final SettingStorage storage, final Setting setting) throws OXException, JSONException {
        if (setting.isLeaf()) {
            final String value = (String) setting.getSingleValue();
            if (null != value && value.length() > 0 && '[' == value.charAt(0)) {
                final JSONArray array = new JSONArray(value);
                if (array.length() == 0) {
                    setting.setEmptyMultiValue();
                } else {
                    for (int i = 0; i < array.length(); i++) {
                        setting.addMultiValue(array.getString(i));
                    }
                }
                setting.setSingleValue(null);
            }
            storage.save(setting);
        } else {
            final JSONObject json = new JSONObject(setting.getSingleValue().toString());
            final Iterator<String> iter = json.keys();
            OXException exc = null;
            while (iter.hasNext()) {
                final String key = iter.next();
                final Setting sub = ConfigTree.getSettingByPath(setting, new String[] { key });
                sub.setSingleValue(json.getString(key));
                try {
                    // Catch single exceptions if GUI writes not writable fields.
                    saveSettingWithSubs(storage, sub);
                } catch (final OXException e) {
                    exc = e;
                }
            }
            if (null != exc) {
                throw exc;
            }
        }
    }

}

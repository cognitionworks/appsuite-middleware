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

package com.openexchange.api.client.common.calls.infostore;

import java.util.Map;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.annotation.NonNull;
import com.openexchange.api.client.ApiClientExceptions;
import com.openexchange.api.client.HttpResponseParser;
import com.openexchange.api.client.common.calls.AbstractGetCall;
import com.openexchange.api.client.common.calls.infostore.mapping.DefaultFileMapper;
import com.openexchange.api.client.common.parser.AbstractHttpResponseParser;
import com.openexchange.api.client.common.parser.CommonApiResponse;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.groupware.tools.mappings.json.JsonMapping;
import com.openexchange.java.Strings;

/**
 * {@link GetCall}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.5
 */
public class GetCall extends AbstractGetCall<DefaultFile> {

    private final String folder;
    private final String id;
    private final String version;

    /**
     * Initializes a new {@link GetCall}.
     *
     * @param folder The ID of the folder who contains the info item
     * @param id The ID of the requested info item
     */
    public GetCall(String folder, String id) {
        this(folder, id, null);
    }

    /**
     * Initializes a new {@link GetCall}.
     *
     * @param folder The ID of the folder who contains the info item
     * @param id The ID of the requested info item
     * @param version The version to get, or null to get the current version
     */
    public GetCall(String folder, String id, String version) {
        this.folder = folder;
        this.id = id;
        this.version = version;
    }

    @Override
    @NonNull
    public String getModule() {
        return "/infostore";
    }

    @Override
    protected void fillParameters(Map<String, String> parameters) {
        parameters.put("id", id);
        parameters.put("folder", folder);
        putIfPresent(parameters, "version", version);
    }

    @Override
    protected String getAction() {
        return "get";
    }

    @Override
    public HttpResponseParser<DefaultFile> getParser() {
        return new AbstractHttpResponseParser<DefaultFile>() {

            private void setMedia(JSONObject json, DefaultFileMapper mapper, DefaultFile file) throws JSONException, OXException {
                if (json.hasAndNotNull("media") && !mapper.getMappings().isEmpty()) {
                    JSONObject jsonMedia = json.getJSONObject("media");
                    for (Field mediaField : File.Field.MEDIA_FIELDS) {
                        JsonMapping<? extends Object, DefaultFile> jsonMapping = mapper.getMappings().get(mediaField);
                        if (jsonMapping != null && Strings.isNotEmpty(jsonMapping.getAjaxName()) && jsonMedia.has(jsonMapping.getAjaxName())) {
                            jsonMapping.deserialize(jsonMedia, file);
                        }
                    }
                }
            }

            @Override
            public DefaultFile parse(CommonApiResponse commonResponse, HttpContext httpContext) throws OXException, JSONException {

                if (commonResponse.isJSONObject()) {
                    JSONObject jsonObject = commonResponse.getJSONObject();
                    DefaultFileMapper mapper = new DefaultFileMapper();
                    DefaultFile file = mapper.deserialize(jsonObject, mapper.getMappedFields());
                    //"media" is actually not a field but a nested object
                    setMedia(jsonObject, mapper, file);
                    return file;
                }
                throw ApiClientExceptions.JSON_ERROR.create("Not an JSON object");
            }
        };
    }
}

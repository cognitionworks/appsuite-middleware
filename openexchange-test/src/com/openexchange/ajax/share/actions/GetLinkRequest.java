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

package com.openexchange.ajax.share.actions;

import java.io.IOException;
import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AJAXRequest;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.ajax.framework.Header;
import com.openexchange.ajax.framework.Params;
import com.openexchange.java.util.TimeZones;
import com.openexchange.share.ShareTarget;

/**
 * {@link GetLinkRequest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class GetLinkRequest implements AJAXRequest<GetLinkResponse> {

    private final ShareTarget target;
    private final TimeZone timeZone;
    private boolean failOnError = true;

    /**
     * Initializes a new {@link GetLinkRequest} with UTC as time zone
     *
     * @param target The share target
     */
    public GetLinkRequest(ShareTarget target) {
        super();
        this.target = target;
        this.timeZone = TimeZones.UTC;
    }

    /**
     * Initializes a new {@link GetLinkRequest}.
     *
     * @param target The share target
     * @param timeZone The client timezone
     */
    public GetLinkRequest(ShareTarget target, TimeZone timeZone) {
        super();
        this.target = target;
        this.timeZone = timeZone;
    }

    @Override
    public Method getMethod() {
        return Method.PUT;
    }

    @Override
    public String getServletPath() {
        return "/ajax/share/management";
    }

    @Override
    public Parameter[] getParameters() throws IOException, JSONException {
        return new Params(AJAXServlet.PARAMETER_ACTION, "getLink").toArray();
    }

    @Override
    public AbstractAJAXParser<GetLinkResponse> getParser() {
        return new Parser(failOnError, timeZone);
    }

    @Override
    public Object getBody() throws IOException, JSONException {
        return ShareWriter.writeTarget(target);
    }

    @Override
    public Header[] getHeaders() {
        return NO_HEADER;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    private static final class Parser extends AbstractAJAXParser<GetLinkResponse> {

        private final TimeZone timeZone;

        /**
         * Initializes a new {@link Parser}.
         *
         * @param failOnError
         */
        protected Parser(boolean failOnError, TimeZone timeZone) {
            super(failOnError);
            this.timeZone = timeZone;
        }

        @Override
        protected GetLinkResponse createResponse(Response response) throws JSONException {
            if (!response.hasError()) {
                JSONObject data = (JSONObject) response.getData();
                ShareLink shareLink = new ShareLink(data, timeZone);
                return new GetLinkResponse(response, shareLink);
            }

            return new GetLinkResponse(response, null);
        }

    }

}

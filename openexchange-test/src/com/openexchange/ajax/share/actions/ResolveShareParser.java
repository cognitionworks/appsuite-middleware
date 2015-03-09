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

package com.openexchange.ajax.share.actions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import com.openexchange.ajax.framework.AbstractRedirectParser;
import com.openexchange.java.Strings;

/**
 * {@link ResolveShareParser}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ResolveShareParser extends AbstractRedirectParser<ResolveShareResponse> {

    /**
     * Initializes a new {@link ResolveShareParser}.
     */
    public ResolveShareParser() {
        this(true);
    }

    /**
     * Initializes a new {@link ResolveShareParser}.
     *
     * @param failOnNonRedirect <code>true</code> to fail if request is not redirected, <code>false</code>, otherwise
     */
    public ResolveShareParser(boolean failOnNonRedirect) {
        super(false, failOnNonRedirect, failOnNonRedirect);
    }

    @Override
    public String checkResponse(HttpResponse resp) throws ParseException, IOException {
        return super.checkResponse(resp);
    }

    @Override
    protected ResolveShareResponse createResponse(String location) {
        Map<String, String> map = new HashMap<String, String>();
        String path = location;
        if (false == Strings.isEmpty(location)) {
            int fragIndex = location.indexOf('#');
            if (-1 != fragIndex) {
                path = location.substring(0, fragIndex);
                String[] params = location.substring(fragIndex + 1).split("&");
                for (String param : params) {
                    int assignPos = param.indexOf('=');
                    if (-1 == assignPos) {
                        map.put(param, null);
                    } else {
                        map.put(param.substring(0, assignPos), param.substring(assignPos + 1));
                    }
                }
            }
        }
        return new ResolveShareResponse(getStatusCode(), path, map);
    }
}

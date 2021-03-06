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

package com.openexchange.dav.caldav.bugs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.junit.Test;
import com.openexchange.dav.Config;
import com.openexchange.dav.PropertyNames;
import com.openexchange.dav.caldav.CalDAVTest;

/**
 * {@link Bug40298Test}
 *
 * Thunderbird/Lightning assumes Free/Busy not available in CalDAV
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug40298Test extends CalDAVTest {

    @Test
    public void testResourceProperties() throws Exception {
        /*
         * discover current user principal & owner
         */
        DavPropertyNameSet props = new DavPropertyNameSet();
        props.add(PropertyNames.RESOURCETYPE);
        props.add(PropertyNames.OWNER);
        props.add(PropertyNames.CURRENT_USER_PRINCIPAL);
        props.add(PropertyNames.SUPPORTED_REPORT_SET);
        props.add(PropertyNames.SUPPORTED_CALENDAR_COMPONENT_SET);
        props.add(PropertyNames.GETCTAG);
        PropFindMethod propFind = new PropFindMethod(getWebDAVClient().getBaseURI() + Config.getPathPrefix() + "/caldav/" + encodeFolderID(getDefaultFolderID()), DavConstants.PROPFIND_BY_PROPERTY, props, DavConstants.DEPTH_0);
        MultiStatusResponse[] responses = getWebDAVClient().doPropFind(propFind);
        assertNotNull("got no response", responses);
        MultiStatusResponse response = assertSingleResponse(responses);
        String currentUserPrincipal = extractHref(PropertyNames.CURRENT_USER_PRINCIPAL, response);
        assertNotNull(currentUserPrincipal);
        assertTrue(currentUserPrincipal.contains("/" + getClient().getValues().getUserId()));
    }

}

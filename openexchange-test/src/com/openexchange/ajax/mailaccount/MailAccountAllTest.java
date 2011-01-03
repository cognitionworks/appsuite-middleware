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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.ajax.mailaccount;

import java.io.IOException;
import java.util.List;
import org.json.JSONException;
import org.xml.sax.SAXException;
import com.openexchange.ajax.mailaccount.actions.MailAccountAllRequest;
import com.openexchange.ajax.mailaccount.actions.MailAccountAllResponse;
import com.openexchange.mailaccount.Attribute;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.tools.servlet.AjaxException;


/**
 * {@link MailAccountAllTest}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class MailAccountAllTest extends AbstractMailAccountTest {

    public MailAccountAllTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        createMailAccount();
    }

    public void tearDown() throws Exception {
        if (null != mailAccountDescription && 0 != mailAccountDescription.getId()) {
            deleteMailAccount();
        }
        super.tearDown();
    }
    
    public void testAllShouldNotIncludePassword() throws AjaxException, IOException, SAXException, JSONException {
        int[] fields = new int[]{Attribute.ID_LITERAL.getId(), Attribute.PASSWORD_LITERAL.getId()};
        MailAccountAllResponse response = getClient().execute(new MailAccountAllRequest(fields));
        
        List<MailAccountDescription> descriptions = response.getDescriptions();
        assertFalse(descriptions.isEmpty());
        
        boolean found = false;
        for(MailAccountDescription description : descriptions) {
            if(description.getId() == mailAccountDescription.getId()) {
                assertTrue("Password was not null", null == description.getPassword());
                found = true;
            }
        }
        assertTrue("Did not find mail account in response" ,found);
    }
}

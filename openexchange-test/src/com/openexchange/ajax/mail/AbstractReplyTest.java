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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.ajax.mail;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.mail.actions.AllRequest;
import com.openexchange.ajax.mail.actions.ForwardRequest;
import com.openexchange.ajax.mail.actions.ForwardResponse;
import com.openexchange.ajax.mail.actions.GetRequest;
import com.openexchange.ajax.mail.actions.ReplyAllRequest;
import com.openexchange.ajax.mail.actions.ReplyAllResponse;
import com.openexchange.ajax.mail.actions.ReplyRequest;
import com.openexchange.ajax.mail.actions.ReplyResponse;
import com.openexchange.tools.servlet.AjaxException;

/**
 * {@link AbstractReplyTest} - test for the Reply/ReplyAll/Forward family of requests.
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public abstract class AbstractReplyTest extends AbstractMailTest {

    /**
     * Initializes a new {@link AbstractReplyTest}.
     * @param name
     */
    public AbstractReplyTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        clearFolder(getInboxFolder());
        clearFolder(getSentFolder());
    }

    @Override
    protected void tearDown() throws Exception {
        clearFolder(getInboxFolder());
        clearFolder(getSentFolder());
        super.tearDown();
    }

    protected boolean contains(List<String> from, String string) {
        for (String str2 : from) {
            if (str2.contains(string))
                return true;
        }
        return false;
    }

    protected JSONObject getReplyEMail(TestMail testMail) throws AjaxException, IOException, SAXException, JSONException {
        ReplyRequest reply = new ReplyRequest(testMail.getFolder(), testMail.getId());
        reply.setFailOnError(true);
        client = getClient();
        ReplyResponse response = client.execute(reply);
        return (JSONObject) response.getData();
    }
    

    protected JSONObject getReplyAllEMail(TestMail testMail) throws AjaxException, IOException, SAXException, JSONException {
        ReplyRequest reply = new ReplyAllRequest(testMail.getFolder(), testMail.getId());
        reply.setFailOnError(true);
        client = getClient();
        ReplyAllResponse response = (ReplyAllResponse) client.execute(reply);
        return (JSONObject) response.getData();
    }
    
    protected JSONObject getForwardMail(TestMail testMail) throws AjaxException, IOException, SAXException, JSONException {
        ReplyRequest reply = new ForwardRequest(testMail.getFolder(), testMail.getId());
        reply.setFailOnError(true);
        client = getClient();
        ForwardResponse response = (ForwardResponse) client.execute(reply);
        return (JSONObject) response.getData();
    }

    protected JSONObject getFirstMailInFolder(String inboxFolder) throws AjaxException, IOException, SAXException, JSONException {
        CommonAllResponse response = getClient().execute(new AllRequest(inboxFolder, new int[] { 600 }, -1, null));
        JSONArray arr = (JSONArray) response.getData();
        JSONArray mailFields = arr.getJSONArray(0);
        String id = mailFields.getString(0);
        AbstractAJAXResponse response2 = getClient().execute(new GetRequest(inboxFolder, id));
        return (JSONObject) response2.getData();
    }

    public static void assertNullOrEmpty(String msg, Collection coll){
        if(coll == null)
            return;
        if(coll.size() == 0)
            return;
        fail(msg);
    }

}

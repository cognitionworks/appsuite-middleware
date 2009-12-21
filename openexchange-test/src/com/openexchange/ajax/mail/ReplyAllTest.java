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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXClient.User;
import com.openexchange.ajax.mail.contenttypes.MailContentType;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.tools.servlet.AjaxException;


/**
 * {@link ReplyAllTest}
 *
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class ReplyAllTest extends AbstractReplyTest {

    public ReplyAllTest(String name) {
        super(name);
    }
    
    public List<Contact> extract(int amount, Contact[] source, List<String> excludedEmail){
        List<Contact> returnees = new LinkedList<Contact>();
        int used = 0;
        for (Contact elem : source)
            if (!(excludedEmail.contains(elem.getEmail1()) || excludedEmail.contains(elem.getEmail2()) || excludedEmail.contains(elem.getEmail3())) && used < amount) {
                returnees.add(elem);
                used++;
            }
        return returnees;
    }
    
    public void testShouldReplyToSenderAndAllRecipients() throws AjaxException, IOException, SAXException, JSONException, ConfigurationException {
        AJAXClient client1 = new AJAXClient(User.User1);
        AJAXClient client2 = new AJAXClient(User.User2);
        
        
        String mail1 = client1.getValues().getSendAddress(); // note: doesn't work the other way around on the dev system, because only the
        String mail2 = client2.getValues().getSendAddress(); // first account is set up correctly.

        List<Contact> otherContacts = extract(2, manager.searchAction("*", 6), Arrays.asList(mail1,mail2));
        assertTrue("Precondition: This test needs at least to other contacts in the global address book to work", otherContacts.size() > 1);
        
        this.client = client2;
        String anotherMail = otherContacts.get(0).getEmail1();
        String yetAnotherMail = otherContacts.get(1).getEmail1();
        
        JSONObject mySentMail = createEMail(adresses(mail1, anotherMail, yetAnotherMail), "ReplyAll test", MailContentType.ALTERNATIVE.toString(), MAIL_TEXT_BODY);
        sendMail(mySentMail.toString());

        this.client = client1;
        JSONObject myReceivedMail = getFirstMailInFolder(getInboxFolder());
        TestMail myReplyMail = new TestMail(getReplyAllEMail(new TestMail(myReceivedMail)));

        assertTrue("Should contain indicator that this is a reply in the subject line", myReplyMail.getSubject().startsWith("Re:"));

        List<String> toAndCC = myReplyMail.getTo();
        toAndCC.addAll(myReplyMail.getCc()); //need to do both because depending on user settings, it might be one of these
        
        assertTrue("Sender of original message should become recipient in reply", contains(toAndCC, mail2));
        assertTrue("1st recipient ("+anotherMail+") of original message should still be recipient in reply, but TO/CC field only has these: " + toAndCC, contains(toAndCC, anotherMail));
        assertTrue("2nd recipient ("+yetAnotherMail+") of original message should still be recipient in reply, but TO/CC field only has these: " + toAndCC, contains(toAndCC, yetAnotherMail));
    }
    
    
    protected String adresses(String... mails){
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for(String mail: mails){
            builder.append("[null,");
            builder.append(mail);
            builder.append("],");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("]");
        return builder.toString();
    }

}

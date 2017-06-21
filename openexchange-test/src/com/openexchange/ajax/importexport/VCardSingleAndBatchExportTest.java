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

package com.openexchange.ajax.importexport;

import static org.junit.Assert.*;
import java.io.IOException;
import org.json.JSONException;
import org.junit.Test;
import com.openexchange.ajax.contact.AbstractManagedContactTest;
import com.openexchange.ajax.importexport.actions.CSVExportRequest;
import com.openexchange.ajax.importexport.actions.CSVExportResponse;
import com.openexchange.ajax.importexport.actions.VCardExportRequest;
import com.openexchange.ajax.importexport.actions.VCardExportResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;

/**
 * {@link VCardSingleAndBatchExportTest}
 *
 * @author <a href="mailto:Jan-Oliver.Huhn@open-xchange.com">Jan-Oliver Huhn</a>
 * @since v7.10
 */
public class VCardSingleAndBatchExportTest extends AbstractManagedContactTest {

    public VCardSingleAndBatchExportTest() {
        super();
    }
    
    private final String DOT_DELIMITER = ".";
    private final String COMMA_DELIMITER = ",";
    
//    @Test
//    public void testVCardSingleExport() throws OXException, IOException, JSONException{
//        Contact contact = generateContact("Singlecontact");
//        int contactId = cotm.newAction(contact).getObjectID();
//        
//        String singleId = folderID+DOT_DELIMITER+contactId;
//        
//        VCardExportResponse vcardExportResponse = getClient().execute(new VCardExportRequest(folderID, false, 0, singleId, false));
//        String vcard = (String) vcardExportResponse.getData(); 
//        String[] result = vcard.split("END:VCARD\\r?\\nBEGIN:VCARD");
//        assertEquals("One vCard expected!", 1, result.length);
//    }
//    
//    @Test
//    public void testVCardMultipleExport() throws OXException, IOException, JSONException{
//        Contact firstContact = generateContact("First Contact");
//        int firstId = cotm.newAction(firstContact).getObjectID();
//        
//        Contact secondContact = generateContact("Second Contact");
//        int secondId = cotm.newAction(secondContact).getObjectID();
//        
//        Contact thirdContact = generateContact("Third Contact");
//        int thirdId = cotm.newAction(thirdContact).getObjectID();
//        
//        String batchIds = folderID+DOT_DELIMITER+firstId+COMMA_DELIMITER+folderID+DOT_DELIMITER+secondId+COMMA_DELIMITER+folderID+DOT_DELIMITER+thirdId;
//        VCardExportResponse vcardExportResponse = getClient().execute(new VCardExportRequest(folderID, false, 0, batchIds, false));
//        String vcard = (String) vcardExportResponse.getData(); 
//        String[] result = vcard.split("END:VCARD\\r?\\nBEGIN:VCARD");
//        assertEquals("Three vCards expected!", 3, result.length);
//    }
//    
//    @Test
//    public void testVCardOldFolderExport() throws OXException, IOException, JSONException{
//        Contact firstContact = generateContact("First Contact");
//        cotm.newAction(firstContact).getObjectID();
//        
//        Contact secondContact = generateContact("Second Contact");
//        cotm.newAction(secondContact).getObjectID();
//        
//        VCardExportResponse vcardExportResponse = getClient().execute(new VCardExportRequest(folderID, false));
//        String vcard = (String) vcardExportResponse.getData();        
//        String[] result = vcard.split("END:VCARD\\r?\\nBEGIN:VCARD");
//        assertEquals("Two vCards expected!", 2, result.length);
//    }    
    
    @Test
    public void testCrossFolderBatchExportTest() throws OXException, IOException, JSONException{
        Contact firstContact = generateContact("First Contact");
        int firstId = cotm.newAction(firstContact).getObjectID();
        
        Contact secondContact = generateContact("Second Contact");
        int secondId = cotm.newAction(secondContact).getObjectID();
        
        Contact thirdContact = generateContact("Third Contact", secondFolderID);
        int thirdId = cotm.newAction(thirdContact).getObjectID();
        
        String batchIds = folderID+DOT_DELIMITER+firstId+COMMA_DELIMITER+folderID+DOT_DELIMITER+secondId+COMMA_DELIMITER+secondFolderID+DOT_DELIMITER+thirdId;
        VCardExportResponse vcardExportResponse = getClient().execute(new VCardExportRequest(-1, false, 0, batchIds, false));
        String vcard = (String) vcardExportResponse.getData(); 
        String[] result = vcard.split("END:VCARD\\r?\\nBEGIN:VCARD");
        assertEquals("Three vCards expected!", 3, result.length);        
    }
}

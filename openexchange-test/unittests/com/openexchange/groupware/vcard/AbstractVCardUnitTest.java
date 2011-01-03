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

package com.openexchange.groupware.vcard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import junit.framework.TestCase;

import com.openexchange.groupware.container.Contact;
import com.openexchange.tools.versit.Versit;
import com.openexchange.tools.versit.VersitDefinition;
import com.openexchange.tools.versit.VersitException;
import com.openexchange.tools.versit.VersitObject;
import com.openexchange.tools.versit.converter.ConverterException;
import com.openexchange.tools.versit.converter.OXContainerConverter;

public class AbstractVCardUnitTest extends TestCase {

    public final String mime1  = "text/x-vcard";
    public final String mime2  = "text/vcard";
    
	
	public List<Contact> performTest(final String testName, final String vcard, final String mime) throws ConverterException, IOException{
		final OXContainerConverter oxContainerConverter = new OXContainerConverter((TimeZone) null, (String) null);
		final VersitDefinition def = Versit.getDefinition(mime);
		final VersitDefinition.Reader versitReader = def.getReader(new ByteArrayInputStream(vcard.getBytes("UTF-8")), "UTF-8");
		List<Contact> results = new LinkedList<Contact>();
		try {
			VersitObject versitObject = def.parse(versitReader);
			while (versitObject != null) {
				final Contact contactObj = oxContainerConverter.convertContact(versitObject);
				versitObject = def.parse(versitReader);
				results.add(contactObj);
			}
			assertTrue(testName + " passed", true);
		} catch (final VersitException e){
			fail(testName + " failed with exception: " + e);
		}
		return results;
	}
}

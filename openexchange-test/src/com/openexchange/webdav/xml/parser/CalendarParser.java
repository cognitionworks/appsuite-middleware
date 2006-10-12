/*
 *
 *    OPEN-XCHANGE - "the communication and information enviroment"
 *
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    OPEN-XCHANGE is a trademark of Netline Internet Service GmbH and all
 *    other brand and product names are or may be trademarks of, and are
 *    used to identify products or services of, their respective owners.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original code will still remain
 *    copyrighted by the copyright holder(s) or original author(s).
 *
 *
 *     Copyright (C) 1998 - 2005 Netline Internet Service GmbH
 *     mail:	                 info@netline-is.de
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License as published by the Free
 *     Software Foundation; either version 2 of the License, or (at your option)
 *     any later version.
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
 *
 
 */

package com.openexchange.webdav.xml.parser;

import com.openexchange.api.OXConflictException;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.GroupParticipant;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.Participants;
import com.openexchange.groupware.container.ResourceParticipant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.ldap.Group;
import com.openexchange.groupware.ldap.GroupStorage;
import com.openexchange.webdav.xml.XmlServlet;
import com.openexchange.webdav.xml.fields.CalendarFields;
import com.sun.java_cup.internal.parser;
import java.util.List;
import org.jdom.Element;

/**
 * CalendarParser
 *
 * @author <a href="mailto:sebastian.kauss@netline-is.de">Sebastian Kauss</a>
 */

public abstract class CalendarParser extends CommonParser {
	
	protected void parseElementCalendar(CalendarObject calendarobject, Element eProp) throws Exception {		if (hasElement(eProp.getChild(CalendarFields.RECURRENCE_ID, XmlServlet.NS))) {
			calendarobject.setRecurrenceID(getValueAsInt(eProp.getChild(CalendarFields.RECURRENCE_ID, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.RECURRENCE_POSITION, XmlServlet.NS))) {
			calendarobject.setRecurrencePosition(getValueAsInt(eProp.getChild(CalendarFields.RECURRENCE_POSITION, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.RECURRENCE_DATE_POSITION, XmlServlet.NS))) {
			calendarobject.setRecurrenceDatePosition(getValueAsDate(eProp.getChild(CalendarFields.RECURRENCE_DATE_POSITION, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.MONTH, XmlServlet.NS))) {
			calendarobject.setMonth(getValueAsInt(eProp.getChild(CalendarFields.MONTH, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.DAY_IN_MONTH, XmlServlet.NS))) {
			calendarobject.setDayInMonth(getValueAsInt(eProp.getChild(CalendarFields.DAY_IN_MONTH, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.DAYS, XmlServlet.NS))) {
			calendarobject.setDays(getValueAsInt(eProp.getChild(CalendarFields.DAYS, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.INTERVAL, XmlServlet.NS))) {
			calendarobject.setInterval(getValueAsInt(eProp.getChild(CalendarFields.INTERVAL, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.UNTIL, XmlServlet.NS))) {
			calendarobject.setUntil(getValueAsDate(eProp.getChild(CalendarFields.UNTIL, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.START_DATE, XmlServlet.NS))) {
			calendarobject.setStartDate(getValueAsDate(eProp.getChild(CalendarFields.START_DATE, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.END_DATE, XmlServlet.NS))) {
			calendarobject.setEndDate(getValueAsDate(eProp.getChild(CalendarFields.END_DATE, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.RECURRENCE_TYPE, XmlServlet.NS))) {
			int recurrenceType = parseRecurrenceType(getValue(eProp.getChild(CalendarFields.RECURRENCE_TYPE, XmlServlet.NS)));
		
			calendarobject.setRecurrenceType(recurrenceType);
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.NOTIFICATION, XmlServlet.NS))) {
			calendarobject.setNotification(getValueAsBoolean(eProp.getChild(CalendarFields.NOTIFICATION, XmlServlet.NS)));
		} 
		
		if (hasElement(eProp.getChild(CalendarFields.TITLE, XmlServlet.NS))) {
			calendarobject.setTitle(getValue(eProp.getChild(CalendarFields.TITLE, XmlServlet.NS)));
		} 

		if (hasElement(eProp.getChild(CalendarFields.NOTE, XmlServlet.NS))) {
			calendarobject.setNote(getValue(eProp.getChild(CalendarFields.NOTE, XmlServlet.NS)));
		} 
		
		parseElementParticipants(calendarobject, eProp.getChild(CalendarFields.PARTICIPANTS, XmlServlet.NS));
		
		parseElementCommon(calendarobject, eProp);
	}
	
	protected int parseRecurrenceType(String value) throws Exception {
		if (value.equals("none")) {
			return CalendarObject.NONE;
		} else if (value.equals("daily")) {
			return CalendarObject.DAILY;
		} else if (value.equals("weekly")) {
			return CalendarObject.WEEKLY;
		} else if (value.equals("monthly")) {
			return CalendarObject.MONTHLY;
		} else if (value.equals("yearly")) {
			return CalendarObject.YEARLY;
		} else {
			throw new OXConflictException("unknown value in " + CalendarFields.RECURRENCE_TYPE + ": " + value);
		}
	}
	
	protected void parseElementParticipants(CalendarObject calendarObj, Element eParticipant) throws Exception {
		if (eParticipant == null) {
			return ;
		}
		
		Participants participants = new Participants();
		
		boolean hasParticipants = false;
		
		List elementUsers = eParticipant.getChildren("user", XmlServlet.NS);
		List elementGroups = eParticipant.getChildren("group", XmlServlet.NS);
		List elementResources = eParticipant.getChildren("resource", XmlServlet.NS);
		
		if (elementUsers != null) {
			for (int a = 0; a < elementUsers.size(); a++) {
				parseElementUser((Element)elementUsers.get(a), participants);
				hasParticipants = true;
			}
		}
		
		if (elementGroups != null) {
			for (int a = 0; a < elementGroups.size(); a++) {
				parseElementGroup((Element)elementGroups.get(a), participants);
				hasParticipants = true;
			}
		}
		
		if (elementResources != null) {
			for (int a = 0; a < elementResources.size(); a++) {
				parseElementResource((Element)elementResources.get(a), participants);
				hasParticipants = true;
			}
		}
		
		if (hasParticipants) {
			calendarObj.setUsers(participants.getUsers());
		}
	}
	
	
	private void parseElementUser(Element e, Participants participants) throws Exception {
		UserParticipant userparticipant = new UserParticipant();
		String confirm = e.getAttributeValue("confirm", XmlServlet.NS);
		 
		if (confirm != null) {
			if (confirm.equals("accept")) {
				userparticipant.setConfirm(CalendarObject.ACCEPT);
			} else if (confirm.equals("decline")) {
				userparticipant.setConfirm(CalendarObject.DECLINE);
			} else if (confirm.equals("tentative")) {
				userparticipant.setConfirm(CalendarObject.TENTATIVE);
			} else if (confirm.equals("none")) {
				userparticipant.setConfirm(CalendarObject.NONE);
			} else {
				throw new OXConflictException("unknown value in confirm attribute: " + confirm);
			}
		}
		 
		userparticipant.setIdentifier(getValueAsInt(e));
		participants.add(userparticipant);
	}
	
	private void parseElementGroup(Element e, Participants participants) throws Exception {
		int id = getValueAsInt(e);
		 
		Participant p = new GroupParticipant();
		p.setIdentifier(id);
		 
		participants.add(p);
	}
	
	private void parseElementResource(Element e, Participants participants) throws Exception {
		Participant p = new ResourceParticipant();
		p.setIdentifier(getValueAsInt(e));
		participants.add(p);
	}
}





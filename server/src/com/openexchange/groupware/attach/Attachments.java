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

package com.openexchange.groupware.attach;

import java.util.HashMap;
import java.util.Map;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.attach.impl.AttachmentBaseImpl;
import com.openexchange.groupware.attach.impl.OverridableAttachmentAuthorization;
import com.openexchange.groupware.attach.impl.OverridableAttachmentListener;
import com.openexchange.groupware.calendar.CalendarAttachments;
import com.openexchange.groupware.contact.ContactsAttachment;
import com.openexchange.groupware.tasks.TaskAttachmentListener;
import com.openexchange.groupware.tasks.TaskAuthorization;
import com.openexchange.groupware.tx.DBPoolProvider;
import com.openexchange.tools.service.ServicePriorityConflictException;
import com.openexchange.tools.service.SpecificServiceChooser;

public abstract class Attachments {

    private static final Map<Integer, SpecificServiceChooser<AttachmentAuthorization>> authz = new HashMap<Integer, SpecificServiceChooser<AttachmentAuthorization>>();
    private static final Map<Integer, SpecificServiceChooser<AttachmentListener>> listener = new HashMap<Integer, SpecificServiceChooser<AttachmentListener>>();
	
    private static final AttachmentBaseImpl impl = new AttachmentBaseImpl(new DBPoolProvider());
    
    static {
        try {
            SpecificServiceChooser<AttachmentAuthorization> taskAuth = new SpecificServiceChooser<AttachmentAuthorization>();
            taskAuth.registerForEverything(new TaskAuthorization(), 0);
        
            SpecificServiceChooser<AttachmentAuthorization> contactAuth = new SpecificServiceChooser<AttachmentAuthorization>();
            contactAuth.registerForEverything(new ContactsAttachment(), 0);
            
            SpecificServiceChooser<AttachmentAuthorization> appointmentAuth = new SpecificServiceChooser<AttachmentAuthorization>();
            appointmentAuth.registerForEverything(new CalendarAttachments(), 0);
            
            authz.put(Types.TASK, taskAuth);
            authz.put(Types.CONTACT, contactAuth);
            authz.put(Types.APPOINTMENT, appointmentAuth);
            
            
            SpecificServiceChooser<AttachmentListener> taskListener = new SpecificServiceChooser<AttachmentListener>();
            taskListener.registerForEverything(new TaskAttachmentListener(), 0);
            
            SpecificServiceChooser<AttachmentListener> contactListener = new SpecificServiceChooser<AttachmentListener>();
            contactListener.registerForEverything(new ContactsAttachment(), 0);
            
            SpecificServiceChooser<AttachmentListener> appointmentListener = new SpecificServiceChooser<AttachmentListener>();
            appointmentListener.registerForEverything(new CalendarAttachments(), 0);
            
            listener.put(Types.TASK, taskListener);
            listener.put(Types.CONTACT, contactListener);
            listener.put(Types.APPOINTMENT, appointmentListener);

            impl.addAuthorization(new OverridableAttachmentAuthorization(taskAuth), Types.TASK);
            impl.registerAttachmentListener(new OverridableAttachmentListener(taskListener) ,Types.TASK);
            impl.addAuthorization(new OverridableAttachmentAuthorization(contactAuth), Types.CONTACT);
            impl.registerAttachmentListener(new OverridableAttachmentListener(contactListener) ,Types.CONTACT);
            impl.addAuthorization(new OverridableAttachmentAuthorization(appointmentAuth), Types.APPOINTMENT);
            impl.registerAttachmentListener(new OverridableAttachmentListener(appointmentListener),Types.APPOINTMENT);

            
        } catch (ServicePriorityConflictException e) {
            // Doesn't happen
            e.printStackTrace();
        }
    }
    
    public static SpecificServiceChooser<AttachmentAuthorization> getAuthorizationChooserForModule(int module) {
        return authz.get(module);
    }
    
    public static SpecificServiceChooser<AttachmentListener> getListenerChooserForModule(int module) {
        return listener.get(module);
    }


    public static AttachmentBase getInstance(){
		return impl;
	}
}

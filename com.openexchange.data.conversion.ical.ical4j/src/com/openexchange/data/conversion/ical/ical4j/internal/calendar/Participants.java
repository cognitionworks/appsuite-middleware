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

package com.openexchange.data.conversion.ical.ical4j.internal.calendar;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.ResourceList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Resources;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.ConversionWarning.Code;
import com.openexchange.data.conversion.ical.ical4j.internal.AbstractVerifyingAttributeConverter;
import com.openexchange.data.conversion.ical.ical4j.internal.OXResourceResolver;
import com.openexchange.data.conversion.ical.ical4j.internal.ResourceResolver;
import com.openexchange.data.conversion.ical.ical4j.internal.UserResolver;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.ResourceParticipant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceException;
import com.openexchange.server.ServiceException;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public class Participants<T extends CalendarComponent, U extends CalendarObject> extends AbstractVerifyingAttributeConverter<T,U> {

    private static Log LOG = LogFactory.getLog(Participants.class);

    public static UserResolver userResolver = new UserResolver() {

        public List<User> findUsers(List<String> mails, Context ctx) throws LdapException {
            return new ArrayList<User>();
        }

        public User loadUser(int userId, Context ctx) throws LdapException {
            return null;
        }
    };
    
    public static ResourceResolver resourceResolver = new OXResourceResolver();

    public boolean isSet(U cObj) {
        return cObj.containsParticipants();
    }

    public void emit(int index, U cObj, T component, List<ConversionWarning> warnings, Context ctx) throws ConversionError {
        List<ResourceParticipant> resources = new LinkedList<ResourceParticipant>();
        for(Participant p : cObj.getParticipants()) {
            switch(p.getType()) {
                case Participant.USER:
                    addUserAttendee(index, (UserParticipant)p, ctx, component);
                    break;
                case Participant.EXTERNAL_USER:
                    addExternalAttendee((ExternalUserParticipant)p, component);
                    break;
                case Participant.RESOURCE:
                    resources.add((ResourceParticipant) p);

            }
        }
        if(resources.size() == 0) { return; }
        setResources(index, component, resources, ctx);
    }

    private void setResources(final int index, final T component,
        final List<ResourceParticipant> resources, final Context ctx) throws ConversionError {
        final ResourceList list = new ResourceList();
        for (ResourceParticipant res : resources) {
            String displayName = res.getDisplayName();
            if (null == displayName) {
                try {
                    Resource resource = resourceResolver.load(res.getIdentifier(), ctx);
                    displayName = resource.getDisplayName();
                } catch (final ResourceException e) {
                    throw new ConversionError(index, e);
                } catch (final ServiceException e) {
                    throw new ConversionError(index, e);
                }
            }
            list.add(displayName);
        }
        Resources property = new Resources(list);
        component.getProperties().add(property);
    }

    private void addExternalAttendee(ExternalUserParticipant externalUserParticipant, T component) {
        Attendee attendee = new Attendee();
        try {
            attendee.setValue("MAILTO:"+externalUserParticipant.getEmailAddress());
            component.getProperties().add(attendee);
        } catch (URISyntaxException e) {
            LOG.error(e); // Shouldn't happen
        }
    }

    private void addUserAttendee(int index, UserParticipant userParticipant, Context ctx, T component) throws ConversionError {
        Attendee attendee = new Attendee();
        try {
            String address = userParticipant.getEmailAddress();
            if(address == null) {
                try {
                    User user = userResolver.loadUser(userParticipant.getIdentifier(), ctx);
                    address = user.getMail();
                } catch (final LdapException e) {
                    throw new ConversionError(index, e);
                }
            }
            attendee.setValue("MAILTO:"+ address);
            component.getProperties().add(attendee);
        } catch (final URISyntaxException e) {
            LOG.error(e.getMessage(), e); // Shouldn't happen
        }
    }

    public boolean hasProperty(T component) {
        PropertyList properties = component.getProperties("ATTENDEE");
        PropertyList resourcesList = component.getProperties("RESOURCES");
        return properties.size() > 0 || resourcesList.size() > 0;
    }

    public void parse(int index, T component, U cObj, TimeZone timeZone, Context ctx, List<ConversionWarning> warnings) throws ConversionError {
        PropertyList properties = component.getProperties("ATTENDEE");
        List<String> mails = new LinkedList<String>();

        for(int i = 0, size = properties.size(); i < size; i++) {
            Attendee attendee = (Attendee) properties.get(i);
            URI uri = attendee.getCalAddress();
            if("mailto".equalsIgnoreCase(uri.getScheme())) {
                String mail = uri.getSchemeSpecificPart();
                mails.add( mail );
            }
        }

        List<User> users;
	    try {
            users = userResolver.findUsers(mails, ctx);
        } catch (final LdapException e) {
            throw new ConversionError(index, e);
        }

        for(User user : users) {
            cObj.addParticipant( new UserParticipant(user.getId()) );
            mails.remove(user.getMail());
        }

        for(String mail : mails) {
            ExternalUserParticipant external = new ExternalUserParticipant(mail);
            external.setDisplayName(null);
            cObj.addParticipant(external);
        }

        final List<String> resourceNames = new LinkedList<String>();
        PropertyList resourcesList = component.getProperties("RESOURCES");
        for (int i = 0, size = resourcesList.size(); i < size; i++) {
            Resources resources = (Resources) resourcesList.get(i);
            final Iterator<?> resObjects = resources.getResources().iterator();
            while (resObjects.hasNext()) {
                resourceNames.add(resObjects.next().toString());
            }
        }
        final List<Resource> resources;
        try {
            resources = resourceResolver.find(resourceNames, ctx);
        } catch (final ResourceException e) {
            throw new ConversionError(index, e);
        } catch (final ServiceException e) {
            throw new ConversionError(index, e);
        }

        for (Resource resource : resources) {
            cObj.addParticipant(new ResourceParticipant(resource.getIdentifier()));
            resourceNames.remove(resource.getDisplayName());
        }
        for (String resourceName : resourceNames) {
            final ConversionWarning warning = new ConversionWarning(index,
                Code.CANT_RESOLVE_RESOURCE, resourceName);
            warnings.add(warning);
        }
    }
}

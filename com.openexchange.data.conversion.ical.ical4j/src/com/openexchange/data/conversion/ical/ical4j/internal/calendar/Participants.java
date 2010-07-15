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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.ResourceList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
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
import com.openexchange.groupware.container.participants.ConfirmableParticipant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserException;
import com.openexchange.groupware.notify.NotificationConfig;
import com.openexchange.groupware.notify.NotificationConfig.NotificationProperty;
import com.openexchange.groupware.userconfiguration.UserConfigurationException;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourceException;
import com.openexchange.server.ServiceException;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public class Participants<T extends CalendarComponent, U extends CalendarObject> extends AbstractVerifyingAttributeConverter<T,U> {

    private static Log LOG = LogFactory.getLog(Participants.class);

    public static UserResolver userResolver = UserResolver.EMPTY;

    public static ResourceResolver resourceResolver = new OXResourceResolver();

    public boolean isSet(final U cObj) {
        return cObj.containsParticipants();
    }

    public void emit(final int index, final U cObj, final T component, final List<ConversionWarning> warnings, final Context ctx, Object... args) throws ConversionError {
        final List<ResourceParticipant> resources = new LinkedList<ResourceParticipant>();
        for(final Participant p : cObj.getParticipants()) {
            switch(p.getType()) {
                case Participant.USER:
                    addUserAttendee(index, (UserParticipant)p, ctx, component, cObj);
                    break;
                case Participant.EXTERNAL_USER:
                    addExternalAttendee((ExternalUserParticipant)p, component);
                    break;
                case Participant.RESOURCE:
                    resources.add((ResourceParticipant) p);
                    break;
                default:
            }
        }
        if(resources.isEmpty()) { return; }
        setResources(index, component, resources, ctx);
    }

    private void setResources(final int index, final T component,
        final List<ResourceParticipant> resources, final Context ctx) throws ConversionError {
        final ResourceList list = new ResourceList();
        for (final ResourceParticipant res : resources) {
            String displayName = res.getDisplayName();
            if (null == displayName) {
                try {
                    final Resource resource = resourceResolver.load(res.getIdentifier(), ctx);
                    displayName = resource.getDisplayName();
                } catch (final ResourceException e) {
                    throw new ConversionError(index, e);
                } catch (final ServiceException e) {
                    throw new ConversionError(index, e);
                }
            }
            list.add(displayName);
        }
        final Resources property = new Resources(list);
        component.getProperties().add(property);
    }

    protected void addExternalAttendee(final ExternalUserParticipant externalUserParticipant, final T component) {
        final Attendee attendee = new Attendee();
        try {
            attendee.setValue("mailto:" + externalUserParticipant.getEmailAddress());
            ParameterList parameters = attendee.getParameters();
            parameters.add(CuType.INDIVIDUAL);
            parameters.add(PartStat.NEEDS_ACTION);
            parameters.add(Role.REQ_PARTICIPANT);
            parameters.add(Rsvp.TRUE);
            component.getProperties().add(attendee);
        } catch (final URISyntaxException e) {
            LOG.error(e); // Shouldn't happen
        }
    }

    protected void addUserAttendee(int index, UserParticipant userParticipant, Context ctx, T component, U obj) throws ConversionError {
        final Attendee attendee = new Attendee();
        try {
            String address ="";
            //This sets the attendees email-addresses to their DefaultSenderAddress if configured via com.openexchange.notification.fromSource in notification.properties
            String senderSource = NotificationConfig.getProperty(NotificationProperty.FROM_SOURCE, "primaryMail");
            if ("defaultSenderAddress".equals(senderSource)) { 
                try {
                    address = UserSettingMailStorage.getInstance().loadUserSettingMail(userParticipant.getIdentifier(), ctx).getSendAddr();
                } catch (UserConfigurationException e) {
                    LOG.error(e.getMessage(), e);
                    address = resolveUserMail(index, userParticipant, ctx);
                }
            } else {
                address = resolveUserMail(index, userParticipant, ctx);
            }
            attendee.setValue("mailto:" + address);
            ParameterList parameters = attendee.getParameters();
            parameters.add(Role.REQ_PARTICIPANT);
            parameters.add(CuType.INDIVIDUAL);
            switch (userParticipant.getConfirm()) {
            case CalendarObject.ACCEPT:
                parameters.add(PartStat.ACCEPTED);
                break;
            case CalendarObject.DECLINE:
                parameters.add(PartStat.DECLINED);
                break;
            case CalendarObject.TENTATIVE:
                parameters.add(PartStat.NEEDS_ACTION);
                break;
            case CalendarObject.NONE:
            default:
            }
            component.getProperties().add(attendee);
        } catch (final URISyntaxException e) {
            LOG.error(e.getMessage(), e); // Shouldn't happen
        }
    }

    protected String resolveUserMail(int index, UserParticipant userParticipant, Context ctx) throws ConversionError {
        String address = userParticipant.getEmailAddress();
        if (address == null) {
            try {
                final User user = userResolver.loadUser(userParticipant.getIdentifier(), ctx);
                address = user.getMail();
            } catch (UserException e) {
                throw new ConversionError(index, e);
            } catch (ServiceException e) {
                throw new ConversionError(index, e);
            }
        }
        return address;
    }

    public boolean hasProperty(final T component) {
        final PropertyList properties = component.getProperties(Property.ATTENDEE);
        final PropertyList resourcesList = component.getProperties(Property.RESOURCES);
        return properties.size() > 0 || resourcesList.size() > 0;
    }

    public void parse(final int index, final T component, final U cObj, final TimeZone timeZone, final Context ctx, final List<ConversionWarning> warnings) throws ConversionError {
        final PropertyList properties = component.getProperties(Property.ATTENDEE);
        final Map<String, ICalParticipant> mails = new HashMap<String, ICalParticipant>();
        final List<String> resourceNames = new LinkedList<String>();
        
        String comment = component.getProperty(Property.COMMENT) == null ? null : component.getProperty(Property.COMMENT).getValue();

        for(int i = 0, size = properties.size(); i < size; i++) {
            final Attendee attendee = (Attendee) properties.get(i);
            if (attendee.getParameter(Parameter.CUTYPE) != null && CuType.RESOURCE.equals(attendee.getParameter(Parameter.CUTYPE))) {
                final Parameter cn = attendee.getParameter(Parameter.CN);
                if (cn != null) {
                    resourceNames.add(cn.getValue());
                }
            } else {
                final URI uri = attendee.getCalAddress();
                if("mailto".equalsIgnoreCase(uri.getScheme())) {
                    final String mail = uri.getSchemeSpecificPart();
                    ICalParticipant icalP = createIcalParticipant(attendee, mail, comment);
                    mails.put(mail, icalP);
                }
            }

        }

        List<User> users;
        try {
            users = userResolver.findUsers(new ArrayList<String>(mails.keySet()), ctx);
        } catch (final UserException e) {
            throw new ConversionError(index, e);
        } catch (final ServiceException e) {
            throw new ConversionError(index, e);
        }

        for(final User user : users) {
            UserParticipant up = new UserParticipant(user.getId());
            ICalParticipant icalP = null;
            for (String alias: user.getAliases()) {
                icalP = mails.get(alias);
                if (icalP != null) {
                    mails.remove(alias);
                    break;
                }
            }
            if (icalP == null)
                throw new IllegalStateException("Should not be possible to find a user by it's alias and then be unable to remove that alias from list");
            if (icalP.message != null)
                up.setConfirmMessage(icalP.message);
            if (icalP.status != -1)
                up.setConfirm(icalP.status);
            
            cObj.addParticipant( new UserParticipant(user.getId()) ); //TODO Is that a bug? Which one?
        }

        List<ConfirmableParticipant> confirmableParticipants = new ArrayList<ConfirmableParticipant>();
        for(final String mail : mails.keySet()) {
            final ExternalUserParticipant external = new ExternalUserParticipant(mail);
            external.setDisplayName(null);

            ICalParticipant icalP = mails.get(mail);
            
            if (icalP.message != null)
                external.setMessage(icalP.message);
            if (icalP.status != -1)
                external.setConfirm(icalP.status);
            
            if (comment != null)
                external.setMessage(comment);
            
            cObj.addParticipant(external);
            confirmableParticipants.add(external);
        }
        
        if (confirmableParticipants.size() > 0) {
            cObj.setConfirmations(confirmableParticipants.toArray(new ConfirmableParticipant[confirmableParticipants.size()]));
        }

        final PropertyList resourcesList = component.getProperties(Property.RESOURCES);
        for (int i = 0, size = resourcesList.size(); i < size; i++) {
            final Resources resources = (Resources) resourcesList.get(i);
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

        for (final Resource resource : resources) {
            cObj.addParticipant(new ResourceParticipant(resource.getIdentifier()));
            resourceNames.remove(resource.getDisplayName());
        }
        for (final String resourceName : resourceNames) {
            final ConversionWarning warning = new ConversionWarning(index,
                Code.CANT_RESOLVE_RESOURCE, resourceName);
            warnings.add(warning);
        }
    }
    
    /**
     * @param attendee
     * @return
     */
    private ICalParticipant createIcalParticipant(Attendee attendee, String mail, String message) {
        ICalParticipant retval = new ICalParticipant(mail, -1, message);

        Parameter parameter = attendee.getParameter(Parameter.PARTSTAT);
        if (parameter != null) {
            if (parameter.equals(PartStat.ACCEPTED)) {
                retval.status = CalendarObject.ACCEPT;
            } else if (parameter.equals(PartStat.DECLINED)) {
                retval.status = CalendarObject.DECLINE;
            } else if (parameter.equals(PartStat.TENTATIVE)) {
                retval.status = CalendarObject.TENTATIVE;
            } else {
                retval.status = CalendarObject.NONE;
            }
        }
        return retval;
    }

    private class ICalParticipant {
        public String mail;
        public int status;
        public String message;
        
        public ICalParticipant(String mail, int status, String message) {
            this.mail = mail;
            this.status = status;
            this.message = message;
        }
    }
}

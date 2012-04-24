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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

import java.net.URISyntaxException;
import java.util.List;
import java.util.TimeZone;
import javax.mail.internet.AddressException;
import javax.mail.internet.IDNA;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.property.Organizer;
import org.apache.commons.logging.Log;
import com.openexchange.log.LogFactory;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.Mode;
import com.openexchange.data.conversion.ical.ical4j.internal.AbstractVerifyingAttributeConverter;
import com.openexchange.data.conversion.ical.ical4j.internal.UserResolver;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.notify.NotificationConfig;
import com.openexchange.groupware.notify.NotificationConfig.NotificationProperty;
import com.openexchange.mail.usersetting.UserSettingMailStorage;

/**
 * Test implementation to write the organizer.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class CreatedBy<T extends CalendarComponent, U extends CalendarObject> extends AbstractVerifyingAttributeConverter<T, U> {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(CreatedBy.class));

    public static UserResolver userResolver = UserResolver.EMPTY;

    @Override
    public void emit(final Mode mode, final int index, final U calendar, final T component, final List<ConversionWarning> warnings, final Context ctx, final Object... args) {
        final Organizer organizer = new Organizer();
        try {
            final String senderSource = NotificationConfig.getProperty(NotificationProperty.FROM_SOURCE, "primaryMail");
            String address;
            if (calendar.containsOrganizer()) {
                address = calendar.getOrganizer();
            } else if ("defaultSenderAddress".equals(senderSource)) {
                try {
                    address = UserSettingMailStorage.getInstance().loadUserSettingMail(calendar.getCreatedBy(), ctx).getSendAddr();
                } catch (final OXException e) {
                    LOG.error(e.getMessage(), e);
                    address = userResolver.loadUser(calendar.getCreatedBy(), ctx).getMail();
                }
            } else {
                address = userResolver.loadUser(calendar.getCreatedBy(), ctx).getMail();
            }
            address = IDNA.toACE(address);
            organizer.setValue("mailto:" + address);
        } catch (final URISyntaxException e) {
            warnings.add(new ConversionWarning(index, ConversionWarning.Code.UNEXPECTED_ERROR, e, "URI problem."));
        } catch (final OXException e) {
            warnings.add(new ConversionWarning(index, ConversionWarning.Code.UNEXPECTED_ERROR, e));
        } catch (AddressException e) {
            warnings.add(new ConversionWarning(index, ConversionWarning.Code.UNEXPECTED_ERROR, e, "Address Problem,"));
        }
        component.getProperties().add(organizer);
    }

    @Override
    public boolean hasProperty(final T component) {
        return null != component.getProperty(Property.ORGANIZER);
    }

    @Override
    public boolean isSet(final U calendar) {
        return calendar.containsOrganizer() || calendar.containsCreatedBy();
    }

    @Override
    public void parse(final int index, final T component, final U calendar, final TimeZone timeZone, final Context ctx, final List<ConversionWarning> warnings) {
        String organizer = component.getProperty(Property.ORGANIZER).getValue();
        organizer = organizer.toLowerCase().startsWith("mailto:") ? organizer.substring(7, organizer.length()) : organizer;
        organizer = IDNA.toIDN(organizer);
        calendar.setOrganizer(organizer);
    }
}

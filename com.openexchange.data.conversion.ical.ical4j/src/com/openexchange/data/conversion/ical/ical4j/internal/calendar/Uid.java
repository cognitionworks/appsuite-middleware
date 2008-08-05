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

import java.net.SocketException;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.util.UidGenerator;

import com.openexchange.data.conversion.ical.ConversionError;
import com.openexchange.data.conversion.ical.ConversionWarning;
import com.openexchange.data.conversion.ical.ConversionWarning.Code;
import com.openexchange.data.conversion.ical.ical4j.internal.AbstractVerifyingAttributeConverter;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.contexts.Context;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Uid<T extends CalendarComponent, U extends CalendarObject> extends AbstractVerifyingAttributeConverter<T, U> {

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(Uid.class);

    /**
     * Default constructor.
     */
    public Uid() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSet(final CalendarObject calendar) {
        // Must be true. Otherwise emit() is not called. 
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void emit(int index, final U calendar, final T component,
        final List<ConversionWarning> warnings, Context ctx) throws ConversionError {
        final UidGenerator generator;
        try {
            generator = new UidGenerator(String.valueOf(Thread.currentThread().getId()));
        } catch (final SocketException e) {
            LOG.error(e.getMessage(), e);
            throw new ConversionError(index, Code.CANT_GENERATE_UID, e);
        }
        component.getProperties().add(generator.generateUid());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasProperty(final CalendarComponent component) {
        return null != component.getProperty(Property.UID);
    }

    /**
     * {@inheritDoc}
     */
    public void parse(int index, final T component, final U calendar, final TimeZone timeZone,
        final Context ctx, final List<ConversionWarning> warnings) throws ConversionError {
        // Nothing to parse.
    }
}

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

package com.openexchange.data.conversion.ical;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import com.openexchange.data.conversion.ical.ical4j.ICal4JParser;
import com.openexchange.data.conversion.ical.ical4j.internal.ResourceResolver;
import com.openexchange.data.conversion.ical.ical4j.internal.UserResolver;
import com.openexchange.data.conversion.ical.ical4j.internal.calendar.Participants;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextImpl;
import com.openexchange.groupware.ldap.MockUserLookup;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.resource.Resource;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public abstract class AbstractICalParserTest {

    protected ICALFixtures fixtures;
    protected ICalParser parser;
    protected MockUserLookup users;
    protected ResourceResolver oldResourceResolver;
    protected UserResolver oldUserResolver;

    @Before
    public void setUp() throws Exception {
        fixtures = new ICALFixtures();
        users = new MockUserLookup();
        parser = new ICal4JParser();
        oldUserResolver = Participants.userResolver;
        Participants.userResolver = new UserResolver() {

            @Override
            public List<User> findUsers(final List<String> mails, final Context ctx) {
                final List<User> found = new LinkedList<User>();
                for (final String mail : mails) {
                    final User user = AbstractICalParserTest.this.users.getUserByMail(mail);
                    if (user != null) {
                        found.add(user);
                    }
                }

                return found;
            }

            @Override
            public User loadUser(final int userId, final Context ctx) throws OXException {
                return AbstractICalParserTest.this.users.getUser(userId);
            }
        };
        oldResourceResolver = Participants.resourceResolver;
        Participants.resourceResolver = new ResourceResolver() {

            private final List<Resource> resources = new ArrayList<Resource>() {

                {
                    final Resource toaster = new Resource();
                    toaster.setDisplayName("Toaster");
                    toaster.setIdentifier(1);
                    add(toaster);

                    final Resource deflector = new Resource();
                    deflector.setDisplayName("Deflector");
                    deflector.setIdentifier(2);
                    add(deflector);

                    final Resource subspaceAnomaly = new Resource();
                    subspaceAnomaly.setDisplayName("Subspace Anomaly");
                    subspaceAnomaly.setIdentifier(3);
                    add(subspaceAnomaly);
                }
            };

            @Override
            public List<Resource> find(final List<String> names, final Context ctx) throws OXException {
                final List<Resource> retval = new ArrayList<Resource>();
                for (final String name : names) {
                    for (final Resource resource : resources) {
                        if (resource.getDisplayName().equals(name)) {
                            retval.add(resource);
                        }
                    }
                }
                return retval;
            }

            @Override
            public Resource load(final int resourceId, final Context ctx) throws OXException {
                return null;
            }
        };
    }

    protected List<User> U(final int... ids) {
        final List<User> found = new LinkedList<User>();
        for (final int i : ids) {
            try {
                found.add(users.getUser(i));
            } catch (OXException e) {
                //IGNORE
            }
        }
        return found;
    }

    @After
    public void tearDown() throws Exception {
        Participants.userResolver = oldUserResolver;
        Participants.resourceResolver = oldResourceResolver;
    }

    //single task
    protected Task parseTask(final String icalText, final TimeZone defaultTZ) throws ConversionError {
        return parseTasks(icalText, defaultTZ).get(0);
    }

    protected Task parseTask(final String icalText) throws ConversionError {
        return parseTasks(icalText).get(0);
    }

    //multiple tasks
    protected List<Task> parseTasks(final String icalText, final TimeZone defaultTZ) throws ConversionError {
        return parser.parseTasks(icalText, defaultTZ, new ContextImpl(23), new ArrayList<ConversionError>(), new ArrayList<ConversionWarning>()).getImportedObjects();
    }

    protected List<Task> parseTasks(final String icalText) throws ConversionError {
        return parseTasks(icalText, TimeZone.getDefault());
    }

    protected Task taskWithRecurrence(final String recurrence, final Date start, final Date end) throws ConversionError {

        final TimeZone utc = TimeZone.getTimeZone("UTC");

        final String icalText = fixtures.vtodoWithSimpleProperties(start, end, "RRULE", recurrence);
        final Task task = parseTask(icalText, utc);

        return task;
    }

}

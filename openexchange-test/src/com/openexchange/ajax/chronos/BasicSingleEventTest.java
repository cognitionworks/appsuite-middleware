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

package com.openexchange.ajax.chronos;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.ChronosApiException;
import com.openexchange.ajax.chronos.util.AssertUtil;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.configuration.asset.Asset;
import com.openexchange.configuration.asset.AssetType;
import com.openexchange.testing.httpclient.models.ChronosAttachment;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.UpdatesResult;

/**
 *
 * {@link BasicSingleEventTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class BasicSingleEventTest extends AbstractChronosTest {

    /**
     * Initialises a new {@link BasicSingleEventTest}.
     */
    public BasicSingleEventTest() {
        super();
    }

    /**
     * Test the creation of a single event
     */
    @Test
    public void testCreateSingle() throws Exception {
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(defaultUserApi.getCalUser(), testUser.getLogin(), "testCreateSingle"));
        EventData actualEventData = eventManager.getEvent(expectedEventData.getId());
        AssertUtil.assertEventsEqual(expectedEventData, actualEventData);
    }

    /**
     * Tests the deletion of a single event
     */
    @Test
    public void testDeleteSingle() throws Exception {
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(defaultUserApi.getCalUser(), testUser.getLogin(), "testDeleteSingle"));

        EventId eventId = new EventId();
        eventId.setId(expectedEventData.getId());
        eventId.setFolderId(folderId);

        eventManager.deleteEvent(eventId);

        try {
            eventManager.getEvent(expectedEventData.getId(), true);
            fail("No exception was thrown");
        } catch (ChronosApiException e) {
            assertNotNull(e);
            assertEquals("CAL-4040", e.getErrorCode());
        }
    }

    /**
     * Tests the update of a single event
     */
    @Test
    public void testUpdateSingle() throws Exception {
        EventData initialEvent = EventFactory.createSingleTwoHourEvent(defaultUserApi.getCalUser(), testUser.getLogin(), "testUpdateSingle");
        EventData event = eventManager.createEvent(initialEvent);
        event.setEndDate(DateTimeUtil.incrementDateTimeData(event.getEndDate(), 5000));

        EventData updatedEvent = eventManager.updateEvent(event);

        assertNotEquals("The timestamp matches", event.getLastModified(), updatedEvent.getLastModified());
        assertNotEquals("The sequence matches", event.getSequence(), updatedEvent.getSequence());

        event.setLastModified(updatedEvent.getLastModified());
        event.setSequence(updatedEvent.getSequence());
        AssertUtil.assertEventsEqual(event, updatedEvent);
    }

    /**
     * Tests the retrieval of a single event
     */
    @Test
    public void testGetEvent() throws Exception {
        Date date = new Date();
        Date today = DateTimeUtil.incrementDateTimeData(TimeZone.getTimeZone("UTC"), date, 0);
        Date tomorrow = DateTimeUtil.incrementDateTimeData(TimeZone.getTimeZone("UTC"), date, 1);

        // Create a single event
        EventData event = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(defaultUserApi.getCalUser(), testUser.getLogin(), "testGetEvent"));
        EventId eventId = new EventId();
        eventId.setId(event.getId());
        eventId.setFolderId(folderId);

        // Get event directly
        EventData actualEvent = eventManager.getEvent(event.getId());
        AssertUtil.assertEventsEqual(event, actualEvent);

        // Get all events
        List<EventData> events = eventManager.getAllEvents(today, tomorrow);
        assertEquals(1, events.size());
        AssertUtil.assertEventsEqual(event, events.get(0));

        // Get updates
        UpdatesResult updatesResult = eventManager.getUpdates(date);
        assertEquals(1, updatesResult.getNewAndModified().size());
        AssertUtil.assertEventsEqual(event, updatesResult.getNewAndModified().get(0));

        // List events
        events = eventManager.listEvents(Collections.singletonList(eventId));
        assertEquals(1, events.size());
        AssertUtil.assertEventsEqual(event, events.get(0));

    }

    /**
     * Tests the creation of a single event with different timezones
     */
    @Test
    public void testCreateSingleWithDifferentTimeZones() throws Exception {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        start.setTimeInMillis(System.currentTimeMillis());

        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        end.setTimeInMillis(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(10));

        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleEvent(defaultUserApi.getCalUser(), testUser.getLogin(), "testCreateSingle", DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end)));
        EventData actualEventData = eventManager.getEvent(expectedEventData.getId());
        AssertUtil.assertEventsEqual(expectedEventData, actualEventData);
    }

    ////////////////////////////////// Attachment Tests ///////////////////////////////////

    /**
     * Tests the creation of a single event with attachment
     */
    @Test
    public void testCreateSingleWithAttachment() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);

        EventData expectedEventData = eventManager.createEventWithAttachment(EventFactory.createSingleEventWithAttachment(defaultUserApi.getCalUser(), testUser.getLogin(), "testCreateSingleWithAttachment", asset), asset);
        assertEquals("The amount of attachments is not correct", 1, expectedEventData.getAttachments().size());

        EventData actualEventData = eventManager.getEvent(expectedEventData.getId());
        AssertUtil.assertEventsEqual(expectedEventData, actualEventData);
    }

    /**
     * Tests the update of a single event with attachment
     */
    @Test
    public void testUpdateSingleWithAttachment() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);

        EventData expectedEventData = eventManager.createEventWithAttachment(EventFactory.createSingleEventWithAttachment(defaultUserApi.getCalUser(), testUser.getLogin(), "testUpdateSingleWithAttachment", asset), asset);
        assertEquals("The amount of attachments is not correct", 1, expectedEventData.getAttachments().size());

        EventData actualEventData = eventManager.getEvent(expectedEventData.getId());
        AssertUtil.assertEventsEqual(expectedEventData, actualEventData);

        asset = assetManager.getRandomAsset(AssetType.png);
        actualEventData.getAttachments().get(0).setManagedId(actualEventData.getAttachments().get(0).getManagedId());
        actualEventData.getAttachments().add(EventFactory.createAttachment(asset));

        expectedEventData = eventManager.updateEventWithAttachment(actualEventData, asset);
        assertEquals("The amount of attachments is not correct", 2, expectedEventData.getAttachments().size());

        actualEventData = eventManager.getEvent(expectedEventData.getId());
        AssertUtil.assertEventsEqual(expectedEventData, actualEventData);
    }

    /**
     * Tests the retrieval of an attachment from an event
     */
    @Test
    public void testGetAttachment() throws Exception {
        Asset asset = assetManager.getRandomAsset(AssetType.jpg);

        Path path = Paths.get(asset.getAbsolutePath());
        byte[] expectedAttachmentData = Files.readAllBytes(path);

        EventData expectedEventData = eventManager.createEventWithAttachment(EventFactory.createSingleEventWithAttachment(defaultUserApi.getCalUser(), testUser.getLogin(), "testUpdateSingleWithAttachment", asset), asset);
        assertEquals("The amount of attachments is not correct", 1, expectedEventData.getAttachments().size());

        byte[] actualAttachmentData = eventManager.getAttachment(expectedEventData.getId(), expectedEventData.getAttachments().get(0).getManagedId());
        assertArrayEquals("The attachment binary data does not match", expectedAttachmentData, actualAttachmentData);
    }

    /**
     * Tests the creation of an event with two attachments and the deletion of one of them
     * during an update call
     */
    @Test
    public void testUpdateSingleWithAttachmentsDeleteOne() throws Exception {
        Asset assetA = assetManager.getRandomAsset(AssetType.jpg);
        Asset assetB = assetManager.getRandomAsset(AssetType.png);
        List<Asset> assets = new ArrayList<>(2);
        assets.add(assetA);
        assets.add(assetB);

        EventData expectedEventData = eventManager.createEventWithAttachments(EventFactory.createSingleEventWithAttachments(defaultUserApi.getCalUser(), testUser.getLogin(), "testUpdateSingleWithAttachmentsDeleteOne", assets), assets);
        assertEquals("The amount of attachments is not correct", 2, expectedEventData.getAttachments().size());

        EventData actualEventData = eventManager.getEvent(expectedEventData.getId());
        AssertUtil.assertEventsEqual(expectedEventData, actualEventData);

        // Set the managed id for the retained attachment
        ChronosAttachment retainedAttachment = EventFactory.createAttachment(assetA);
        retainedAttachment.setManagedId(actualEventData.getAttachments().get(0).getManagedId());
        // Create the delta
        EventData updateData = new EventData();
        updateData.setId(actualEventData.getId());
        updateData.setAttachments(Collections.singletonList(retainedAttachment));

        // Update the event
        actualEventData = eventManager.updateEvent(updateData);
        // Get...
        expectedEventData = eventManager.getEvent(actualEventData.getId());
        // ...and assert
        AssertUtil.assertEventsEqual(expectedEventData, actualEventData);
    }
}

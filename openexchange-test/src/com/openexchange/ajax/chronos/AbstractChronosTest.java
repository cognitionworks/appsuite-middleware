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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.configuration.asset.AssetManager;
import com.openexchange.exception.OXException;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.FolderBody;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.FoldersVisibilityResponse;
import com.openexchange.testing.httpclient.modules.ChronosApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;

/**
 * {@link AbstractChronosTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
public class AbstractChronosTest extends AbstractAPIClientSession {

    Map<UserApi, List<EventId>> eventIds;
    Map<UserApi, List<String>> folderToDelete;
    private long lastTimeStamp;

    protected UserApi defaultUserApi;
    protected ChronosApi chronosApi;
    protected EventManager eventManager;
    protected AssetManager assetManager;
    protected String folderId;

    /**
     * Initialises a new {@link AbstractChronosTest}.
     */
    public AbstractChronosTest() {
        super();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ApiClient client = getClient();
        rememberClient(client);
        defaultUserApi = new UserApi(client, testUser);
        chronosApi = defaultUserApi.getChronosApi();
        folderId = getDefaultFolder();
        assetManager = new AssetManager();
        eventManager = new EventManager(defaultUserApi, folderId);
    }

    @Override
    public void tearDown() throws Exception {
        Exception exception = null;
        try {
            if (eventIds != null) {
                for (UserApi api : eventIds.keySet()) {
                    // FIXME: Switch to EventManager
                    api.getChronosApi().deleteEvent(api.getSession(), System.currentTimeMillis(), eventIds.get(api), null, null, false);
                }
            }
        } catch (Exception e) {
            exception = e;
        }
        if (folderToDelete != null) {
            for (UserApi api : folderToDelete.keySet()) {
                api.getFoldersApi().deleteFolders(api.getSession(), folderToDelete.get(api), "0", System.currentTimeMillis(), "event", true, true, false);
            }
        }

        // Clean-up event manager
        eventManager.cleanUp();

        super.tearDown();

        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Keeps track of the specified {@link EventId} for the specified user
     *
     * @param userApi The {@link UserApi}
     * @param eventId The {@link EventId}
     */
    protected void rememberEventId(UserApi userApi, EventId eventId) {
        if (eventIds == null) {
            eventIds = new HashMap<>();
        }
        if (!eventIds.containsKey(userApi)) {
            eventIds.put(userApi, new ArrayList<>(1));
        }
        eventIds.get(userApi).add(eventId);
    }

    /**
     * Keeps track of the specified folder for the specified user
     *
     * @param userApi The {@link UserApi}
     * @param folder The folder
     */
    protected void rememberFolder(UserApi userApi, String folder) {
        if (folderToDelete == null) {
            folderToDelete = new HashMap<>();
        }
        if (!folderToDelete.containsKey(userApi)) {
            folderToDelete.put(userApi, new ArrayList<>(1));
        }
        folderToDelete.get(userApi).add(folder);
    }

    /**
     * Creates a new folder and remembers it.
     *
     * @param api The {@link UserApi}
     * @param session The user's session
     * @param parent The parent folder
     * @param entity The user id
     * @return The result of the operation
     * @throws ApiException if an API error is occurred
     */
    protected String createAndRememberNewFolder(UserApi api, String session, String parent, int entity) throws ApiException {
        FolderPermission perm = new FolderPermission();
        perm.setEntity(entity);
        perm.setGroup(false);
        perm.setBits(403710016);

        List<FolderPermission> permissions = new ArrayList<>();
        permissions.add(perm);

        FolderData folderData = new FolderData();
        folderData.setModule("event");
        folderData.setSubscribed(true);
        folderData.setTitle("chronos_test_" + new UID().toString());
        folderData.setPermissions(permissions);

        FolderBody body = new FolderBody();
        body.setFolder(folderData);

        FolderUpdateResponse createFolder = api.getFoldersApi().createFolder(parent, session, body, "0", "calendar");
        checkResponse(createFolder.getError(), createFolder.getErrorDesc(), createFolder.getData());

        String result = createFolder.getData();
        rememberFolder(api, result);

        return result;
    }

    /**
     * Retrieves the default calendar folder of the current user
     *
     * @return The default calendar folder of the current user
     * @throws Exception if the default calendar folder cannot be found
     */
    protected String getDefaultFolder() throws Exception {
        return getDefaultFolder(defaultUserApi.getSession(), defaultUserApi.getFoldersApi());
    }

    /**
     * Retrieves the default calendar folder of the user with the specified session
     *
     * @param session The session of the user
     * @param client The {@link ApiClient}
     * @return The default calendar folder of the user
     * @throws Exception if the default calendar folder cannot be found
     */
    protected String getDefaultFolder(String session, ApiClient client) throws Exception {
        return getDefaultFolder(session, new FoldersApi(client));
    }

    /**
     * Retrieves the default calendar folder of the user with the specified session
     *
     * @param session The session of the user
     * @param foldersApi The {@link FoldersApi}
     * @return The default calendar folder of the user
     * @throws Exception if the default calendar folder cannot be found
     */
    @SuppressWarnings("unchecked")
    private String getDefaultFolder(String session, FoldersApi foldersApi) throws Exception {
        FoldersVisibilityResponse visibleFolders = foldersApi.getVisibleFolders(session, "event", "1,308", "0");
        if (visibleFolders.getError() != null) {
            throw new OXException(new Exception(visibleFolders.getErrorDesc()));
        }

        Object privateFolders = visibleFolders.getData().getPrivate();
        ArrayList<ArrayList<?>> privateList = (ArrayList<ArrayList<?>>) privateFolders;
        if (privateList.size() == 1) {
            return (String) privateList.get(0).get(0);
        } else {
            for (ArrayList<?> folder : privateList) {
                if ((Boolean) folder.get(1)) {
                    return (String) folder.get(0);
                }
            }
        }
        throw new Exception("Unable to find default calendar folder!");
    }

    /**
     * Sets the last timestamp
     *
     * @param timestamp the last timestamp to set
     */
    protected void setLastTimestamp(long timestamp) {
        this.lastTimeStamp = timestamp;
    }

    /**
     * Gets the last timestamp
     *
     * @return the last timestamp
     */
    protected long getLastTimestamp() {
        return lastTimeStamp;
    }

    /**
     * Changes the timezone of the default user to the given value
     *
     * @param tz The new timezone
     * @throws ApiException
     */
    protected void changeTimezone(TimeZone tz) throws ApiException {
        String body = "{timezone: \"" + tz.getID() + "\"}";
        CommonResponse updateJSlob = defaultUserApi.getJslob().updateJSlob(defaultUserApi.getSession(), body, "io.ox/core", null);
        assertNull(updateJSlob.getErrorDesc(), updateJSlob.getError());
    }

    /**
     * Checks if a response doesn't contain any errors
     *
     * @param error The error element of the response
     * @param errorDesc The error description element of the response
     * @param data The data element of the response
     * @return The data
     */
    protected <T> T checkResponse(String error, String errorDesc, T data) {
        assertNull(errorDesc, error);
        assertNotNull(data);
        return data;
    }

    /**
     * Handles the result response of an event creation
     *
     * @param createEvent The result
     * @return The created event
     */
    protected EventData handleCreation(ChronosCalendarResultResponse createEvent) {
        CalendarResult result = checkResponse(createEvent.getError(), createEvent.getErrorDesc(), createEvent.getData());
        assertEquals("Found unexpected conflicts", 0, result.getConflicts().size());
        EventData event = result.getCreated().get(0);
        EventId eventId = new EventId();
        eventId.setId(event.getId());
        eventId.setFolderId(event.getFolder());
        rememberEventId(defaultUserApi, eventId);
        this.setLastTimestamp(createEvent.getTimestamp());
        return event;
    }
}

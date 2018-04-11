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

package com.openexchange.ajax.infostore.apiclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.modules.Module;
import com.openexchange.junit.Assert;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.InfoItemData;
import com.openexchange.testing.httpclient.models.InfoItemResponse;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.modules.ConfigApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.InfostoreApi;
import com.openexchange.tools.io.IOTools;
import net.fortuna.ical4j.model.Date;

public class InfostoreApiClientTest extends AbstractAPIClientSession {

    protected static final int[] virtualFolders = { FolderObject.SYSTEM_INFOSTORE_FOLDER_ID, FolderObject.VIRTUAL_LIST_INFOSTORE_FOLDER_ID, FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID };

    public static final String INFOSTORE_FOLDER = "infostore.folder";

    protected String folderId;

    protected String hostName = null;

    private String privateInfostoreFolder;

    protected InfostoreApi infostoreApi;

    private Long timestamp = null;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.folderId = createFolderForTest();
        infostoreApi = new InfostoreApi(getApiClient());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (folderId != null) {
            FoldersApi folderApi = new FoldersApi(getApiClient());
            folderApi.deleteFolders(getApiClient().getSession(), java.util.Collections.singletonList(folderId), "1", new Date().getTime(), null, true, true, false);
        }
        super.tearDown();
    }

    protected String uploadInfoItem(File file, String mimeType) throws ApiException, FileNotFoundException, IOException {
        return uploadInfoItem(null, file, mimeType, null);
    }

    protected String uploadInfoItem(String id, File file, String mimeType, String versionComment) throws ApiException, FileNotFoundException, IOException {
        byte[] bytes = IOTools.getBytes(new FileInputStream(file));
        return uploadInfoItem(id, file, mimeType, versionComment, bytes, null, null);
    }

    protected String uploadInfoItem(String id, File file, String mimeType, String versionComment, byte[] bytes, Long offset, Long filesize) throws ApiException, FileNotFoundException, IOException {

InfoItemUpdateResponse uploadInfoItem = infostoreApi.uploadInfoItem(getApiClient().getSession(),
                                                                    folderId,
                                                                    file.getName(),
                                                                    bytes,
                                                                    filesize,
                                                                    id,
                                                                    file.getName(),
                                                                    mimeType,
                                                                    null,
                                                                    null,
                                                                    null,
                                                                    null,
                                                                    versionComment,
                                                                    null,
                                                                    null,
                                                                    filesize == null ? Long.valueOf(bytes.length) : filesize,
                                                                    false,
                                                                    false,
                                                                    offset);
        Assert.assertNull(uploadInfoItem.getError());
        Assert.assertNotNull(uploadInfoItem.getData());
        timestamp = uploadInfoItem.getTimestamp();
        return uploadInfoItem.getData();
    }


    protected InfoItemData getItem(String id) throws ApiException {
        InfoItemResponse infoItem = infostoreApi.getInfoItem(getApiClient().getSession(), id, folderId);
        Assert.assertNull(infoItem.getError());
        Assert.assertNotNull(infoItem.getData());
        return infoItem.getData();
    }

    private String createFolderForTest() throws ApiException {
        final String parent = getPrivateInfostoreFolder();
        FoldersApi folderApi = new FoldersApi(getApiClient());
        NewFolderBody body = new NewFolderBody();
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setModule(Module.INFOSTORE.getName());
        folder.setSummary("NewInfostoreFolder" + UUID.randomUUID().toString());
        folder.setTitle(folder.getSummary());
        folder.setSubscribed(true);
        folder.setPermissions(null);
        body.setFolder(folder);
        FolderUpdateResponse folderUpdateResponse = folderApi.createFolder(parent, getApiClient().getSession(), body, "1", null);
        return checkResponse(folderUpdateResponse);
    }

    public String getPrivateInfostoreFolder() throws ApiException {
        if (null == privateInfostoreFolder) {
            ConfigApi configApi = new ConfigApi(getApiClient());
            ConfigResponse configNode = configApi.getConfigNode(Tree.PrivateInfostoreFolder.getPath(), getApiClient().getSession());
            Object data = checkResponse(configNode);
            privateInfostoreFolder = String.valueOf(data);

        }
        return privateInfostoreFolder;
    }

    private Object checkResponse(ConfigResponse resp) {
        Assert.assertNull(resp.getErrorDesc(), resp.getError());
        Assert.assertNotNull(resp.getData());
        return resp.getData();
    }


    private String checkResponse(FolderUpdateResponse resp) {
        Assert.assertNull(resp.getErrorDesc(), resp.getError());
        Assert.assertNotNull(resp.getData());
        return resp.getData();
    }

}

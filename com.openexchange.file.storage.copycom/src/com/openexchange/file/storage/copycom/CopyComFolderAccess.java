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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.file.storage.copycom;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.copy.api.Folder;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStorageFolderAccess;
import com.openexchange.file.storage.Quota;
import com.openexchange.file.storage.Quota.Type;
import com.openexchange.file.storage.copycom.access.CopyComAccess;
import com.openexchange.session.Session;

/**
 * {@link CopyComFolderAccess}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CopyComFolderAccess extends AbstractCopyComResourceAccess implements FileStorageFolderAccess {

    private final CopyComAccountAccess accountAccess;
    final int userId;
    final String accountDisplayName;

    /**
     * Initializes a new {@link CopyComFolderAccess}.
     */
    public CopyComFolderAccess(final CopyComAccess copyComAccess, final FileStorageAccount account, final Session session, final CopyComAccountAccess accountAccess) {
        super(copyComAccess, account, session);
        this.accountAccess = accountAccess;
        userId = session.getUserId();
        accountDisplayName = account.getDisplayName();
    }

    static final Set<String> TYPES_FOLDER = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("dir", "copy", "root")));

    protected CopyComFolder parseFolder(Folder restFolder) throws OXException {
        return new CopyComFolder(userId).parseDirEntry(restFolder, rootFolderId, accountDisplayName);
    }

    protected CopyComFolder parseFolder(JSONObject jFolder) throws OXException {
        return new CopyComFolder(userId).parseDirEntry(jFolder, rootFolderId, accountDisplayName);
    }

    @Override
    public boolean exists(final String folderId) throws OXException {
        return perform(new CopyComClosure<Boolean>() {

            @Override
            protected Boolean doPerform(CopyComAccess access) throws OXException, JSONException, IOException {
                HttpRequestBase request = null;
                try {
                    HttpGet method = new HttpGet(buildUri("meta/" + toCopyComFolderId(folderId), null));
                    request = method;
                    access.sign(request);

                    HttpResponse response = access.getHttpClient().execute(method);
                    return Boolean.valueOf(200 == response.getStatusLine().getStatusCode());
                } catch (HttpResponseException e) {
                    if (404 == e.getStatusCode()) {
                        return Boolean.FALSE;
                    }
                    throw e;
                } finally {
                    reset(request);
                }

            }

        }).booleanValue();
    }

    @Override
    public FileStorageFolder getFolder(final String folderId) throws OXException {
        return perform(new CopyComClosure<FileStorageFolder>() {

            @Override
            protected FileStorageFolder doPerform(CopyComAccess access) throws OXException, JSONException, IOException {
                HttpRequestBase request = null;
                try {
                    String fid = toCopyComFolderId(folderId);
                    HttpGet method = new HttpGet(buildUri("meta/"+fid, null));
                    request = method;
                    access.sign(request);

                    Folder restFolder = handleHttpResponse(access.getHttpClient().execute(method), Folder.class);
                    return parseFolder(restFolder);
                } finally {
                    if (null != request) {
                        request.releaseConnection();
                    }
                }
            }
        });
    }

    @Override
    public FileStorageFolder getPersonalFolder() throws OXException {
        throw FileStorageExceptionCodes.NO_SUCH_FOLDER.create();
    }

    @Override
    public FileStorageFolder getTrashFolder() throws OXException {
        throw FileStorageExceptionCodes.NO_SUCH_FOLDER.create();
    }

    @Override
    public FileStorageFolder getPicturesFolder() throws OXException {
        throw FileStorageExceptionCodes.NO_SUCH_FOLDER.create();
    }

    @Override
    public FileStorageFolder getMusicFolder() throws OXException {
        throw FileStorageExceptionCodes.NO_SUCH_FOLDER.create();
    }

    @Override
    public FileStorageFolder getDocumentsFolder() throws OXException {
        throw FileStorageExceptionCodes.NO_SUCH_FOLDER.create();
    }

    @Override
    public FileStorageFolder getVideosFolder() throws OXException {
        throw FileStorageExceptionCodes.NO_SUCH_FOLDER.create();
    }

    @Override
    public FileStorageFolder getTemplatesFolder() throws OXException {
        throw FileStorageExceptionCodes.NO_SUCH_FOLDER.create();
    }

    @Override
    public FileStorageFolder[] getPublicFolders() throws OXException {
        return new FileStorageFolder[0];
    }

    @Override
    public FileStorageFolder[] getSubfolders(final String parentIdentifier, final boolean all) throws OXException {
        return perform(new CopyComClosure<FileStorageFolder[]>() {

            @Override
            protected FileStorageFolder[] doPerform(CopyComAccess access) throws OXException, JSONException, IOException {
                HttpRequestBase request = null;
                try {
                    String fid = toCopyComFolderId(parentIdentifier);
                    List<FileStorageFolder> folders = new LinkedList<FileStorageFolder>();

                    {
                        HttpGet method = new HttpGet(buildUri("meta/" + fid, null));
                        request = method;
                        access.sign(request);

                        JSONObject jResponse = handleHttpResponse(access.getHttpClient().execute(method), JSONObject.class);
                        JSONArray jChildren = jResponse.getJSONArray("children");

                        int length = jChildren.length();
                        for (int i = 0; i < length; i++) {
                            JSONObject jItem = jChildren.getJSONObject(i);
                            if (TYPES_FOLDER.contains(jItem.optString("type", null))) {
                                folders.add(new CopyComFolder(userId).parseDirEntry(jItem, rootFolderId, accountDisplayName));
                            }
                        }
                        reset(request);
                        request = null;
                    }

                    return folders.toArray(new FileStorageFolder[folders.size()]);
                } finally {
                    reset(request);
                }
            }
        });
    }

    @Override
    public FileStorageFolder getRootFolder() throws OXException {
        return getFolder(FileStorageFolder.ROOT_FULLNAME);
    }

    @Override
    public String createFolder(final FileStorageFolder toCreate) throws OXException {
        return perform(new CopyComClosure<String>() {

            @Override
            protected String doPerform(CopyComAccess access) throws OXException, JSONException, IOException {
                HttpRequestBase request = null;
                try {
                    HttpPost method = new HttpPost(buildUri("files/" + toCopyComFolderId(toCreate.getParentId()) + "/" + toCreate.getName(), null));
                    request = method;
                    access.sign(request);

                    JSONObject jResponse = handleHttpResponse(access.getHttpClient().execute(method), JSONObject.class);
                    return jResponse.getString("id");
                } finally {
                    reset(request);
                }
            }
        });
    }

    @Override
    public String updateFolder(String identifier, FileStorageFolder toUpdate) throws OXException {
        // Neither support for subscription nor permissions
        return identifier;
    }

    @Override
    public String moveFolder(String folderId, String newParentId) throws OXException {
        return moveFolder(folderId, newParentId, null);
    }

    @Override
    public String moveFolder(final String folderId, final String newParentId, final String newName) throws OXException {
        return perform(new CopyComClosure<String>() {

            @Override
            protected String doPerform(CopyComAccess access) throws OXException, JSONException, IOException {
                HttpRequestBase request = null;
                try {
                    String fid = toCopyComFolderId(folderId);
                    String name = newName;
                    if (null == name) {
                        int pos = fid.lastIndexOf('/');
                        name = fid.substring(pos+1);
                    }

                    List<NameValuePair> qparams = new LinkedList<NameValuePair>();
                    String newPath = newParentId + "/" + name;
                    qparams.add(new BasicNameValuePair("path", newPath));
                    qparams.add(new BasicNameValuePair("overwrite", "true"));
                    HttpPut method = new HttpPut(buildUri("files/" + fid, null));
                    request = method;
                    access.sign(request);

                    handleHttpResponse(access.getHttpClient().execute(method), Void.class);
                    reset(request);
                    request = null;

                    return newPath;
                } catch (HttpResponseException e) {
                    throw handleHttpResponseError(folderId, e);
                } finally {
                    reset(request);
                }
            }
        });
    }

    @Override
    public String renameFolder(final String folderId, final String newName) throws OXException {
        return perform(new CopyComClosure<String>() {

            @Override
            protected String doPerform(CopyComAccess access) throws OXException, JSONException, IOException {
                HttpRequestBase request = null;
                try {
                    List<NameValuePair> qparams = new LinkedList<NameValuePair>();
                    qparams.add(new BasicNameValuePair("name", newName));
                    String fid = toCopyComFolderId(folderId);
                    HttpPut method = new HttpPut(buildUri("files/" + fid, null));
                    request = method;
                    access.sign(request);

                    handleHttpResponse(access.getHttpClient().execute(method), Void.class);
                    reset(request);
                    request = null;

                    int pos = fid.lastIndexOf('/');
                    String parentId = pos > 0 ? fid.substring(pos, pos) : "";

                    return parentId + "/" + newName;
                } catch (HttpResponseException e) {
                    throw handleHttpResponseError(folderId, e);
                } finally {
                    reset(request);
                }
            }
        });
    }

    @Override
    public String deleteFolder(String folderId) throws OXException {
        return deleteFolder(folderId, false);
    }

    @Override
    public String deleteFolder(final String folderId, boolean hardDelete) throws OXException {
        return perform(new CopyComClosure<String>() {

            @Override
            protected String doPerform(CopyComAccess access) throws OXException, JSONException, IOException {
                HttpRequestBase request = null;
                try {

                    HttpDelete method = new HttpDelete(buildUri("files/" + toCopyComFolderId(folderId),null));
                    request = method;
                    access.sign(request);

                    handleHttpResponse(access.getHttpClient().execute(method), STATUS_CODE_POLICY_IGNORE_NOT_FOUND, Void.class);
                    reset(request);
                    request = null;

                    return folderId;
                } catch (HttpResponseException e) {
                    throw handleHttpResponseError(folderId, e);
                } finally {
                    reset(request);
                }
            }
        });
    }

    @Override
    public void clearFolder(String folderId) throws OXException {
        clearFolder(folderId, false);
    }

    @Override
    public void clearFolder(final String folderId, boolean hardDelete) throws OXException {
        perform(new CopyComClosure<Void>() {

            @Override
            protected Void doPerform(CopyComAccess access) throws OXException, JSONException, IOException {
                HttpRequestBase request = null;
                try {
                    String fid = toCopyComFolderId(folderId);
                    List<String> list = new LinkedList<String>();

                    {
                        HttpGet method = new HttpGet(buildUri("meta/" + fid, null));
                        request = method;
                        access.sign(request);

                        JSONObject jResponse = handleHttpResponse(access.getHttpClient().execute(method), JSONObject.class);
                        JSONArray jChildren = jResponse.getJSONArray("children");

                        int length = jChildren.length();
                        for (int i = 0; i < length; i++) {
                            JSONObject jItem = jChildren.getJSONObject(i);
                            list.add(jItem.optString("id", null));
                        }
                        reset(request);
                        request = null;
                    }

                    for (String id : list) {
                        HttpDelete method = new HttpDelete(buildUri("files/" + id,null));
                        request = method;
                        access.sign(request);

                        handleHttpResponse(access.getHttpClient().execute(method), STATUS_CODE_POLICY_IGNORE_NOT_FOUND, Void.class);
                        reset(request);
                        request = null;
                    }

                    return null;
                } finally {
                    reset(request);
                }
            }
        });
    }

    @Override
    public FileStorageFolder[] getPath2DefaultFolder(final String folderId) throws OXException {
        return perform(new CopyComClosure<FileStorageFolder[]>() {

            @Override
            protected FileStorageFolder[] doPerform(CopyComAccess access) throws OXException, JSONException, IOException {

                List<FileStorageFolder> list = new LinkedList<FileStorageFolder>();

                String fid = folderId;
                FileStorageFolder f = getFolder(fid);
                list.add(f);

                while (!FileStorageFolder.ROOT_FULLNAME.equals(fid)) {
                    fid = f.getParentId();
                    f = getFolder(fid);
                    list.add(f);
                }

                return list.toArray(new FileStorageFolder[list.size()]);
            }
        });
    }

    @Override
    public Quota getStorageQuota(final String folderId) throws OXException {
        return Type.STORAGE.getUnlimited();
    }

    @Override
    public Quota getFileQuota(final String folderId) throws OXException {
        return Type.FILE.getUnlimited();
    }

    @Override
    public Quota[] getQuotas(final String folder, final Type[] types) throws OXException {
        final Quota[] ret = new Quota[types.length];
        for (int i = 0; i < types.length; i++) {
            ret[i] = types[i].getUnlimited();
        }
        return ret;
    }

}

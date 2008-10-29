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

package com.openexchange.ajax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.ajax.config.ConfigTools;
import com.openexchange.ajax.fields.FolderFields;
import com.openexchange.ajax.folder.FolderTools;
import com.openexchange.ajax.folder.actions.GetRequest;
import com.openexchange.ajax.folder.actions.GetResponse;
import com.openexchange.ajax.folder.actions.ListRequest;
import com.openexchange.ajax.folder.actions.ListResponse;
import com.openexchange.ajax.folder.actions.UpdatesRequest;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AJAXSession;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.parser.FolderParser;
import com.openexchange.api2.OXException;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.test.TestException;
import com.openexchange.tools.URLParameter;
import com.openexchange.tools.servlet.AjaxException;

public class FolderTest extends AbstractAJAXTest {

	private String sessionId;

	public FolderTest(final String name) {
		super(name);
	}

	public static final String FOLDER_URL = "/ajax/folders";

	private static final String URL_ENCODED_COMMA = "%2C";

	private static String getCommaSeperatedIntegers(final int[] intArray) {
		final StringBuffer sb = new StringBuffer();
		for (int i = 0; i < intArray.length - 1; i++) {
			sb.append(intArray[i]);
			sb.append(',');
		}
		sb.append(intArray[intArray.length - 1]);
		return sb.toString();
	}

	private static int[] parsePermissionBits(int bits) {
		final int[] retval = new int[5];
		for (int i = retval.length - 1; i >= 0; i--) {
			final int exponent = (i * 7); // Number of bits to be shifted
			retval[i] = bits >> exponent;
			bits -= (retval[i] << exponent);
			if (retval[i] == Folder.MAX_PERMISSION) {
				retval[i] = OCLPermission.ADMIN_PERMISSION;
			} else if (i < (retval.length - 1)) {
				retval[i] = mapping_01[retval[i]];
			} else {
				retval[i] = retval[i];
			}
		}
		return retval;
	}

	private static final int[] mapping_01 = { 0, 2, 4, -1, 8 };

	/**
	 * @deprecated use {@link ConfigTools#getUserId(WebConversation, String, String)}.
	 */
	@Deprecated
	public static final int getUserId(final WebConversation conversation, final String hostname,
			final String entityArg, final String password) throws IOException, SAXException, JSONException,
			AjaxException, ConfigurationException {
		final String sessionId = LoginTest.getSessionId(conversation, hostname, entityArg, password);
		return ConfigTools.getUserId(conversation, hostname, sessionId);
	}

	public static List<FolderObject> getRootFolders(final WebConversation conversation, final String hostname,
			final String sessionId, final boolean printOutput) throws MalformedURLException, IOException, SAXException,
			JSONException, OXException {
		return getRootFolders(conversation, null, hostname, sessionId, printOutput);
	}
	
	public static List<FolderObject> getRootFolders(final WebConversation conversation, final String protocol, final String hostname,
			final String sessionId, final boolean printOutput) throws MalformedURLException, IOException, SAXException,
			JSONException, OXException {
		final WebRequest req = new GetMethodWebRequest(((null == protocol) ? PROTOCOL : (protocol + "://")) + hostname + FOLDER_URL);
		req.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
		req.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_ROOT);
		final String columns = FolderObject.OBJECT_ID + "," + FolderObject.MODULE + "," + FolderObject.FOLDER_NAME + ","
				+ FolderObject.SUBFOLDERS;
		req.setParameter(AJAXServlet.PARAMETER_COLUMNS, columns);
		final WebResponse resp = conversation.getResponse(req);
		final List<FolderObject> folders = new ArrayList<FolderObject>();
		final JSONObject respObj = new JSONObject(resp.getText());
		final JSONArray arr = respObj.getJSONArray("data");
		if (printOutput) {
			System.out.println("data: " + arr.toString());
		}
		for (int i = 0; i < arr.length(); i++) {
			final JSONArray nestedArr = arr.getJSONArray(i);
			final FolderObject rootFolder = new FolderObject();
			rootFolder.setObjectID(nestedArr.getInt(0));
			rootFolder.setModule(FolderParser.getModuleFromString(nestedArr.getString(1), nestedArr.getInt(0)));
			rootFolder.setFolderName(nestedArr.getString(2));
			rootFolder.setSubfolderFlag(nestedArr.getBoolean(3));
			folders.add(rootFolder);
		}
		return folders;
	}

	public static List<FolderObject> getSubfolders(final WebConversation conversation, final String hostname,
			final String sessionId, final String parentIdentifier, final boolean printOutput)
			throws MalformedURLException, IOException, SAXException, JSONException, OXException, AjaxException {
		return getSubfolders(conversation, null, hostname, sessionId, parentIdentifier, printOutput);
	}
	
	public static List<FolderObject> getSubfolders(final WebConversation conversation, final String protocol, final String hostname,
			final String sessionId, final String parentIdentifier, final boolean printOutput)
			throws MalformedURLException, IOException, SAXException, JSONException, OXException, AjaxException {
		return getSubfolders(conversation, protocol, hostname, sessionId, parentIdentifier, false, false);
	}
	
	public static List<FolderObject> getSubfolders(final WebConversation conversation, final String hostname,
			final String sessionId, final String parentIdentifier, final boolean printOutput, final boolean ignoreMailfolder)
			throws MalformedURLException, IOException, SAXException, JSONException, OXException, AjaxException {
		return getSubfolders(conversation, null, hostname, sessionId, parentIdentifier, printOutput, ignoreMailfolder);
	}

	public static List<FolderObject> getSubfolders(final WebConversation conversation, final String protocol, final String hostname,
			final String sessionId, final String parentIdentifier, final boolean printOutput, final boolean ignoreMailfolder)
			throws MalformedURLException, IOException, SAXException, JSONException, OXException, AjaxException {
	    final AJAXSession session = new AJAXSession(conversation, sessionId);
	    return FolderTools.getSubFolders(session, protocol, hostname, parentIdentifier, ignoreMailfolder);
	}

	public static FolderObject getFolder(final WebConversation conversation, final String hostname,
			final String sessionId, final String folderIdentifier, final Calendar timestamp, final boolean printOutput)
			throws MalformedURLException, IOException, SAXException, JSONException, OXException {
		return getFolder(conversation, null, hostname, sessionId, folderIdentifier, timestamp, printOutput);
	}
	
	public static FolderObject getFolder(final WebConversation conversation, final String protocol, final String hostname,
			final String sessionId, final String folderIdentifier, final Calendar timestamp, final boolean printOutput)
			throws MalformedURLException, IOException, SAXException, JSONException, OXException {
		final WebRequest req = new GetMethodWebRequest(((null == protocol) ? PROTOCOL : (protocol + "://")) + hostname + FOLDER_URL);
		req.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
		req.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_GET);
		req.setParameter(AJAXServlet.PARAMETER_ID, folderIdentifier);
		req.setParameter(AJAXServlet.PARAMETER_COLUMNS, getCommaSeperatedIntegers(new int[] { FolderObject.OBJECT_ID,
				FolderObject.FOLDER_NAME, FolderObject.OWN_RIGHTS, FolderObject.PERMISSIONS_BITS }));
		final WebResponse resp = conversation.getResponse(req);
		final JSONObject respObj = new JSONObject(resp.getText());
		if (respObj.has("error") && !respObj.isNull("error")) {
			throw new OXException("Error occured: " + respObj.getString("error"));
		}
		if (!respObj.has("data") || respObj.isNull("data")) {
			throw new OXException("Error occured: Missing key \"data\"");
		}
		final JSONObject jsonFolder = respObj.getJSONObject("data");
		final FolderObject fo = new FolderObject();
		try {
			fo.setObjectID(jsonFolder.getInt("id"));
		} catch (final JSONException exc) {
			fo.removeObjectID();
			fo.setFullName(jsonFolder.getString("id"));
		}
		if (!jsonFolder.isNull("created_by")) {
			fo.setCreatedBy(jsonFolder.getInt("created_by"));
		}

		if (!jsonFolder.isNull("creation_date")) {
			fo.setCreationDate(new Date(jsonFolder.getLong("creation_date")));
		}
		fo.setFolderName(jsonFolder.getString("title"));

		if (!jsonFolder.isNull("module")) {
			fo.setModule(FolderParser.getModuleFromString(jsonFolder.getString("module"), fo.containsObjectID() ? fo
					.getObjectID() : -1));
		}

		if (jsonFolder.has(FolderFields.PERMISSIONS) && !jsonFolder.isNull(FolderFields.PERMISSIONS)) {
			final JSONArray jsonArr = jsonFolder.getJSONArray(FolderFields.PERMISSIONS);
			final OCLPermission[] perms = new OCLPermission[jsonArr.length()];
			for (int i = 0; i < jsonArr.length(); i++) {
				final JSONObject elem = jsonArr.getJSONObject(i);
				int entity;
				entity = elem.getInt(FolderFields.ENTITY);
				final OCLPermission oclPerm = new OCLPermission();
				oclPerm.setEntity(entity);
				if (fo.containsObjectID()) {
					oclPerm.setFuid(fo.getObjectID());
				}
				final int[] permissionBits = parsePermissionBits(elem.getInt(FolderFields.BITS));
				if (!oclPerm.setAllPermission(permissionBits[0], permissionBits[1], permissionBits[2],
						permissionBits[3])) {
					throw new OXException("Invalid permission values: fp=" + permissionBits[0] + " orp="
							+ permissionBits[1] + " owp=" + permissionBits[2] + " odp=" + permissionBits[3]);
				}
				oclPerm.setFolderAdmin(permissionBits[4] > 0 ? true : false);
				oclPerm.setGroupPermission(elem.getBoolean(FolderFields.GROUP));
				perms[i] = oclPerm;
			}
			fo.setPermissionsAsArray(perms);
		}

		if (respObj.has("timestamp") && !respObj.isNull("timestamp")) {
			timestamp.setTimeInMillis(respObj.getLong("timestamp"));
		}
		if (printOutput) {
			System.out.println(respObj.toString());
		}
		return fo;
	}
	
	public static int insertFolder(final WebConversation conversation, final String hostname, final String sessionId,
			final int entityId, final boolean isGroup, final int[] permsArr, final boolean isAdmin,
			final int parentFolderId, final String folderName, final String moduleStr, final int type,
			final int sharedForUserId, final int[] sharedPermsArr, final boolean sharedIsAdmin,
			final boolean printOutput) throws JSONException, MalformedURLException, IOException, SAXException,
			OXException {
		return insertFolder(conversation, null, hostname, sessionId, entityId, isGroup, parentFolderId, folderName, moduleStr, type, sharedForUserId, printOutput);
	}

	public static int insertFolder(final WebConversation conversation, final String protocol, final String hostname, final String sessionId,
			final int entityId, final boolean isGroup, final int[] permsArr, final boolean isAdmin,
			final int parentFolderId, final String folderName, final String moduleStr, final int type,
			final int sharedForUserId, final int[] sharedPermsArr, final boolean sharedIsAdmin,
			final boolean printOutput) throws JSONException, MalformedURLException, IOException, SAXException,
			OXException {
		final JSONObject jsonFolder = new JSONObject();
		jsonFolder.put("title", folderName);
		final JSONArray perms = new JSONArray();
		JSONObject jsonPermission = new JSONObject();
		jsonPermission.put("entity", entityId);
		jsonPermission.put("group", isGroup);
		jsonPermission.put("bits", createPermissionBits(permsArr[0], permsArr[1], permsArr[2], permsArr[3], isAdmin));
		perms.put(jsonPermission);
		if (sharedForUserId != -1) {
			jsonPermission = new JSONObject();
			jsonPermission.put("entity", sharedForUserId);
			jsonPermission.put("group", false);
			jsonPermission.put("bits", createPermissionBits(sharedPermsArr[0], sharedPermsArr[1], sharedPermsArr[2],
					sharedPermsArr[3], sharedIsAdmin));
			perms.put(jsonPermission);
		}
		jsonFolder.put("permissions", perms);
		jsonFolder.put("module", moduleStr);
		jsonFolder.put("type", type);
		final URLParameter urlParam = new URLParameter();
		urlParam.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_NEW);
		urlParam.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
		urlParam.setParameter(FolderFields.FOLDER_ID, String.valueOf(parentFolderId));
		final byte[] bytes = jsonFolder.toString().getBytes("UTF-8");
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		final WebRequest req = new PutMethodWebRequest(((null == protocol) ? PROTOCOL : (protocol + "://")) + hostname + FOLDER_URL + urlParam.getURLParameters(),
				bais, "text/javascript; charset=UTF-8");
		final WebResponse resp = conversation.getResponse(req);
		final JSONObject respObj = new JSONObject(resp.getText());
		if (printOutput) {
			System.out.println(respObj.toString());
		}
		if (!respObj.has("data") || respObj.has("error")) {
			throw new OXException("Folder Insert failed"
					+ (respObj.has("error") ? (": " + respObj.getString("error")) : ""));
		}
		return respObj.getInt("data");
	}
	
	public static int insertFolder(final WebConversation conversation, final String hostname, final String sessionId,
			final int entityId, final boolean isGroup, final int parentFolderId, final String folderName,
			final String moduleStr, final int type, final int sharedForUserId, final boolean printOutput)
			throws JSONException, MalformedURLException, IOException, SAXException, OXException {
		return insertFolder(conversation, null, hostname, sessionId, entityId, isGroup,
				parentFolderId, folderName, moduleStr, type, sharedForUserId, printOutput);
	}

	public static int insertFolder(final WebConversation conversation, final String protocol, final String hostname, final String sessionId,
			final int entityId, final boolean isGroup, final int parentFolderId, final String folderName,
			final String moduleStr, final int type, final int sharedForUserId, final boolean printOutput)
			throws JSONException, MalformedURLException, IOException, SAXException, OXException {
		final JSONObject jsonFolder = new JSONObject();
		jsonFolder.put("title", folderName);
		final JSONArray perms = new JSONArray();
		JSONObject jsonPermission = new JSONObject();
		jsonPermission.put("entity", entityId);
		jsonPermission.put("group", isGroup);
		jsonPermission.put("bits", createPermissionBits(8, 8, 8, 8, true));
		perms.put(jsonPermission);
		if (sharedForUserId != -1) {
			jsonPermission = new JSONObject();
			jsonPermission.put("entity", sharedForUserId);
			jsonPermission.put("group", false);
			jsonPermission.put("bits", createPermissionBits(OCLPermission.CREATE_OBJECTS_IN_FOLDER, 4, 0, 0, false));
			perms.put(jsonPermission);
		}
		jsonFolder.put("permissions", perms);
		jsonFolder.put("module", moduleStr);
		jsonFolder.put("type", type);
		final URLParameter urlParam = new URLParameter();
		urlParam.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_NEW);
		urlParam.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
		urlParam.setParameter(FolderFields.FOLDER_ID, String.valueOf(parentFolderId));
		final byte[] bytes = jsonFolder.toString().getBytes("UTF-8");
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		final WebRequest req = new PutMethodWebRequest(((null == protocol) ? PROTOCOL : (protocol + "://")) + hostname + FOLDER_URL + urlParam.getURLParameters(),
				bais, "text/javascript; charset=UTF-8");
		final WebResponse resp = conversation.getResponse(req);
		final JSONObject respObj = new JSONObject(resp.getText());
		if (printOutput) {
			System.out.println(respObj.toString());
		}
		if (!respObj.has("data") || respObj.has("error")) {
			throw new OXException("Folder Insert failed"
					+ (respObj.has("error") ? (": " + respObj.getString("error")) : ""));
		}
		return respObj.getInt("data");
	}
	
	public static boolean renameFolder(final WebConversation conversation, final String hostname,
			final String sessionId, final int folderId, final String folderName, final String moduleStr,
			final int type, final long timestamp, final boolean printOutput) throws JSONException,
			MalformedURLException, IOException, SAXException {
		return renameFolder(conversation, null, hostname, sessionId, folderId,
				folderName, moduleStr, type, timestamp, printOutput);
	}

	public static boolean renameFolder(final WebConversation conversation, final String protocol, final String hostname,
			final String sessionId, final int folderId, final String folderName, final String moduleStr,
			final int type, final long timestamp, final boolean printOutput) throws JSONException,
			MalformedURLException, IOException, SAXException {
		final JSONObject jsonFolder = new JSONObject();
		jsonFolder.put("id", folderId);
		jsonFolder.put("title", folderName);
		jsonFolder.put("module", moduleStr);
		jsonFolder.put("type", type);
		final URLParameter urlParam = new URLParameter();
		urlParam.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_UPDATE);
		urlParam.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
		urlParam.setParameter(AJAXServlet.PARAMETER_ID, String.valueOf(folderId));
		urlParam.setParameter("timestamp", String.valueOf(timestamp));
		final byte[] bytes = jsonFolder.toString().getBytes("UTF-8");
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		final WebRequest req = new PutMethodWebRequest(((null == protocol) ? PROTOCOL : (protocol + "://")) + hostname + FOLDER_URL + urlParam.getURLParameters(),
				bais, "text/javascript; charset=UTF-8");
		final WebResponse resp = conversation.getResponse(req);
		final JSONObject respObj = new JSONObject(resp.getText());
		if (printOutput) {
			System.out.println(respObj.toString());
		}
		if (respObj.has("error")) {
			return false;
		}
		return true;
	}
	
	public static boolean updateFolder(final WebConversation conversation, final String hostname,
			final String sessionId, final String entityArg, final String secondEntityArg, final int folderId,
			final long timestamp, final boolean printOutput) throws JSONException, MalformedURLException, IOException,
			SAXException {
		return updateFolder(conversation, null, hostname, sessionId, entityArg, secondEntityArg, folderId, timestamp, printOutput);
	}

	public static boolean updateFolder(final WebConversation conversation, final String protocol, final String hostname,
			final String sessionId, final String entityArg, final String secondEntityArg, final int folderId,
			final long timestamp, final boolean printOutput) throws JSONException, MalformedURLException, IOException,
			SAXException {
		final String entity = entityArg.indexOf('@') == -1 ? entityArg : entityArg.substring(0, entityArg.indexOf('@'));
		final String secondEntity;
		if (secondEntityArg == null) {
			secondEntity = null;
		} else {
			secondEntity = secondEntityArg.indexOf('@') == -1 ? secondEntityArg : secondEntityArg.substring(0,
					secondEntityArg.indexOf('@'));
		}
		final JSONObject jsonFolder = new JSONObject();
		jsonFolder.put("id", folderId);
		final JSONArray perms = new JSONArray();
		JSONObject jsonPermission = new JSONObject();
		jsonPermission.put("entity", entity);
		jsonPermission.put("group", false);
		jsonPermission.put("bits", createPermissionBits(8, 8, 8, 8, true));
		perms.put(jsonPermission);
		jsonPermission = new JSONObject();
		jsonPermission.put("entity", secondEntity);
		jsonPermission.put("group", false);
		jsonPermission.put("bits", createPermissionBits(4, 0, 0, 0, false));
		perms.put(jsonPermission);
		jsonFolder.put("permissions", perms);
		final URLParameter urlParam = new URLParameter();
		urlParam.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_UPDATE);
		urlParam.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
		urlParam.setParameter(AJAXServlet.PARAMETER_ID, String.valueOf(folderId));
		urlParam.setParameter("timestamp", String.valueOf(timestamp));
		final byte[] bytes = jsonFolder.toString().getBytes("UTF-8");
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		final WebRequest req = new PutMethodWebRequest(((null == protocol) ? PROTOCOL : (protocol + "://")) + hostname + FOLDER_URL + urlParam.getURLParameters(),
				bais, "text/javascript; charset=UTF-8");
		final WebResponse resp = conversation.getResponse(req);
		final JSONObject respObj = new JSONObject(resp.getText());
		if (printOutput) {
			System.out.println(respObj.toString());
		}
		if (respObj.has("error")) {
			return false;
		}
		return true;
	}
	
	public static boolean moveFolder(final WebConversation conversation, final String hostname, final String sessionId,
			final String folderId, final String tgtFolderId, final long timestamp, final boolean printOutput)
			throws JSONException, MalformedURLException, IOException, SAXException {
		return moveFolder(conversation, null, hostname, sessionId, folderId, tgtFolderId, timestamp, printOutput);
	}

	public static boolean moveFolder(final WebConversation conversation, final String protocol, final String hostname, final String sessionId,
			final String folderId, final String tgtFolderId, final long timestamp, final boolean printOutput)
			throws JSONException, MalformedURLException, IOException, SAXException {
		final JSONObject jsonFolder = new JSONObject();
		jsonFolder.put("id", folderId);
		jsonFolder.put("folder_id", tgtFolderId);
		final URLParameter urlParam = new URLParameter();
		urlParam.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_UPDATE);
		urlParam.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
		urlParam.setParameter(AJAXServlet.PARAMETER_ID, String.valueOf(folderId));
		urlParam.setParameter("timestamp", String.valueOf(timestamp));
		final byte[] bytes = jsonFolder.toString().getBytes("UTF-8");
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		final WebRequest req = new PutMethodWebRequest(((null == protocol) ? PROTOCOL : (protocol + "://")) + hostname + FOLDER_URL + urlParam.getURLParameters(),
				bais, "text/javascript; charset=UTF-8");
		final WebResponse resp = conversation.getResponse(req);
		final JSONObject respObj = new JSONObject(resp.getText());
		if (printOutput) {
			System.out.println(respObj.toString());
		}
		if (respObj.has("error")) {
			return false;
		}
		return true;
	}
	
	public static int[] deleteFolders(final WebConversation conversation, final String hostname,
			final String sessionId, final int[] folderIds, final long timestamp, final boolean printOutput)
			throws JSONException, IOException, SAXException {
		return deleteFolders(conversation, null, hostname, sessionId, folderIds, timestamp, printOutput);
	}

	public static int[] deleteFolders(final WebConversation conversation, final String protocol, final String hostname,
			final String sessionId, final int[] folderIds, final long timestamp, final boolean printOutput)
			throws JSONException, IOException, SAXException {
		final JSONArray deleteIds = new JSONArray(Arrays.toString(folderIds));
		final byte[] bytes = deleteIds.toString().getBytes("UTF-8");
		final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		final URLParameter urlParam = new URLParameter();
		urlParam.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_DELETE);
		urlParam.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
		urlParam.setParameter(AJAXServlet.PARAMETER_TIMESTAMP, String.valueOf(timestamp));
		final WebRequest req = new PutMethodWebRequest(((null == protocol) ? PROTOCOL : (protocol + "://")) + hostname + FOLDER_URL + urlParam.getURLParameters(),
				bais, "text/javascript; charset=UTF-8");
		final WebResponse resp = conversation.getResponse(req);
		final JSONObject respObj = new JSONObject(resp.getText());
		if (printOutput) {
			System.out.println(respObj.toString());
		}
		if (respObj.has("error")) {
			throw new JSONException("JSON Response object contains an error: " + respObj.getString("error"));
		}
		final JSONArray arr = respObj.getJSONArray("data");
		final int[] retval = new int[arr.length()];
		for (int i = 0; i < arr.length(); i++) {
			retval[i] = arr.getInt(i);
		}
		return retval;
	}

	// public static boolean deleteTestFolders(final WebConversation
	// conversation, final String hostname,
	// final String sessionId, final int[] folderIds, final boolean printOutput)
	// throws JSONException,
	// IOException, SAXException {
	// String deleteIds = Arrays.toString(folderIds);
	// deleteIds = deleteIds.substring(1, deleteIds.length() -
	// 1).replaceAll("\\s+", "");
	//
	// byte[] bytes = "".getBytes("UTF-8");
	// ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
	//
	// URLParameter urlParam = new URLParameter();
	// urlParam.setParameter(AJAXServlet.PARAMETER_ACTION, "removetestfolders");
	// urlParam.setParameter("del_ids", deleteIds);
	// urlParam.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
	//
	// final WebRequest req = new PutMethodWebRequest(PROTOCOL + hostname +
	// FOLDER_URL + urlParam.getURLParameters(),
	// bais, "text/javascript; charset=UTF-8");
	//
	// final WebResponse resp = conversation.getResponse(req);
	//
	// JSONObject respObj = new JSONObject(resp.getText());
	// if (printOutput)
	// System.out.println(respObj.toString());
	// if (respObj.has("error"))
	// throw new JSONException("JSON Response object contains an error: " +
	// respObj.getString("error"));
	// String retval = respObj.getString("data");
	// return retval.equalsIgnoreCase("ok");
	// }

	private static final int[] mapping = { 0, -1, 1, -1, 2, -1, -1, -1, 4 };

	public static int createPermissionBits(final int fp, final int orp, final int owp, final int odp, final boolean adminFlag) {
		final int[] perms = new int[5];
		perms[0] = fp;
		perms[1] = orp;
		perms[2] = owp;
		perms[3] = odp;
		perms[4] = adminFlag ? 1 : 0;
		return createPermissionBits(perms);
	}

	private static int createPermissionBits(final int[] permission) {
		int retval = 0;
		boolean first = true;
		for (int i = permission.length - 1; i >= 0; i--) {
			final int exponent = (i * 7); // Number of bits to be shifted
			if (first) {
				retval += permission[i] << exponent;
				first = false;
			} else {
				if (permission[i] == OCLPermission.ADMIN_PERMISSION) {
					retval += Folder.MAX_PERMISSION << exponent;
				} else {
					retval += mapping[permission[i]] << exponent;
				}
			}
		}
		return retval;
	}
	
	public static FolderObject getStandardFolder(final int module, final String protocol, 
			final WebConversation conversation, final String hostname, final String sessionId) 
			throws MalformedURLException, OXException, AjaxException, IOException, SAXException, JSONException {
		final List<FolderObject> subfolders = getSubfolders(conversation, protocol, hostname, 
				sessionId, Integer.toString(FolderObject.SYSTEM_PRIVATE_FOLDER_ID), false, true);
		if (null != subfolders && 0 < subfolders.size()) {
			for (final FolderObject subfolder : subfolders) {
				if (module == subfolder.getModule() && subfolder.isDefaultFolder()) {
					return subfolder;
				}
			}
		}
		throw new OXException(String.format("No standard folder for module '%d' found", module));
	}
			                         
	public static FolderObject getStandardTaskFolder(final WebConversation conversation, final String hostname,
			final String sessionId) throws MalformedURLException, IOException, SAXException, JSONException, OXException, AjaxException {
		final List<FolderObject> subfolders = getSubfolders(conversation, hostname, sessionId, ""
				+ FolderObject.SYSTEM_PRIVATE_FOLDER_ID, false, true);
		for (final Iterator iter = subfolders.iterator(); iter.hasNext();) {
			final FolderObject subfolder = (FolderObject) iter.next();
			if (subfolder.getModule() == FolderObject.TASK && subfolder.isDefaultFolder()) {
				return subfolder;
			}
		}
		throw new OXException("No Standard Task Folder found!");
	}

	public static FolderObject getStandardCalendarFolder(final WebConversation conversation, final String hostname,
			final String sessionId) throws MalformedURLException, IOException, SAXException, JSONException, OXException, AjaxException {
		final List<FolderObject> subfolders = getSubfolders(conversation, hostname, sessionId, ""
				+ FolderObject.SYSTEM_PRIVATE_FOLDER_ID, false, true);
		for (final Iterator iter = subfolders.iterator(); iter.hasNext();) {
			final FolderObject subfolder = (FolderObject) iter.next();
			if (subfolder.getModule() == FolderObject.CALENDAR && subfolder.isDefaultFolder()) {
				return subfolder;
			}
		}
		throw new OXException("No Standard Calendar Folder found!");
	}

	public static FolderObject getStandardContactFolder(final WebConversation conversation, final String hostname,
			final String sessionId) throws MalformedURLException, IOException, SAXException, JSONException, OXException, AjaxException {
		final List<FolderObject> subfolders = getSubfolders(conversation, hostname, sessionId, ""
				+ FolderObject.SYSTEM_PRIVATE_FOLDER_ID, false, true);
		for (final Iterator iter = subfolders.iterator(); iter.hasNext();) {
			final FolderObject subfolder = (FolderObject) iter.next();
			if (subfolder.getModule() == FolderObject.CONTACT && subfolder.isDefaultFolder()) {
				return subfolder;
			}
		}
		throw new OXException("No Standard Contact Folder found!");
	}

	public static FolderObject getStandardInfostoreFolder(final WebConversation conversation, final String hostname,
			final String sessionId) throws MalformedURLException, IOException, SAXException, JSONException, OXException, AjaxException {
		final List<FolderObject> subfolders = getSubfolders(conversation, hostname, sessionId, ""
				+ FolderObject.SYSTEM_PRIVATE_FOLDER_ID, false, true);
		for (final Iterator iter = subfolders.iterator(); iter.hasNext();) {
			final FolderObject subfolder = (FolderObject) iter.next();
			if (subfolder.getModule() == FolderObject.INFOSTORE && subfolder.isDefaultFolder()) {
				return subfolder;
			}
		}
		throw new OXException("No Standard Infostore Folder found!");
	}

	public static FolderObject getMyInfostoreFolder(final WebConversation conversation, final String hostname,
			final String sessionId, final int loginId) throws MalformedURLException, IOException, SAXException,
			JSONException, OXException, TestException, AjaxException {
		FolderObject infostore = null;
		List<FolderObject> l = getRootFolders(conversation, hostname, sessionId, false);
		for (final Iterator<FolderObject> iter = l.iterator(); iter.hasNext();) {
			final FolderObject rf = iter.next();
			if (rf.getObjectID() == FolderObject.SYSTEM_INFOSTORE_FOLDER_ID) {
				infostore = rf;
				break;
			}
		}
		if (null == infostore) {
		    throw new TestException("System infostore folder not found!");
		}
		FolderObject userStore = null;
		l = getSubfolders(conversation, hostname, sessionId, String.valueOf(infostore.getObjectID()), false);
		for (final Iterator<FolderObject> iter = l.iterator(); iter.hasNext();) {
			final FolderObject f = iter.next();
			if (f.getObjectID() == FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID) {
				userStore = f;
				break;
			}
		}
		if (null == userStore) {
		    throw new TestException("System user store folder not found!");
		}
		l = getSubfolders(conversation, hostname, sessionId, String.valueOf(userStore.getObjectID()), false);
		for (final Iterator<FolderObject> iter = l.iterator(); iter.hasNext();) {
			final FolderObject f = iter.next();
			if (f.containsDefaultFolder() && f.isDefaultFolder() && f.getCreator() == loginId) {
				return f;
			}
		}
		throw new TestException("Private infostore folder not found!");
	}

	public static void printTestStart(final String testName) {
		System.out.println("\n\n\n--------------------------------" + testName + "--------------------------------");
	}

	public static void printTestEnd(final String testName) {
		System.out.println("--------------------------------" + testName + "--------------------------------");
	}

	@Override
	public void setUp() throws Exception {
		sessionId = getSessionId();
	}

	@Override
	public void tearDown() throws Exception {
		logout();
	}

	public void testUnknownAction() {
		try {
			printTestStart("testUnknownAction");
			final WebRequest req = new GetMethodWebRequest(PROTOCOL + getHostName() + FOLDER_URL);
			req.setParameter(AJAXServlet.PARAMETER_SESSION, getSessionId());
			req.setParameter(AJAXServlet.PARAMETER_ACTION, "unknown");
			final WebResponse resp = getWebConversation().getResponse(req);
			final JSONObject respObj = new JSONObject(resp.getText());
			System.out.println("Response-Object: " + respObj.toString());
			assertTrue(respObj.has("error")
					&& respObj.getString("error").indexOf("Action \"unknown\" NOT supported via GET on /ajax/folders") != -1);
			printTestEnd("testUnknownAction");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetUserId() {
		try {
			final int userId = getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
			System.out.println(new StringBuilder(100).append("ID of user \"").append(getLogin()).append("\": ").append(
					userId));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetRootFolders() {
		try {
			printTestStart("testGetRootFolders");
			final int[] assumedIds = { 1, 2, 3, 9 };
			final List<FolderObject> l = getRootFolders(getWebConversation(), getHostName(), getSessionId(), true);
			assertFalse(l == null || l.size() == 0);
			int i = 0;
			for (final Iterator iter = l.iterator(); iter.hasNext();) {
				final FolderObject rf = (FolderObject) iter.next();
				assertTrue(rf.getObjectID() == assumedIds[i]);
				i++;
			}
			printTestEnd("testGetRootFolders");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testDeleteFolder() {
		try {
			printTestStart("testDeleteFolder");
			final int userId = getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
			final int parent = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PUBLIC_FOLDER_ID, "DeleteMeImmediately", "calendar", FolderObject.PUBLIC, -1,
					true);
			assertFalse(parent == -1);
			final Calendar cal = GregorianCalendar.getInstance();
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + parent, cal, true);

			final int child01 = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false, parent,
					"DeleteMeImmediatelyChild01", "calendar", FolderObject.PUBLIC, -1, true);
			assertFalse(child01 == -1);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + child01, cal, true);

			final int child02 = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false, parent,
					"DeleteMeImmediatelyChild02", "calendar", FolderObject.PUBLIC, -1, true);
			assertFalse(child02 == -1);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + child02, cal, true);

			final int[] failedIds = deleteFolders(getWebConversation(), getHostName(), getSessionId(), new int[] { parent },
					cal.getTimeInMillis(), true);
			assertTrue((failedIds == null || failedIds.length == 0));
			printTestEnd("testDeleteFolder");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testFailDeleteFolder() {
		try {
			printTestStart("testFailDeleteFolder");
			final int userId = getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
			final int secId = getUserId(getWebConversation(), getHostName(), getSeconduser(), getPassword());
			final int parent = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false, new int[] {
					8, 8, 8, 8 }, true, FolderObject.SYSTEM_PUBLIC_FOLDER_ID, "DeleteMeImmediately", "calendar",
					FolderObject.PUBLIC, secId, new int[] { 8, 8, 8, 8 }, false, true);
			assertFalse(parent == -1);
			final Calendar cal = GregorianCalendar.getInstance();
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + parent, cal, true);

			final int child01 = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false, new int[] {
					8, 8, 8, 8 }, true, parent, "DeleteMeImmediatelyChild01", "calendar", FolderObject.PUBLIC, secId,
					new int[] { 8, 8, 8, 8 }, false, true);
			assertFalse(child01 == -1);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + child01, cal, true);

			final int child02 = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false, new int[] {
					8, 8, 8, 8 }, true, parent, "DeleteMeImmediatelyChild02", "calendar", FolderObject.PUBLIC, secId,
					new int[] { 8, 8, 8, 8 }, false, true);
			assertFalse(child02 == -1);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + child02, cal, true);

			final int subchild01 = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					new int[] { 8, 8, 8, 8 }, false, child01, "NonDeleteableSubChild01", "calendar",
					FolderObject.PUBLIC, secId, new int[] { 8, 8, 8, 8 }, true, true);
			assertFalse(subchild01 == -1);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + subchild01, cal, true);

			Exception exc = null;
			try {
				final int[] failedIds = deleteFolders(getWebConversation(), getHostName(), getSessionId(),
						new int[] { parent }, cal.getTimeInMillis(), true);
			} catch (final JSONException e) {
				exc = e;
			}

			final byte[] bytes = "ok".getBytes("UTF-8");
			final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			final URLParameter urlParam = new URLParameter();
			urlParam.setParameter(AJAXServlet.PARAMETER_ACTION, "removetestfolders");
			urlParam.setParameter(AJAXServlet.PARAMETER_SESSION, sessionId);
			urlParam.setParameter("del_ids", parent + "," + child01 + "," + child02 + "," + subchild01);
			final WebRequest req = new PutMethodWebRequest(PROTOCOL + getHostName() + FOLDER_URL
					+ urlParam.getURLParameters(), bais, "text/javascript; charset=UTF-8");
			final WebResponse resp = getWebConversation().getResponse(req);
			System.out.println(resp.toString());

    			assertTrue(exc != null);

			printTestEnd("testFailDeleteFolder");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testCheckFolderPermissions() {
		try {
			printTestStart("testCheckFolderPermissions");
			final int userId = getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
			final int fuid = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PUBLIC_FOLDER_ID, "CheckMyPermissions", "calendar", FolderObject.PUBLIC, -1,
					true);
			final Calendar cal = GregorianCalendar.getInstance();
			FolderObject fo = getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
			System.out.println("-------------------------------- START FolderObject --------------------------------");
			System.out.println(fo.toString());
			System.out.println("-------------------------------- END FolderObject ----------------------------------");
			updateFolder(getWebConversation(), getHostName(), getSessionId(), getLogin(), getSeconduser(), fuid, cal
					.getTimeInMillis(), true);
			fo = getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
			System.out.println("-------------------------------- START FolderObject --------------------------------");
			System.out.println(fo.toString());
			System.out.println("-------------------------------- END FolderObject ----------------------------------");
			deleteFolders(getWebConversation(), getHostName(), getSessionId(), new int[] { fuid }, cal
					.getTimeInMillis(), true);
			printTestEnd("testCheckFolderPermissions");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testInsertRenameFolder() {
		int fuid = -1;
		int[] failedIds = null;
		boolean updated = false;
		try {
			printTestStart("testInsertRenameFolder");
			final int userId = getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
			fuid = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PRIVATE_FOLDER_ID, "NewPrivateFolder", "calendar", FolderObject.PRIVATE, -1,
					true);
			assertFalse(fuid == -1);
			final Calendar cal = GregorianCalendar.getInstance();
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
			updated = renameFolder(getWebConversation(), getHostName(), getSessionId(), fuid,
					"ChangedPrivateFolderName", "calendar", FolderObject.PRIVATE, cal.getTimeInMillis(), true);
			assertTrue(updated);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
			failedIds = deleteFolders(getWebConversation(), getHostName(), getSessionId(), new int[] { fuid }, cal
					.getTimeInMillis(), true);
			assertFalse((failedIds != null && failedIds.length > 0));
			fuid = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PUBLIC_FOLDER_ID, "NewPublicFolder", "calendar", FolderObject.PRIVATE, -1, true);
			assertFalse(fuid == -1);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
			failedIds = deleteFolders(getWebConversation(), getHostName(), getSessionId(), new int[] { fuid }, cal
					.getTimeInMillis(), true);
			assertFalse((failedIds != null && failedIds.length > 0));
			fuid = -1;
			final FolderObject myInfostore = getMyInfostoreFolder(getWebConversation(), getHostName(), getSessionId(), userId);
			System.out.println("MyINfostore Folder: " + myInfostore.toString());
			fuid = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false, myInfostore
					.getObjectID(), "NewInfostoreFolder", "infostore", FolderObject.PUBLIC, -1, true);
			assertFalse(fuid == -1);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
			updated = renameFolder(getWebConversation(), getHostName(), getSessionId(), fuid,
					"ChangedInfostoreFolderName", "infostore", FolderObject.PUBLIC, cal.getTimeInMillis(), true);
			assertTrue(updated);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
			failedIds = deleteFolders(getWebConversation(), getHostName(), getSessionId(), new int[] { fuid }, cal
					.getTimeInMillis(), true);
			assertFalse((failedIds != null && failedIds.length > 0));
			fuid = -1;
			printTestEnd("testInsertRenameFolder");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				if (fuid != -1) {
					final Calendar cal = GregorianCalendar.getInstance();
					/*
					 * Call getFolder to receive a valid timestamp for deletion
					 */
					getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, false);
					deleteFolders(getWebConversation(), getHostName(), getSessionId(), new int[] { fuid }, cal
							.getTimeInMillis(), false);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void testInsertUpdateFolder() {
		int fuid = -1;
		int[] failedIds = null;
		boolean updated = false;
		try {
			printTestStart("testInsertUpdateFolder");
			final int userId = getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
			fuid = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PRIVATE_FOLDER_ID, "ChangeMyPermissions", "calendar", FolderObject.PRIVATE, -1,
					true);
			assertFalse(fuid == -1);
			final Calendar cal = GregorianCalendar.getInstance();
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
			updated = updateFolder(getWebConversation(), getHostName(), getSessionId(), getLogin(), getSeconduser(),
					fuid, cal.getTimeInMillis(), true);
			assertTrue(updated);
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
			failedIds = deleteFolders(getWebConversation(), getHostName(), getSessionId(), new int[] { fuid }, cal
					.getTimeInMillis(), true);
			assertFalse((failedIds != null && failedIds.length > 0));
			fuid = -1;
			printTestEnd("testInsertUpdateFolder");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				if (fuid != -1) {
					final Calendar cal = GregorianCalendar.getInstance();
					/*
					 * Call getFolder to receive a valid timestamp for deletion
					 */
					getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, false);
					deleteFolders(getWebConversation(), getHostName(), getSessionId(), new int[] { fuid }, cal
							.getTimeInMillis(), false);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void testSharedFolder() {
		int fuid01 = -1;
		int fuid02 = -1;
		String anotherSessionId = null;
		try {
			printTestStart("testSharedFolder");
			/*
			 * Create a shared folder with login as creator and define share
			 * right for second user
			 */
			final int userId = getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
			final int secId = getUserId(getWebConversation(), getHostName(), getSeconduser(), getPassword());
			fuid01 = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PRIVATE_FOLDER_ID, "SharedFolder01", "calendar", FolderObject.PRIVATE, secId,
					true);
			assertFalse(fuid01 == -1);
			fuid02 = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PRIVATE_FOLDER_ID, "SharedFolder02", "calendar", FolderObject.PRIVATE, secId,
					true);
			assertFalse(fuid02 == -1);
			/*
			 * Connect with second user and verify that folder is visible
			 * beneath system shared folder
			 */
			anotherSessionId = LoginTest.getSessionId(getWebConversation(), getHostName(), getSeconduser(),
					getPassword());
			boolean found01 = false;
			boolean found02 = false;
			final List<FolderObject> l = getSubfolders(getWebConversation(), getHostName(), anotherSessionId, ""
					+ FolderObject.SYSTEM_SHARED_FOLDER_ID, true);
			assertFalse(l == null || l.size() == 0);
			Next: for (final Iterator iter = l.iterator(); iter.hasNext();) {
				final FolderObject virtualFO = (FolderObject) iter.next();
				final List<FolderObject> subList = getSubfolders(getWebConversation(), getHostName(), anotherSessionId,
						virtualFO.getFullName(), true);
				for (final Iterator iterator = subList.iterator(); iterator.hasNext();) {
					final FolderObject sharedFolder = (FolderObject) iterator.next();
					if (sharedFolder.getObjectID() == fuid01) {
						found01 = true;
						if (found01 && found02) {
							break Next;
						}
					}
					if (sharedFolder.getObjectID() == fuid02) {
						found02 = true;
						if (found01 && found02) {
							break Next;
						}
					}
				}
			}
			assertTrue(found01);
			assertTrue(found02);
			final String sesID = LoginTest.getSessionId(getWebConversation(), getHostName(), getLogin(), getPassword());

			deleteFolders(getWebConversation(), getHostName(), sesID, new int[] { fuid01, fuid02 }, System
					.currentTimeMillis(), false);
			// deleteTestFolders(getWebConversation(), getHostName(), sesID, new
			// int[] { fuid01, fuid02 }, false);

			printTestEnd("testSharedFolder");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetSubfolder() {
		int fuid = -1;
		int[] subfuids = null;
		try {
			printTestStart("testGetSubfolder");
			/*
			 * Create a temp folder with subfolders
			 */
			final int userId = getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
			fuid = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PRIVATE_FOLDER_ID, "NewPrivateFolder", "calendar", FolderObject.PRIVATE, -1,
					true);
			final DecimalFormat df = new DecimalFormat("00");
			subfuids = new int[3];
			for (int i = 0; i < subfuids.length; i++) {
				subfuids[i] = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false, fuid,
						"NewPrivateSubFolder" + df.format((i + 1)), "calendar", FolderObject.PRIVATE, -1, true);
			}
			/*
			 * Get subfolder list
			 */
			final List<FolderObject> l = getSubfolders(getWebConversation(), getHostName(), getSessionId(), "" + fuid, true);
			assertFalse(l == null || l.size() == 0);
			int i = 0;
			for (final Iterator iter = l.iterator(); iter.hasNext();) {
				final FolderObject subFolder = (FolderObject) iter.next();
				assertTrue(subFolder.getObjectID() == subfuids[i]);
				i++;
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fuid != -1) {
					final Calendar cal = GregorianCalendar.getInstance();
					/*
					 * Call getFolder to receive a valid timestamp for deletion
					 */
					getFolder(getWebConversation(), getHostName(), getSessionId(), "" + fuid, cal, true);
					final int[] failedIds = deleteFolders(getWebConversation(), getHostName(), getSessionId(),
							new int[] { fuid }, cal.getTimeInMillis(), true);
					if (failedIds != null && failedIds.length > 0) {
						if (subfuids != null) {
							for (int i = 0; i < subfuids.length; i++) {
								if (subfuids[i] > 0) {
									/*
									 * Call getFolder to receive a valid
									 * timestamp for deletion
									 */
									getFolder(getWebConversation(), getHostName(), getSessionId(), "" + subfuids[i],
											cal, true);
									deleteFolders(getWebConversation(), getHostName(), getSessionId(),
											new int[] { subfuids[i] }, cal.getTimeInMillis(), true);
								}
							}
						}
					}
				}
			} catch (final Exception e1) {
				e1.printStackTrace();
			}
			printTestEnd("testGetSubfolder");
		}
	}

	public void testMoveFolder() {
		int parent01 = -1;
		int parent02 = -1;
		int moveFuid = -1;
		int[] failedIds = null;
		boolean moved = false;
		try {
			printTestStart("testMoveFolder");
			final int userId = getUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
			parent01 = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PRIVATE_FOLDER_ID, "Parent01", "calendar", FolderObject.PRIVATE, -1, true);
			assertFalse(parent01 == -1);
			parent02 = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false,
					FolderObject.SYSTEM_PRIVATE_FOLDER_ID, "Parent02", "calendar", FolderObject.PRIVATE, -1, true);
			assertFalse(parent02 == -1);
			moveFuid = insertFolder(getWebConversation(), getHostName(), getSessionId(), userId, false, parent01,
					"MoveMe", "calendar", FolderObject.PRIVATE, -1, true);
			assertFalse(moveFuid == -1);
			final Calendar cal = GregorianCalendar.getInstance();
			getFolder(getWebConversation(), getHostName(), getSessionId(), "" + moveFuid, cal, true);
			moved = moveFolder(getWebConversation(), getHostName(), getSessionId(), "" + moveFuid, "" + parent02, cal
					.getTimeInMillis(), true);
			assertTrue(moved);
			FolderObject movedFolderObj = null;
			movedFolderObj = getFolder(getWebConversation(), getHostName(), getSessionId(), "" + moveFuid, cal, true);
			assertTrue(movedFolderObj.containsParentFolderID() ? movedFolderObj.getParentFolderID() == parent02 : true);
			failedIds = deleteFolders(getWebConversation(), getHostName(), getSessionId(), new int[] { parent01,
					parent02 }, cal.getTimeInMillis(), true);
			assertFalse((failedIds != null && failedIds.length > 0));
			printTestEnd("testMoveFolder");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetMailInbox() {
		try {
			printTestStart("testGetMailInbox");
			List<FolderObject> l = getSubfolders(getWebConversation(), getHostName(), getSessionId(), ""
					+ FolderObject.SYSTEM_PRIVATE_FOLDER_ID, true);
			FolderObject defaultIMAPFolder = null;
			for (int i = 0; i < l.size(); i++) {
				final FolderObject fo = l.get(i);
				if (fo.containsFullName() && fo.getFullName().equals(MailFolder.DEFAULT_FOLDER_ID)) {
					defaultIMAPFolder = fo;
					break;
				}
			}
			assertTrue(defaultIMAPFolder != null && defaultIMAPFolder.hasSubfolders());
			l = getSubfolders(getWebConversation(), getHostName(), getSessionId(), defaultIMAPFolder.getFullName(),
					true);
			assertTrue(l != null && l.size() > 0);
			FolderObject inboxFolder = null;
			for (int i = 0; i < l.size() && (inboxFolder == null); i++) {
				final FolderObject fo = l.get(i);
				if (fo.getFullName().endsWith("INBOX")) {
					inboxFolder = fo;
				}
			}
			assertTrue(inboxFolder != null);
			final Calendar cal = GregorianCalendar.getInstance();
			getFolder(getWebConversation(), getHostName(), getSessionId(), inboxFolder.getFullName(), cal, true);
			printTestEnd("testGetMailInbox");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testFolderNamesShouldBeEqualRegardlessOfRequestMethod() {
		try {
			final List<FolderObject> rootFolders = getRootFolders(getWebConversation(), getHostName(), getSessionId(), true);
			for (final FolderObject rootFolder : rootFolders) {
				final FolderObject individuallyLoaded = getFolder(getWebConversation(), getHostName(), getSessionId(), ""
						+ rootFolder.getObjectID(), Calendar.getInstance(), true);
				assertEquals("Foldernames differ : " + rootFolder.getFolderName() + " != "
						+ individuallyLoaded.getFolderName(), rootFolder.getFolderName(), individuallyLoaded
						.getFolderName());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

    // Node 2652

    public void testLastModifiedUTCInGet() throws JSONException, AjaxException, IOException, SAXException {
        final AJAXClient client = new AJAXClient(new AJAXSession(getWebConversation(), getSessionId()));
        // Load an existing folder
        final GetRequest getRequest = new GetRequest(""+FolderObject.SYSTEM_PUBLIC_FOLDER_ID, new int[]{FolderObject.LAST_MODIFIED_UTC}, true);
        final GetResponse response = Executor.execute(client, getRequest);
        assertTrue(((JSONObject)response.getData()).has("last_modified_utc"));
    }

    // Node 2652

    public void testLastModifiedUTCInList() throws JSONException, IOException, SAXException, AjaxException {
        final AJAXClient client = new AJAXClient(new AJAXSession(getWebConversation(), getSessionId()));
        // List known folder
        final ListRequest listRequest = new ListRequest(""+FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID, new int[]{FolderObject.LAST_MODIFIED_UTC}, false);
        final ListResponse listResponse = Executor.execute(client, listRequest);
        final JSONArray arr = (JSONArray) listResponse.getData();
        final int size = arr.length();
        assertTrue(size > 0);
        for(int i = 0; i < size; i++) {
            final JSONArray row = arr.optJSONArray(i);
            assertNotNull(row);
            assertTrue(row.length() == 1);
            assertNotNull(row.get(0));
        }
    }

    // Node 2652

    public void testLastModifiedUTCInUpdates() throws JSONException, AjaxException, IOException, SAXException {
        final AJAXClient client = new AJAXClient(new AJAXSession(getWebConversation(), getSessionId()));
        // List known folder
        final UpdatesRequest updatesRequest = new UpdatesRequest(FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID, new int[]{FolderObject.LAST_MODIFIED_UTC}, -1, null, new Date(0));
        final AbstractAJAXResponse response = Executor.execute(client, updatesRequest);

        final JSONArray arr = (JSONArray) response.getData();
        final int size = arr.length();
        assertTrue(size > 0);
        for(int i = 0; i < size; i++) {
            final JSONArray row = arr.optJSONArray(i);
            assertNotNull(row);
            assertTrue(row.length() == 1);
            assertNotNull(row.get(0));
        }
    }


}

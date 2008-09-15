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

package com.openexchange.mail.json.writer;

import static com.openexchange.mail.utils.MailFolderUtility.prepareFullname;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.Folder;
import com.openexchange.ajax.fields.FolderFields;
import com.openexchange.api2.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.mail.MailException;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.permission.MailPermission;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.tools.oxfolder.OXFolderException;
import com.openexchange.tools.oxfolder.OXFolderException.FolderCode;

/**
 * {@link FolderWriter} - Writes {@link MailFolder} instances as JSON strings
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class FolderWriter {

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(FolderWriter.class);

	public static abstract class MailFolderFieldWriter {
		public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey)
				throws MailException {
			writeField(jsonContainer, folder, withKey, null, -1);
		}

		public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
				final String name, final int hasSubfolders) throws MailException {
			writeField(jsonContainer, folder, withKey, name, hasSubfolders, null, -1, false);
		}

		public abstract void writeField(Object jsonContainer, MailFolder folder, boolean withKey, String name,
				int hasSubfolders, String fullName, int module, boolean all) throws MailException;
	}

	private static abstract class ExtendedMailFolderFieldWriter extends MailFolderFieldWriter {

		public final MailConfig mailConfig;

		public ExtendedMailFolderFieldWriter(final MailConfig mailConfig) {
			super();
			this.mailConfig = mailConfig;
		}

	}

	/**
	 * Maps folder field constants to corresponding instance of
	 * {@link MailFolderFieldWriter}
	 */
	private static final Map<Integer, MailFolderFieldWriter> WRITERS_MAP = new HashMap<Integer, MailFolderFieldWriter>(
			20);

	static {
		WRITERS_MAP.put(Integer.valueOf(FolderObject.OBJECT_ID), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.ID, prepareFullname(fullName == null ? folder
								.getFullname() : fullName));
					} else {
						((JSONArray) jsonContainer).put(prepareFullname(fullName == null ? folder.getFullname()
								: fullName));
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.CREATED_BY), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.CREATED_BY, -1);
					} else {
						((JSONArray) jsonContainer).put(-1);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.MODIFIED_BY), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.MODIFIED_BY, -1);
					} else {
						((JSONArray) jsonContainer).put(-1);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.CREATION_DATE), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.CREATION_DATE, 0);
					} else {
						((JSONArray) jsonContainer).put(0);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.LAST_MODIFIED), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.LAST_MODIFIED, 0);
					} else {
						((JSONArray) jsonContainer).put(0);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.FOLDER_ID), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					final Object parent;
					if (null == folder.getParentFullname()) {
						parent = JSONObject.NULL;
					} else {
						parent = prepareFullname(folder.getParentFullname());
					}
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.FOLDER_ID, parent);
					} else {
						((JSONArray) jsonContainer).put(parent);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.FOLDER_NAME), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.TITLE, name == null ? folder.getName() : name);
					} else {
						((JSONArray) jsonContainer).put(name == null ? folder.getName() : name);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.MODULE), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.MODULE, AJAXServlet.getModuleString(
								module == -1 ? FolderObject.MAIL : module, -1));
					} else {
						((JSONArray) jsonContainer).put(AJAXServlet.getModuleString(module == -1 ? FolderObject.MAIL
								: module, -1));
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.TYPE), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.TYPE, FolderObject.MAIL);
					} else {
						((JSONArray) jsonContainer).put(FolderObject.MAIL);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.SUBFOLDERS), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					final boolean boolVal;
					if (hasSubfolders == -1) {
						boolVal = all ? folder.hasSubfolders() : folder.hasSubscribedSubfolders();
					} else {
						boolVal = hasSubfolders > 0;
					}
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.SUBFOLDERS, boolVal);
					} else {
						((JSONArray) jsonContainer).put(boolVal);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.OWN_RIGHTS), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					int permissionBits = 0;
					if (folder.isRootFolder()) {
						final MailPermission rootPermission = folder.getOwnPermission();
						if (rootPermission == null) {
							permissionBits = createPermissionBits(OCLPermission.CREATE_SUB_FOLDERS,
									OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS,
									OCLPermission.NO_PERMISSIONS, false);
						} else {
							permissionBits = createPermissionBits(folder.getOwnPermission());
						}
					} else {
						permissionBits = createPermissionBits(folder.getOwnPermission());
						if (folder.isSupportsUserFlags()) {
							permissionBits |= BIT_USER_FLAG;
						}
					}
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.OWN_RIGHTS, permissionBits);
					} else {
						((JSONArray) jsonContainer).put(permissionBits);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				} catch (final OXException e) {
					throw new MailException(e);
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.PERMISSIONS_BITS), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					final JSONArray ja = new JSONArray();
					final OCLPermission[] perms = folder.getPermissions();
					for (int j = 0; j < perms.length; j++) {
						final JSONObject jo = new JSONObject();
						jo.put(FolderFields.BITS, createPermissionBits(perms[j]));
						jo.put(FolderFields.ENTITY, perms[j].getEntity());
						jo.put(FolderFields.GROUP, perms[j].isGroupPermission());
						ja.put(jo);
					}
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.PERMISSIONS, ja);
					} else {
						((JSONArray) jsonContainer).put(ja);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				} catch (final OXException e) {
					throw new MailException(e);
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.SUMMARY), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					/*
					 * Put value
					 */
					final String value = folder.isRootFolder() ? "" : new StringBuilder(16).append('(').append(
							folder.getMessageCount()).append('/').append(folder.getUnreadMessageCount()).append(')')
							.toString();
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.SUMMARY, value);
					} else {
						((JSONArray) jsonContainer).put(value);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.STANDARD_FOLDER), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.STANDARD_FOLDER,
								folder.containsDefaultFolder() ? folder.isDefaultFolder() : false);
					} else {
						((JSONArray) jsonContainer).put(folder.containsDefaultFolder() ? folder.isDefaultFolder()
								: false);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.TOTAL), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.TOTAL, folder.getMessageCount());
					} else {
						((JSONArray) jsonContainer).put(folder.getMessageCount());
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.NEW), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.NEW, folder.getNewMessageCount());
					} else {
						((JSONArray) jsonContainer).put(folder.getNewMessageCount());
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.UNREAD), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.UNREAD, folder.getUnreadMessageCount());
					} else {
						((JSONArray) jsonContainer).put(folder.getUnreadMessageCount());
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.DELETED), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.DELETED, folder.getDeletedMessageCount());
					} else {
						((JSONArray) jsonContainer).put(folder.getDeletedMessageCount());
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.SUBSCRIBED), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					final Object boolVal;
					if (MailConfig.isIgnoreSubscription()) {
						boolVal = Boolean.FALSE;
					} else {
						boolVal = folder.containsSubscribed() ? Boolean.valueOf(folder.isSubscribed())
								: JSONObject.NULL;
					}
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.SUBSCRIBED, boolVal);
					} else {
						((JSONArray) jsonContainer).put(boolVal);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
		WRITERS_MAP.put(Integer.valueOf(FolderObject.SUBSCR_SUBFLDS), new MailFolderFieldWriter() {
			@Override
			public void writeField(final Object jsonContainer, final MailFolder folder, final boolean withKey,
					final String name, final int hasSubfolders, final String fullName, final int module,
					final boolean all) throws MailException {
				try {
					final Object boolVal;
					if (MailConfig.isIgnoreSubscription()) {
						boolVal = hasSubfolders == -1 ? Boolean.valueOf(folder.hasSubfolders()) : Boolean
								.valueOf(hasSubfolders > 0);
					} else if (hasSubfolders == -1) {
						boolVal = folder.hasSubfolders() ? Boolean.valueOf(folder.hasSubscribedSubfolders())
								: Boolean.FALSE;
					} else {
						boolVal = Boolean.valueOf(hasSubfolders > 0);
					}
					/*
					 * Put value
					 */
					if (withKey) {
						((JSONObject) jsonContainer).put(FolderFields.SUBSCR_SUBFLDS, boolVal);
					} else {
						((JSONArray) jsonContainer).put(boolVal);
					}
				} catch (final JSONException e) {
					throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
				}
			}
		});
	}

	/**
	 * No instantiation
	 */
	private FolderWriter() {
		super();
	}

	private static final int[] ALL_FLD_FIELDS = com.openexchange.ajax.writer.FolderWriter.getAllFolderFields();

	/**
	 * Writes whole folder as a JSON object
	 * 
	 * @param folder
	 *            The folder to write
	 * @return The written JSON object
	 * @throws MailException
	 */
	public static JSONObject writeMailFolder(final MailFolder folder, final MailConfig mailConfig) throws MailException {
		final JSONObject jsonObject = new JSONObject();
		final MailFolderFieldWriter[] writers = getMailFolderFieldWriter(ALL_FLD_FIELDS, mailConfig);
		for (final MailFolderFieldWriter writer : writers) {
			writer.writeField(jsonObject, folder, true);
		}
		return jsonObject;
	}

	private static final int BIT_USER_FLAG = (1 << 29);

	private static final String STR_UNKNOWN_COLUMN = "Unknown column";

	/**
	 * Generates appropriate field writers for given mail folder fields
	 * 
	 * @param fields
	 *            The fields to write
	 * @param mailConfig
	 *            Current mail configuration
	 * @return Appropriate field writers as an array of
	 *         {@link MailFolderFieldWriter}
	 */
	public static MailFolderFieldWriter[] getMailFolderFieldWriter(final int[] fields, final MailConfig mailConfig) {
		final MailFolderFieldWriter[] retval = new MailFolderFieldWriter[fields.length];
		for (int i = 0; i < retval.length; i++) {
			final MailFolderFieldWriter mffw = WRITERS_MAP.get(Integer.valueOf(fields[i]));
			if (mffw == null) {
				if (FolderObject.CAPABILITIES == fields[i]) {
					retval[i] = new ExtendedMailFolderFieldWriter(mailConfig) {
						@Override
						public void writeField(final Object jsonContainer, final MailFolder folder,
								final boolean withKey, final String name, final int hasSubfolders,
								final String fullName, final int module, final boolean all) throws MailException {
							try {
								/*
								 * Put value
								 */
								if (withKey) {
									((JSONObject) jsonContainer).put(FolderFields.CAPABILITIES, Integer
											.valueOf(this.mailConfig.getCapabilities().getCapabilities()));
								} else {
									((JSONArray) jsonContainer).put(Integer.valueOf(this.mailConfig.getCapabilities()
											.getCapabilities()));
								}
							} catch (final JSONException e) {
								throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
							}
						}
					};
				} else {
					LOG.error("Unknown column: " + fields[i]);
					retval[i] = new MailFolderFieldWriter() {
						@Override
						public void writeField(final Object jsonContainer, final MailFolder folder,
								final boolean withKey, final String name, final int hasSubfolders,
								final String fullName, final int module, final boolean all) throws MailException {
							try {
								if (withKey) {
									((JSONObject) jsonContainer).put(STR_UNKNOWN_COLUMN, JSONObject.NULL);
								} else {
									((JSONArray) jsonContainer).put(JSONObject.NULL);
								}
							} catch (final JSONException e) {
								throw new MailException(MailException.Code.JSON_ERROR, e, e.getLocalizedMessage());
							}
						}
					};
				}
			} else {
				retval[i] = mffw;
			}
		}
		return retval;
	}

	private static final int[] mapping = { 0, -1, 1, -1, 2, -1, -1, -1, 4 };

	static int createPermissionBits(final OCLPermission perm) throws OXException {
		return createPermissionBits(perm.getFolderPermission(), perm.getReadPermission(), perm.getWritePermission(),
				perm.getDeletePermission(), perm.isFolderAdmin());
	}

	static int createPermissionBits(final int fp, final int orp, final int owp, final int odp, final boolean adminFlag)
			throws OXException {
		final int[] perms = new int[5];
		perms[0] = fp == Folder.MAX_PERMISSION ? OCLPermission.ADMIN_PERMISSION : fp;
		perms[1] = orp == Folder.MAX_PERMISSION ? OCLPermission.ADMIN_PERMISSION : orp;
		perms[2] = owp == Folder.MAX_PERMISSION ? OCLPermission.ADMIN_PERMISSION : owp;
		perms[3] = odp == Folder.MAX_PERMISSION ? OCLPermission.ADMIN_PERMISSION : odp;
		perms[4] = adminFlag ? 1 : 0;
		return createPermissionBits(perms);
	}

	private static int createPermissionBits(final int[] permission) throws OXException {
		int retval = 0;
		boolean first = true;
		for (int i = permission.length - 1; i >= 0; i--) {
			final int shiftVal = (i * 7); // Number of bits to be shifted
			if (first) {
				retval += permission[i] << shiftVal;
				first = false;
			} else {
				if (permission[i] == OCLPermission.ADMIN_PERMISSION) {
					retval += Folder.MAX_PERMISSION << shiftVal;
				} else {
					try {
						retval += mapping[permission[i]] << shiftVal;
					} catch (final Exception e) {
						throw new OXFolderException(FolderCode.MAP_PERMISSION_FAILED, e, Integer.valueOf(permission[i]));
					}
				}
			}
		}
		return retval;
	}

}

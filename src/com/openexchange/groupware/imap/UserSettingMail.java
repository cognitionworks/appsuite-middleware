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



package com.openexchange.groupware.imap;

import static com.openexchange.tools.sql.DBUtils.closeResources;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.UserConfigurationException;
import com.openexchange.groupware.UserConfigurationException.UserConfigurationCode;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFailedException;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.server.DBPool;
import com.openexchange.server.DBPoolingException;

/**
 * UserSettingMail
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class UserSettingMail implements DeleteListener {
	
	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(UserSettingMail.class);
	
	private static final int INT_DISPLAY_HTML_INLINE_CONTENT = 1;
	
	private static final int INT_USE_COLOR_QUOTE = 2;
	
	private static final int INT_SHOW_GRAPHIC_EMOTICONS = 4;
	
	private static final int INT_HARD_DELETE_MSGS = 8;
	
	private static final int INT_FORWARD_AS_ATTACHMENT = 16;
	
	private static final int INT_APPEND_VCARD = 32;
	
	private static final int INT_NOTIFY_ON_READ_ACK = 64;
	
	private static final int INT_MSG_PREVIEW = 128;
	
	private static final int INT_NOTIFY_APPOINTMENTS = 256;
	
	private static final int INT_NOTIFY_TASKS = 512;
	
	private static final int INT_IGNORE_ORIGINAL_TEXT_ON_REPLY = 1024;
	
	private static final int INT_NO_COPY_INTO_SENT_FOLDER = 2048; // All bits: 4095
	
	
	public static final int MSG_FORMAT_TEXT_ONLY = 1;
	
	public static final int MSG_FORMAT_HTML_ONLY = 2;
	
	public static final int MSG_FORMAT_BOTH = 3;
	
	public static final String STD_TRASH = "Trash";
	
	public static final String STD_DRAFTS = "Drafts";
	
	public static final String STD_SENT = "Sent";
	
	public static final String STD_SPAM = "Spam";
	
	private boolean modifiedDuringSession;
	
	private boolean displayHtmlInlineContent;
	
	private boolean useColorQuote;
	
	private boolean showGraphicEmoticons;
	
	private boolean hardDeleteMsgs;
	
	private boolean forwardAsAttachment;
	
	private boolean appendVCard;
	
	private boolean notifyOnReadAck;
	
	private boolean notifyAppointments;
	
	private boolean notifyTasks;
	
	private boolean msgPreview;
	
	private boolean stdFoldersSet;
	
	private boolean ignoreOriginalMailTextOnReply;
	
	private boolean noCopyIntoStandardSentFolder;
	
	private Signature[] signatures;
	
	private String sendAddr;
	
	private String replyToAddr;
	
	private int msgFormat = MSG_FORMAT_TEXT_ONLY;
	
	private String[] displayMsgHeaders;
	
	private int autoLinebreak = 80;
	
	private String stdTrashName;
	
	private String stdDraftsName;
	
	private String stdSentName;
	
	private String stdSpamName;
	
	private long uploadQuota;
	
	private long uploadQuotaPerFile;
	
	private final String[] stdFolderFullnames; 
	
	private final Lock stdFolderCreationLock;
	
	public UserSettingMail() {
		super();
		stdFolderFullnames = new String[4];
		stdFolderCreationLock = new ReentrantLock();
	}
	
	private static final String SQL_LOAD = "SELECT bits, send_addr, reply_to_addr, msg_format, display_msg_headers, auto_linebreak, std_trash, std_sent, std_drafts, std_spam, " +
			"upload_quota, upload_quota_per_file FROM user_setting_mail WHERE cid = ? AND user = ?";
	
	private static final String SQL_INSERT = "INSERT INTO user_setting_mail (cid, user, bits, send_addr, reply_to_addr, msg_format, display_msg_headers, auto_linebreak, std_trash, std_sent, std_drafts, std_spam, upload_quota, upload_quota_per_file) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String SQL_UPDATE = "UPDATE user_setting_mail SET bits = ?, send_addr = ?, reply_to_addr = ?, msg_format = ?, display_msg_headers = ?, auto_linebreak = ?, std_trash = ?, std_sent = ?, std_drafts = ?, std_spam = ?, upload_quota = ?, upload_quota_per_file = ? WHERE cid = ? AND user = ?";
	
	private static final String SQL_DELETE = "DELETE FROM user_setting_mail WHERE cid = ? AND user = ?";
	
	private static final String SQL_LOAD_SIGNATURES = "SELECT id, signature FROM user_setting_mail_signature WHERE cid = ? AND user = ?";
	
	private static final String SQL_INSERT_SIGNATURE = "INSERT INTO user_setting_mail_signature (cid, user, id, signature) VALUES (?, ?, ?, ?)";
	
	private static final String SQL_DELETE_SIGNATURES = "DELETE FROM user_setting_mail_signature WHERE cid = ? AND user = ?";
	
	public final void saveUserSettingMail(final int user, final Context ctx) throws OXException {
		saveUserSettingMail(user, ctx, null);
	}
	
	public final void saveUserSettingMail(final int user, final Context ctx, final Connection writeConArg) throws OXException {
		try {
			Connection writeCon = writeConArg;
			final boolean createCon = (writeCon == null);
			PreparedStatement stmt = null;
			ResultSet rs = null;
			boolean insert = false;
			Connection tmpCon = null;
			try {
				tmpCon = DBPool.pickup(ctx);
				stmt = tmpCon.prepareStatement(SQL_LOAD);
				stmt.setInt(1, ctx.getContextId());
				stmt.setInt(2, user);
				rs = stmt.executeQuery();
				insert = (!rs.next());
			} finally {
				closeResources(rs, stmt, tmpCon, true, ctx);
				rs = null;
				stmt = null;
				tmpCon = null;
			}
			try {
				if (createCon) {
					writeCon = DBPool.pickupWriteable(ctx);
				}
				if (insert) {
					stmt = writeCon.prepareStatement(SQL_INSERT);
					stmt.setInt(1, ctx.getContextId());
					stmt.setInt(2, user);
					stmt.setInt(3, getBitsValue());
					//stmt.setString(4, signature);
					stmt.setString(4, sendAddr);
					stmt.setString(5, replyToAddr);
					stmt.setInt(6, msgFormat);
					String s = getDisplayMsgHeadersString();
					if (s == null) {
						stmt.setNull(7, Types.VARCHAR);
					} else {
						stmt.setString(7, s);
					}
					s = null;
					stmt.setInt(8, autoLinebreak);
					stmt.setString(9, stdTrashName);
					stmt.setString(10, stdSentName);
					stmt.setString(11, stdDraftsName);
					stmt.setString(12, stdSpamName);
					stmt.setLong(13, uploadQuota);
					stmt.setLong(14, uploadQuotaPerFile);
				} else {
					stmt = writeCon.prepareStatement(SQL_UPDATE);
					stmt.setInt(1, getBitsValue());
					//stmt.setString(2, signature);
					stmt.setString(2, sendAddr);
					stmt.setString(3, replyToAddr);
					stmt.setInt(4, msgFormat);
					String s = getDisplayMsgHeadersString();
					if (s == null) {
						stmt.setNull(5, Types.VARCHAR);
					} else {
						stmt.setString(5, s);
					}
					s = null;
					stmt.setInt(6, autoLinebreak);
					stmt.setString(7, stdTrashName);
					stmt.setString(8, stdSentName);
					stmt.setString(9, stdDraftsName);
					stmt.setString(10, stdSpamName);
					stmt.setLong(11, uploadQuota);
					stmt.setLong(12, uploadQuotaPerFile);
					stmt.setInt(13, ctx.getContextId());
					stmt.setInt(14, user);
				}
				stmt.executeUpdate();
				saveSignatures(user, ctx, writeCon);
			} finally {
				closeResources(rs, stmt, createCon ? writeCon : null, false, ctx);
			}
			modifiedDuringSession = false;
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.SQL_ERROR, e, new Object[0]);
		} catch (DBPoolingException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.DBPOOL_ERROR, e, new Object[0]);
		}
	}
	
	public final void deleteUserSettingMail(final int user, final Context ctx) throws OXException {
		deleteUserSettingMail(user, ctx, null);
	}
	
	public final void deleteUserSettingMail(final int user, final Context ctx, final Connection writeConArg) throws OXException {
		Connection writeCon = writeConArg;
		try {
			final boolean createWriteCon = (writeCon == null);
			PreparedStatement stmt = null;
			try {
				if (createWriteCon) {
					writeCon = DBPool.pickupWriteable(ctx);
				}
				/*
				 * Delete signatures
				 */
				stmt = writeCon.prepareStatement(SQL_DELETE_SIGNATURES);
				stmt.setInt(1, ctx.getContextId());
				stmt.setInt(2, user);
				stmt.executeUpdate();
				stmt.close();
				/*
				 * Delete user setting
				 */
				stmt = writeCon.prepareStatement(SQL_DELETE);
				stmt.setInt(1, ctx.getContextId());
				stmt.setInt(2, user);
				stmt.executeUpdate();
				stmt.close();
				stmt = null;
			} finally {
				closeResources(null, stmt, createWriteCon ? writeCon : null, false, ctx);
				stmt = null;
			}
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.SQL_ERROR, e, new Object[0]);
		} catch (DBPoolingException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.DBPOOL_ERROR, e, new Object[0]);
		}
	}
	
	
	public final void loadUserSettingMail(final int user, final Context ctx) throws OXException {
		loadUserSettingMail(user, ctx, null);
	}
	
	public final void loadUserSettingMail(final int user, final Context ctx, Connection readCon) throws OXException {
		try {
			final boolean createCon = (readCon == null);
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				if (createCon) {
					readCon = DBPool.pickup(ctx);
				}
				stmt = readCon.prepareStatement(SQL_LOAD);
				stmt.setInt(1, ctx.getContextId());
				stmt.setInt(2, user);
				rs = stmt.executeQuery();
				if (!rs.next()) {
					rs.close();
					stmt.close();
					Connection writeCon = null;
					try {
						writeCon = DBPool.pickupWriteable(ctx);
						stmt = writeCon.prepareStatement(SQL_INSERT);
						stmt.setInt(1, ctx.getContextId());
						stmt.setInt(2, user);
						stmt.setInt(3, 0);
						//stmt.setString(4, signature);
						stmt.setString(4, sendAddr);
						stmt.setString(5, replyToAddr);
						stmt.setInt(6, msgFormat);
						stmt.setString(7, getDisplayMsgHeadersString());
						stmt.setInt(8, autoLinebreak);
						stmt.setString(9, stdTrashName);
						stmt.setString(10, stdSentName);
						stmt.setString(11, stdDraftsName);
						stmt.setString(12, stdSpamName);
						stmt.setLong(13, uploadQuota);
						stmt.setLong(14, uploadQuotaPerFile);
						stmt.executeUpdate();
						return;
					} finally {
						if (writeCon != null) {
							DBPool.closeWriterSilent(ctx, writeCon);
						}
					}
				}
				parseBits(rs.getInt(1));
				sendAddr = rs.getString(2);
				replyToAddr = rs.getString(3);
				msgFormat = rs.getInt(4);
				setDisplayMsgHeadersString(rs.getString(5));
				autoLinebreak = rs.getInt(6) >= 0 ? rs.getInt(6) : 0;
				stdTrashName = rs.getString(7);
				stdSentName = rs.getString(8);
				stdDraftsName = rs.getString(9);
				stdSpamName = rs.getString(10);
				uploadQuota = rs.getLong(11);
				uploadQuotaPerFile = rs.getLong(12);
				loadSignatures(user, ctx, readCon);
				modifiedDuringSession = false;
			} finally {
				closeResources(rs, stmt, createCon ? readCon : null, true, ctx);
			}
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.SQL_ERROR, e, new Object[0]);
		} catch (DBPoolingException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.DBPOOL_ERROR, e, new Object[0]);
		}
	}
	
	private final void loadSignatures(final int user, final Context ctx, final Connection readConArg) throws OXException {
		try {
			Connection readCon = readConArg;
			final boolean createCon = (readCon == null);
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				if (createCon) {
					readCon = DBPool.pickup(ctx);
				}
				stmt = readCon.prepareStatement(SQL_LOAD_SIGNATURES);
				stmt.setInt(1, ctx.getContextId());
				stmt.setInt(2, user);
				rs = stmt.executeQuery();
				if (rs.next()) {
					final Map<String, String> sigMap = new HashMap<String, String>();
					do {
						sigMap.put(rs.getString(1), rs.getString(2));
					} while (rs.next());
					final int size = sigMap.size();
					signatures = new Signature[size];
					final Iterator<Map.Entry<String,String>> iter = sigMap.entrySet().iterator();
					for (int i = 0; i< size; i++) {
						final Map.Entry<String, String> e = iter.next();
						signatures[i] = new Signature(e.getKey(), e.getValue());
					}
				} else {
					signatures = null;
				}
			} finally {
				closeResources(rs, stmt, createCon ? readCon : null, true, ctx);
			}
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.SQL_ERROR, e, new Object[0]);
		} catch (DBPoolingException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.DBPOOL_ERROR, e, new Object[0]);
		}
	}
	
	private final boolean saveSignatures(final int user, final Context ctx, final Connection writeConArg) throws OXException {
		try {
			Connection writeCon = writeConArg;
			final boolean createCon = (writeCon == null);
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				if (createCon) {
					writeCon = DBPool.pickupWriteable(ctx);
				}
				/*
				 * Delete old
				 */
				stmt = writeCon.prepareStatement(SQL_DELETE_SIGNATURES);
				stmt.setInt(1, ctx.getContextId());
				stmt.setInt(2, user);
				stmt.executeUpdate();
				stmt.close();
				if (signatures == null || signatures.length == 0) {
					return true;
				}
				/*
				 * Insert new
				 */
				stmt = writeCon.prepareStatement(SQL_INSERT_SIGNATURE);
				for (int i = 0; i < signatures.length; i++) {
					final Signature sig = signatures[i];
					stmt.setInt(1, ctx.getContextId());
					stmt.setInt(2, user);
					stmt.setString(3, sig.getId());
					stmt.setString(4, sig.getSignature());
					stmt.addBatch();
				}
				return (stmt.executeBatch().length > 0);
			} finally {
				closeResources(rs, stmt, createCon ? writeCon : null, false, ctx);
			}
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.SQL_ERROR, e, new Object[0]);
		} catch (DBPoolingException e) {
			LOG.error(e.getMessage(), e);
			throw new UserConfigurationException(UserConfigurationCode.DBPOOL_ERROR, e, new Object[0]);
		}
	}
	
	private final void parseBits(final int onOffOptions) {
		displayHtmlInlineContent = ((onOffOptions & INT_DISPLAY_HTML_INLINE_CONTENT) == INT_DISPLAY_HTML_INLINE_CONTENT);
		useColorQuote = ((onOffOptions & INT_USE_COLOR_QUOTE) == INT_USE_COLOR_QUOTE);
		showGraphicEmoticons = ((onOffOptions & INT_SHOW_GRAPHIC_EMOTICONS) == INT_SHOW_GRAPHIC_EMOTICONS);
		hardDeleteMsgs = ((onOffOptions & INT_HARD_DELETE_MSGS) == INT_HARD_DELETE_MSGS);
		forwardAsAttachment = ((onOffOptions & INT_FORWARD_AS_ATTACHMENT) == INT_FORWARD_AS_ATTACHMENT);
		appendVCard = ((onOffOptions & INT_APPEND_VCARD) == INT_APPEND_VCARD);
		notifyOnReadAck = ((onOffOptions & INT_NOTIFY_ON_READ_ACK) == INT_NOTIFY_ON_READ_ACK);
		msgPreview = ((onOffOptions & INT_MSG_PREVIEW) == INT_MSG_PREVIEW);
		notifyAppointments = ((onOffOptions & INT_NOTIFY_APPOINTMENTS) == INT_NOTIFY_APPOINTMENTS);
		notifyTasks = ((onOffOptions & INT_NOTIFY_TASKS) == INT_NOTIFY_TASKS);
		ignoreOriginalMailTextOnReply = ((onOffOptions & INT_IGNORE_ORIGINAL_TEXT_ON_REPLY) == INT_IGNORE_ORIGINAL_TEXT_ON_REPLY);
		noCopyIntoStandardSentFolder = ((onOffOptions & INT_NO_COPY_INTO_SENT_FOLDER) == INT_NO_COPY_INTO_SENT_FOLDER);
	}
	
	private final int getBitsValue() {
		int retval = 0;
		retval = displayHtmlInlineContent ? (retval | INT_DISPLAY_HTML_INLINE_CONTENT) : retval;
		retval = useColorQuote ? (retval | INT_USE_COLOR_QUOTE) : retval;
		retval = showGraphicEmoticons ? (retval | INT_SHOW_GRAPHIC_EMOTICONS) : retval;
		retval = hardDeleteMsgs ? (retval | INT_HARD_DELETE_MSGS) : retval;
		retval = forwardAsAttachment ? (retval | INT_FORWARD_AS_ATTACHMENT) : retval;
		retval = appendVCard ? (retval | INT_APPEND_VCARD) : retval;
		retval = notifyOnReadAck ? (retval | INT_NOTIFY_ON_READ_ACK) : retval;
		retval = msgPreview ? (retval | INT_MSG_PREVIEW) : retval;
		retval = notifyAppointments ? (retval | INT_NOTIFY_APPOINTMENTS) : retval;
		retval = notifyTasks ? (retval | INT_NOTIFY_TASKS) : retval;
		retval = ignoreOriginalMailTextOnReply ? (retval | INT_IGNORE_ORIGINAL_TEXT_ON_REPLY) : retval;
		retval = noCopyIntoStandardSentFolder ? (retval | INT_NO_COPY_INTO_SENT_FOLDER) : retval;
		return retval;
	}
	
	public final boolean isDisplayHtmlInlineContent() {
		return displayHtmlInlineContent;
	}

	public final void setDisplayHtmlInlineContent(final boolean htmlPreview) {
		this.displayHtmlInlineContent = htmlPreview;
		modifiedDuringSession = true;
	}

	public final boolean isShowGraphicEmoticons() {
		return showGraphicEmoticons;
	}

	public final void setShowGraphicEmoticons(final boolean showGraphicEmoticons) {
		this.showGraphicEmoticons = showGraphicEmoticons;
		modifiedDuringSession = true;
	}

	public final boolean isUseColorQuote() {
		return useColorQuote;
	}

	public final void setUseColorQuote(final boolean useColorQuote) {
		this.useColorQuote = useColorQuote;
		modifiedDuringSession = true;
	}
	
	public final boolean isHardDeleteMsgs() {
		return hardDeleteMsgs;
	}

	public final void setHardDeleteMsgs(final boolean hardDeleteMessages) {
		this.hardDeleteMsgs = hardDeleteMessages;
		modifiedDuringSession = true;
	}

	public final boolean isForwardAsAttachment() {
		return forwardAsAttachment;
	}

	public final void setForwardAsAttachment(final boolean forwardAsAttachment) {
		this.forwardAsAttachment = forwardAsAttachment;
		modifiedDuringSession = true;
	}
	
	public final boolean isAppendVCard() {
		return appendVCard;
	}

	public final void setAppendVCard(final boolean appendVCard) {
		this.appendVCard = appendVCard;
		modifiedDuringSession = true;
	}

	public final boolean isNotifyOnReadAck() {
		return notifyOnReadAck;
	}

	public final void setNotifyOnReadAck(final boolean notifyOnReadAck) {
		this.notifyOnReadAck = notifyOnReadAck;
		modifiedDuringSession = true;
	}

	public final Signature[] getSignatures() {
		if (signatures == null) {
			return null;
		}
		final Signature[] retval = new Signature[signatures.length];
		System.arraycopy(signatures, 0, retval, 0, signatures.length);
		return retval;
	}

	public final void setSignatures(final Signature[] signatures) {
		if (signatures == null) {
			this.signatures = null;
			modifiedDuringSession = true;
			return;
		}
		this.signatures = new Signature[signatures.length];
		System.arraycopy(signatures, 0, this.signatures, 0, signatures.length);
		modifiedDuringSession = true;
	}

	public final int getAutoLinebreak() {
		return autoLinebreak;
	}

	public final void setAutoLinebreak(final int autoLineBreak) {
		this.autoLinebreak = autoLineBreak >= 0 ? autoLineBreak : 0;
		modifiedDuringSession = true;
	}

	public final String[] getDisplayMsgHeaders() {
		if (displayMsgHeaders == null) {
			return null;
		}
		final String[] retval = new String[displayMsgHeaders.length];
		System.arraycopy(displayMsgHeaders, 0, retval, 0, displayMsgHeaders.length);
		return retval;
	}

	public final void setDisplayMsgHeaders(final String[] displayMsgHeaders) {
		if (displayMsgHeaders == null) {
			this.displayMsgHeaders = null;
			modifiedDuringSession = true;
			return;
		}
		this.displayMsgHeaders = new String[displayMsgHeaders.length];
		System.arraycopy(displayMsgHeaders, 0, this.displayMsgHeaders, 0, displayMsgHeaders.length);
	}
	
	private final String getDisplayMsgHeadersString() {
		if (displayMsgHeaders == null || displayMsgHeaders.length == 0) {
			return null;
		}
		String tmp = Arrays.toString(displayMsgHeaders);
		tmp = tmp.substring(1, tmp.length() - 1);
		return tmp;
	}
	
	private final void setDisplayMsgHeadersString(final String displayMsgHeadersStr) {
		if (displayMsgHeadersStr == null) {
			this.displayMsgHeaders = null;
			modifiedDuringSession = true;
			return;
		}
		displayMsgHeaders = displayMsgHeadersStr.split(" *, *");
		modifiedDuringSession = true;
	}

	public final int getMsgFormat() {
		return msgFormat;
	}

	public final void setMsgFormat(final int msgFormat) {
		this.msgFormat = msgFormat;
		modifiedDuringSession = true;
	}

	public final boolean isMsgPreview() {
		return msgPreview;
	}

	public final void setMsgPreview(final boolean msgPreview) {
		this.msgPreview = msgPreview;
		modifiedDuringSession = true;
	}
	
	public final boolean isStdFoldersSetDuringSession() {
		return stdFoldersSet;
	}

	public final void setStdFoldersSetDuringSession(final boolean stdFoldersSet) {
		this.stdFoldersSet = stdFoldersSet;
		modifiedDuringSession = true;
	}

	public final String getReplyToAddr() {
		return replyToAddr;
	}

	public final void setReplyToAddr(final String replyToAddr) {
		this.replyToAddr = replyToAddr;
		modifiedDuringSession = true;
	}

	public final String getSendAddr() {
		return sendAddr;
	}

	public final void setSendAddr(final String sendAddr) {
		this.sendAddr = sendAddr;
		modifiedDuringSession = true;
	}

	public final String getStdDraftsName() {
		return stdDraftsName;
	}

	public final void setStdDraftsName(final String stdDraftsName) {
		this.stdDraftsName = stdDraftsName;
		modifiedDuringSession = true;
	}

	public final String getStdSentName() {
		return stdSentName;
	}

	public final void setStdSentName(final String stdSentName) {
		this.stdSentName = stdSentName;
		modifiedDuringSession = true;
	}

	public final String getStdSpamName() {
		return stdSpamName;
	}

	public final void setStdSpamName(final String stdSpamName) {
		this.stdSpamName = stdSpamName;
		modifiedDuringSession = true;
	}

	public final String getStdTrashName() {
		return stdTrashName;
	}

	public final void setStdTrashName(final String stdTrashName) {
		this.stdTrashName = stdTrashName;
		modifiedDuringSession = true;
	}

	public final boolean isNotifyAppointments() {
		return notifyAppointments;
	}

	public final void setNotifyAppointments(final boolean notifyAppointments) {
		this.notifyAppointments = notifyAppointments;
		modifiedDuringSession = true;
	}

	public final boolean isNotifyTasks() {
		return notifyTasks;
	}

	public final void setNotifyTasks(final boolean notifyTasks) {
		this.notifyTasks = notifyTasks;
		modifiedDuringSession = true;
	}

	public final boolean isIgnoreOriginalMailTextOnReply() {
		return ignoreOriginalMailTextOnReply;
	}

	public final void setIgnoreOriginalMailTextOnReply(final boolean appendOriginalMailTextToReply) {
		this.ignoreOriginalMailTextOnReply = appendOriginalMailTextToReply;
		modifiedDuringSession = true;
	}

	public final boolean isNoCopyIntoStandardSentFolder() {
		return noCopyIntoStandardSentFolder;
	}

	public final void setNoCopyIntoStandardSentFolder(final boolean noCopyIntoStandardSentFolder) {
		this.noCopyIntoStandardSentFolder = noCopyIntoStandardSentFolder;
		modifiedDuringSession = true;
	}

	public final long getUploadQuota() {
		return uploadQuota;
	}

	public final void setUploadQuota(final long uploadQuota) {
		this.uploadQuota = uploadQuota;
		modifiedDuringSession = true;
	}

	public final long getUploadQuotaPerFile() {
		return uploadQuotaPerFile;
	}

	public final void setUploadQuotaPerFile(final long uploadQuotaPerFile) {
		this.uploadQuotaPerFile = uploadQuotaPerFile;
		modifiedDuringSession = true;
	}
	
	public final boolean isModifiedDuringSession() {
		return modifiedDuringSession;
	}
	
	public final void setStandardFolder(final int index, final String fullname) {
		this.stdFolderFullnames[index] = fullname;
	}
	
	public final String getStandardFolder(final int index) {
		return this.stdFolderFullnames[index];
	}
	
	public final Lock getStdFolderCreationLock() {
		return stdFolderCreationLock;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.groupware.delete.DeleteListener#deletePerformed(com.openexchange.groupware.delete.DeleteEvent,
	 *      java.sql.Connection, java.sql.Connection)
	 */
	public void deletePerformed(final DeleteEvent delEvent, final Connection readCon, final Connection writeCon)
			throws DeleteFailedException {
		if (delEvent.getType() == DeleteEvent.TYPE_USER) {
			try {
				this.deleteUserSettingMail(delEvent.getId(), delEvent.getContext(), writeCon);
			} catch (OXException e) {
				throw new DeleteFailedException(e.getMessage(), e);
			}
		}
	}
	
	public static class Signature implements Cloneable {
		private String id;

		private String signature;

		public Signature(final String id, final String signature) {
			this.id = id;
			this.signature = signature;
		}

		public String getId() {
			return id;
		}

		public String getSignature() {
			return signature;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#clone()
		 */
		public Object clone() {
			try {
				final Signature clone = (Signature) super.clone();
				clone.id = this.id;
				clone.signature = this.signature;
				return clone;
			} catch (CloneNotSupportedException e) {
				/*
				 * Cannot occur since we are cloneable
				 */
				LOG.error(e.getMessage(), e);
				return null;
			}
		}

	}
}

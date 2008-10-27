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

package com.openexchange.imap.command;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeUtility;

import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.ExtendedMimeMessage;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.BODYSTRUCTURE;
import com.sun.mail.imap.protocol.ENVELOPE;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.INTERNALDATE;
import com.sun.mail.imap.protocol.Item;
import com.sun.mail.imap.protocol.RFC822DATA;
import com.sun.mail.imap.protocol.RFC822SIZE;
import com.sun.mail.imap.protocol.UID;

/**
 * {@link FetchIMAPCommand} - performs a prefetch of messages in given folder
 * with only those fields set that need to be present for display and sorting. A
 * corresponding instance of <code>javax.mail.FetchProfile</code> is going to be
 * generated from given fields.
 * 
 * <p>
 * This method avoids calling JavaMail's fetch() methods which implicitly
 * requests whole message envelope (FETCH 1:* (ENVELOPE INTERNALDATE
 * RFC822.SIZE)) when later working on returned <code>javax.mail.Message</code>
 * objects.
 * 
 * </p>
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class FetchIMAPCommand extends AbstractIMAPCommand<Message[]> {

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(FetchIMAPCommand.class);

	private static interface SeqNumFetcher {
		public int getNextSeqNum(int messageIndex);

		public int getIndexOf(int value);
	}

	private static class MsgSeqNumFetcher implements SeqNumFetcher {

		private final SeqNumFetcher delegate;

		public MsgSeqNumFetcher(final Message[] msgs) {
			/*
			 * Create array from messages' sequence numbers
			 */
			final int[] arr = new int[msgs.length];
			for (int i = 0; i < arr.length; i++) {
				arr[i] = msgs[i].getMessageNumber();
			}
			/*
			 * Create delegate
			 */
			this.delegate = new IntSeqNumFetcher(arr);
		}

		public int getNextSeqNum(final int index) {
			return delegate.getNextSeqNum(index);
		}

		public int getIndexOf(final int value) {
			return delegate.getIndexOf(value);
		}
	}

	private static class IntSeqNumFetcher implements SeqNumFetcher {

		private final int[] arr;

		public IntSeqNumFetcher(final int[] arr) {
			this.arr = arr;
		}

		public int getNextSeqNum(final int index) {
			return arr[index];
		}

		public int getIndexOf(final int value) {
			for (int i = 0; i < arr.length; i++) {
				if (arr[i] == value) {
					return i;
				}
			}
			return -1;
		}
	}

	private final char separator;

	private String[] args;

	private final String command;

	private SeqNumFetcher seqNumFetcher;

	private boolean uid;

	private int length;

	private int index;

	private FetchItemHandler[] itemHandlers;

	private ExtendedMimeMessage[] retval;

	private final boolean loadBody;

	/**
	 * Constructor
	 * 
	 * @param imapFolder
	 *            - the IMAP folder
	 * @param isRev1
	 *            Whether IMAP server has <i>IMAP4rev1</i> capability or not
	 * @param arr
	 *            - the source array (either <code>long</code> UIDs,
	 *            <code>int</code> SeqNums or instances of <code>Message</code>)
	 * @param fp
	 *            - the fetch profile
	 * @param isSequential
	 *            - whether the source array values are sequential
	 * @param keepOrder
	 *            - whether to keep or to ignore given order through parameter
	 *            <code>arr</code>; only has effect if parameter
	 *            <code>arr</code> is of type <code>Message[]</code> or
	 *            <code>int[]</code>
	 * @throws MessagingException
	 */
	public FetchIMAPCommand(final IMAPFolder imapFolder, final boolean isRev1, final Object arr, final FetchProfile fp,
			final boolean isSequential, final boolean keepOrder) throws MessagingException {
		this(imapFolder, isRev1, arr, fp, isSequential, keepOrder, false);
	}

	/**
	 * Constructor
	 * 
	 * @param imapFolder
	 *            - the IMAP folder
	 * @param isRev1
	 *            Whether IMAP server has <i>IMAP4rev1</i> capability or not
	 * @param arr
	 *            - the source array (either <code>long</code> UIDs,
	 *            <code>int</code> SeqNums or instances of <code>Message</code>)
	 * @param fp
	 *            - the fetch profile
	 * @param isSequential
	 *            - whether the source array values are sequential
	 * @param keepOrder
	 *            - whether to keep or to ignore given order through parameter
	 *            <code>arr</code>; only has effect if parameter
	 *            <code>arr</code> is of type <code>Message[]</code> or
	 *            <code>int[]</code>
	 * @param loadBody
	 *            <code>true</code> to load complete messages' bodies; otherwise
	 *            <code>false</code>
	 * @throws MessagingException
	 */
	public FetchIMAPCommand(final IMAPFolder imapFolder, final boolean isRev1, final Object arr, final FetchProfile fp,
			final boolean isSequential, final boolean keepOrder, final boolean loadBody) throws MessagingException {
		super(imapFolder);
		if (imapFolder.getMessageCount() == 0) {
			returnDefaultValue = true;
		}
		this.loadBody = loadBody;
		this.separator = imapFolder.getSeparator();
		command = getFetchCommand(isRev1, fp, loadBody);
		set(arr, isSequential, keepOrder);
	}

	/**
	 * Apply a new numeric argument to this IMAP <i>FETCH</i> command
	 * 
	 * @param arr
	 *            - the source array (either <code>long</code> UIDs,
	 *            <code>int</code> SeqNums or instances of <code>Message</code>)
	 * @param isSequential
	 *            whether the source array values are sequential
	 * @param keepOrder
	 *            whether to keep or to ignore given order through parameter
	 *            <code>arr</code>; only has effect if parameter
	 *            <code>arr</code> is of type <code>Message[]</code> or
	 *            <code>int[]</code>
	 * @throws MessagingException
	 */
	public void set(final Object arr, final boolean isSequential, final boolean keepOrder) throws MessagingException {
		if (null == arr) {
			returnDefaultValue = true;
		} else {
			createArgs(arr, isSequential, keepOrder);
		}
		retval = new ExtendedMimeMessage[length];
		index = 0;
	}

	private void createArgs(final Object arr, final boolean isSequential, final boolean keepOrder)
			throws MessagingException {
		if (arr instanceof int[]) {
			final int[] seqNums = (int[]) arr;
			uid = false;
			length = seqNums.length;
			args = isSequential ? new String[] { new StringBuilder(64).append(seqNums[0]).append(':').append(
					seqNums[seqNums.length - 1]).toString() } : IMAPNumArgSplitter.splitSeqNumArg(seqNums, keepOrder);
			seqNumFetcher = keepOrder ? new IntSeqNumFetcher(seqNums) : null;
		} else if (arr instanceof long[]) {
			if (keepOrder) {
				/*
				 * Turn UIDs to corresponding sequence number to initialize
				 * seqNumFetcher which keeps track or proper order
				 */
				final int[] seqNums = IMAPCommandsCollection.uids2SeqNums(imapFolder, (long[]) arr);
				uid = false;
				length = seqNums.length;
				args = isSequential ? new String[] { new StringBuilder(64).append(seqNums[0]).append(':').append(
						seqNums[seqNums.length - 1]).toString() } : IMAPNumArgSplitter.splitSeqNumArg(seqNums,
						keepOrder);
				seqNumFetcher = keepOrder ? new IntSeqNumFetcher(seqNums) : null;
			} else {
				final long[] uids = (long[]) arr;
				uid = true;
				length = uids.length;
				args = isSequential ? new String[] { new StringBuilder(64).append(uids[0]).append(':').append(
						uids[uids.length - 1]).toString() } : IMAPNumArgSplitter.splitUIDArg(uids, true);
				seqNumFetcher = null;
			}
		} else if (arr instanceof Message[]) {
			final Message[] msgs = (Message[]) arr;
			uid = false;
			length = msgs.length;
			args = isSequential ? new String[] { new StringBuilder(64).append(msgs[0].getMessageNumber()).append(':')
					.append(msgs[msgs.length - 1].getMessageNumber()).toString() } : IMAPNumArgSplitter
					.splitMessageArg(msgs, keepOrder);
			seqNumFetcher = keepOrder ? new MsgSeqNumFetcher(msgs) : null;
		} else {
			throw new MessagingException(new StringBuilder("Invalid array type! ").append(arr.getClass().getName())
					.toString());
		}
	}

	/**
	 * Constructor to fetch all messages of given folder
	 * <p>
	 * <b>Note</b>: Ensure that denoted folder is empty
	 * 
	 * @param imapFolder
	 *            - the IMAP folder
	 * @param isRev1
	 *            Whether IMAP server has <i>IMAP4rev1</i> capability or not
	 * @param fp
	 *            - the fetch profile
	 * @param fetchLen
	 *            - the total message count
	 * @throws MessagingException
	 *             If a messaging error occurs
	 */
	public FetchIMAPCommand(final IMAPFolder imapFolder, final boolean isRev1, final FetchProfile fp, final int fetchLen)
			throws MessagingException {
		this(imapFolder, isRev1, fp, fetchLen, false);
	}

	/**
	 * Constructor to fetch all messages of given folder
	 * <p>
	 * <b>Note</b>: Ensure that denoted folder is not empty.
	 * 
	 * @param imapFolder
	 *            - the IMAP folder
	 * @param isRev1
	 *            Whether IMAP server has <i>IMAP4rev1</i> capability or not
	 * @param fp
	 *            - the fetch profile
	 * @param fetchLen
	 *            - the total message count
	 * @param loadBody
	 *            <code>true</code> to load complete messages' bodies; otherwise
	 *            <code>false</code>
	 * @throws MessagingException
	 *             If a messaging error occurs
	 */
	public FetchIMAPCommand(final IMAPFolder imapFolder, final boolean isRev1, final FetchProfile fp,
			final int fetchLen, final boolean loadBody) throws MessagingException {
		super(imapFolder);
		if (imapFolder.getMessageCount() == 0) {
			returnDefaultValue = true;
		}
		this.loadBody = loadBody;
		this.separator = imapFolder.getSeparator();
		if (0 == fetchLen) {
			returnDefaultValue = true;
		}
		args = AbstractIMAPCommand.ARGS_ALL;
		uid = false;
		length = fetchLen;
		command = getFetchCommand(isRev1, fp, loadBody);
		retval = new ExtendedMimeMessage[length];
		index = 0;
	}

	@Override
	protected String getDebugInfo(final int argsIndex) {
		final StringBuilder sb = new StringBuilder(command.length() + 64);
		if (uid) {
			sb.append("UID ");
		}
		sb.append("FETCH ");
		final String arg = this.args[argsIndex];
		if (arg.length() > 32) {
			final int pos = arg.indexOf(',');
			if (pos == -1) {
				sb.append("...");
			} else {
				sb.append(arg.substring(0, pos)).append(",...,").append(arg.substring(arg.lastIndexOf(',') + 1));
			}
		} else {
			sb.append(arg);
		}
		sb.append(" (").append(command).append(')');
		return sb.toString();
	}

	@Override
	protected boolean addLoopCondition() {
		return (index < length);
	}

	@Override
	protected String[] getArgs() {
		return args;
	}

	@Override
	protected String getCommand(final int argsIndex) {
		final StringBuilder sb = new StringBuilder(args[argsIndex].length() + 64);
		if (uid) {
			sb.append("UID ");
		}
		sb.append("FETCH ");
		sb.append(args[argsIndex]);
		sb.append(" (").append(command).append(')');
		return sb.toString();
	}

	private static final ExtendedMimeMessage[] EMPTY_ARR = new ExtendedMimeMessage[0];

	@Override
	protected Message[] getDefaultValue() {
		return EMPTY_ARR;
	}

	@Override
	protected Message[] getReturnVal() {
		return retval;
	}

	@Override
	protected void handleLastResponse(final Response lastResponse) throws ProtocolException {
		if (!lastResponse.isOK()) {
			throw new ProtocolException(lastResponse);
		}
	}

	@Override
	protected void handleResponse(final Response currentReponse) throws MessagingException {
		/*
		 * Response is null or not a FetchResponse
		 */
		if (!FetchResponse.class.isInstance(currentReponse)) {
			return;
		}
		final FetchResponse fetchResponse = (FetchResponse) currentReponse;
		int seqnum;
		final int pos;
		if (null == seqNumFetcher) {
			seqnum = fetchResponse.getNumber();
			pos = index;
		} else {
			seqnum = seqNumFetcher.getNextSeqNum(index);
			if (seqnum == fetchResponse.getNumber()) {
				pos = index;
			} else {
				/*
				 * Assign to current response's sequence number
				 */
				seqnum = fetchResponse.getNumber();
				/*
				 * Look-up position
				 */
				pos = seqNumFetcher.getIndexOf(seqnum);
				if (pos == -1) {
					throw new MessagingException("Unexpected sequence number in untagged FETCH response: " + seqnum);
				}
				seqnum = fetchResponse.getNumber();
			}
		}
		index++;
		final ExtendedMimeMessage msg = new ExtendedMimeMessage(imapFolder.getFullName(), separator, seqnum);
		final int itemCount = fetchResponse.getItemCount();
		if ((itemHandlers == null) || (itemCount != itemHandlers.length)) {
			itemHandlers = createItemHandlers(itemCount, fetchResponse, loadBody);
		}
		boolean repeatItem = true;
		boolean error = false;
		do {
			try {
				for (int j = 0; j < itemCount; j++) {
					itemHandlers[j].handleItem(fetchResponse.getItem(j), msg);
				}
				repeatItem = false;
			} catch (final MessagingException e) {
				/*
				 * Discard corrupt message
				 */
				final MailException imapExc = MIMEMailException.handleMessagingException(e);
				LOG.error(new StringBuilder(128).append("Message #").append(msg.getMessageNumber()).append(
						" discarded: ").append(imapExc.getMessage()).toString(), imapExc);
				error = true;
				repeatItem = false;
			} catch (final MailException e) {
				/*
				 * Discard corrupt message
				 */
				LOG.error(new StringBuilder(128).append("Message #").append(msg.getMessageNumber()).append(
						" discarded: ").append(e.getMessage()).toString(), e);
				error = true;
				repeatItem = false;
			} catch (final ClassCastException e) {
				/*
				 * Obviously the order of fetch items has changed during FETCH
				 * response. Re-Build fetch item handlers according to current
				 * untagged fetch response.
				 */
				itemHandlers = createItemHandlers(itemCount, fetchResponse, loadBody);
				repeatItem = true;
			}
		} while (repeatItem);
		if (!error) {
			retval[pos] = msg;
		}
	}

	@Override
	protected boolean performHandleResult() {
		return true;
	}

	@Override
	protected boolean performNotifyResponseHandlers() {
		return false;
	}

	private static final Set<Integer> ENV_FIELDS;

	static {
		ENV_FIELDS = new HashSet<Integer>(6);
		/*
		 * The Envelope is an aggregation of the common attributes of a Message:
		 * From, To, Cc, Bcc, ReplyTo, Subject and Date.
		 */
		ENV_FIELDS.add(Integer.valueOf(MailListField.FROM.getField()));
		ENV_FIELDS.add(Integer.valueOf(MailListField.TO.getField()));
		ENV_FIELDS.add(Integer.valueOf(MailListField.CC.getField()));
		ENV_FIELDS.add(Integer.valueOf(MailListField.BCC.getField()));
		ENV_FIELDS.add(Integer.valueOf(MailListField.SUBJECT.getField()));
		ENV_FIELDS.add(Integer.valueOf(MailListField.SENT_DATE.getField()));
		/*
		 * Discard the two extra fetch profile items contained in JavaMail's
		 * ENVELOPE constant: RFC822.SIZE and INTERNALDATE
		 */
		//ENV_FIELDS.add(Integer.valueOf(MailListField.RECEIVED_DATE.getField())
		// );
		// ENV_FIELDS.add(Integer.valueOf(MailListField.SIZE.getField()));
	}

	private static void addFetchItem(final FetchProfile fp, final int field) {
		if (field == MailListField.ID.getField()) {
			fp.add(UIDFolder.FetchProfileItem.UID);
		} else if (field == MailListField.ATTACHMENT.getField()) {
			fp.add(FetchProfile.Item.CONTENT_INFO);
		} else if (field == MailListField.FROM.getField()) {
			fp.add(MessageHeaders.HDR_FROM);
		} else if (field == MailListField.TO.getField()) {
			fp.add(MessageHeaders.HDR_TO);
		} else if (field == MailListField.CC.getField()) {
			fp.add(MessageHeaders.HDR_CC);
		} else if (field == MailListField.BCC.getField()) {
			fp.add(MessageHeaders.HDR_BCC);
		} else if (field == MailListField.SUBJECT.getField()) {
			fp.add(MessageHeaders.HDR_SUBJECT);
		} else if (field == MailListField.SIZE.getField()) {
			fp.add(IMAPFolder.FetchProfileItem.SIZE);
		} else if (field == MailListField.SENT_DATE.getField()) {
			fp.add(MessageHeaders.HDR_DATE);
		} else if (field == MailListField.FLAGS.getField()) {
			if (!fp.contains(FetchProfile.Item.FLAGS)) {
				fp.add(FetchProfile.Item.FLAGS);
			}
		} else if (field == MailListField.DISPOSITION_NOTIFICATION_TO.getField()) {
			fp.add(MessageHeaders.HDR_DISP_NOT_TO);
		} else if (field == MailListField.PRIORITY.getField()) {
			fp.add(MessageHeaders.HDR_X_PRIORITY);
		} else if (field == MailListField.COLOR_LABEL.getField()) {
			if (!fp.contains(FetchProfile.Item.FLAGS)) {
				fp.add(FetchProfile.Item.FLAGS);
			}
		} else if ((field == MailListField.FLAG_SEEN.getField()) && !fp.contains(FetchProfile.Item.FLAGS)) {
			fp.add(FetchProfile.Item.FLAGS);
		}
	}

	private static interface HeaderHandler {
		public void handleHeader(String hdrValue, ExtendedMimeMessage msg) throws MessagingException, MailException;
	}

	private static abstract class FetchItemHandler {

		static final MailDateFormat mailDateFormat = new MailDateFormat();

		private Map<String, HeaderHandler> hdrHandlers;

		public FetchItemHandler() {
			super();
		}

		public final Map<String, HeaderHandler> getHdrHandlers() {
			return hdrHandlers;
		}

		public final HeaderHandler getHdrHandler(final String headerName) {
			return hdrHandlers.get(headerName);
		}

		public final int getHeadersSize() {
			return hdrHandlers.size();
		}

		public final boolean containsHeaderHandlers() {
			return this.hdrHandlers != null;
		}

		public final static void createHeaderHandlers(final FetchItemHandler itemHandler, final InternetHeaders h) {
			itemHandler.hdrHandlers = new HashMap<String, HeaderHandler>();
			for (final Enumeration<?> e = h.getAllHeaders(); e.hasMoreElements();) {
				final Header hdr = (Header) e.nextElement();
				addHeaderHandlers(itemHandler, hdr);
			}
		}

		public final static void addHeaderHandlers(final FetchItemHandler itemHandler, final Header hdr) {
			if (hdr.getName().equals(MessageHeaders.HDR_FROM)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_FROM, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						try {
							msg.addFrom(InternetAddress.parse(hdrValue, false));
						} catch (final AddressException e) {
							msg.setHeader(MessageHeaders.HDR_FROM, hdrValue);
						}
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_TO)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_TO, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						try {
							msg.setRecipients(RecipientType.TO, InternetAddress.parse(hdrValue, false));
						} catch (final AddressException e) {
							msg.setHeader(MessageHeaders.HDR_TO, hdrValue);
						}
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_CC)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_CC, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						try {
							msg.setRecipients(RecipientType.CC, InternetAddress.parse(hdrValue, false));
						} catch (final AddressException e) {
							msg.setHeader(MessageHeaders.HDR_CC, hdrValue);
						}
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_BCC)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_BCC, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						try {
							msg.setRecipients(RecipientType.BCC, InternetAddress.parse(hdrValue, false));
						} catch (final AddressException e) {
							msg.setHeader(MessageHeaders.HDR_BCC, hdrValue);
						}
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_REPLY_TO)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_REPLY_TO, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						try {
							msg.setReplyTo(InternetAddress.parse(hdrValue, true));
						} catch (final AddressException e) {
							msg.setHeader(MessageHeaders.HDR_REPLY_TO, hdrValue);
						}
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_SUBJECT)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_SUBJECT, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						String decVal = MIMEMessageUtility.decodeMultiEncodedHeader(hdrValue);
						if (decVal.indexOf("=?") == -1) {
							/*
							 * Something went wrong during decoding
							 */
							try {
								decVal = MimeUtility.decodeText(MimeUtility.unfold(hdrValue));
							} catch (final UnsupportedEncodingException e) {
								LOG.error("Unsupported encoding in a message detected and monitored.", e);
								MailServletInterface.mailInterfaceMonitor.addUnsupportedEncodingExceptions(e
										.getMessage());
								decVal = hdrValue;
							}
						}
						msg.setSubject(decVal, MailConfig.getDefaultMimeCharset());
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_DATE)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_DATE, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						try {
							msg.setSentDate(mailDateFormat.parse(hdrValue));
						} catch (final ParseException e) {
							throw new MessagingException(e.getMessage());
						}
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_X_PRIORITY)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_X_PRIORITY, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						msg.setHeader(MessageHeaders.HDR_X_PRIORITY, hdrValue);
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_MESSAGE_ID)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_MESSAGE_ID, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						msg.setHeader(MessageHeaders.HDR_MESSAGE_ID, hdrValue);
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_IN_REPLY_TO)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_IN_REPLY_TO, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						msg.setHeader(MessageHeaders.HDR_IN_REPLY_TO, hdrValue);
					}
				});
			} else if (hdr.getName().equals(MessageHeaders.HDR_REFERENCES)) {
				itemHandler.hdrHandlers.put(MessageHeaders.HDR_REFERENCES, new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						msg.setHeader(MessageHeaders.HDR_REFERENCES, hdrValue);
					}
				});
			} else {
				itemHandler.hdrHandlers.put(hdr.getName(), new HeaderHandler() {
					public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg)
							throws MessagingException {
						msg.setHeader(hdr.getName(), hdrValue);
					}
				});
			}
		}

		/**
		 * Handles given <code>com.sun.mail.imap.protocol.Item</code> instance
		 * and applies it to given message.
		 */
		public abstract void handleItem(final Item item, final ExtendedMimeMessage msg) throws MessagingException,
				MailException;
	}

	private static final String MULTI_SUBTYPE_MIXED = "MIXED";

	/*
	 * ++++++++++++++ Item handlers ++++++++++++++
	 */

	private static final FetchItemHandler FLAGS_ITEM_HANDLER = new FetchItemHandler() {
		@Override
		public void handleItem(final Item item, final ExtendedMimeMessage msg) throws MessagingException {
			msg.setFlags((Flags) item, true);
		}
	};

	private static final FetchItemHandler ENVELOPE_ITEM_HANDLER = new FetchItemHandler() {
		@Override
		public void handleItem(final Item item, final ExtendedMimeMessage msg) throws MessagingException {
			final ENVELOPE env = (ENVELOPE) item;
			msg.addFrom(env.from);
			msg.setRecipients(RecipientType.TO, env.to);
			msg.setRecipients(RecipientType.CC, env.cc);
			msg.setRecipients(RecipientType.BCC, env.bcc);
			msg.setReplyTo(env.replyTo);
			msg.setHeader(MessageHeaders.HDR_IN_REPLY_TO, env.inReplyTo);
			msg.setHeader(MessageHeaders.HDR_MESSAGE_ID, env.messageId);
			try {
				msg.setSubject(env.subject == null ? "" : MimeUtility.decodeText(env.subject), MailConfig
						.getDefaultMimeCharset());
			} catch (final UnsupportedEncodingException e) {
				LOG.error("Unsupported encoding in a message detected and monitored.", e);
				MailServletInterface.mailInterfaceMonitor.addUnsupportedEncodingExceptions(e.getMessage());
				msg.setSubject(MIMEMessageUtility.decodeMultiEncodedHeader(env.subject));
			}
			msg.setSentDate(env.date);
		}
	};

	private static final FetchItemHandler INTERNALDATE_ITEM_HANDLER = new FetchItemHandler() {
		@Override
		public void handleItem(final Item item, final ExtendedMimeMessage msg) {
			msg.setReceivedDate(((INTERNALDATE) item).getDate());
		}
	};

	private static final FetchItemHandler SIZE_ITEM_HANDLER = new FetchItemHandler() {
		@Override
		public void handleItem(final Item item, final ExtendedMimeMessage msg) {
			msg.setSize(((RFC822SIZE) item).size);
		}
	};

	private static final FetchItemHandler BODYSTRUCTURE_ITEM_HANDLER = new FetchItemHandler() {
		@Override
		public void handleItem(final Item item, final ExtendedMimeMessage msg) throws MailException {
			final BODYSTRUCTURE bs = (BODYSTRUCTURE) item;
			msg.setBodystructure(bs);
			final StringBuilder sb = new StringBuilder();
			sb.append(bs.type).append('/').append(bs.subtype);
			if (bs.cParams != null) {
				sb.append(bs.cParams);
			}
			try {
				msg.setContentType(new ContentType(sb.toString()));
			} catch (final MailException e) {
				if (LOG.isWarnEnabled()) {
					LOG.warn(e.getMessage(), e);
				}
				msg.setContentType(new ContentType(MIMETypes.MIME_DEFAULT));
			}
			msg.setHasAttachment(bs.isMulti()
					&& (MULTI_SUBTYPE_MIXED.equalsIgnoreCase(bs.subtype) || MIMEMessageUtility.hasAttachments(bs)));
		}
	};

	private static final FetchItemHandler UID_ITEM_HANDLER = new FetchItemHandler() {
		@Override
		public void handleItem(final Item item, final ExtendedMimeMessage msg) {
			msg.setUid(((UID) item).uid);
		}
	};

	private static final FetchItemHandler HEADER_ITEM_HANDLER = new FetchItemHandler() {
		@Override
		public void handleItem(final Item item, final ExtendedMimeMessage msg) throws MessagingException, MailException {
			final InternetHeaders h;
			{
				final InputStream headerStream;
				if (item instanceof RFC822DATA) {
					/*
					 * IMAP4
					 */
					headerStream = ((RFC822DATA) item).getByteArrayInputStream();
				} else {
					/*
					 * IMAP4rev1
					 */
					headerStream = ((BODY) item).getByteArrayInputStream();
				}
				h = new InternetHeaders();
				if (null == headerStream) {
					if (LOG.isWarnEnabled()) {
						LOG.warn(new StringBuilder(32).append("Cannot retrieve headers from message #").append(
								msg.getMessageNumber()).append(" in folder ").append(msg.getFullname()).toString());
					}
				} else {
					h.load(headerStream);
				}
			}
			if (!this.containsHeaderHandlers()) {
				FetchItemHandler.createHeaderHandlers(this, h);
			}
			for (final Enumeration<?> e = h.getAllHeaders(); e.hasMoreElements();) {
				final Header hdr = (Header) e.nextElement();
				HeaderHandler hdrHandler = this.getHdrHandler(hdr.getName());
				if (hdrHandler == null) {
					FetchItemHandler.addHeaderHandlers(this, hdr);
					hdrHandler = this.getHdrHandler(hdr.getName());
					hdrHandler.handleHeader(hdr.getValue(), msg);
				} else {
					hdrHandler.handleHeader(hdr.getValue(), msg);
				}
			}
		}
	};

	private static final FetchItemHandler BODY_ITEM_HANDLER = new FetchItemHandler() {
		@Override
		public void handleItem(final Item item, final ExtendedMimeMessage msg) throws MessagingException, MailException {
			final InputStream msgStream;
			if (item instanceof RFC822DATA) {
				/*
				 * IMAP4
				 */
				msgStream = ((RFC822DATA) item).getByteArrayInputStream();
			} else {
				/*
				 * IMAP4rev1
				 */
				msgStream = ((BODY) item).getByteArrayInputStream();
			}
			if (null == msgStream) {
				if (LOG.isWarnEnabled()) {
					LOG.warn(new StringBuilder(32).append("Cannot retrieve body from message #").append(
							msg.getMessageNumber()).append(" in folder ").append(msg.getFullname()).toString());
				}
			} else {
				msg.parseStream(msgStream);
			}
		}
	};

	private static FetchItemHandler[] createItemHandlers(final int itemCount, final FetchResponse f,
			final boolean loadBody) {
		final FetchItemHandler[] itemHandlers = new FetchItemHandler[itemCount];
		for (int j = 0; j < itemCount; j++) {
			final Item item = f.getItem(j);
			if (item instanceof UID) {
				itemHandlers[j] = UID_ITEM_HANDLER;
			} else if (item instanceof INTERNALDATE) {
				itemHandlers[j] = INTERNALDATE_ITEM_HANDLER;
			} else if (item instanceof Flags) {
				itemHandlers[j] = FLAGS_ITEM_HANDLER;
			} else if (item instanceof ENVELOPE) {
				itemHandlers[j] = ENVELOPE_ITEM_HANDLER;
			} else if (item instanceof RFC822SIZE) {
				itemHandlers[j] = SIZE_ITEM_HANDLER;
			} else if (item instanceof BODYSTRUCTURE) {
				itemHandlers[j] = BODYSTRUCTURE_ITEM_HANDLER;
			} else if ((item instanceof RFC822DATA) || (item instanceof BODY)) {
				if (loadBody) {
					itemHandlers[j] = BODY_ITEM_HANDLER;
				} else {
					itemHandlers[j] = HEADER_ITEM_HANDLER;
				}
			}
		}
		return itemHandlers;
	}

	/*
	 * ++++++++++++++ End of item handlers ++++++++++++++
	 */

	private static final String EnvelopeCmd = "ENVELOPE INTERNALDATE RFC822.SIZE";

	private static String getFetchCommand(final boolean isRev1, final FetchProfile fp, final boolean loadBody) {
		final StringBuilder command = new StringBuilder(128);
		final boolean envelope;
		if (fp.contains(FetchProfile.Item.ENVELOPE)) {
			if (loadBody) {
				command.append("INTERNALDATE");
				envelope = false;
			} else {
				command.append(EnvelopeCmd);
				envelope = true;
			}
		} else {
			command.append("INTERNALDATE");
			envelope = false;
		}
		if (fp.contains(FetchProfile.Item.FLAGS)) {
			command.append(" FLAGS");
		}
		if (fp.contains(FetchProfile.Item.CONTENT_INFO)) {
			command.append(" BODYSTRUCTURE");
		}
		if (fp.contains(UIDFolder.FetchProfileItem.UID)) {
			command.append(" UID");
		}
		boolean allHeaders = false;
		if (fp.contains(IMAPFolder.FetchProfileItem.HEADERS) && !loadBody) {
			allHeaders = true;
			if (isRev1) {
				command.append(" BODY.PEEK[HEADER]");
			} else {
				command.append(" RFC822.HEADER");
			}
		}
		if (!envelope && fp.contains(IMAPFolder.FetchProfileItem.SIZE)) {
			command.append(" RFC822.SIZE");
		}
		/*
		 * If we're not fetching all headers, fetch individual headers
		 */
		if (!allHeaders && !loadBody) {
			final String[] hdrs = fp.getHeaderNames();
			if (hdrs.length > 0) {
				command.append(' ');
				command.append(createHeaderCmd(isRev1, hdrs));
			}
		}
		if (loadBody) {
			/*
			 * Load full message
			 */
			if (isRev1) {
				command.append(" BODY.PEEK[]");
			} else {
				command.append(" RFC822");
			}
		}
		return command.toString();
	}

	private static String createHeaderCmd(final boolean isREV1, final String[] hdrs) {
		final StringBuilder sb;
		if (isREV1) {
			sb = new StringBuilder("BODY.PEEK[HEADER.FIELDS (");
		} else {
			sb = new StringBuilder("RFC822.HEADER.LINES (");
		}
		sb.append(hdrs[0]);
		for (int i = 1; i < hdrs.length; i++) {
			sb.append(' ');
			sb.append(hdrs[i]);
		}
		if (isREV1) {
			sb.append(")]");
		} else {
			sb.append(')');
		}
		return sb.toString();
	}

}

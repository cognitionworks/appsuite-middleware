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

package com.openexchange.mail.mime.filler;

import static com.openexchange.mail.text.HTMLProcessing.formatHrefLinks;
import static com.openexchange.mail.text.HTMLProcessing.getConformHTML;
import static com.openexchange.mail.text.HTMLProcessing.replaceHTMLSimpleQuotesForDisplay;
import static com.openexchange.mail.text.TextProcessing.performLineFolding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import com.openexchange.api2.OXException;
import com.openexchange.api2.RdbContactSQLInterface;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contact.Contacts;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.MailStrings;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.upload.ManagedUploadFile;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailSessionParameterNames;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.ComposedMailPart;
import com.openexchange.mail.dataobjects.compose.ReferencedMailPart;
import com.openexchange.mail.dataobjects.compose.ComposedMailPart.ComposedPartType;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEType2ExtMap;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.openexchange.mail.text.Html2TextConverter;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.server.impl.DBPool;
import com.openexchange.server.impl.Version;
import com.openexchange.session.Session;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;
import com.openexchange.tools.versit.Versit;
import com.openexchange.tools.versit.VersitDefinition;
import com.openexchange.tools.versit.VersitObject;
import com.openexchange.tools.versit.converter.ConverterException;
import com.openexchange.tools.versit.converter.OXContainerConverter;

/**
 * {@link MIMEMessageFiller} - Provides basic methods to fills an instance of
 * {@link MimeMessage} with headers/contents given through an instance of
 * {@link ComposedMailMessage}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public class MIMEMessageFiller {

	private static final String PREFIX_PART = "part";

	private static final String EXT_EML = ".eml";

	private static final int BUF_SIZE = 0x2000;

	private static final String VERSION_1_0 = "1.0";

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(MIMEMessageFiller.class);

	private static final String VCARD_ERROR = "Error while appending user VCard";

	/*
	 * Constants for Multipart types
	 */
	private static final String MP_ALTERNATIVE = "alternative";

	private static final String MP_RELATED = "related";

	/*
	 * Patterns for common MIME text types
	 */
	private static final String REPLACE_CS = "#CS#";

	private static final String PAT_TEXT_CT = "text/plain; charset=#CS#";

	private static final String PAT_HTML_CT = "text/html; charset=#CS#";

	/*
	 * Fields
	 */
	protected final Session session;

	protected final Context ctx;

	protected final UserSettingMail usm;

	private Set<String> uploadFileIDs;

	private Html2TextConverter converter;

	/**
	 * Initializes a new {@link MIMEMessageFiller}
	 * 
	 * @param session
	 *            The session providing user data
	 * @param ctx
	 *            The context
	 */
	public MIMEMessageFiller(final Session session, final Context ctx) {
		this(session, ctx, null);
	}

	/**
	 * Initializes a new {@link MIMEMessageFiller}
	 * 
	 * @param session
	 *            The session providing user data
	 * @param ctx
	 *            The context
	 * @param usm
	 *            The user's mail settings
	 */
	public MIMEMessageFiller(final Session session, final Context ctx, final UserSettingMail usm) {
		super();
		this.session = session;
		this.ctx = ctx;
		this.usm = usm == null ? UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), ctx)
				: usm;
	}

	protected final Html2TextConverter getConverter() {
		if (converter == null) {
			converter = new Html2TextConverter();
		}
		return converter;
	}

	/**
	 * Deletes referenced local uploaded files from session and disk after
	 * filled instance of <code>{@link MimeMessage}</code> is dispatched
	 */
	public void deleteReferencedUploadFiles() {
		if (uploadFileIDs != null) {
			final int size = uploadFileIDs.size();
			final Iterator<String> iter = uploadFileIDs.iterator();
			final StringBuilder sb;
			if (LOG.isInfoEnabled()) {
				sb = new StringBuilder(128);
			} else {
				sb = null;
			}
			for (int i = 0; i < size; i++) {
				final ManagedUploadFile uploadFile = session.removeUploadedFile(iter.next());
				final String fileName = uploadFile.getFile().getName();
				uploadFile.delete();
				if (null != sb) {
					sb.setLength(0);
					LOG.info(sb.append("Upload file \"").append(fileName).append(
							"\" removed from session and deleted from disk"));
				}
			}
			uploadFileIDs.clear();
		}
	}

	/**
	 * Sets common headers in given MIME message: <code>X-Mailer</code> and
	 * <code>Organization</code>.
	 * 
	 * @param mimeMessage
	 *            The MIME message
	 * @throws MessagingException
	 *             If headers cannot be set
	 */
	public void setCommonHeaders(final MimeMessage mimeMessage) throws MessagingException {
		/*
		 * Set mailer
		 */
		mimeMessage.setHeader(MessageHeaders.HDR_X_MAILER, "Open-Xchange Mailer v" + Version.VERSION_STRING);
		/*
		 * Set organization to context-admin's company field setting
		 */
		try {
			final Object org = session.getParameter(MailSessionParameterNames.PARAM_ORGANIZATION_HDR);
			if (null == org) {
				/*
				 * Get context's admin contact object
				 */
				final ContactObject c = new RdbContactSQLInterface(session).getObjectById(UserStorage.getInstance()
						.getUser(ctx.getMailadmin(), ctx).getContactId(), FolderObject.SYSTEM_LDAP_FOLDER_ID);
				if (null != c && c.getCompany() != null && c.getCompany().length() > 0) {
					session.setParameter(MailSessionParameterNames.PARAM_ORGANIZATION_HDR, c.getCompany());
					mimeMessage.setHeader(MessageHeaders.HDR_ORGANIZATION, c.getCompany());
				} else {
					session.setParameter(MessageHeaders.HDR_ORGANIZATION, "null");
				}
			} else if (!"null".equals(org.toString())) {
				/*
				 * Apply value from session parameter
				 */
				mimeMessage.setHeader(MessageHeaders.HDR_ORGANIZATION, org.toString());
			}
		} catch (final Exception e) {
			LOG.warn("Header \"Organization\" could not be set", e);
		}
	}

	/**
	 * Sets necessary headers in specified MIME message: <code>From</code>/
	 * <code>Sender</code>, <code>To</code>, <code>Cc</code>, <code>Bcc</code>,
	 * <code>Reply-To</code>, <code>Subject</code>, etc.
	 * 
	 * @param mail
	 *            The composed mail
	 * @param mimeMessage
	 *            The MIME message
	 * @throws MessagingException
	 *             If headers cannot be set
	 */
	public void setMessageHeaders(final ComposedMailMessage mail, final MimeMessage mimeMessage)
			throws MessagingException {
		/*
		 * Set from/sender
		 */
		if (mail.containsFrom()) {
			InternetAddress sender = null;
			if (usm.getSendAddr() != null && usm.getSendAddr().length() > 0) {
				try {
					sender = new InternetAddress(usm.getSendAddr(), true);
				} catch (final AddressException e) {
					LOG.error("Default send address cannot be parsed", e);
				}
			}
			final InternetAddress from = mail.getFrom()[0];
			mimeMessage.setFrom(from);
			/*
			 * Taken from RFC 822 section 4.4.2: In particular, the "Sender"
			 * field MUST be present if it is NOT the same as the "From" Field.
			 */
			if (sender != null && !from.equals(sender)) {
				mimeMessage.setSender(sender);
			}
		}
		/*
		 * Set to
		 */
		if (mail.containsTo()) {
			mimeMessage.setRecipients(RecipientType.TO, mail.getTo());
		}
		/*
		 * Set cc
		 */
		if (mail.containsCc()) {
			mimeMessage.setRecipients(RecipientType.CC, mail.getCc());
		}
		/*
		 * Bcc
		 */
		if (mail.containsBcc()) {
			mimeMessage.setRecipients(RecipientType.BCC, mail.getBcc());
		}
		/*
		 * Reply-To
		 */
		if (usm.getReplyToAddr() == null || usm.getReplyToAddr().length() == 0) {
			if (mail.containsFrom()) {
				mimeMessage.setReplyTo(mail.getFrom());
			}
		} else {
			try {
				mimeMessage.setReplyTo(InternetAddress.parse(usm.getReplyToAddr(), true));
			} catch (final AddressException e) {
				LOG.error("Default Reply-To address cannot be parsed", e);
				try {
					mimeMessage.setHeader(MessageHeaders.HDR_REPLY_TO, MimeUtility.encodeWord(usm.getReplyToAddr(),
							MailConfig.getDefaultMimeCharset(), "Q"));
				} catch (final UnsupportedEncodingException e1) {
					/*
					 * Cannot occur since default mime charset is supported by
					 * JVM
					 */
					LOG.error(e1.getMessage(), e1);
				}
			}
		}
		/*
		 * Set subject
		 */
		if (mail.containsSubject()) {
			mimeMessage.setSubject(mail.getSubject(), MailConfig.getDefaultMimeCharset());
		}
		/*
		 * Set sent date
		 */
		if (mail.containsSentDate()) {
			mimeMessage.setSentDate(mail.getSentDate());
		}
		/*
		 * Set flags
		 */
		final Flags msgFlags = new Flags();
		if (mail.isAnswered()) {
			msgFlags.add(Flags.Flag.ANSWERED);
		}
		if (mail.isDeleted()) {
			msgFlags.add(Flags.Flag.DELETED);
		}
		if (mail.isDraft()) {
			msgFlags.add(Flags.Flag.DRAFT);
		}
		if (mail.isFlagged()) {
			msgFlags.add(Flags.Flag.FLAGGED);
		}
		if (mail.isRecent()) {
			msgFlags.add(Flags.Flag.RECENT);
		}
		if (mail.isSeen()) {
			msgFlags.add(Flags.Flag.SEEN);
		}
		if (mail.isUser()) {
			msgFlags.add(Flags.Flag.USER);
		}
		if (mail.isForwarded()) {
			msgFlags.add(MailMessage.USER_FORWARDED);
		}
		if (mail.isReadAcknowledgment()) {
			msgFlags.add(MailMessage.USER_READ_ACK);
		}
		if (mail.getColorLabel() != MailMessage.COLOR_LABEL_NONE) {
			msgFlags.add(new StringBuilder(MailMessage.COLOR_LABEL_PREFIX).append(mail.getColorLabel()).toString());
		}
		{
			final String[] userFlags = mail.getUserFlags();
			if (null != userFlags && userFlags.length > 0) {
				for (final String userFlag : userFlags) {
					msgFlags.add(userFlag);
				}
			}
		}
		/*
		 * Finally, apply flags to message
		 */
		mimeMessage.setFlags(msgFlags, true);
		/*
		 * Set disposition notification
		 */
		if (mail.getDispositionNotification() != null) {
			mimeMessage.setHeader(MessageHeaders.HDR_DISP_TO, mail.getDispositionNotification().toString());
		}
		/*
		 * Set priority
		 */
		mimeMessage.setHeader(MessageHeaders.HDR_X_PRIORITY, String.valueOf(mail.getPriority()));
		/*
		 * Headers
		 */
		final int size = mail.getHeadersSize();
		final Iterator<Map.Entry<String, String>> iter = mail.getHeadersIterator();
		for (int i = 0; i < size; i++) {
			final Map.Entry<String, String> entry = iter.next();
			mimeMessage.addHeader(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Sets the appropriate headers <code>In-Reply-To</code> and
	 * <code>References</code> in specified MIME message.
	 * <p>
	 * Moreover the <code>Reply-To</code> header is set.
	 * 
	 * @param referencedMail
	 *            The referenced mail
	 * @param mimeMessage
	 *            The MIME message
	 * @throws MessagingException
	 *             If setting the reply headers fails
	 */
	public void setReplyHeaders(final MailMessage referencedMail, final MimeMessage mimeMessage)
			throws MessagingException {
		final String pMsgId = referencedMail.getHeader(MessageHeaders.HDR_MESSAGE_ID);
		if (pMsgId != null) {
			mimeMessage.setHeader(MessageHeaders.HDR_IN_REPLY_TO, pMsgId);
		}
		/*
		 * Set References header field
		 */
		final String pReferences = referencedMail.getHeader(MessageHeaders.HDR_REFERENCES);
		final String pInReplyTo = referencedMail.getHeader(MessageHeaders.HDR_IN_REPLY_TO);
		final StringBuilder refBuilder = new StringBuilder();
		if (pReferences != null) {
			/*
			 * The "References:" field will contain the contents of the parent's
			 * "References:" field (if any) followed by the contents of the
			 * parent's "Message-ID:" field (if any).
			 */
			refBuilder.append(pReferences);
		} else if (pInReplyTo != null) {
			/*
			 * If the parent message does not contain a "References:" field but
			 * does have an "In-Reply-To:" field containing a single message
			 * identifier, then the "References:" field will contain the
			 * contents of the parent's "In-Reply-To:" field followed by the
			 * contents of the parent's "Message-ID:" field (if any).
			 */
			refBuilder.append(pInReplyTo);
		}
		if (pMsgId != null) {
			if (refBuilder.length() > 0) {
				refBuilder.append(' ');
			}
			refBuilder.append(pMsgId);
		}
		if (refBuilder.length() > 0) {
			/*
			 * If the parent has none of the "References:", "In-Reply-To:", or
			 * "Message-ID:" fields, then the new message will have no
			 * "References:" field.
			 */
			mimeMessage.setHeader(MessageHeaders.HDR_REFERENCES, refBuilder.toString());
		}
	}

	/**
	 * Sets the appropriate headers before message's transport:
	 * <code>Reply-To</code>, <code>Date</code>, and <code>Subject</code>
	 * 
	 * @param mail
	 *            The source mail
	 * @param mimeMessage
	 *            The MIME message
	 * @throws AddressException
	 * @throws MessagingException
	 */
	public void setSendHeaders(final ComposedMailMessage mail, final MimeMessage mimeMessage) throws AddressException,
			MessagingException {
		/*
		 * Set the Reply-To header for future replies to this new message
		 */
		final InternetAddress[] ia;
		if (usm.getReplyToAddr() == null) {
			ia = mail.getFrom();
		} else {
			ia = MIMEMessageUtility.parseAddressList(usm.getReplyToAddr(), false);
		}
		mimeMessage.setReplyTo(ia);
		/*
		 * Set sent date if not done, yet
		 */
		if (mimeMessage.getSentDate() == null) {
			mimeMessage.setSentDate(new Date());
		}
		/*
		 * Set default subject if none set
		 */
		final String subject;
		if ((subject = mimeMessage.getSubject()) == null || subject.length() == 0) {
			mimeMessage.setSubject(new StringHelper(UserStorage.getStorageUser(session.getUserId(), ctx).getLocale())
					.getString(MailStrings.DEFAULT_SUBJECT));
		}
	}

	/**
	 * Fills the body of given instance of {@link MimeMessage} with the contents
	 * specified through given instance of {@link ComposedMailMessage}.
	 * 
	 * @param mail
	 *            The source composed mail
	 * @param mimeMessage
	 *            The MIME message to fill
	 * @param type
	 *            The compose type
	 * @throws MessagingException
	 *             If a messaging error occurs
	 * @throws MailException
	 *             If a mail error occurs
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public void fillMailBody(final ComposedMailMessage mail, final MimeMessage mimeMessage, final ComposeType type)
			throws MessagingException, MailException, IOException {
		/*
		 * Store some flags
		 */
		// TODO: final boolean hasNestedMessages =
		// (msgObj.getNestedMsgs().size() > 0);
		final boolean hasNestedMessages = false;
		final int size = mail.getEnclosedCount();
		final boolean hasAttachments = size > 0;
		/*
		 * A non-inline forward message
		 */
		final boolean isAttachmentForward = ((ComposeType.FORWARD.equals(type)) && (usm.isForwardAsAttachment() || (size > 1 && hasOnlyReferencedMailAttachments(
				mail, size))));
		/*
		 * Initialize primary multipart
		 */
		Multipart primaryMultipart = null;
		/*
		 * Detect if primary multipart is of type multipart/mixed
		 */
		if (hasNestedMessages || hasAttachments || mail.isAppendVCard() || isAttachmentForward) {
			primaryMultipart = new MimeMultipart();
		}
		/*
		 * Content is expected to be multipart/alternative
		 */
		final boolean sendMultipartAlternative;
		if (mail.isDraft()) {
			sendMultipartAlternative = false;
			if (mail.getContentType().isMimeType(MIMETypes.MIME_MULTIPART_ALTERNATIVE)) {
				/*
				 * Allow only html if a draft message should be "sent"
				 */
				mail.setContentType(MIMETypes.MIME_TEXT_HTML);
			}
		} else {
			sendMultipartAlternative = mail.getContentType().isMimeType(MIMETypes.MIME_MULTIPART_ALTERNATIVE);
		}
		/*
		 * Html content with embedded images
		 */
		final boolean embeddedImages = (sendMultipartAlternative || (mail.getContentType()
				.isMimeType(MIMETypes.MIME_TEXT_HTM_ALL)))
				&& (MIMEMessageUtility.hasEmbeddedImages((String) mail.getContent()) || MIMEMessageUtility
						.hasReferencedLocalImages((String) mail.getContent(), session));
		/*
		 * Compose message
		 */
		if (hasAttachments || sendMultipartAlternative || isAttachmentForward || mail.isAppendVCard() || embeddedImages) {
			/*
			 * If any condition is true, we ought to create a multipart/ message
			 */
			if (sendMultipartAlternative || embeddedImages) {
				final Multipart alternativeMultipart = createMultipartAlternative(mail, embeddedImages);
				if (primaryMultipart == null) {
					primaryMultipart = alternativeMultipart;
				} else {
					final BodyPart bodyPart = new MimeBodyPart();
					bodyPart.setContent(alternativeMultipart);
					primaryMultipart.addBodyPart(bodyPart);
				}
			} else {
				if (primaryMultipart == null) {
					primaryMultipart = new MimeMultipart();
				}
				/*
				 * Convert html content to regular text if mail text is demanded
				 * to be text/plain
				 */
				if (mail.getContentType().isMimeType(MIMETypes.MIME_TEXT_PLAIN)) {
					/*
					 * Append text content
					 */
					primaryMultipart.addBodyPart(createTextBodyPart((String) mail.getContent()));
				} else {
					/*
					 * Append html content
					 */
					primaryMultipart.addBodyPart(createHtmlBodyPart((String) mail.getContent()));
				}
			}
			if (isAttachmentForward) {
				/*
				 * Add referenced mail(s)
				 */
				final StringBuilder sb = new StringBuilder(32);
				final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(BUF_SIZE);
				final byte[] bbuf = new byte[BUF_SIZE];
				for (int i = 0; i < size; i++) {
					addNestedMessage(mail.getEnclosedMailPart(i), primaryMultipart, sb, out, bbuf);
				}
			} else {
				/*
				 * Add referenced parts from ONE referenced mail
				 */
				for (int i = 0; i < size; i++) {
					addMessageBodyPart(primaryMultipart, mail.getEnclosedMailPart(i), false);
				}
			}
			/*
			 * Append VCard
			 */
			AppendVCard: if (mail.isAppendVCard()) {
				final String fileName = MimeUtility.encodeText(new StringBuilder(UserStorage.getStorageUser(
						session.getUserId(), ctx).getDisplayName().replaceAll(" +", "")).append(".vcf").toString(),
						MailConfig.getDefaultMimeCharset(), "Q");
				for (int i = 0; i < size; i++) {
					final MailPart part = mail.getEnclosedMailPart(i);
					if (fileName.equalsIgnoreCase(part.getFileName())) {
						/*
						 * VCard already attached in (former draft) message
						 */
						break AppendVCard;
					}
				}
				if (primaryMultipart == null) {
					primaryMultipart = new MimeMultipart();
				}
				try {
					final String userVCard = getUserVCard(MailConfig.getDefaultMimeCharset());
					/*
					 * Create a body part for vcard
					 */
					final MimeBodyPart vcardPart = new MimeBodyPart();
					/*
					 * Define content
					 */
					final ContentType ct = new ContentType(MIMETypes.MIME_TEXT_X_VCARD);
					ct.setCharsetParameter(MailConfig.getDefaultMimeCharset());
					vcardPart.setDataHandler(new DataHandler(new MessageDataSource(userVCard, ct)));
					vcardPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, ct.toString());
					vcardPart.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
					vcardPart.setFileName(fileName);
					/*
					 * Append body part
					 */
					primaryMultipart.addBodyPart(vcardPart);
				} catch (final MailException e) {
					LOG.error(VCARD_ERROR, e);
				}
			}
			/*
			 * Attach forwarded messages
			 */
			// if (isAttachmentForward) {
			// if (primaryMultipart == null) {
			// primaryMultipart = new MimeMultipart();
			// }
			// final int count = mail.getEnclosedCount();
			// final MailMessage[] refMails = mail.getReferencedMails();
			// final StringBuilder sb = new StringBuilder(32);
			// final ByteArrayOutputStream out = new
			// UnsynchronizedByteArrayOutputStream();
			// for (final MailMessage refMail : refMails) {
			// out.reset();
			// sb.setLength(0);
			// refMail.writeTo(out);
			// addNestedMessage(primaryMultipart, new DataHandler(new
			// MessageDataSource(out.toByteArray(),
			// MIMETypes.MIME_MESSAGE_RFC822)), sb.append(
			// refMail.getSubject().replaceAll("\\p{Blank}+",
			// "_")).append(".eml").toString());
			// }
			// }
			/*
			 * Finally set multipart
			 */
			if (primaryMultipart != null) {
				mimeMessage.setContent(primaryMultipart);
			}
			return;
		}
		/*
		 * Create a non-multipart message
		 */
		if (mail.getContentType().isMimeType(MIMETypes.MIME_TEXT_ALL)) {
			final boolean isPlainText = mail.getContentType().isMimeType(MIMETypes.MIME_TEXT_PLAIN);
			if (mail.getContentType().getCharsetParameter() == null) {
				mail.getContentType().setCharsetParameter(MailConfig.getDefaultMimeCharset());
			}
			if (primaryMultipart == null) {
				final String mailText;
				if (isPlainText) {
					/*
					 * Convert html content to regular text
					 */
					mailText = performLineFolding(getConverter().convertWithQuotes((String) mail.getContent()), false,
							usm.getAutoLinebreak());
				} else {
					mailText = getConformHTML(replaceHTMLSimpleQuotesForDisplay(formatHrefLinks((String) mail
							.getContent())), mail.getContentType());
				}
				mimeMessage.setContent(mailText, mail.getContentType().toString());
				mimeMessage.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
				mimeMessage.setHeader(MessageHeaders.HDR_CONTENT_TYPE, mail.getContentType().toString());
			} else {
				final MimeBodyPart msgBodyPart = new MimeBodyPart();
				msgBodyPart.setContent(mail.getContent(), mail.getContentType().toString());
				msgBodyPart.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
				msgBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, mail.getContentType().toString());
				primaryMultipart.addBodyPart(msgBodyPart);
			}
		} else {
			Multipart mp = null;
			if (primaryMultipart == null) {
				primaryMultipart = mp = new MimeMultipart();
			} else {
				mp = primaryMultipart;
			}
			final MimeBodyPart msgBodyPart = new MimeBodyPart();
			msgBodyPart.setText("", MailConfig.getDefaultMimeCharset());
			msgBodyPart.setDisposition(Part.INLINE);
			mp.addBodyPart(msgBodyPart);
			addMessageBodyPart(mp, mail, true);
		}
		/*
		 * if (hasNestedMessages) { if (primaryMultipart == null) {
		 * primaryMultipart = new MimeMultipart(); }
		 * 
		 * message/rfc822 final int nestedMsgSize =
		 * msgObj.getNestedMsgs().size(); final Iterator<JSONMessageObject> iter
		 * = msgObj.getNestedMsgs().iterator(); for (int i = 0; i <
		 * nestedMsgSize; i++) { final JSONMessageObject nestedMsgObj =
		 * iter.next(); final MimeMessage nestedMsg = new
		 * MimeMessage(mailSession); fillMessage(nestedMsgObj, nestedMsg,
		 * sendType); final MimeBodyPart msgBodyPart = new MimeBodyPart();
		 * msgBodyPart.setContent(nestedMsg, MIME_MESSAGE_RFC822);
		 * primaryMultipart.addBodyPart(msgBodyPart); } }
		 */
		/*
		 * Finally set multipart
		 */
		if (primaryMultipart != null) {
			mimeMessage.setContent(primaryMultipart);
		}
	}

	protected final String getUserVCard(final String charset) throws MailException {
		final User userObj = UserStorage.getStorageUser(session.getUserId(), ctx);
		Connection readCon = null;
		try {
			final OXContainerConverter converter = new OXContainerConverter(session, ctx);
			try {
				readCon = DBPool.pickup(ctx);
				ContactObject contactObj = null;
				try {
					contactObj = Contacts.getContactById(userObj.getContactId(), userObj.getId(), userObj.getGroups(),
							ctx, UserConfigurationStorage.getInstance().getUserConfigurationSafe(session.getUserId(),
									ctx), readCon);
				} catch (final OXException oxExc) {
					throw new MailException(oxExc);
				} catch (final Exception e) {
					throw new MailException(MailException.Code.VERSIT_ERROR, e, e.getMessage());
				}
				final VersitObject versitObj = converter.convertContact(contactObj, "2.1");
				final ByteArrayOutputStream os = new UnsynchronizedByteArrayOutputStream();
				final VersitDefinition def = Versit.getDefinition(MIMETypes.MIME_TEXT_X_VCARD);
				final VersitDefinition.Writer w = def.getWriter(os, MailConfig.getDefaultMimeCharset());
				def.write(w, versitObj);
				w.flush();
				os.flush();
				return new String(os.toByteArray(), charset);
			} finally {
				if (readCon != null) {
					DBPool.closeReaderSilent(ctx, readCon);
					readCon = null;
				}
				converter.close();
			}
		} catch (final ConverterException e) {
			throw new MailException(MailException.Code.VERSIT_ERROR, e, e.getMessage());
		} catch (final AbstractOXException e) {
			throw new MailException(e);
		} catch (final IOException e) {
			throw new MailException(MailException.Code.IO_ERROR, e, e.getMessage());
		}
	}

	/**
	 * Creates a <code>javax.mail.Multipart</code> instance of MIME type
	 * multipart/alternative. If <code>embeddedImages</code> is
	 * <code>true</code> a sub-multipart of MIME type multipart/related is going
	 * to be appended as the "html" version
	 */
	protected final Multipart createMultipartAlternative(final ComposedMailMessage mail, final boolean embeddedImages)
			throws MailException, MessagingException, IOException {
		/*
		 * Create an "alternative" multipart
		 */
		final Multipart alternativeMultipart = new MimeMultipart(MP_ALTERNATIVE);
		/*
		 * Create a body part for both text and html content
		 */
		final String mailBody = (String) mail.getContent();
		/*
		 * Define & add text content
		 */
		alternativeMultipart.addBodyPart(createTextBodyPart(mailBody));
		/*
		 * Define html content
		 */
		if (embeddedImages) {
			/*
			 * Create "related" multipart
			 */
			final Multipart relatedMultipart = new MimeMultipart(MP_RELATED);
			/*
			 * Process referenced local image files and insert returned html
			 * content as a new body part to first index
			 */
			relatedMultipart.addBodyPart(createHtmlBodyPart(processReferencedLocalImages(mailBody, relatedMultipart,
					this)), 0);
			/*
			 * Traverse Content-IDs
			 */
			final List<String> cidList = MIMEMessageUtility.getContentIDs(mailBody);
			NextImg: for (final String cid : cidList) {
				/*
				 * Get & remove inline image (to prevent being sent twice)
				 */
				final MailPart imgPart = getAndRemoveImageAttachment(cid, mail);
				if (imgPart == null) {
					continue NextImg;
				}
				/*
				 * Create new body part from part's data handler
				 */
				final BodyPart relatedImageBodyPart = new MimeBodyPart();
				relatedImageBodyPart.setDataHandler(imgPart.getDataHandler());
				for (final Iterator<Map.Entry<String, String>> iter = imgPart.getHeadersIterator(); iter.hasNext();) {
					final Map.Entry<String, String> e = iter.next();
					relatedImageBodyPart.setHeader(e.getKey(), e.getValue());
				}
				/*
				 * Add image to "related" multipart
				 */
				relatedMultipart.addBodyPart(relatedImageBodyPart);
			}
			/*
			 * Add multipart/related as a body part to superior multipart
			 */
			final BodyPart altBodyPart = new MimeBodyPart();
			altBodyPart.setContent(relatedMultipart);
			alternativeMultipart.addBodyPart(altBodyPart);
		} else {
			final BodyPart html = createHtmlBodyPart(mailBody);
			/*
			 * Add html part to superior multipart
			 */
			alternativeMultipart.addBodyPart(html);
		}
		return alternativeMultipart;
	}

	protected final void addMessageBodyPart(final Multipart mp, final MailPart part, final boolean inline)
			throws MessagingException, MailException, IOException {
		final MimeBodyPart messageBodyPart = new MimeBodyPart();
		if (part.getContentType().isMimeType(MIMETypes.MIME_APPL_OCTET) && part.getFileName() != null) {
			/*
			 * Try to determine MIME type
			 */
			final String ct = MIMEType2ExtMap.getContentType(part.getFileName());
			final int pos = ct.indexOf('/');
			part.getContentType().setPrimaryType(ct.substring(0, pos));
			part.getContentType().setSubType(ct.substring(pos + 1));
		} else if (part.getContentType().isMimeType(MIMETypes.MIME_MESSAGE_RFC822)) {
			// TODO: Works correctly?
			final StringBuilder sb = new StringBuilder(32);
			final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(BUF_SIZE);
			final byte[] bbuf = new byte[BUF_SIZE];
			addNestedMessage(part, mp, sb, out, bbuf);
			return;
		}
		messageBodyPart.setDataHandler(part.getDataHandler());
		messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, part.getContentType().toString());
		/*
		 * Filename
		 */
		if (part.getFileName() != null) {
			try {
				messageBodyPart.setFileName(MimeUtility.encodeText(part.getFileName(), MailConfig
						.getDefaultMimeCharset(), "Q"));
			} catch (final UnsupportedEncodingException e) {
				messageBodyPart.setFileName(part.getFileName());
			}
		}
		if (!inline) {
			/*
			 * Force base64 encoding to keep data as it is
			 */
			messageBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TRANSFER_ENC, "base64");
		}
		/*
		 * Disposition
		 */
		messageBodyPart.setDisposition(inline ? Part.INLINE : Part.ATTACHMENT);
		/*
		 * Content-ID
		 */
		if (part.getContentId() != null) {
			final String cid = part.getContentId().charAt(0) == '<' ? part.getContentId() : new StringBuilder(part
					.getContentId().length() + 2).append('<').append(part.getContentId()).append('>').toString();
			messageBodyPart.setContentID(cid);
		}
		/*
		 * Add to parental multipart
		 */
		mp.addBodyPart(messageBodyPart);
	}

	protected void addNestedMessage(final MailPart mailPart, final Multipart primaryMultipart, final StringBuilder sb,
			final ByteArrayOutputStream out, final byte[] bbuf) throws MailException, IOException, MessagingException {
		final byte[] rfcBytes;
		{
			final InputStream in = mailPart.getInputStream();
			try {
				int len;
				while ((len = in.read(bbuf)) != -1) {
					out.write(bbuf, 0, len);
				}
			} finally {
				in.close();
			}
			rfcBytes = out.toByteArray();
		}
		out.reset();
		final String fn;
		if (null == mailPart.getFileName()) {
			String subject = new InternetHeaders(new UnsynchronizedByteArrayInputStream(rfcBytes)).getHeader(
					MessageHeaders.HDR_SUBJECT, null);
			if (null == subject || subject.length() == 0) {
				fn = sb.append(PREFIX_PART).append(EXT_EML).toString();
			} else {
				subject = MIMEMessageUtility.decodeMultiEncodedHeader(MimeUtility.unfold(subject));
				fn = sb.append(subject.replaceAll("\\p{Blank}+", "_")).append(EXT_EML).toString();
				sb.setLength(0);
			}
		} else {
			fn = mailPart.getFileName();
		}
		addNestedMessage(primaryMultipart, new DataHandler(new MessageDataSource(rfcBytes,
				MIMETypes.MIME_MESSAGE_RFC822)), fn, Part.INLINE.equalsIgnoreCase(mailPart.getContentDisposition()
				.getDisposition()));
	}

	private final void addNestedMessage(final Multipart mp, final DataHandler dataHandler, final String filename,
			final boolean inline) throws MessagingException {
		/*
		 * Create a body part for original message
		 */
		final MimeBodyPart origMsgPart = new MimeBodyPart();
		/*
		 * Set data handler
		 */
		origMsgPart.setDataHandler(dataHandler);
		/*
		 * Set content type
		 */
		origMsgPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, MIMETypes.MIME_MESSAGE_RFC822);
		/*
		 * Set content disposition
		 */
		origMsgPart.setDisposition(inline ? Part.INLINE : Part.ATTACHMENT);
		if (null != filename) {
			/*
			 * Determine nested message's filename
			 */
			try {
				origMsgPart.setFileName(MimeUtility.encodeText(filename, MailConfig.getDefaultMimeCharset(), "Q"));
			} catch (final UnsupportedEncodingException e) {
				/*
				 * Cannot occur
				 */
				origMsgPart.setFileName(filename);
			}
		}
		mp.addBodyPart(origMsgPart);
	}

	/*
	 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 * +++++++++++++++++++++++++ HELPER METHODS +++++++++++++++++++++++++
	 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	 */

	/**
	 * Creates a body part of type <code>text/plain</code> from given HTML
	 * content
	 * 
	 * @param htmlContent
	 *            The HTML content
	 * @return A body part of type <code>text/plain</code> from given HTML
	 *         content
	 * @throws MessagingException
	 *             If a messaging error occurs
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	protected final BodyPart createTextBodyPart(final String htmlContent) throws MessagingException, IOException {
		/*
		 * Convert html content to regular text. First: Create a body part for
		 * text content
		 */
		final MimeBodyPart text = new MimeBodyPart();
		/*
		 * Define text content
		 */
		text.setText(performLineFolding(getConverter().convertWithQuotes(htmlContent), false, usm.getAutoLinebreak()),
				MailConfig.getDefaultMimeCharset());
		text.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
		text.setHeader(MessageHeaders.HDR_CONTENT_TYPE, PAT_TEXT_CT.replaceFirst(REPLACE_CS, MailConfig
				.getDefaultMimeCharset()));
		return text;
	}

	/**
	 * Creates a body part of type <code>text/html</code> from given HTML
	 * content
	 * 
	 * @param htmlContent
	 *            The HTML content
	 * @return A body part of type <code>text/html</code> from given HTML
	 *         content
	 * @throws MessagingException
	 *             If a messaging error occurs
	 * @throws MailException
	 *             If an I/O error occurs
	 */
	protected final static BodyPart createHtmlBodyPart(final String htmlContent) throws MessagingException,
			MailException {
		final ContentType htmlCT = new ContentType(PAT_HTML_CT.replaceFirst(REPLACE_CS, MailConfig
				.getDefaultMimeCharset()));
		final MimeBodyPart html = new MimeBodyPart();
		html.setContent(getConformHTML(replaceHTMLSimpleQuotesForDisplay(formatHrefLinks(htmlContent)), htmlCT), htmlCT
				.toString());
		html.setHeader(MessageHeaders.HDR_MIME_VERSION, VERSION_1_0);
		html.setHeader(MessageHeaders.HDR_CONTENT_TYPE, htmlCT.toString());
		return html;
	}

	private static final String IMG_PAT = "<img src=\"cid:#1#\">";

	/**
	 * Processes referenced local images, inserts them as inlined html images
	 * and adds their binary data to parental instance of <code>
	 * {@link Multipart}</code>
	 * 
	 * @param htmlContent
	 *            The html content whose &lt;img&gt; tags must be replaced with
	 *            real content ids
	 * @param mp
	 *            The parental instance of <code>{@link Multipart}</code>
	 * @param msgFiller
	 *            The message filler
	 * @return the replaced html content
	 * @throws MessagingException
	 *             If appending as body part fails
	 */
	protected final static String processReferencedLocalImages(final String htmlContent, final Multipart mp,
			final MIMEMessageFiller msgFiller) throws MessagingException {
		final StringBuffer sb = new StringBuffer(htmlContent.length());
		final Matcher m = MIMEMessageUtility.PATTERN_REF_IMG.matcher(htmlContent);
		if (m.find()) {
			msgFiller.uploadFileIDs = new HashSet<String>();
			final StringBuilder tmp = new StringBuilder(128);
			NextImg: do {
				final String id = m.group(5);
				final ManagedUploadFile uploadFile = msgFiller.session.getUploadedFile(id);
				if (uploadFile == null) {
					if (LOG.isWarnEnabled()) {
						tmp.setLength(0);
						LOG.warn(tmp.append("No upload file found with id \"").append(id).append(
								"\". Referenced image is skipped.").toString());
					}
					continue NextImg;
				}
				final boolean appendBodyPart;
				if (!msgFiller.uploadFileIDs.contains(id)) {
					/*
					 * Remember id to avoid duplicate attachment and for later
					 * cleanup
					 */
					msgFiller.uploadFileIDs.add(id);
					appendBodyPart = true;
				} else {
					appendBodyPart = false;
				}
				/*
				 * Replace image tag
				 */
				m.appendReplacement(sb, IMG_PAT.replaceFirst("#1#", processLocalImage(uploadFile, id, appendBodyPart,
						tmp, mp)));
			} while (m.find());
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Processes a local image and returns its content id
	 * 
	 * @param uploadFile
	 *            The uploaded file
	 * @param id
	 *            uploaded file's ID
	 * @param appendBodyPart
	 * @param tmp
	 *            An instance of {@link StringBuilder}
	 * @param mp
	 *            The parental instance of {@link Multipart}
	 * @return the content id
	 * @throws MessagingException
	 *             If appending as body part fails
	 */
	protected final static String processLocalImage(final ManagedUploadFile uploadFile, final String id,
			final boolean appendBodyPart, final StringBuilder tmp, final Multipart mp) throws MessagingException {
		/*
		 * Determine filename
		 */
		String fileName;
		try {
			fileName = MimeUtility.encodeText(uploadFile.getFileName(), MailConfig.getDefaultMimeCharset(), "Q");
		} catch (final UnsupportedEncodingException e) {
			fileName = uploadFile.getFileName();
		}
		/*
		 * ... and cid
		 */
		tmp.setLength(0);
		tmp.append(fileName).append('@').append(id);
		final String cid = tmp.toString();
		if (appendBodyPart) {
			/*
			 * Append body part
			 */
			final MimeBodyPart imgBodyPart = new MimeBodyPart();
			imgBodyPart.setDataHandler(new DataHandler(new FileDataSource(uploadFile.getFile())));
			imgBodyPart.setFileName(fileName);
			tmp.setLength(0);
			imgBodyPart.setContentID(tmp.append('<').append(cid).append('>').toString());
			imgBodyPart.setDisposition(Part.INLINE);
			imgBodyPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, uploadFile.getContentType());
			mp.addBodyPart(imgBodyPart);
		}
		return cid;
	}

	/**
	 * Gets and removes the image attachment from specified mail whose
	 * <code>Content-Id</code> matches given <code>cid</code> argument
	 * 
	 * @param cid
	 *            The <code>Content-Id</code> of the image attachment
	 * @param mail
	 *            The mail containing the image attachment
	 * @return The removed image attachment
	 * @throws MailException
	 *             If a mail error occurs
	 */
	protected final static MailPart getAndRemoveImageAttachment(final String cid, final ComposedMailMessage mail)
			throws MailException {
		final int size = mail.getEnclosedCount();
		for (int i = 0; i < size; i++) {
			final MailPart enclosedPart = mail.getEnclosedMailPart(i);
			if (enclosedPart.containsContentId() && MIMEMessageUtility.equalsCID(cid, enclosedPart.getContentId())) {
				return mail.removeEnclosedPart(i);
			}
		}
		return null;
	}

	private static final boolean hasOnlyReferencedMailAttachments(final ComposedMailMessage mail, final int size)
			throws MailException {
		for (int i = 0; i < size; i++) {
			final MailPart part = mail.getEnclosedMailPart(i);
			if (!ComposedPartType.REFERENCE.equals(((ComposedMailPart) part).getType())
					|| !((ReferencedMailPart) part).isMail()) {
				return false;
			}
		}
		return true;
	}
}

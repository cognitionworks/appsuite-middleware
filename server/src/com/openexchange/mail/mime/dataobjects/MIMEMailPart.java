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

package com.openexchange.mail.mime.dataobjects;

import static com.openexchange.mail.mime.ContentType.isMimeType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.openexchange.mail.MailException;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEDefaultSession;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link MIMEMailPart} - Represents a MIME part as per RFC 822.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class MIMEMailPart extends MailPart {

	private static final long serialVersionUID = -1142595512657302179L;

	private static final transient org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(MIMEMailPart.class);

	private static final String ERR_NULL_PART = "Underlying part is null";

	/**
	 * If delegate {@link Part} object is an instance of {@link MimeMessage}
	 */
	private static final int STYPE_MIME_MSG = 1;

	/**
	 * If delegate {@link Part} object is an instance of {@link MimeBodyPart}
	 * whose content type signals a nested message: <code>message/rfc822</code>
	 */
	private static final int STYPE_MIME_BODY_MSG = 2;

	/**
	 * If delegate {@link Part} object is an instance of {@link MimeBodyPart}
	 * whose content type signals a multipart content: code>multipart/*</code>
	 */
	private static final int STYPE_MIME_BODY_MULTI = 3;

	/**
	 * If delegate {@link Part} object is an instance of {@link MimeBodyPart}
	 * whose content type is different from <code>message/rfc822</code> and
	 * <code>multipart/*</code>
	 */
	private static final int STYPE_MIME_BODY = 4;

	/**
	 * The delegate {@link Part} object
	 */
	private transient Part part;

	/**
	 * Cached instance of {@link Multipart}
	 */
	private transient Multipart multipart;

	/**
	 * Whether this part's content is of MIME type <code>multipart/*</code>
	 */
	private boolean isMulti;

	/**
	 * Indicates whether content has been loaded via {@link #loadContent()} or
	 * not
	 */
	private boolean contentLoaded;

	/**
	 * Remembers serialize type on serialization
	 */
	private int serializeType;

	/**
	 * Remembers delegate {@link Part} object's serialized content
	 */
	private byte[] serializedContent;

	/**
	 * Remembers delegate {@link Part} object's content type
	 */
	private String serializedContentType;

	/**
	 * Constructor - Only applies specified part, but does not set any
	 * attributes
	 */
	public MIMEMailPart(final Part part) {
		super();
		applyPart(part);
	}

	/**
	 * Sets this mail part's content
	 * 
	 * @param part
	 *            The part
	 */
	public void setContent(final Part part) {
		applyPart(part);
	}

	private void applyPart(final Part part) {
		this.part = part;
		if (null == part) {
			isMulti = false;
		} else {
			boolean tmp = false;
			try {
				/*
				 * Ensure that proper content-type is set and check if
				 * content-type denotes a multipart/ message
				 */
				final String ct = part.getContentType();
				this.setContentType(ct);
				tmp = isMimeType(ct, MIMETypes.MIME_MULTIPART_ALL);
			} catch (final MailException e) {
				LOG.error(e.getMessage(), e);
			} catch (final MessagingException e) {
				LOG.error(e.getMessage(), e);
			}
			isMulti = tmp;
		}
	}

	@Override
	public Object getContent() throws MailException {
		if (null == part) {
			throw new IllegalStateException(ERR_NULL_PART);
		} else if (isMulti) {
			return null;
		}
		try {
			final Object obj = part.getContent();
			if (obj instanceof MimeMessage) {
				return MIMEMessageConverter.convertMessage((MimeMessage) obj);
			} else if (obj instanceof Part) {
				return MIMEMessageConverter.convertPart((Part) obj);
			} else {
				return obj;
			}
		} catch (final IOException e) {
			throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
		} catch (final MessagingException e) {
			throw new MailException(MailException.Code.MESSAGING_ERROR, e, e.getLocalizedMessage());
		}
	}

	@Override
	public DataHandler getDataHandler() throws MailException {
		if (null == part) {
			throw new IllegalStateException(ERR_NULL_PART);
		} else if (isMulti) {
			return null;
		}
		try {
			return part.getDataHandler();
		} catch (final MessagingException e) {
			throw new MailException(MailException.Code.MESSAGING_ERROR, e, e.getLocalizedMessage());
		}
	}

	@Override
	public InputStream getInputStream() throws MailException {
		if (null == part) {
			throw new IllegalStateException(ERR_NULL_PART);
		} else if (isMulti) {
			return null;
		}
		try {
			try {
				return part.getInputStream();
			} catch (final IOException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(new StringBuilder(256).append("Part's input stream could not be obtained: ").append(
							e.getMessage() == null ? "<no error message given>" : e.getMessage()).append(
							". Trying to read from part's raw input stream instead").toString(), e);
				}
				try {
					if (part instanceof MimeBodyPart) {
						return ((MimeBodyPart) part).getRawInputStream();
					} else if (part instanceof MimeMessage) {
						return ((MimeMessage) part).getRawInputStream();
					}
				} catch (final MessagingException me) {
					me.setNextException(e);
					throw me;
				}
				throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
			} catch (final MessagingException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(new StringBuilder(256).append("Part's input stream could not be obtained: ").append(
							e.getMessage() == null ? "<no error message given>" : e.getMessage()).append(
							". Trying to read from part's raw input stream instead").toString(), e);
				}
				try {
					if (part instanceof MimeBodyPart) {
						return ((MimeBodyPart) part).getRawInputStream();
					} else if (part instanceof MimeMessage) {
						return ((MimeMessage) part).getRawInputStream();
					}
				} catch (final MessagingException me) {
					me.setNextException(e);
					throw me;
				}
				throw new MailException(MailException.Code.MESSAGING_ERROR, e, e.getLocalizedMessage());
			}
		} catch (final MessagingException e) {
			throw new MailException(MailException.Code.MESSAGING_ERROR, e, e.getLocalizedMessage());
		}
	}

	@Override
	public MailPart getEnclosedMailPart(final int index) throws MailException {
		if (null == part) {
			throw new IllegalStateException(ERR_NULL_PART);
		} else if (isMulti) {
			try {
				if (null == multipart) {
					multipart = (Multipart) part.getContent();
				}
				return MIMEMessageConverter.convertPart(multipart.getBodyPart(index));
			} catch (final MessagingException e) {
				throw new MailException(MailException.Code.MESSAGING_ERROR, e, e.getLocalizedMessage());
			} catch (final IOException e) {
				throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
			}
		}
		return null;
	}

	@Override
	public int getEnclosedCount() throws MailException {
		if (null == part) {
			throw new IllegalStateException(ERR_NULL_PART);
		} else if (isMulti) {
			try {
				if (null == multipart) {
					multipart = (Multipart) part.getContent();
				}
				return multipart.getCount();
			} catch (final MessagingException e) {
				throw new MailException(MailException.Code.MESSAGING_ERROR, e, e.getLocalizedMessage());
			} catch (final IOException e) {
				throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
			}
		}
		return NO_ENCLOSED_PARTS;
	}

	@Override
	public void writeTo(final OutputStream out) throws MailException {
		if (null == part) {
			throw new IllegalStateException(ERR_NULL_PART);
		}
		try {
			part.writeTo(out);
		} catch (final IOException e) {
			throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
		} catch (final MessagingException e) {
			throw new MailException(MailException.Code.MESSAGING_ERROR, e, e.getLocalizedMessage());
		}
	}

	@Override
	public void prepareForCaching() {
		/*
		 * Release references
		 */
		if (!contentLoaded) {
			multipart = null;
			part = null;
		}
	}

	@Override
	public void loadContent() throws MailException {
		if (null == part) {
			throw new IllegalStateException(ERR_NULL_PART);
		}
		try {
			if (part instanceof MimeBodyPart) {
				final ContentType contentType = new ContentType(part.getContentType());
				if (contentType.isMimeType(MIMETypes.MIME_MESSAGE_RFC822)) {
					/*
					 * Compose a new body part with message/rfc822 data
					 */
					part = createBodyMessage(getBytesFromPart((Message) part.getContent()));
					contentLoaded = true;
				} else if (contentType.isMimeType(MIMETypes.MIME_MULTIPART_ALL)) {
					/*
					 * Compose a new body part with multipart/ data
					 */
					part = createBodyMultipart(getBytesFromMultipart((Multipart) part.getContent()), contentType
							.toString());
					multipart = null;
					contentLoaded = true;
				} else {
					part = createBodyPart(getBytesFromPart(part));
					contentLoaded = true;
				}
			} else if (part instanceof MimeMessage) {
				part = createMessage(getBytesFromPart(part));
				contentLoaded = true;
			}
		} catch (final MessagingException e) {
			throw new MailException(MailException.Code.MESSAGING_ERROR, e, e.getLocalizedMessage());
		} catch (final IOException e) {
			throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
		}
	}

	/**
	 * The writeObject method is responsible for writing the state of the object
	 * for its particular class so that the corresponding readObject method can
	 * restore it. The default mechanism for saving the Object's fields can be
	 * invoked by calling {@link ObjectOutputStream#defaultWriteObject()}. The
	 * method does not need to concern itself with the state belonging to its
	 * super classes or subclasses. State is saved by writing the individual
	 * fields to the ObjectOutputStream using the writeObject method or by using
	 * the methods for primitive data types supported by {@link DataOutput}.
	 * 
	 * @param out
	 *            The object output stream
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
		multipart = null;
		if (part == null) {
			serializeType = 0;
			serializedContent = null;
			out.defaultWriteObject();
			return;
		}
		/*
		 * Remember serialize type and content
		 */
		try {
			if (part instanceof MimeBodyPart) {
				final ContentType contentType = new ContentType(part.getContentType());
				if (contentType.isMimeType(MIMETypes.MIME_MESSAGE_RFC822)) {
					serializeType = STYPE_MIME_BODY_MSG;
					serializedContent = getBytesFromPart((Message) part.getContent());
				} else if (contentType.isMimeType(MIMETypes.MIME_MULTIPART_ALL)) {
					serializeType = STYPE_MIME_BODY_MULTI;
					serializedContent = getBytesFromMultipart((Multipart) part.getContent());
					serializedContentType = contentType.toString();
				} else {
					serializeType = STYPE_MIME_BODY;
					serializedContent = getBytesFromPart(part);
				}
			} else if (part instanceof MimeMessage) {
				serializeType = STYPE_MIME_MSG;
				serializedContent = getBytesFromPart(part);
			}
			/*
			 * Write common fields
			 */
			out.defaultWriteObject();
		} catch (final MailException e) {
			final IOException ioe = new IOException(e.getMessage());
			ioe.initCause(e);
			throw ioe;
		} catch (final MessagingException e) {
			final IOException ioe = new IOException(e.getMessage());
			ioe.initCause(e);
			throw ioe;
		} finally {
			/*
			 * Discard content created for serialization
			 */
			serializeType = 0;
			serializedContent = null;
			serializedContentType = null;
		}
	}

	/**
	 * The readObject method is responsible for reading from the stream and
	 * restoring the classes fields. It may call in.defaultReadObject to invoke
	 * the default mechanism for restoring the object's non-static and
	 * non-transient fields. The {@link ObjectInputStream#defaultReadObject()}
	 * method uses information in the stream to assign the fields of the object
	 * saved in the stream with the correspondingly named fields in the current
	 * object. This handles the case when the class has evolved to add new
	 * fields. The method does not need to concern itself with the state
	 * belonging to its super classes or subclasses. State is saved by writing
	 * the individual fields to the ObjectOutputStream using the writeObject
	 * method or by using the methods for primitive data types supported by
	 * {@link DataOutput}.
	 * 
	 * @param in
	 *            The object input stream
	 * @throws IOException
	 *             If an I/O error occurs
	 * @throws ClassNotFoundException
	 *             If a casting fails
	 */
	private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		/*
		 * Restore common fields
		 */
		in.defaultReadObject();
		if (serializeType > 0) {
			try {
				/*
				 * Restore part
				 */
				if (STYPE_MIME_BODY_MSG == serializeType) {
					/*
					 * Compose a new body part with message/rfc822 data
					 */
					part = createBodyMessage(serializedContent);
					contentLoaded = true;
				} else if (STYPE_MIME_BODY_MULTI == serializeType) {
					/*
					 * Compose a new body part with multipart/ data
					 */
					part = createBodyMultipart(serializedContent, serializedContentType);
					multipart = null;
					contentLoaded = true;
				} else if (STYPE_MIME_BODY == serializeType) {
					part = createBodyPart(serializedContent);
					contentLoaded = true;
				} else if (STYPE_MIME_MSG == serializeType) {
					part = createMessage(serializedContent);
					contentLoaded = true;
				}
			} catch (final MessagingException e) {
				final IOException ioe = new IOException(e.getMessage());
				ioe.initCause(e);
				throw ioe;
			} finally {
				/*
				 * Discard content created for serialization
				 */
				serializeType = 0;
				serializedContent = null;
				serializedContentType = null;
			}
		}
	}

	/**
	 * Compose a new MIME body part with message/rfc822 data
	 * 
	 * @param data
	 *            The message/rfc822 data
	 * @return A new MIME body part with message/rfc822 data
	 * @throws MessagingException
	 *             If a messaging error occurs
	 */
	private static MimeBodyPart createBodyMessage(final byte[] data) throws MessagingException {
		final MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(new MimeMessage(MIMEDefaultSession.getDefaultSession(),
				new UnsynchronizedByteArrayInputStream(data)), MIMETypes.MIME_MESSAGE_RFC822);
		return mimeBodyPart;
	}

	/**
	 * Compose a new MIME body part with multipart/* data
	 * 
	 * @param data
	 *            The multipart/* data
	 * @param contentType
	 *            The multipart's content type (containing important boundary
	 *            parameter)
	 * @return A new MIME body part with multipart/* data
	 * @throws MessagingException
	 *             If a messaging error occurs
	 */
	private static MimeBodyPart createBodyMultipart(final byte[] data, final String contentType)
			throws MessagingException {
		final MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(new MimeMultipart(new MessageDataSource(data, contentType)));
		return mimeBodyPart;
	}

	/**
	 * Compose a new MIME body part directly from specified data
	 * 
	 * @param data
	 *            The part's data
	 * @return A new MIME body part
	 * @throws MessagingException
	 *             If a messaging error occurs
	 */
	private static MimeBodyPart createBodyPart(final byte[] data) throws MessagingException {
		return new MimeBodyPart(new UnsynchronizedByteArrayInputStream(data));
	}

	/**
	 * Compose a new MIME message directly from specified data
	 * 
	 * @param data
	 *            The message's data
	 * @return A new MIME message
	 * @throws MessagingException
	 *             If a messaging error occurs
	 */
	private static MimeMessage createMessage(final byte[] data) throws MessagingException {
		return new MimeMessage(MIMEDefaultSession.getDefaultSession(), new UnsynchronizedByteArrayInputStream(data));
	}

	/**
	 * Gets the bytes of specified part's raw data.
	 * 
	 * @param part
	 *            Either a message or a body part
	 * @return The bytes of specified part's raw data (with the optional empty
	 *         starting line omitted)
	 * @throws IOException
	 *             If an I/O error occurs
	 * @throws MessagingException
	 *             If a messaging error occurs
	 */
	private static byte[] getBytesFromPart(final Part part) throws IOException, MessagingException {
		byte[] data;
		{
			final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(4096);
			part.writeTo(out);
			data = out.toByteArray();
		}
		return stripEmptyStartingLine(data);
	}

	/**
	 * Gets the bytes of specified multipart's raw data.
	 * 
	 * @param multipart
	 *            A multipart object
	 * @return The bytes of specified multipart's raw data (with the optional
	 *         empty starting line omitted)
	 * @throws IOException
	 *             If an I/O error occurs
	 * @throws MessagingException
	 *             If a messaging error occurs
	 */
	private static byte[] getBytesFromMultipart(final Multipart multipart) throws IOException, MessagingException {
		byte[] data;
		{
			final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(4096);
			multipart.writeTo(out);
			data = out.toByteArray();
		}
		return stripEmptyStartingLine(data);
	}

	/**
	 * Strips the possible empty starting line from specified byte array
	 * 
	 * @param data
	 *            The byte array
	 * @return The stripped byte array
	 */
	private static byte[] stripEmptyStartingLine(final byte[] data) {
		/*
		 * Starts with an empty line?
		 */
		int start = 0;
		if (data[start] == '\r') {
			start++;
		}
		if (data[start] == '\n') {
			start++;
		}
		if (start > 0) {
			final byte[] data0 = new byte[data.length - start];
			System.arraycopy(data, start, data0, 0, data0.length);
			return data0;
		}
		return data;
	}
}

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

package com.openexchange.mail.dataobjects.compose;

import static com.openexchange.mail.utils.MessageUtility.readStream;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Part;

import com.openexchange.configuration.ServerConfig;
import com.openexchange.conversion.DataProperties;
import com.openexchange.groupware.upload.ManagedUploadFile;
import com.openexchange.groupware.upload.impl.AJAXUploadFile;
import com.openexchange.mail.MailException;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.config.MailConfigException;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.transport.config.TransportConfig;
import com.openexchange.session.Session;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link DataMailPart}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public abstract class DataMailPart extends MailPart implements ComposedMailPart {

	private static final int DEFAULT_BUF_SIZE = 0x2000;

	private static final String FILE_PREFIX = "openexchange";

	private static transient final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(DataMailPart.class);

	private static final int MB = 1048576;

	private byte[] bytes;

	private transient Object cachedContent;

	private transient DataSource dataSource;

	private File file;

	private String fileId;

	/**
	 * Initializes a new {@link DataMailPart}
	 * 
	 * @param data
	 *            The data (from a data source)
	 * @param dataProperties
	 *            The data properties
	 * @param session
	 *            The session
	 * @throws MailException
	 *             If data's content is not supported
	 */
	protected DataMailPart(final Object data, final Map<String, String> dataProperties, final Session session)
			throws MailException {
		super();
		setHeaders(dataProperties);
		if (data instanceof InputStream) {
			final InputStream inputStream = (InputStream) data;
			long size;
			try {
				size = Long.parseLong(dataProperties.get(DataProperties.PROPERTY_SIZE));
				setSize(size);
			} catch (final NumberFormatException e) {
				size = 0;
			}
			handleInputStream(inputStream, size, session);
		} else if (data instanceof byte[]) {
			this.bytes = (byte[]) data;
		} else {
			throw new MailException(MailException.Code.UNSUPPORTED_DATASOURCE);
		}
	}

	private void applyByteContent(final String charset) throws MailException {
		try {
			cachedContent = new String(bytes, charset);
		} catch (final UnsupportedEncodingException e) {
			throw new MailException(MailException.Code.ENCODING_ERROR, e, e.getLocalizedMessage());
		}
	}

	private void applyFileContent(final String charset) throws MailException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			cachedContent = readStream(fis, charset);
		} catch (final FileNotFoundException e) {
			throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
		} catch (final IOException e) {
			throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (final IOException e) {
					LOG.error(e.getLocalizedMessage(), e);
				}
			}
		}
	}

	private void copy2ByteArr(final InputStream in) throws IOException {
		final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(DEFAULT_BUF_SIZE * 2);
		final byte[] bbuf = new byte[DEFAULT_BUF_SIZE];
		int len;
		while ((len = in.read(bbuf)) != -1) {
			out.write(bbuf, 0, len);
		}
		out.flush();
		this.bytes = out.toByteArray();
	}

	private void copy2File(final InputStream in, final Session session) throws IOException {
		long totalBytes = 0;
		{
			final File tmpFile = File.createTempFile(FILE_PREFIX, null, new File(ServerConfig
					.getProperty(ServerConfig.Property.UploadDirectory)));
			tmpFile.deleteOnExit();
			OutputStream out = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(tmpFile), DEFAULT_BUF_SIZE);
				final byte[] bbuf = new byte[DEFAULT_BUF_SIZE];
				int len;
				while ((len = in.read(bbuf)) != -1) {
					out.write(bbuf, 0, len);
					totalBytes += len;
				}
				out.flush();
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (final IOException e) {
						LOG.error(e.getLocalizedMessage(), e);
					}
				}
			}
			file = tmpFile;
		}
		final ManagedUploadFile uploadFile = new AJAXUploadFile(file, System.currentTimeMillis());
		fileId = UUID.randomUUID().toString();
		session.putUploadedFile(fileId, uploadFile);
		setSize(totalBytes);
	}

	@Override
	public Object getContent() throws MailException {
		if (cachedContent != null) {
			return cachedContent;
		}
		if (getContentType().isMimeType(MIMETypes.MIME_TEXT_ALL)) {
			if (bytes != null) {
				String charset = getContentType().getCharsetParameter();
				if (null == charset) {
					charset = MailConfig.getDefaultMimeCharset();
				}
				applyByteContent(charset);
				return cachedContent;
			}
			if (file != null) {
				String charset = getContentType().getCharsetParameter();
				if (null == charset) {
					charset = System.getProperty("file.encoding", MailConfig.getDefaultMimeCharset());
				}
				applyFileContent(charset);
				return cachedContent;
			}
		}
		return null;
	}

	@Override
	public DataHandler getDataHandler() throws MailException {
		return new DataHandler(getDataSource());
	}

	private DataSource getDataSource() throws MailException {
		/*
		 * Lazy creation
		 */
		if (null == dataSource) {
			try {
				final ContentType contentType = getContentType();
				if (bytes != null) {
					if (!contentType.containsCharsetParameter() && contentType.isMimeType(MIMETypes.MIME_TEXT_ALL)) {
						/*
						 * Add default mail charset
						 */
						contentType.setCharsetParameter(MailConfig.getDefaultMimeCharset());
					}
					return (dataSource = new MessageDataSource(bytes, contentType.toString()));
				}
				if (file != null) {
					if (!contentType.containsCharsetParameter() && contentType.isMimeType(MIMETypes.MIME_TEXT_ALL)) {
						/*
						 * Add system charset
						 */
						contentType.setCharsetParameter(System.getProperty("file.encoding", MailConfig
								.getDefaultMimeCharset()));
					}
					return (dataSource = new MessageDataSource(new FileInputStream(file), contentType));
				}
				throw new MailException(MailException.Code.NO_CONTENT);
			} catch (final MailConfigException e) {
				LOG.error(e.getLocalizedMessage(), e);
				dataSource = new MessageDataSource(new byte[0], "application/octet-stream");
			} catch (final IOException e) {
				LOG.error(e.getLocalizedMessage(), e);
				dataSource = new MessageDataSource(new byte[0], "application/octet-stream");
			}
		}
		return dataSource;
	}

	@Override
	public int getEnclosedCount() throws MailException {
		return NO_ENCLOSED_PARTS;
	}

	@Override
	public MailPart getEnclosedMailPart(final int index) throws MailException {
		return null;
	}

	@Override
	public InputStream getInputStream() throws MailException {
		try {
			if (bytes != null) {
				return new UnsynchronizedByteArrayInputStream(bytes);
			}
			if (file != null) {
				return new FileInputStream(file);
			}
			throw new MailException(MailException.Code.NO_CONTENT);
		} catch (final IOException e) {
			throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
		}
	}

	public ComposedPartType getType() {
		return ComposedPartType.DATA;
	}

	private void handleInputStream(final InputStream inputStream, final long size, final Session session)
			throws MailException {
		try {
			if (size > 0 && size <= TransportConfig.getReferencedPartLimit()) {
				copy2ByteArr(inputStream);
				return;
			}
			copy2File(inputStream, session);
		} catch (final IOException e) {
			throw new MailException(MailException.Code.IO_ERROR, e, e.getMessage());
		}
		if (LOG.isInfoEnabled()) {
			LOG.info(new StringBuilder("Data mail part exeeds ").append(
					Float.valueOf(TransportConfig.getReferencedPartLimit() / MB).floatValue()).append(
					"MB limit. A temporary disk copy has been created: ").append(file.getName()));
		}
	}

	@Override
	public void loadContent() throws MailException {
		if (LOG.isTraceEnabled()) {
			LOG.trace("DataSourceMailPart.loadContent()");
		}
	}

	@Override
	public void prepareForCaching() {
		if (LOG.isTraceEnabled()) {
			LOG.trace("DataSourceMailPart.prepareForCaching()");
		}
	}

	/**
	 * Gets this data part's file ID if its content has been written to disc.
	 * 
	 * @return The file ID or <code>null</code> if content is kept inside rather
	 *         than on disc.
	 */
	public String getFileID() {
		return this.fileId;
	}

	private void setHeaders(final Map<String, String> dataProperties) throws MailException {
		if (!dataProperties.containsKey(DataProperties.PROPERTY_CONTENT_TYPE)) {
			throw new MailException(MailException.Code.MISSING_PARAMETER, DataProperties.PROPERTY_CONTENT_TYPE);
		}
		final ContentType contentType = new ContentType(dataProperties.get(DataProperties.PROPERTY_CONTENT_TYPE));
		if (dataProperties.containsKey(DataProperties.PROPERTY_CHARSET)) {
			contentType.setCharsetParameter(dataProperties.get(DataProperties.PROPERTY_CHARSET));
		}
		setContentType(contentType);
		final ContentDisposition contentDisposition = new ContentDisposition();
		contentDisposition.setContentDisposition(Part.ATTACHMENT);
		if (dataProperties.containsKey(DataProperties.PROPERTY_NAME)) {
			final String filename = dataProperties.get(DataProperties.PROPERTY_NAME);
			setFileName(filename);
			contentDisposition.setFilenameParameter(filename);
		}
		setContentDisposition(contentDisposition);
	}

}

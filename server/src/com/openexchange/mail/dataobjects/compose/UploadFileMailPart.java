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

import static com.openexchange.mail.utils.CharsetDetector.detectCharset;
import static com.openexchange.mail.utils.MessageUtility.readStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Part;

import com.openexchange.groupware.upload.impl.UploadFile;
import com.openexchange.mail.MailException;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.datasource.MessageDataSource;

/**
 * {@link UploadFileMailPart} - A {@link MailPart} implementation that keeps a
 * reference to a temporary uploaded file that shall be added as an attachment
 * later
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public abstract class UploadFileMailPart extends MailPart implements ComposedMailPart {

	private static final long serialVersionUID = 257902073011243269L;

	private static final transient org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(UploadFileMailPart.class);

	private final transient File uploadFile;

	private transient DataSource dataSource;

	private transient Object cachedContent;

	/**
	 * Initializes a new {@link UploadFileMailPart}
	 * 
	 * @param uploadFile
	 *            The upload file
	 * @throws MailException
	 *             If upload file's content type cannot be parsed
	 */
	protected UploadFileMailPart(final UploadFile uploadFile) throws MailException {
		super();
		this.uploadFile = uploadFile.getTmpFile();
		setContentType(uploadFile.getContentType());
		setFileName(uploadFile.getPreparedFileName());
		setSize(uploadFile.getSize());
		ContentDisposition cd = new ContentDisposition();
		cd.setDisposition(Part.ATTACHMENT);
		cd.setFilenameParameter(getFileName());
		setContentDisposition(cd);
	}

	private DataSource getDataSource() {
		/*
		 * Lazy creation
		 */
		if (null == dataSource) {
			try {
				if (getContentType().getCharsetParameter() == null
						&& getContentType().isMimeType(MIMETypes.MIME_TEXT_ALL)) {
					/*
					 * Guess charset for textual attachment
					 */
					getContentType().setCharsetParameter(detectCharset(new FileInputStream(uploadFile)));
				}
				dataSource = new MessageDataSource(new FileInputStream(uploadFile), getContentType());
			} catch (final IOException e) {
				LOG.error(e.getLocalizedMessage(), e);
				dataSource = new MessageDataSource(new byte[0], MIMETypes.MIME_APPL_OCTET);
			}
		}
		return dataSource;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.mail.dataobjects.MailPart#getContent()
	 */
	@Override
	public Object getContent() throws MailException {
		if (cachedContent != null) {
			return cachedContent;
		}
		if (getContentType().isMimeType(MIMETypes.MIME_TEXT_ALL)) {
			String charset = getContentType().getCharsetParameter();
			if (charset == null) {
				charset = System.getProperty("file.encoding", MailConfig.getDefaultMimeCharset());
			}
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(uploadFile);
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
			return cachedContent;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.mail.dataobjects.MailPart#getDataHandler()
	 */
	@Override
	public DataHandler getDataHandler() throws MailException {
		return new DataHandler(getDataSource());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.mail.dataobjects.MailPart#getEnclosedCount()
	 */
	@Override
	public int getEnclosedCount() throws MailException {
		return NO_ENCLOSED_PARTS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.mail.dataobjects.MailPart#getEnclosedMailPart(int)
	 */
	@Override
	public MailPart getEnclosedMailPart(final int index) throws MailException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.mail.dataobjects.MailPart#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws MailException {
		try {
			return new FileInputStream(uploadFile);
		} catch (final FileNotFoundException e) {
			throw new MailException(MailException.Code.IO_ERROR, e, e.getLocalizedMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.mail.dataobjects.MailPart#loadContent()
	 */
	@Override
	public void loadContent() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.mail.dataobjects.MailPart#prepareForCaching()
	 */
	@Override
	public void prepareForCaching() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.openexchange.mail.transport.smtp.dataobjects.SMTPMailPart#getType()
	 */
	public ComposedPartType getType() {
		return ComposedMailPart.ComposedPartType.FILE;
	}
}

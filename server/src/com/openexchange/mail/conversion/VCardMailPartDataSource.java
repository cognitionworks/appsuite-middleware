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

package com.openexchange.mail.conversion;

import java.io.InputStream;

import com.openexchange.conversion.DataExceptionCodes;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataException;
import com.openexchange.conversion.DataProperties;
import com.openexchange.conversion.SimpleData;
import com.openexchange.mail.MailException;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.session.Session;

/**
 * {@link VCardMailPartDataSource} - The {@link MailPartDataSource} for VCard
 * parts.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class VCardMailPartDataSource extends MailPartDataSource {

	private DataProperties properties;

	/**
	 * Initializes a new {@link VCardMailPartDataSource}
	 */
	public VCardMailPartDataSource() {
		super();
	}

	public <D> Data<D> getData(final Class<? extends D> type, final DataArguments dataArguments, final Session session)
			throws DataException {
		if (!InputStream.class.equals(type)) {
			throw DataExceptionCodes.TYPE_NOT_SUPPORTED.create(type.getName());
		}
		final MailPart mailPart;
		{
			final String fullname = MailFolderUtility.prepareMailFolderParam(dataArguments.get(ARGS[0]));
			final long mailId;
			try {
				mailId = Long.parseLong(dataArguments.get(ARGS[1]));
			} catch (final NumberFormatException e) {
				throw DataExceptionCodes.INVALID_ARGUMENT.create(ARGS[1], dataArguments.get(ARGS[1]));
			}
			final String sequenceId = dataArguments.get(ARGS[2]);
			mailPart = getMailPart(fullname, mailId, sequenceId, session);
			final ContentType contentType = mailPart.getContentType();
			if (contentType != null) {
				properties = new DataProperties();
				properties.put(DataProperties.PROPERTY_CONTENT_TYPE, contentType.getBaseType());
				final String charset = contentType.getCharsetParameter();
				if (charset == null) {
					properties.put(DataProperties.PROPERTY_CHARSET, MailConfig.getDefaultMimeCharset());
				} else {
					properties.put(DataProperties.PROPERTY_CHARSET, charset);
				}
			}
		}
		try {
			return new SimpleData<D>((D) mailPart.getInputStream(), properties);
		} catch (final MailException e) {
			throw new DataException(e);
		}

	}

}

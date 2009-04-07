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
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataException;
import com.openexchange.conversion.DataExceptionCodes;
import com.openexchange.conversion.DataProperties;
import com.openexchange.conversion.DataSource;
import com.openexchange.conversion.SimpleData;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailException;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.session.Session;

/**
 * {@link InlineImageDataSource} - A generic {@link DataSource} for mail parts.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class InlineImageDataSource implements DataSource {

    /**
     * Common required arguments for uniquely determining a mail part:
     * <ul>
     * <li>com.openexchange.mail.conversion.fullname</li>
     * <li>com.openexchange.mail.conversion.mailid</li>
     * <li>com.openexchange.mail.conversion.cid</li>
     * </ul>
     */
    private static final String[] ARGS = {
        "com.openexchange.mail.conversion.fullname", "com.openexchange.mail.conversion.mailid", "com.openexchange.mail.conversion.cid" };

    private static final Class<?>[] TYPES = { InputStream.class };

    /**
     * Initializes a new {@link InlineImageDataSource}
     */
    public InlineImageDataSource() {
        super();
    }

    private MailPart getImagePart(final int accountId, final String fullname, final long mailId, final String cid, final Session session) throws DataException {
        final MailAccess<?, ?> mailAccess;
        try {
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect();
        } catch (final MailException e) {
            throw new DataException(e);
        }
        try {
            final MailPart imagePart = mailAccess.getMessageStorage().getImageAttachment(fullname, mailId, cid);
            imagePart.loadContent();
            return imagePart;
        } catch (final MailException e) {
            throw new DataException(e);
        } finally {
            mailAccess.close(true);
        }
    }

    /**
     * Common required arguments for uniquely determining a mail part:
     * <ul>
     * <li>com.openexchange.mail.conversion.fullname</li>
     * <li>com.openexchange.mail.conversion.mailid</li>
     * <li>com.openexchange.mail.conversion.cid</li>
     * </ul>
     */
    public String[] getRequiredArguments() {
        return ARGS;
    }

    public Class<?>[] getTypes() {
        return TYPES;
    }

    public <D> Data<D> getData(final Class<? extends D> type, final DataArguments dataArguments, final Session session) throws DataException {
        if (!InputStream.class.equals(type)) {
            throw DataExceptionCodes.TYPE_NOT_SUPPORTED.create(type.getName());
        }
        final MailPart mailPart;
        {
            final FullnameArgument arg = MailFolderUtility.prepareMailFolderParam(dataArguments.get(ARGS[0]));
            final String fullname = arg.getFullname();
            final long mailId;
            try {
                mailId = Long.parseLong(dataArguments.get(ARGS[1]));
            } catch (final NumberFormatException e) {
                throw DataExceptionCodes.INVALID_ARGUMENT.create(ARGS[1], dataArguments.get(ARGS[1]));
            }
            final String cid = dataArguments.get(ARGS[2]);
            mailPart = getImagePart(arg.getAccountId(), fullname, mailId, cid, session);
            final ContentType contentType = mailPart.getContentType();
            if (contentType == null) {
                throw DataExceptionCodes.ERROR.create("Missing header 'Content-Type' in requested mail part");
            }
            if (!contentType.isMimeType(MIMETypes.MIME_IMAGE_ALL)) {
                throw DataExceptionCodes.ERROR.create("Requested mail part is not an image: " + contentType.getBaseType());
            }
            final DataProperties properties = new DataProperties();
            properties.put(DataProperties.PROPERTY_CONTENT_TYPE, contentType.getBaseType());
            final String charset = contentType.getCharsetParameter();
            if (charset != null) {
                properties.put(DataProperties.PROPERTY_CHARSET, charset);
            }
            properties.put(DataProperties.PROPERTY_SIZE, String.valueOf(mailPart.getSize()));
            properties.put(DataProperties.PROPERTY_NAME, mailPart.getFileName());
            try {
                return new SimpleData<D>((D) mailPart.getInputStream(), properties);
            } catch (final MailException e) {
                throw new DataException(e);
            }
        }
    }
}

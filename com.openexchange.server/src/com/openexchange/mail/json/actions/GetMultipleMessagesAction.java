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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.mail.json.actions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import com.openexchange.ajax.Mail;
import com.openexchange.ajax.container.ByteArrayFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link GetMultipleMessagesAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class GetMultipleMessagesAction extends AbstractMailAction {

    /**
     * Initializes a new {@link GetMultipleMessagesAction}.
     *
     * @param services
     */
    public GetMultipleMessagesAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final MailRequest req) throws OXException {
        try {
            // final ServerSession session = req.getSession();
            /*
             * Read in parameters
             */
            final String folderPath = req.checkParameter(Mail.PARAMETER_FOLDERID);
            final String[] ids = req.checkStringArray(Mail.PARAMETER_ID);
            /*
             * Get mail interface
             */
            final MailServletInterface mailInterface = getMailInterface(req);
            ManagedFile mf = null;
            try {
                mf = mailInterface.getMessages(folderPath, ids);
                /*
                 * Set Content-Type and Content-Disposition header
                 */
                final String fileName = "mails.zip";
                /*
                 * We are supposed to offer attachment for download. Therefore enforce application/octet-stream and attachment disposition.
                 */
                final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream();
                /*
                 * Write from content's input stream to response output stream
                 */
                final InputStream zipInputStream = mf.getInputStream();
                try {
                    final byte[] buffer = new byte[0xFFFF];
                    for (int len; (len = zipInputStream.read(buffer, 0, buffer.length)) != -1;) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                } finally {
                    zipInputStream.close();
                }
                /*
                 * Create file holder
                 */
                req.getRequest().setFormat("file");
                final ByteArrayFileHolder fileHolder = new ByteArrayFileHolder(out.toByteArray());
                fileHolder.setName(fileName);
                fileHolder.setContentType("application/octet-stream");
                return new AJAXRequestResult(fileHolder, "file");
            } finally {
                if (null != mf) {
                    mf.delete();
                    mf = null;
                }
            }
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

}

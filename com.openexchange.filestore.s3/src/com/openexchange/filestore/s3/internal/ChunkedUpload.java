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
 *     Copyright (C) 2004-2013 Open-Xchange, Inc.
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

package com.openexchange.filestore.s3.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.openexchange.ajax.container.ThresholdFileHolder;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorageCodes;
import com.openexchange.java.Streams;

/**
 * {@link ChunkedUpload}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ChunkedUpload implements Closeable {

    private static final int CIPHER_BLOCK_SIZE = 16;

    private final DigestInputStream digestStream;
    private final boolean encrypted;
    private boolean hasNext;

    /**
     * Initializes a new {@link ChunkedUpload}.
     *
     * @param data The underlying input stream
     * @param encrypted Whether encryption is enabled
     * @throws OXException If initialization fails
     */
    public ChunkedUpload(InputStream data, boolean encrypted) throws OXException {
        super();
        this.encrypted = encrypted;
        try {
            this.digestStream = new DigestInputStream(data, MessageDigest.getInstance("MD5"));
            if (encrypted) {
                // Disable updating digest if encrypted
                digestStream.on(false);
            }
        } catch (NoSuchAlgorithmException e) {
            throw FileStorageCodes.IOERROR.create(e);
        }
        hasNext = true;
    }

    /**
     * Gets the next upload chunk.
     *
     * @return The next upload chunk
     * @throws OXException
     */
    public UploadChunk next() throws OXException {
        try {
            ThresholdFileHolder fileHolder = new ThresholdFileHolder();
            byte[] buffer = new byte[0xFFFF]; // 64k
            for (int read; (read = digestStream.read(buffer, 0, buffer.length)) > 0;) {
                fileHolder.write(buffer, 0, read);
                if (fileHolder.getCount() >= UploadChunk.MIN_CHUNK_SIZE) {
                    // chunk size reached
                    if (!encrypted) {
                        byte[] digest = digestStream.getMessageDigest().digest();
                        return new UploadChunk(fileHolder, digest);
                    }

                    // chunk sizes for encrypted multipart uploads must be multiples of the cipher block size (16) with the exception of the last part.
                    int res = (int) (fileHolder.getCount() % CIPHER_BLOCK_SIZE);
                    if (0 == res) {
                        return new UploadChunk(fileHolder, null);
                    }

                    // Try to read missing bytes to have multiples of the cipher block size (16)
                    res = CIPHER_BLOCK_SIZE - res;
                    byte[] buf = new byte[res];
                    int rd = digestStream.read(buf, 0, res);
                    if (rd >= res) {
                        fileHolder.write(buf, 0, rd);
                        return new UploadChunk(fileHolder, null);
                    }

                    // No more data available - fall-through
                    hasNext = false;
                    return new UploadChunk(fileHolder, null);
                }
            }

            // end of input reached
            hasNext = false;
            if (encrypted) {
                return new UploadChunk(fileHolder, null);
            }
            byte[] digest = digestStream.getMessageDigest().digest();
            return new UploadChunk(fileHolder, digest);
        } catch (IOException e) {
            throw FileStorageCodes.IOERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets a value indicating whether further chunks are available or not.
     *
     * @return <code>true</code> if there are more, <code>false</code>, otherwise
     */
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public void close() throws IOException {
        Streams.close(digestStream);
    }

}

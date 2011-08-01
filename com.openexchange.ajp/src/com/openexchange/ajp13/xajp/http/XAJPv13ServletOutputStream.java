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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.ajp13.xajp.http;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.locks.Lock;
import javax.servlet.ServletOutputStream;
import org.xsocket.connection.INonBlockingConnection;
import com.openexchange.ajp13.AJPv13Response;
import com.openexchange.ajp13.xajp.XAJPv13Session;
import com.openexchange.concurrent.NonBlockingSynchronizer;
import com.openexchange.concurrent.Synchronizable;
import com.openexchange.concurrent.Synchronizer;

/**
 * {@link XAJPv13ServletOutputStream} - The AJP's servlet output stream.
 * <p>
 * This servlet output stream supports to work in both modes: synchronized and unsynchronized. To switch between these two modes the
 * {@link Synchronizable} interface is implemented. By default the unsynchronized mode is active.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class XAJPv13ServletOutputStream extends ServletOutputStream implements Synchronizable {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(XAJPv13ServletOutputStream.class));

    private static final String ERR_OUTPUT_CLOSED = "OutputStream is closed";

    private final INonBlockingConnection ajpCon;

    private final byte[] buf;

    private int count;

    private boolean isClosed;

    private final Synchronizer synchronizer;

    /**
     * Initializes a new {@link XAJPv13ServletOutputStream}.
     *
     * @param ajpCon The associated AJP connection
     */
    public XAJPv13ServletOutputStream(final INonBlockingConnection ajpCon) {
        super();
        this.ajpCon = ajpCon;
        buf = new byte[AJPv13Response.MAX_SEND_BODY_CHUNK_SIZE];
        synchronizer = new NonBlockingSynchronizer();
    }

    /**
     * Resets this output stream's buffer.
     */
    public void resetBuffer() {
        final Lock l = synchronizer.acquire();
        try {
            count = 0;
        } finally {
            synchronizer.release(l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        final Lock l = synchronizer.acquire();
        try {
            flushByteBuffer();
        } finally {
            synchronizer.release(l);
        }
    }

    @Override
    public void close() throws IOException {
        final Lock l = synchronizer.acquire();
        try {
            if (isClosed) {
                return;
            }
            flushByteBuffer();
            isClosed = true;
        } finally {
            synchronizer.release(l);
        }
    }

    @Override
    public void write(final int i) throws IOException {
        final Lock l = synchronizer.acquire();
        try {
            if (isClosed) {
                throw new IOException(ERR_OUTPUT_CLOSED);
            }
            if (count >= buf.length) {
                responseToWebServer();
            }
            buf[count++] = (byte) i;
        } finally {
            synchronizer.release(l);
        }
    }

    /**
     * Checks if this output stream currently holds any data outstanding for being written to Web Server.
     *
     * @return <code>true</code> if this output stream currently holds any data; otherwise <code>false</code>.
     * @throws IOException If stream is already closed
     */
    public boolean hasData() throws IOException {
        final Lock l = synchronizer.acquire();
        try {
            return count > 0;
        } finally {
            synchronizer.release(l);
        }
    }

    /**
     * Gets current data held in this output stream outstanding for being written to Web Server.
     *
     * @return Current data held in this output stream
     * @throws IOException If stream is already closed
     */
    public byte[] getData() throws IOException {
        final Lock l = synchronizer.acquire();
        try {
            /*
             * try { byteBuffer.flush(); } catch (IOException e) { LOG.error(e.getMessage(), e); }
             */
            final byte[] retval = new byte[count];
            System.arraycopy(buf, 0, retval, 0, count);
            return retval;
        } finally {
            synchronizer.release(l);
        }
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        final Lock l = synchronizer.acquire();
        try {
            if (isClosed) {
                throw new IOException(ERR_OUTPUT_CLOSED);
            } else if (b == null) {
                throw new NullPointerException("AJPv13ServletOutputStream.write(byte[], int, int): Byte array is null");
            } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException("AJPv13ServletOutputStream.write(byte[], int, int): Invalid arguments");
            } else if (len == 0) {
                return;
            }
            final int maxLen = buf.length;
            final int restCapacity = maxLen - count;
            if (len <= restCapacity) {
                /*
                 * Everything fits into buffer!
                 */
                System.arraycopy(b, off, buf, count, len);
                count += len;
            } else {
                /*
                 * Write fitting bytes into buffer and flush buffer to output stream
                 */
                System.arraycopy(b, off, buf, count, restCapacity);
                count += restCapacity;
                responseToWebServer();
                /*
                 * Write exceeding bytes directly to output stream
                 */
                int offset = off + restCapacity; // add written bytes to offset
                final int lenIndex = offset + (len - restCapacity); // subtract written bytes from length
                int remLen = lenIndex - offset;
                while (remLen > maxLen) {
                    responseChunkToWebServer(b, offset, maxLen);
                    offset += maxLen;
                    remLen = lenIndex - offset;
                }
                /*
                 * Keep fitting bytes in byte buffer (if any)
                 */
                System.arraycopy(b, offset, buf, count, remLen);
                count += remLen;
            }
        } finally {
            synchronizer.release(l);
        }
    }

    private static final String ERR_BROKEN_PIPE = "Broken pipe";

    /**
     * Sends response headers to web server if not done before, writes all buffered bytes cut into AJP SEND_BODY_CHUNK packages to web
     * server, and resets byte buffer.
     *
     * @throws IOException If an I/O error occurs
     */
    private void responseToWebServer() throws IOException {
        responseToWebServer(buf, 0, count);
        count = 0;
    }

    /**
     * Sends response headers to web server if not done before and writes specified bytes cut into AJP SEND_BODY_CHUNK packages to web
     * server.
     *
     * @throws IOException If an I/O error occurs
     */
    private void responseToWebServer(final byte[] data, final int off, final int len) throws IOException {
        try {
            final XAJPv13Session session = (XAJPv13Session) ajpCon.getAttachment();
            /*
             * Ensure headers are written first
             */
            session.doWriteHeaders(ajpCon);
            /*
             * Send data cut into MAX_BODY_CHUNK_SIZE pieces
             */
            int offset = off;
            final int lenIndex = off + len;
            final int maxLen = AJPv13Response.MAX_SEND_BODY_CHUNK_SIZE;
            while (offset < lenIndex) {
                final int curLen = Math.min(maxLen, (lenIndex - offset));
                ajpCon.write(AJPv13Response.getSendBodyChunkBytes(data, offset, curLen));
                ajpCon.flush();
                offset += curLen;
            }
        } catch (final SocketException e) {
            if (e.getMessage().indexOf(ERR_BROKEN_PIPE) == -1) {
                LOG.error(e.getMessage(), e);
            } else {
                LOG.warn(new StringBuilder("Underlying (TCP) protocol communication aborted: ").append(e.getMessage()).toString(), e);
            }
            // ajpCon.close();
            final IOException ioexc = new IOException(e.getMessage());
            ioexc.initCause(e);
            throw ioexc;
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            final IOException ioexc = new IOException(e.getMessage());
            ioexc.initCause(e);
            throw ioexc;
        }
    }

    /**
     * Sends response headers to web server if not done before and writes specified chunk which must not exceed MAX_SEND_BODY_CHUNK_SIZE.
     *
     * @throws IOException If an I/O error occurs
     */
    private void responseChunkToWebServer(final byte[] data, final int off, final int len) throws IOException {
        try {
            final XAJPv13Session session = (XAJPv13Session) ajpCon.getAttachment();
            /*
             * Ensure headers are written first
             */
            session.doWriteHeaders(ajpCon);
            /*
             * Send data
             */
            ajpCon.write(AJPv13Response.getSendBodyChunkBytes(data, off, len));
            ajpCon.flush();
        } catch (final SocketException e) {
            if (e.getMessage().indexOf(ERR_BROKEN_PIPE) == -1) {
                LOG.error(e.getMessage(), e);
            } else {
                LOG.warn(new StringBuilder("Underlying (TCP) protocol communication aborted: ").append(e.getMessage()).toString(), e);
            }
            ajpCon.close();
            final IOException ioexc = new IOException(e.getMessage());
            ioexc.initCause(e);
            throw ioexc;
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            final IOException ioexc = new IOException(e.getMessage());
            ioexc.initCause(e);
            throw ioexc;
        }
    }

    /**
     * Flushes the byte buffer into stream.
     *
     * @throws IOException If an I/O error occurs
     */
    private void flushByteBuffer() throws IOException {
        responseToWebServer();
    }

    /**
     * Clears the byte buffer.
     *
     * @throws IOException If an I/O error occurs
     */
    public void clearByteBuffer() throws IOException {
        final Lock l = synchronizer.acquire();
        try {
            count = 0;
        } finally {
            synchronizer.release(l);
        }
    }

    @Override
    public void synchronize() {
        synchronizer.synchronize();
    }

    @Override
    public void unsynchronize() {
        synchronizer.unsynchronize();
    }

}

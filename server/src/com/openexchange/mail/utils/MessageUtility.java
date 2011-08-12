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

package com.openexchange.mail.utils;

import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.exception.OXException;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;

/**
 * {@link MessageUtility} - Provides various helper methods for message processing.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MessageUtility {

    private static final String STR_EMPTY = "";

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MessageUtility.class));

    private static final boolean DEBUG = LOG.isDebugEnabled();

    /**
     * No instantiation.
     */
    private MessageUtility() {
        super();
    }

    /**
     * Gets a valid charset-encoding for specified textual part; meaning its content type matches <code>text/&#42;</code>.
     *
     * @param p The part to detect a charset for
     * @param ct The part's content type
     * @return A valid charset-encoding for specified textual part.
     * @throws OXException If part's input stream cannot be obtained
     */
    public static String checkCharset(final MailPart p, final ContentType ct) throws OXException {
        String cs = ct.getCharsetParameter();
        if (!CharsetDetector.isValid(cs)) {
            StringBuilder sb = null;
            if (cs != null) {
                sb = new StringBuilder(64).append("Illegal or unsupported encoding: \"").append(cs).append("\".");
                mailInterfaceMonitor.addUnsupportedEncodingExceptions(cs);
            }
            cs = CharsetDetector.detectCharset(p.getInputStream());
            if (null != sb && LOG.isWarnEnabled()) {
                sb.append(" Using auto-detected encoding: \"").append(cs).append('"');
                LOG.warn(sb.toString());
            }
        }
        return cs;
    }

    /**
     * Gets a valid charset-encoding for specified textual part; meaning its content type matches <code>text/&#42;</code>.
     *
     * @param p The part to detect a charset for
     * @param ct The part's content type
     * @return A valid charset-encoding for specified textual part.
     */
    public static String checkCharset(final Part p, final ContentType ct) {
        String cs = ct.getCharsetParameter();
        if (!CharsetDetector.isValid(cs)) {
            StringBuilder sb = null;
            if (cs != null) {
                sb = new StringBuilder(64).append("Illegal or unsupported encoding: \"").append(cs).append("\".");
                mailInterfaceMonitor.addUnsupportedEncodingExceptions(cs);
            }
            cs = CharsetDetector.detectCharset(getPartInputStream(p));
            if (null != sb && LOG.isWarnEnabled()) {
                sb.append(" Using auto-detected encoding: \"").append(cs).append('"');
                LOG.warn(sb.toString());
            }
        }
        return cs;
    }

    /**
     * Gets the input stream of specified part.
     *
     * @param p The part whose input stream shall be returned
     * @return The part's input stream.
     */
    public static InputStream getPartInputStream(final Part p) {
        InputStream tmp = null;
        try {
            tmp = p.getInputStream();
            tmp.read();
            return p.getInputStream();
        } catch (final IOException e) {
            return getPartRawInputStream(p);
        } catch (final MessagingException e) {
            return getPartRawInputStream(p);
        } finally {
            if (null != tmp) {
                try {
                    tmp.close();
                } catch (final IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    private static InputStream getPartRawInputStream(final Part p) {
        /*
         * Try to get raw input stream
         */
        if (p instanceof MimeBodyPart) {
            try {
                return ((MimeBodyPart) p).getRawInputStream();
            } catch (final MessagingException e1) {
                return null;
            }
        }
        if (p instanceof MimeMessage) {
            try {
                return ((MimeMessage) p).getRawInputStream();
            } catch (final MessagingException e1) {
                return null;
            }
        }
        /*
         * Neither a MimeBodyPart nor a MimeMessage
         */
        return null;
    }

    /**
     * Reads the string out of MIME part's input stream. On first try the input stream retrieved by
     * <code>javax.mail.Part.getInputStream()</code> is used. If an I/O error occurs (<code>java.io.IOException</code>) then the next try is
     * with part's raw input stream. If everything fails an empty string is returned.
     *
     * @param p The <code>javax.mail.Part</code> object
     * @param ct The part's content type
     * @return The string read from part's input stream or the empty string "" if everything failed
     * @throws MessagingException If an error occurs in part's getter methods
     */
    public static String readMimePart(final Part p, final ContentType ct) throws MessagingException {
        /*
         * Use specified charset if available else use default one
         */
        String charset = ct.getCharsetParameter();
        if (null == charset) {
            charset = ServerConfig.getProperty(ServerConfig.Property.DefaultEncoding);
        }
        return readMimePart(p, charset);
    }

    /**
     * Reads the string out of MIME part's input stream. On first try the input stream retrieved by
     * <code>javax.mail.Part.getInputStream()</code> is used. If an I/O error occurs (<code>java.io.IOException</code>) then the next try is
     * with part's raw input stream. If everything fails an empty string is returned.
     *
     * @param p The <code>javax.mail.Part</code> object
     * @param charset The charset
     * @return The string read from part's input stream or the empty string "" if everything failed
     * @throws MessagingException If an error occurs in part's getter methods
     */
    public static String readMimePart(final Part p, final String charset) throws MessagingException {
        try {
            return readStream(p.getInputStream(), charset);
        } catch (final IOException e) {
            /*
             * Try to get data from raw input stream
             */
            final InputStream rawIn;
            if (p instanceof MimeBodyPart) {
                rawIn = ((MimeBodyPart) p).getRawInputStream();
            } else if (p instanceof MimeMessage) {
                rawIn = ((MimeMessage) p).getRawInputStream();
            } else {
                /*
                 * Neither a MimeBodyPart nor a MimeMessage
                 */
                return STR_EMPTY;
            }
            try {
                return readStream(rawIn, charset);
            } catch (final IOException e1) {
                LOG.error(e1.getMessage(), e1);
                return STR_EMPTY;
            }
        }
    }

    /**
     * Reads the stream content from given mail part.
     *
     * @param mailPart The mail part
     * @param charset The charset encoding used to generate a {@link String} object from raw bytes
     * @return the <code>String</code> read from mail part's stream
     * @throws IOException
     * @throws OXException
     */
    public static String readMailPart(final MailPart mailPart, final String charset) throws IOException, OXException {
        return readStream(mailPart.getInputStream(), charset);
    }

    private static final int BUFSIZE = 8192; // 8K

    private static final int STRBLD_SIZE = 32768; // 32K

    /**
     * The unknown character: <code>'&#65533;'</code>
     */
    public static final char UNKNOWN = '\ufffd';

    /**
     * Reads a string from given input stream using direct buffering.
     *
     * @param inStream The input stream
     * @param charset The charset
     * @return The <code>String</code> read from input stream
     * @throws IOException If an I/O error occurs
     */
    public static String readStream(final InputStream inStream, final String charset) throws IOException {
        if (null == inStream) {
            return STR_EMPTY;
        }
        if (isBig5(charset)) {
            /*
             * Special treatment for possible BIG5 encoded stream
             */
            final byte[] bytes = getBytesFrom(inStream);
            if (bytes.length == 0) {
                return STR_EMPTY;
            }
            final String retval = new String(bytes, "big5");
            if (retval.indexOf(UNKNOWN) < 0) {
                return retval;
            }
            /*
             * Expect the charset to be Big5-HKSCS
             */
            try {
                return new String(bytes, "Big5-HKSCS");
            } catch (final Error error) {
                // Huh..?
                final Throwable cause = error.getCause();
                if (cause.getClass().getName().equals("sun.io.ConversionBufferFullException")) {
                    /*
                     * Retry with auto-detected charset
                     */
                    return new String(bytes, CharsetDetector.detectCharset(new UnsynchronizedByteArrayInputStream(bytes)));
                }
                throw error;
            }
        }
        if (isGB2312(charset)) {
            /*
             * Special treatment for possible GB2312 encoded stream
             */
            final byte[] bytes = getBytesFrom(inStream);
            if (bytes.length == 0) {
                return STR_EMPTY;
            }
            final String retval = new String(bytes, "GB2312");
            if (retval.indexOf(UNKNOWN) < 0) {
                return retval;
            }
            /*
             * Detect the charset
             */
            if (!DEBUG) {
                return new String(bytes, CharsetDetector.detectCharset(new UnsynchronizedByteArrayInputStream(bytes)));
            }
            final String detectedCharset = CharsetDetector.detectCharset(new UnsynchronizedByteArrayInputStream(bytes));
            LOG.debug("Mapped \"GB2312\" charset to \"" + detectedCharset + "\".");
            return new String(bytes, detectedCharset);
        }
        return readStream0(inStream, charset);
    }

    private static String readStream0(final InputStream inStream, final String charset) throws IOException {
        final InputStreamReader isr;
        try {
            isr = new InputStreamReader(inStream, charset);
        } catch (final UnsupportedEncodingException e) {
            LOG.error("Unsupported encoding in a message detected and monitored: \"" + e.getMessage() + '"', e);
            mailInterfaceMonitor.addUnsupportedEncodingExceptions(e.getMessage());
            return STR_EMPTY;
        }
        try {
            int count = 0;
            final char[] cbuf = new char[BUFSIZE];
            if ((count = isr.read(cbuf, 0, cbuf.length)) <= 0) {
                return STR_EMPTY;
            }
            final StringBuilder sb = new StringBuilder(STRBLD_SIZE);
            do {
                sb.append(cbuf, 0, count);
            } while ((count = isr.read(cbuf)) > 0);
            return sb.toString();
        } catch (final IOException e) {
            if ("No content".equals(e.getMessage())) {
                /*-
                 * Special JavaMail I/O error to indicate no content available from IMAP server.
                 * Return the empty string in this case.
                 */
                return STR_EMPTY;
            }
            throw e;
        } finally {
            try {
                isr.close();
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private static final Pattern PATTERN_BIG5 = Pattern.compile("[-_]+");

    private static final String BIG5 = "big5";

    private static final String BIGFIVE = "bigfive";

    /**
     * Checks if specified charset name can be considered as BIG5.
     *
     * @param charset The charset name to check
     * @return <code>true</code> if charset name can be considered as BIG5; otherwise <code>false</code>
     */
    public static boolean isBig5(final String charset) {
        if (null == charset) {
            return false;
        }
        final String lc = charset.toLowerCase(Locale.US);
        if (!lc.startsWith("big", 0)) {
            return false;
        }
        final String wo = PATTERN_BIG5.matcher(lc).replaceAll("");
        return BIG5.equals(wo) || BIGFIVE.equals(wo);
    }

    private static final String GB2312 = "gb2312";

    /**
     * Checks if specified charset name can be considered as GB2312.
     *
     * @param charset The charset name to check
     * @return <code>true</code> if charset name can be considered as GB2312; otherwise <code>false</code>
     */
    public static boolean isGB2312(final String charset) {
        if (null == charset) {
            return false;
        }
        return GB2312.equals(charset.toLowerCase(Locale.US));
    }

    /**
     * Gets the byte content from specified input stream.
     *
     * @param in The input stream to get the byte content from
     * @return The byte content
     * @throws IOException If an I/O error occurs
     */
    public static byte[] getBytesFrom(final InputStream in) throws IOException {
        if (null == in) {
            return new byte[0];
        }
        try {
            final byte[] buf = new byte[BUFSIZE];
            int len = 0;
            if ((len = in.read(buf, 0, buf.length)) <= 0) {
                return new byte[0];
            }
            final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(BUFSIZE);
            do {
                out.write(buf, 0, len);
            } while ((len = in.read(buf, 0, buf.length)) > 0);
            return out.toByteArray();
        } catch (final IOException e) {
            if ("No content".equals(e.getMessage())) {
                /*-
                 * Special JavaMail I/O error to indicate no content available from IMAP server.
                 * Return an empty array in this case.
                 */
                return new byte[0];
            }
            throw e;
        } finally {
            try {
                in.close();
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Check if specified bytes contain ascii-only content.
     *
     * @param bytes The bytes to check
     * @return <code>true</code> if bytes are ascii-only; otherwise <code>false</code>
     */
    public static boolean isAscii(final byte[] bytes) {
        if (null == bytes || 0 == bytes.length) {
            return true;
        }
        final int len = bytes.length;
        boolean isAscci = true;
        for (int i = 0; (i < len) && isAscci; i++) {
            isAscci = bytes[i] >= 0;
        }
        return isAscci;
    }

}

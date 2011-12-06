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

package com.openexchange.ajax.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEType2ExtMap;
import com.openexchange.tools.ImageTypeDetector;
import com.openexchange.tools.encoding.Helper;
import com.openexchange.tools.encoding.URLCoder;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link DownloadUtility} - Utility class for download.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DownloadUtility {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(DownloadUtility.class));

    /**
     * Initializes a new {@link DownloadUtility}.
     */
    private DownloadUtility() {
        super();
    }

    /**
     * Checks specified input stream intended for inline display for harmful data if its Content-Type indicates image content.
     *
     * @param inputStream The input stream
     * @param fileName The file name
     * @param contentTypeStr The content-type string
     * @param userAgent The user agent
     * @return The checked download providing input stream, content type, and content disposition to use
     * @throws OXException If checking download fails
     */
    public static CheckedDownload checkInlineDownload(final InputStream inputStream, final String fileName, final String contentTypeStr, final String userAgent) throws OXException {
        return checkInlineDownload(inputStream, fileName, contentTypeStr, null, userAgent);
    }

    /**
     * Checks specified input stream intended for inline display for harmful data if its Content-Type indicates image content.
     *
     * @param inputStream The input stream
     * @param fileName The file name
     * @param contentTypeStr The content-type string
     * @param overridingDisposition Overrides the content disposition header, optional.
     * @param userAgent The user agent
     * @return The checked download providing input stream, content type, and content disposition to use
     * @throws OXException If checking download fails
     */
    public static CheckedDownload checkInlineDownload(final InputStream inputStream, final String fileName, final String contentTypeStr, final String overridingDisposition, final String userAgent) throws OXException {
        final BrowserDetector browserDetector = new BrowserDetector(userAgent);
        final boolean msieOnWindows = (browserDetector.isMSIE() && browserDetector.isWindows());
        /*
         * We are supposed to let the client display the attachment. Therefore set attachment's Content-Type and inline disposition to let
         * the client decide if it's able to display.
         */
        final ContentType contentType = new ContentType(contentTypeStr);
        if (contentType.startsWith("application/octet-stream")) {
            /*
             * Try to determine MIME type
             */
            final String ct = MIMEType2ExtMap.getContentType(fileName);
            final int pos = ct.indexOf('/');
            contentType.setPrimaryType(ct.substring(0, pos));
            contentType.setSubType(ct.substring(pos + 1));
        }
        InputStream in = inputStream;
        String fn = fileName;
        String preparedFileName = getSaveAsFileName(fileName, msieOnWindows, contentTypeStr);
        /*
         * Check if it's image content requested by Internet Explorer < v8
         */
        if (contentType.startsWith("image/") && msieOnWindows && 8F > browserDetector.getBrowserVersion()) {
            /*
             * Get first 256 bytes
             */
            byte[] sequence = new byte[256];
            {
                final int nRead;
                try {
                    nRead = in.read(sequence, 0, sequence.length);
                } catch (final IOException e) {
                    throw AjaxExceptionCodes.IO_ERROR.create( e, e.getMessage());
                }
                if (nRead < sequence.length) {
                    final byte[] tmp = sequence;
                    sequence = new byte[nRead];
                    System.arraycopy(tmp, 0, sequence, 0, nRead);
                }
            }
            /*
             * Check consistency of content-type, file extension and magic bytes
             */
            final String fileExtension = getFileExtension(fn);
            if (null == fileExtension) {
                /*
                 * Check for HTML since no corresponding file extension is known
                 */
                if (HTMLDetector.containsHTMLTags(sequence)) {
                    return asAttachment(inputStream, preparedFileName);
                }
            } else {
                final Set<String> extensions = new HashSet<String>(MIMEType2ExtMap.getFileExtensions(contentType.getBaseType()));
                if (extensions.isEmpty() || (extensions.size() == 1 && extensions.contains("dat"))) {
                    /*
                     * Content type determined by file name extension is unknown
                     */
                    final String ct = MIMEType2ExtMap.getContentType(fn);
                    if ("application/octet-stream".equals(ct)) {
                        /*
                         * No content type known
                         */
                        if (HTMLDetector.containsHTMLTags(sequence)) {
                            return asAttachment(inputStream, preparedFileName);
                        }
                    } else {
                        final int pos = ct.indexOf('/');
                        contentType.setPrimaryType(ct.substring(0, pos));
                        contentType.setSubType(ct.substring(pos + 1));
                    }
                } else if (!extensions.contains(fileExtension)) {
                    /*
                     * File extension does not fit to MIME type. Reset file name.
                     */
                    fn = addFileExtension(fileExtension, extensions.iterator().next());
                    preparedFileName = getSaveAsFileName(fn, msieOnWindows, contentType.getBaseType());
                }
                final String detectedCT = ImageTypeDetector.getMimeType(sequence);
                if ("application/octet-stream".equals(detectedCT)) {
                    /*
                     * Unknown magic bytes. Check for HTML.
                     */
                    if (HTMLDetector.containsHTMLTags(sequence)) {
                        return asAttachment(inputStream, preparedFileName);
                    }
                } else if (!contentType.isMimeType(detectedCT)) {
                    /*
                     * Image's magic bytes indicate another content type
                     */
                    contentType.setBaseType(detectedCT);
                }
            }
            /*
             * New combined input stream
             */
            in = new CombinedInputStream(sequence, in);
        }

        if (overridingDisposition == null) {
            final String baseType = contentType.getBaseType();
            final StringBuilder builder = new StringBuilder(32).append("attachment");
            appendFilenameParameter(fileName, baseType, userAgent, builder);
            return new CheckedDownload(baseType, builder.toString(), in);
        }
        if (overridingDisposition.indexOf(';') < 0) {
            final String baseType = contentType.getBaseType();
            final StringBuilder builder = new StringBuilder(32).append(overridingDisposition);
            appendFilenameParameter(fileName, baseType, userAgent, builder);
            return new CheckedDownload(baseType, builder.toString(), in);
        }

        return new CheckedDownload(contentType.getBaseType(), overridingDisposition, in);
    }

    private static void appendFilenameParameter(final String fileName, final String baseCT, final String userAgent, final StringBuilder appendTo) {
        if (null == fileName) {
            appendTo.append("; filename=\"").append(DEFAULT_FILENAME).append('"').toString();
            return;
        }
        String fn = fileName;
        if ((null != baseCT) && (null == getFileExtension(fn))) {
            if (baseCT.regionMatches(true, 0, MIME_TEXT_PLAIN, 0, MIME_TEXT_PLAIN.length())) {
                fn += ".txt";
            } else if (baseCT.regionMatches(true, 0, MIME_TEXT_HTML, 0, MIME_TEXT_HTML.length())) {
                fn += ".html";
            }
        }
        fn = escapeBackslashAndQuote(fn);
        if (null != userAgent && new BrowserDetector(userAgent).isMSIE()) {
            // InternetExplorer
            appendTo.append("; filename=\"").append(Helper.encodeFilenameForIE(fn, Charsets.UTF_8)).append('"').toString();
            return;
        }
        /*-
         * On socket layer characters are casted to byte values.
         *
         * See AJPv13Response.writeString():
         * sink.write((byte) chars[i]);
         *
         * Therefore ensure we have a one-character-per-byte charset, as it is with ISO-8859-1
         */
        final String foo = new String(fn.getBytes(Charsets.UTF_8), Charsets.ISO_8859_1);
        appendTo.append("; filename*=UTF-8''").append(URLCoder.encode(fn)).append("; filename=\"").append(foo).append('"').toString();
    }

    private static final Pattern PAT_BSLASH = Pattern.compile("\\\\");

    private static final Pattern PAT_QUOTE = Pattern.compile("\"");

    private static String escapeBackslashAndQuote(final String str) {
        return PAT_QUOTE.matcher(PAT_BSLASH.matcher(str).replaceAll("\\\\\\\\")).replaceAll("\\\\\\\"");
    }

    private static CheckedDownload asAttachment(final InputStream inputStream, final String preparedFileName) {
        /*
         * We are supposed to offer attachment for download. Therefore enforce application/octet-stream and attachment disposition.
         */
        return new CheckedDownload("application/octet-stream", new StringBuilder(64).append("attachment; filename=\"").append(
            preparedFileName).append('"').toString(), inputStream);
    }

    private static final Pattern P = Pattern.compile("^[\\w\\d\\:\\/\\.]+(\\.\\w{3,4})$");
    
    /**
     * Checks if specified file name has a trailing file extension.
     * 
     * @param fileName The file name
     * @return The extension (e.g. <code>".txt"</code>) or <code>null</code>
     */
    private static String getFileExtension(final String fileName) {
        if (null == fileName || fileName.indexOf('.') <= 0) {
            return null;
        }
        final Matcher m = P.matcher(fileName);
        return m.matches() ? m.group(1).toLowerCase(Locale.ENGLISH) : null;
    }

    private static String addFileExtension(final String fileName, final String ext) {
        if (null == fileName) {
            return null;
        }
        final int pos = fileName.indexOf('.');
        if (-1 == pos) {
            return new StringBuilder(fileName).append('.').append(ext).toString();
        }
        return new StringBuilder(fileName.substring(0, pos)).append(".").append(ext).toString();
    }

    private static final String DEFAULT_FILENAME = "file.dat";

    private static final String MIME_TEXT_PLAIN = "text/plain";

    private static final String MIME_TEXT_HTML = "text/htm";

    /**
     * Gets a safe form (as per RFC 2047) for specified file name.
     * <p>
     * {@link BrowserDetector} may be used to parse browser and/or platform identifier from <i>"user-agent"</i> header.
     *
     * @param fileName The file name
     * @param internetExplorer <code>true</code> if <i>"user-agent"</i> header indicates to be Internet Explorer on a Windows platform;
     *            otherwise <code>false</code>
     * @param baseCT The (optional) base content type
     * @return A safe form (as per RFC 2047) for specified file name
     * @see BrowserDetector
     */
    public static final String getSaveAsFileName(final String fileName, final boolean internetExplorer, final String baseCT) {
        if (null == fileName) {
            return DEFAULT_FILENAME;
        }
        final StringBuilder tmp = new StringBuilder(32);
        try {
            if (fileName.indexOf(' ') >= 0) {
                tmp.append(Helper.encodeFilename(fileName.replaceAll(" ", "_"), "UTF-8", internetExplorer));
            } else {
                tmp.append(Helper.encodeFilename(fileName, "UTF-8", internetExplorer));
            }
        } catch (final UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
            return fileName;
        }
        if ((null != baseCT) && (null == getFileExtension(fileName))) {
            if (baseCT.regionMatches(true, 0, MIME_TEXT_PLAIN, 0, MIME_TEXT_PLAIN.length())) {
                tmp.append(".txt");
            } else if (baseCT.regionMatches(true, 0, MIME_TEXT_HTML, 0, MIME_TEXT_HTML.length())) {
                tmp.append(".html");
            }
        }
        return tmp.toString();
    }

    /**
     * {@link CheckedDownload} - Represents a checked download as a result of <tt>DownloadUtility.checkInlineDownload()</tt>.
     */
    public static final class CheckedDownload {

        private final String contentType;

        private final String contentDisposition;

        private final InputStream inputStream;

        CheckedDownload(final String contentType, final String contentDisposition, final InputStream inputStream) {
            super();
            this.contentType = contentType;
            this.contentDisposition = contentDisposition;
            this.inputStream = inputStream;
        }

        /**
         * Gets the content type.
         *
         * @return The content type
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Gets the content disposition.
         *
         * @return The content disposition
         */
        public String getContentDisposition() {
            return contentDisposition;
        }

        /**
         * Gets the input stream.
         *
         * @return The input stream
         */
        public InputStream getInputStream() {
            return inputStream;
        }

    }

}

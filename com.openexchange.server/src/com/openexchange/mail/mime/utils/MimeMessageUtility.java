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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.mail.mime.utils;

import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.james.mime4j.io.LineReaderInputStream;
import org.apache.james.mime4j.io.LineReaderInputStreamAdaptor;
import org.apache.james.mime4j.stream.DefaultFieldBuilder;
import org.apache.james.mime4j.stream.FieldBuilder;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.util.ByteArrayBuffer;
import org.apache.james.mime4j.util.CharsetUtil;

import com.openexchange.ajax.requesthandler.DefaultDispatcherPrefixService;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.image.ImageActionFactory;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentDisposition;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.HeaderName;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeMailException;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.mail.mime.PlainTextAddress;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.dataobjects.MimeMailMessage;
import com.openexchange.mail.mime.dataobjects.MimeMailPart;
import com.openexchange.mail.utils.CharsetDetector;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.TimeZoneUtils;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;
import com.sun.mail.imap.protocol.BODYSTRUCTURE;

/**
 * {@link MimeMessageUtility} - Utilities for MIME messages.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MimeMessageUtility {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(MimeMessageUtility.class));

    private static final boolean TRACE = LOG.isTraceEnabled();

    private static final Set<HeaderName> ENCODINGS;

    private static final MailDateFormat MAIL_DATE_FORMAT;

    static {
        final Set<HeaderName> tmp = new HashSet<HeaderName>(4);
        tmp.add(HeaderName.valueOf("iso-8859-1"));
        tmp.add(HeaderName.valueOf("windows-1258"));
        tmp.add(HeaderName.valueOf("UTF-8"));
        tmp.add(HeaderName.valueOf("us-ascii"));
        ENCODINGS = java.util.Collections.unmodifiableSet(tmp);
        final MailDateFormat mdf = new MailDateFormat();
        mdf.setTimeZone(TimeZoneUtils.getTimeZone("UTC"));
        MAIL_DATE_FORMAT = mdf;
    }

    private static final ConcurrentMap<String, Future<MailDateFormat>> MDF_MAP = new ConcurrentHashMap<String, Future<MailDateFormat>>();

    /**
     * No instantiation
     */
    private MimeMessageUtility() {
        super();
    }

    /**
     * Gets the default {@link MailDateFormat}.
     * <p>
     * Note that returned instance of {@link MailDateFormat} is shared, therefore use a surrounding synchronized block to preserve thread
     * safety:
     * 
     * <pre>
     * ...
     * final MailDateFormat mdf = MIMEMessageUtility.getMailDateFormat(session);
     * synchronized(mdf) {
     *  mimeMessage.setHeader(&quot;Date&quot;, mdf.format(sendDate));
     * }
     * ...
     * </pre>
     * 
     * @return The {@link MailDateFormat} for specified session
     */
    public static MailDateFormat getDefaultMailDateFormat() {
        return MAIL_DATE_FORMAT;
    }

    /**
     * Gets the {@link MailDateFormat} for specified session.
     * <p>
     * Note that returned instance of {@link MailDateFormat} is shared, therefore use a surrounding synchronized block to preserve thread
     * safety:
     * 
     * <pre>
     * ...
     * final MailDateFormat mdf = MIMEMessageUtility.getMailDateFormat(session);
     * synchronized(mdf) {
     *  mimeMessage.setHeader(&quot;Date&quot;, mdf.format(sendDate));
     * }
     * ...
     * </pre>
     * 
     * @param session The user session
     * @return The {@link MailDateFormat} for specified session
     * @throws OXException If {@link MailDateFormat} cannot be returned
     */
    public static MailDateFormat getMailDateFormat(final Session session) throws OXException {
        final User user;
        if (session instanceof ServerSession) {
            user = ((ServerSession) session).getUser();
        } else {
            user = UserStorage.getStorageUser(session.getUserId(), session.getContextId());
        }
        return getMailDateFormat(user.getTimeZone());
    }

    /**
     * Gets the {@link MailDateFormat} for specified time zone identifier.
     * <p>
     * Note that returned instance of {@link MailDateFormat} is shared, therefore use a surrounding synchronized block to preserve thread
     * safety:
     * 
     * <pre>
     * ...
     * final MailDateFormat mdf = MIMEMessageUtility.getMailDateFormat(timeZoneId);
     * synchronized(mdf) {
     *  mimeMessage.setHeader(&quot;Date&quot;, mdf.format(sendDate));
     * }
     * ...
     * </pre>
     * 
     * @param timeZoneId The time zone identifier
     * @return The {@link MailDateFormat} for specified time zone identifier
     * @throws OXException If {@link MailDateFormat} cannot be returned
     */
    public static MailDateFormat getMailDateFormat(final String timeZoneId) throws OXException {
        Future<MailDateFormat> future = MDF_MAP.get(timeZoneId);
        if (null == future) {
            final FutureTask<MailDateFormat> ft = new FutureTask<MailDateFormat>(new Callable<MailDateFormat>() {

                @Override
                public MailDateFormat call() throws Exception {
                    final MailDateFormat mdf = new MailDateFormat();
                    mdf.setTimeZone(TimeZoneUtils.getTimeZone(timeZoneId));
                    return mdf;
                }
            });
            future = MDF_MAP.putIfAbsent(timeZoneId, ft);
            if (null == future) {
                future = ft;
                ft.run();
            }
        }
        try {
            return future.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            throw MailExceptionCode.UNEXPECTED_ERROR.create(cause, cause.getMessage());
        }
    }

    /**
     * Checks if specified headers are empty. The passed headers are considered as all the values for a certain header or <code>null</code>
     * if no headers exist.
     * 
     * @param headers The values for a certain header
     * @return <code>true</code> if specified headers are empty; otherwise <code>false</code>
     */
    public static boolean isEmptyHeader(final String[] headers) {
        if (null == headers || 0 == headers.length) {
            return true;
        }
        boolean isEmpty = true;
        for (int i = 0; isEmpty && i < headers.length; i++) {
            isEmpty = isEmpty(headers[i]);
        }
        return isEmpty;
    }

    private static boolean isEmpty(final String string) {
        if (null == string) {
            return true;
        }
        final int len = string.length();
        boolean isWhitespace = true;
        for (int i = 0; isWhitespace && i < len; i++) {
            isWhitespace = Character.isWhitespace(string.charAt(i));
        }
        return isWhitespace;
    }

    private static final Pattern PATTERN_EMBD_IMG = Pattern.compile(
        "(<img[^>]+src=\"cid:)([^\"]+)(\"[^>]*/?>)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern PATTERN_EMBD_IMG_ALT = Pattern.compile(
        "(<img[^>]+src=\")([0-9a-z&&[^.\\s>\"]]+\\.[0-9a-z&&[^.\\s>\"]]+)(\"[^>]*/?>)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Detects if given HTML content contains inlined images
     * <p>
     * Example:
     * 
     * <pre>
     * &lt;img src=&quot;cid:s345asd845@12drg&quot;&gt;
     * </pre>
     * 
     * @param htmlContent The HTML content
     * @return <code>true</code> if given HTML content contains inlined images; otherwise <code>false</code>
     */
    public static boolean hasEmbeddedImages(final CharSequence htmlContent) {
        return PATTERN_EMBD_IMG.matcher(htmlContent).find() || PATTERN_EMBD_IMG_ALT.matcher(htmlContent).find();
    }

    /**
     * Gathers all occurring content IDs in HTML content and returns them as a list
     * 
     * @param htmlContent The HTML content
     * @return an instance of <code>{@link List}</code> containing all occurring content IDs
     */
    public static List<String> getContentIDs(final CharSequence htmlContent) {
        final List<String> retval = new LinkedList<String>();
        Matcher m = PATTERN_EMBD_IMG.matcher(htmlContent);
        while (m.find()) {
            retval.add(m.group(2));
        }
        m = PATTERN_EMBD_IMG_ALT.matcher(htmlContent);
        while (m.find()) {
            retval.add(m.group(2));
        }
        return retval;
    }

    /**
     * Compares (case insensitive) the given values of message header "Content-ID". The leading/trailing characters '<code>&lt;</code>' and
     * ' <code>&gt;</code>' are ignored during comparison
     * 
     * @param contentId1 The first content ID
     * @param contentId2 The second content ID
     * @return <code>true</code> if both are equal; otherwise <code>false</code>
     */
    public static boolean equalsCID(final String contentId1, final String contentId2) {
        if (null != contentId1 && null != contentId2) {
            final int length1 = contentId1.length();
            final int length2 = contentId2.length();
            final String cid1 = length1 > 0 && contentId1.charAt(0) == '<' ? contentId1.substring(1, length1 - 1) : contentId1;
            return cid1.equalsIgnoreCase(length2 > 0 && contentId2.charAt(0) == '<' ? contentId2.substring(1, length2 - 1) : contentId2);
        }
        return false;
    }

    private static final String IMAGE_ALIAS_APPENDIX = ImageActionFactory.ALIAS_APPENDIX;

    private static final String FILE_ALIAS_APPENDIX = "file";

    /**
     * Checks if specified &lt;image&gt; tag's <code>src</code> attribute seems to point to OX image Servlet.
     * 
     * @param imageTag The &lt;image&gt; tag
     * @return <code>true</code> if image Servlet is addressed; otherwise <code>false</code>
     */
    public static boolean isValidImageUri(final String imageTag) {
        if (isEmpty(imageTag)) {
            return false;
        }
        final String tmp = imageTag.toLowerCase(Locale.US);
        final String srcStart = "src=\"";
        final int pos = tmp.indexOf(srcStart);
        int fromIndex = pos + srcStart.length();
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        String prefix = DefaultDispatcherPrefixService.getInstance().getPrefix();
        if (prefix.charAt(0) == '/') {
            prefix = prefix.substring(1);
        }
        return tmp.indexOf(prefix+IMAGE_ALIAS_APPENDIX, fromIndex) >= 0 || tmp.indexOf(prefix+FILE_ALIAS_APPENDIX, fromIndex) >= 0;
    }

    /**
     * Detects if given HTML content contains references to local image files
     * <ul>
     * <li>Example for an uploaded image file referenced within a composed mail:<br>
     * <code>
     * &nbsp;&nbsp;&lt;img&nbsp;src=&quot;/ajax/file?action=get&amp;session=abcdefg&amp;id=123dfr567zh&quot;&gt;
     * </code></li>
     * <li>Example for a stored image file referenced within a composed mail:<br>
     * <code>
     * &nbsp;&nbsp;&lt;img&nbsp;src=&quot;/ajax/image?uid=12gf356j7&quot;&gt;
     * </code></li>
     * </ul>
     * 
     * @param htmlContent The HTML content
     * @return <code>true</code> if given HTML content contains references to local image files; otherwise <code>false</code>
     */
    public static boolean hasReferencedLocalImages(final CharSequence htmlContent) {
        final ImageMatcher m = ImageMatcher.matcher(htmlContent);
        if (m.find()) {
            final ManagedFileManagement mfm = ServerServiceRegistry.getInstance().getService(ManagedFileManagement.class);
            do {
                final String mid = m.getManagedFileId();
                if (null != mid) {
                    mfm.contains(mid);
                }
            } while (m.find());
            return true;
        }
        return false;
    }

    /**
     * Determines specified part's real filename if any available.
     * 
     * @param part The part whose filename shall be determined
     * @return The part's real filename or <code>null</code> if none present
     */
    public static String getRealFilename(final MailPart part) {
        if (part.getFileName() != null) {
            return part.getFileName();
        }
        final String hdr = part.getFirstHeader(MessageHeaders.HDR_CONTENT_DISPOSITION);
        if (hdr == null) {
            return getContentTypeFilename(part);
        }
        try {
            final String retval = new ContentDisposition(hdr).getFilenameParameter();
            if (retval == null) {
                return getContentTypeFilename(part);
            }
            return retval;
        } catch (final OXException e) {
            return getContentTypeFilename(part);
        }
    }

    private static final String PARAM_NAME = "name";

    private static String getContentTypeFilename(final MailPart part) {
        if (part.containsContentType()) {
            return part.getContentType().getParameter(PARAM_NAME);
        }
        final String hdr = part.getFirstHeader(MessageHeaders.HDR_CONTENT_TYPE);
        if (hdr == null || hdr.length() == 0) {
            return null;
        }
        try {
            return new ContentType(hdr).getParameter(PARAM_NAME);
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    /*
     * Multipart subtype constants
     */
    private static final String MULTI_SUBTYPE_ALTERNATIVE = "ALTERNATIVE";

    // private static final String MULTI_SUBTYPE_MIXED = "MIXED";

    // private static final String MULTI_SUBTYPE_SIGNED = "SIGNED";

    /**
     * Checks if given multipart contains (file) attachments
     * 
     * @param mp The multipart to examine
     * @param subtype The multipart's subtype
     * @return <code>true</code> if given multipart contains (file) attachments; otherwise <code>false</code>
     * @throws MessagingException If a messaging error occurs
     * @throws OXException If a mail error occurs
     * @throws IOException If an I/O error occurs
     */
    public static boolean hasAttachments(final Multipart mp, final String subtype) throws MessagingException, OXException, IOException {
        if (MULTI_SUBTYPE_ALTERNATIVE.equalsIgnoreCase(subtype)) {
            if (mp.getCount() > 2) {
                return true;
            }
            return hasAttachments0(mp);
        }
        // TODO: Think about special check for multipart/signed
        /*
         * if (MULTI_SUBTYPE_SIGNED.equalsIgnoreCase(subtype)) { if (mp.getCount() > 2) { return true; } return hasAttachments0(mp); }
         */
        if (mp.getCount() > 1) {
            return true;
        }
        return hasAttachments0(mp);
    }

    private static boolean hasAttachments0(final Multipart mp) throws MessagingException, OXException, IOException {
        boolean found = false;
        final int count = mp.getCount();
        final ContentType ct = new ContentType();
        for (int i = 0; i < count && !found; i++) {
            final BodyPart part = mp.getBodyPart(i);
            final String[] tmp = part.getHeader(MessageHeaders.HDR_CONTENT_TYPE);
            if (tmp != null && tmp.length > 0) {
                ct.setContentType(MimeMessageUtility.unfold(tmp[0]));
            } else {
                ct.setContentType(MimeTypes.MIME_DEFAULT);
            }
            if (ct.isMimeType(MimeTypes.MIME_MULTIPART_ALL)) {
                found |= hasAttachments((Multipart) part.getContent(), ct.getSubType());
            }
        }
        return found;
    }

    /**
     * Checks if given BODYSTRUCTURE item indicates to contain (file) attachments
     * 
     * @param bodystructure The BODYSTRUCTURE item
     * @return <code>true</code> if given BODYSTRUCTURE item indicates to contain (file) attachments; otherwise <code>false</code>
     */
    public static boolean hasAttachments(final BODYSTRUCTURE bodystructure) {
        if (bodystructure.isMulti()) {
            if (MULTI_SUBTYPE_ALTERNATIVE.equalsIgnoreCase(bodystructure.subtype)) {
                if (bodystructure.bodies.length > 2) {
                    return true;
                }
                return hasAttachments0(bodystructure);
            }
            // TODO: Think about special check for multipart/signed
            /*
             * if (MULTI_SUBTYPE_SIGNED.equalsIgnoreCase(bodystructure.subtype)) { if (bodystructure.bodies.length > 2) { return true; }
             * return hasAttachments0(bodystructure); }
             */
            if (bodystructure.bodies.length > 1) {
                return true;
            }
            return hasAttachments0(bodystructure);
        }
        return false;
    }

    private static boolean hasAttachments0(final BODYSTRUCTURE bodystructure) {
        boolean found = false;
        for (int i = 0; (i < bodystructure.bodies.length) && !found; i++) {
            found |= hasAttachments(bodystructure.bodies[i]);
        }
        return found;
    }

    /**
     * Decodes a "Subject" header obtained from ENVELOPE fetch item.
     * 
     * @param subject The subject obtained from ENVELOPE fetch item
     * @return The decoded subject value
     */
    public static String decodeEnvelopeSubject(final String subject) {
        if (null == subject) {
            return "";
        }
        /*-
         * Hmm... Why does ENVELOPE FETCH response omit CR?LFs in subject?!
         *
         * Example:
         * Subject: =?UTF-8?Q?Nur_noch_kurze_Zeit:_1_Freimona?=
         *  =?UTF-8?Q?t_f=C3=BCr_3_erfolgreiche_Einladungen?=
         *
         * is transferred as:
         * =?UTF-8?Q?Nur_noch_kurze_Zeit:_1_Freimona?= =?UTF-8?Q?t_f=C3=BCr_3_erfolgreiche_Einladungen?=
         */
        final char[] chars = checkNonAscii(subject).toCharArray();
        final StringBuilder sb = new StringBuilder(chars.length);
        int i = 0;
        while (i < chars.length) {
            final char c = chars[i];
            if ('\t' == c || ' ' == c) {
                while ((i + 1) < chars.length && ' ' == chars[i + 1]) {
                    i++;
                }
                sb.append(' ');
            } else if ('\r' != c && '\n' != c) {
                sb.append(c);
            }
            i++;
        }
        return decodeEnvelopeHeader0(sb.toString());
    }

    /**
     * Decodes a string header obtained from ENVELOPE fetch item.
     * 
     * @param value The header value
     * @return The decoded header value
     */
    public static String decodeEnvelopeHeader(final String value) {
        return decodeEnvelopeHeader0(checkNonAscii(value));
    }

    /**
     * The max. length of a RFC 2047 style encoded word.
     */
    private static final int ENCODED_WORD_LEN = 75;

    /**
     * Internal method to decode a string header obtained from ENVELOPE fetch item.
     * 
     * @param value The header value
     * @return The decoded header value
     */
    private static String decodeEnvelopeHeader0(final String value) {
        final int length = value.length();
        /*
         * Passes possibly encoded-word is greater than 75 characters and contains no CR?LF
         */
        if ((length > ENCODED_WORD_LEN) && (value.indexOf('\r') < 0) && (value.indexOf('\n') < 0)) {
            final StringBuilder sb = new StringBuilder(length).append(value);
            final String pattern = "?= =?";
            int i;
            while ((i = sb.indexOf(pattern)) >= 0) {
                sb.deleteCharAt(i + 2);
            }
            return decodeMultiEncodedHeader0(sb.toString(), false);
        }
        return decodeMultiEncodedHeader0(value, true);
    }

    private static final Pattern ENC_PATTERN = Pattern.compile("=\\?(\\S+?)\\?(\\S+?)\\?(.+?)\\?=");

    /**
     * Decodes a multi-mime-encoded header value using the algorithm specified in RFC 2047, Section 6.1.
     * <p>
     * If the charset-conversion fails for any sequence, an {@link UnsupportedEncodingException} is thrown.
     * <p>
     * If the String is not a RFC 2047 style encoded header, it is returned as-is
     * 
     * @param headerValue The possibly encoded header value
     * @return The possibly decoded header value
     */
    public static String decodeMultiEncodedHeader(final String headerValue) {
        if ((null == headerValue) || (headerValue.indexOf("=?") < 0)) {
            // In case of null, no "=?" marker
            return unfold(headerValue);
        }
        return decodeMultiEncodedHeader0(checkNonAscii(headerValue), true);
    }

    private static String decodeMultiEncodedHeader0(final String headerValue, final boolean unfold) {
        if (headerValue == null) {
            return null;
        }
        final String hdrVal = unfold ? unfold(headerValue) : headerValue;
        /*
         * Whether the sequence "=?" exists at all
         */
        if (hdrVal.indexOf("=?") < 0) {
            return hdrVal;
        }
        final Matcher m = ENC_PATTERN.matcher(hdrVal);
        if (m.find()) {
            final StringBuilder sb = new StringBuilder(hdrVal.length());
            int lastMatch = 0;
            do {
                try {
                    sb.append(hdrVal.substring(lastMatch, m.start()));
                    /*
                     * Decode encoded-word
                     */
                    final String charset = m.group(1);
                    if (MessageUtility.isBig5(charset)) {
                        final String encodedWord = m.group();
                        String decodeWord = MimeUtility.decodeWord(encodedWord);
                        if (decodeWord.indexOf(MessageUtility.UNKNOWN) >= 0) {
                            decodeWord = MimeUtility.decodeWord(encodedWord.replaceFirst(Pattern.quote(charset), "Big5-HKSCS"));
                        }
                        sb.append(decodeWord);
                    } else if (MessageUtility.isGB2312(charset)) {
                        final String encodedWord = m.group();
                        String decodeWord = MimeUtility.decodeWord(encodedWord);
                        if (decodeWord.indexOf(MessageUtility.UNKNOWN) >= 0) {
                            decodeWord = MimeUtility.decodeWord(encodedWord.replaceFirst(Pattern.quote(charset), "GB18030"));
                        }
                        sb.append(decodeWord);
                    } else {
                        sb.append(MimeUtility.decodeWord(m.group()));
                    }
                    /*
                     * Remember last match position
                     */
                    lastMatch = m.end();
                } catch (final UnsupportedEncodingException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Unsupported character-encoding in encoded-word: " + m.group(), e);
                    }
                    sb.append(handleUnsupportedEncoding(m));
                    lastMatch = m.end();
                } catch (final ParseException e) {
                    return decodeMultiEncodedHeaderSafe(headerValue);
                }
            } while (m.find());
            sb.append(hdrVal.substring(lastMatch));
            return sb.toString();
        }
        return hdrVal;
    }

    private static String handleUnsupportedEncoding(final Matcher m) {
        final String asciiText = m.group(3);
        final String detectedCharset;
        final byte[] rawBytes;
        {
            final String transferEncoding = m.group(2);
            if ("Q".equalsIgnoreCase(transferEncoding)) {
                try {
                    rawBytes = QuotedPrintableCodec.decodeQuotedPrintable(asciiText.getBytes(com.openexchange.java.Charsets.US_ASCII));
                } catch (final DecoderException e) {
                    /*
                     * Invalid quoted-printable
                     */
                    LOG.warn("Cannot decode quoted-printable: " + e.getMessage(), e);
                    return asciiText;
                }
            } else if ("B".equalsIgnoreCase(transferEncoding)) {
                rawBytes = Base64.decodeBase64(asciiText.getBytes(com.openexchange.java.Charsets.US_ASCII));
            } else {
                /*
                 * Unknown transfer-encoding; just return current match
                 */
                LOG.warn("Unknown transfer-encoding: " + transferEncoding);
                return asciiText;
            }
            detectedCharset = CharsetDetector.detectCharset(new UnsynchronizedByteArrayInputStream(rawBytes));
        }
        try {
            return new String(rawBytes, Charsets.forName(detectedCharset));
        } catch (final UnsupportedCharsetException e) {
            /*
             * Even detected charset is unknown... giving up
             */
            LOG.warn("Unknown character-encoding: " + detectedCharset);
            return asciiText;
        }
    }

    /**
     * Checks if given raw header contains non-ascii characters.
     * 
     * @param rawHeader The raw header
     * @return The proper unicode string
     */
    public static String checkNonAscii(final String rawHeader) {
        if (null == rawHeader || isAscii(rawHeader)) {
            return rawHeader;
        }
        return convertNonAscii(rawHeader);
    }

    private static String convertNonAscii(final String rawHeader) {
        final int length = rawHeader.length();
        final byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) rawHeader.charAt(i);
        }
        try {
            final String detectCharset = CharsetDetector.detectCharset(new UnsynchronizedByteArrayInputStream(bytes));
            return MessageUtility.readStream(new UnsynchronizedByteArrayInputStream(bytes), detectCharset);
        } catch (final IOException e) {
            // Cannot occur
            return rawHeader;
        }
    }

    /**
     * Checks whether the specified string's characters are ASCII 7 bit
     * 
     * @param s The string to check
     * @return <code>true</code> if string's characters are ASCII 7 bit; otherwise <code>false</code>
     */
    private static boolean isAscii(final String s) {
        final int length = s.length();
        boolean isAscci = true;
        for (int i = 0; (i < length) && isAscci; i++) {
            isAscci &= (s.charAt(i) < 128);
        }
        return isAscci;
    }

    /**
     * Decodes a multi-mime-encoded header value using the algorithm specified in RFC 2047, Section 6.1 in a safe manner.
     * 
     * @param headerValue The possibly encoded header value
     * @return The possibly decoded header value
     */
    private static String decodeMultiEncodedHeaderSafe(final String headerValue) {
        if (headerValue == null) {
            return null;
        }
        final String hdrVal = MimeMessageUtility.unfold(headerValue);
        final Matcher m = ENC_PATTERN.matcher(hdrVal);
        if (m.find()) {
            final StringBuilder sb = new StringBuilder(hdrVal.length());
            StringBuilder tmp = null;
            int lastMatch = 0;
            do {
                try {
                    sb.append(hdrVal.substring(lastMatch, m.start()));
                    if ("Q".equalsIgnoreCase(m.group(2))) {
                        final String charset = m.group(1);
                        final String preparedEWord = prepareQEncodedValue(m.group(3), charset);
                        if (null == tmp) {
                            tmp = new StringBuilder(preparedEWord.length() + 16);
                        } else {
                            tmp.setLength(0);
                        }
                        tmp.append("=?").append(charset).append('?').append('Q').append('?').append(preparedEWord).append("?=");
                        sb.append(MimeUtility.decodeWord(tmp.toString()));
                    } else if ("B".equalsIgnoreCase(m.group(2))) {
                        try {
                            sb.append(MimeUtility.decodeWord(m.group()));
                        } catch (final ParseException e) {
                            /*
                             * Retry with another library
                             */
                            sb.append(new String(
                                Base64.decodeBase64(m.group(3).getBytes(com.openexchange.java.Charsets.US_ASCII)),
                                Charsets.forName(m.group(1))));
                        }
                    } else {
                        sb.append(MimeUtility.decodeWord(m.group()));
                    }
                    lastMatch = m.end();
                } catch (final UnsupportedEncodingException e) {
                    LOG.warn("Unsupported character-encoding in encoded-word: " + m.group(), e);
                    sb.append(m.group());
                    lastMatch = m.end();
                } catch (final ParseException e) {
                    LOG.warn("String is not an encoded-word as per RFC 2047: " + m.group(), e);
                    sb.append(m.group());
                    lastMatch = m.end();
                }
            } while (m.find());
            sb.append(hdrVal.substring(lastMatch));
            return sb.toString();
        }
        return hdrVal;
    }

    /**
     * Prepares specified encoded word, thus even corrupt headers are properly handled.<br>
     * Here is an example of such a corrupt header:
     * 
     * <pre>
     * =?windows-1258?Q?foo_bar@mail.foobar.com, _Foo_B=E4r_=28fb@somewhere,
     *  =@unspecified-domain,  =?windows-1258?Q?de=29@mail.foobar.com,
     *  _Jane_Doe@mail.foobar.com, ?=
     * </pre>
     * 
     * @param eword The possibly corrupt encoded word
     * @param charset The charset
     * @return The prepared encoded word which won't cause a {@link ParseException parse error} during decoding
     */
    private static String prepareQEncodedValue(final String eword, final String charset) {
        final int len = eword.length();
        int pos = eword.indexOf('=');
        if (pos == -1) {
            return eword;
        }
        final StringBuilder sb = new StringBuilder(len);
        int prev = 0;
        do {
            final int pos1 = pos + 1;
            final int pos2 = pos + 2;
            if (pos2 < len) {
                final char c1 = eword.charAt(pos1);
                final char c2 = eword.charAt(pos2);
                final int nextPos;
                if (isHex(c1) && isHex(c2)) {
                    final int pos3 = pos + 3;
                    if (pos3 > len) {
                        nextPos = len;
                    } else {
                        nextPos = pos3;
                    }
                    sb.append(eword.substring(prev, nextPos));
                } else {
                    nextPos = pos1;
                    if (ENCODINGS.contains(HeaderName.valueOf(charset))) {
                        sb.append(eword.substring(prev, pos)).append("=3D");
                    } else {
                        sb.append(eword.substring(prev, pos)).append(qencode('=', charset));
                    }
                }
                prev = nextPos;
                pos = nextPos < len ? eword.indexOf('=', nextPos) : -1;
            } else {
                if (prev < len) {
                    sb.append(eword.substring(prev));
                    prev = len;
                }
                pos = -1;
            }
        } while (pos != -1);
        if (prev < len) {
            sb.append(eword.substring(prev));
        }
        return sb.toString();
    }

    private static boolean isHex(final char c) {
        return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
    }

    private static String qencode(final char toEncode, final String charset) {
        if (!Charset.isSupported(charset)) {
            return String.valueOf(toEncode);
        }
        final StringBuilder retval = new StringBuilder(4);
        try {
            final byte[] bytes = String.valueOf(toEncode).getBytes(charset);
            for (int j = 0; j < bytes.length; j++) {
                retval.append('=').append(Integer.toHexString(bytes[j] & 0xFF).toUpperCase(Locale.ENGLISH));
            }
        } catch (final java.io.UnsupportedEncodingException e) {
            // Cannot occur
            LOG.error(e.getMessage(), e);
        }
        return retval.toString();
    }

    /**
     * Get the decoded filename associated with specified mail part.
     * <p>
     * Returns the value of the "filename" parameter from the "Content-Disposition" header field. If its not available, returns the value of
     * the "name" parameter from the "Content-Type" header field. Returns <code>null</code> if both are absent.
     * 
     * @param mailPart The mail part whose filename shall be returned
     * @return The mail part's decoded filename or <code>null</code>.
     */
    public static String getFileName(final MailPart mailPart) {
        // First look-up content-disposition
        String fileName = mailPart.getContentDisposition().getFilenameParameter();
        if (isEmpty(fileName)) {
            // Then look-up content-type
            fileName = mailPart.getContentType().getNameParameter();
        }
        return decodeMultiEncodedHeader(fileName);
    }

    /**
     * Parse the given sequence of addresses into InternetAddress objects by invoking
     * <code>{@link InternetAddress#parse(String, boolean)}</code>. If <code>strict</code> is false, simple email addresses separated by
     * spaces are also allowed. If <code>strict</code> is true, many (but not all) of the RFC822 syntax rules are enforced. In particular,
     * even if <code>strict</code> is true, addresses composed of simple names (with no "@domain" part) are allowed. Such "illegal"
     * addresses are not uncommon in real messages.
     * <p>
     * Non-strict parsing is typically used when parsing a list of mail addresses entered by a human. Strict parsing is typically used when
     * parsing address headers in mail messages.
     * <p>
     * Additionally the personal parts are MIME encoded using default MIME charset.
     * 
     * @param addresslist - comma separated address strings
     * @param strict - <code>true</code> to enforce RFC822 syntax; otherwise <code>false</code>
     * @return An array of <code>InternetAddress</code> objects
     */
    public static InternetAddress[] parseAddressList(final String addresslist, final boolean strict) {
        try {
            return parseAddressList(addresslist, strict, false);
        } catch (final AddressException e) {
            /*
             * Cannot occur
             */
            throw new IllegalStateException("Unexpected exception.", e);
        }
    }

    private static final Pattern SPLITS = Pattern.compile(" *, *");

    private static List<String> splitAddrs(final String addrs) {
        final String[] sa = SPLITS.split(addrs, 0);
        final List<String> ret = new ArrayList<String>(sa.length);
        final StringBuilder tmp = new StringBuilder(24);
        for (final String string : sa) {
            final String trim = string.trim();
            if (trim.charAt(0) == '"') {
                tmp.setLength(0);
                tmp.append(trim).append(", ");
            } else if (trim.indexOf("\" <") >= 0) {
                tmp.append(trim);
                ret.add(tmp.toString());
                tmp.setLength(0);
            } else if (tmp.length() > 0) {
                tmp.append(string).append(", ");
            } else {
                ret.add(string);
            }
        }
        return ret;
    }

    /**
     * Parse the given sequence of addresses into InternetAddress objects by invoking
     * <code>{@link InternetAddress#parse(String, boolean)}</code>. If <code>strict</code> is false, simple email addresses separated by
     * spaces are also allowed. If <code>strict</code> is true, many (but not all) of the RFC822 syntax rules are enforced. In particular,
     * even if <code>strict</code> is true, addresses composed of simple names (with no "@domain" part) are allowed. Such "illegal"
     * addresses are not uncommon in real messages.
     * <p>
     * Non-strict parsing is typically used when parsing a list of mail addresses entered by a human. Strict parsing is typically used when
     * parsing address headers in mail messages.
     * <p>
     * Additionally the personal parts are MIME encoded using default MIME charset.
     * 
     * @param addresslist - comma separated address strings
     * @param strict - <code>true</code> to enforce RFC822 syntax; otherwise <code>false</code>
     * @param failOnError - <code>true</code> to fail if parsing fails; otherwise <code>false</code> to get a plain-text representation
     * @return An array of <code>InternetAddress</code> objects
     * @throws AddressException If parsing fails and <code>failOnError</code> is <code>true</code>
     */
    public static InternetAddress[] parseAddressList(final String addresslist, final boolean strict, final boolean failOnError) throws AddressException {
        if (null == addresslist) {
            return new InternetAddress[0];
        }
        final String al = replaceWithComma(unfold(addresslist));
        InternetAddress[] addrs = null;
        try {
            addrs = QuotedInternetAddress.parse(al, strict);
        } catch (final AddressException e) {
            // Retry with single parse
            final List<String> sAddrs = splitAddrs(al);
            try {
                final List<InternetAddress> addrList = new ArrayList<InternetAddress>(sAddrs.size());
                for (final String sAddr : sAddrs) {
                    final QuotedInternetAddress tmp = new QuotedInternetAddress(sAddr, strict);
                    if (TRACE) {
                        LOG.trace(tmp);
                    }
                    addrList.add(tmp);
                }
                // Hm... single parse did not fail, throw original exception instead
                return addrList.toArray(new InternetAddress[0]);
            } catch (final AddressException e1) {
                if (failOnError) {
                    for (final String sAddr : sAddrs) {
                        final QuotedInternetAddress tmp = new QuotedInternetAddress(sAddr, strict);
                        if (TRACE) {
                            LOG.trace(tmp);
                        }
                    }
                    // Hm... single parse did not fail, throw original exception instead
                    throw e;
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                        new StringBuilder(128).append("Internet addresses could not be properly parsed, ").append(
                            "using plain addresses' string representation instead.").toString(),
                        e);
                }
                addrs = PlainTextAddress.getAddresses(splitAddrs(al).toArray(new String[0]));
            }
        }
        try {
            for (int i = 0; i < addrs.length; i++) {
                addrs[i].setPersonal(addrs[i].getPersonal(), MailProperties.getInstance().getDefaultMimeCharset());
            }
        } catch (final UnsupportedEncodingException e) {
            /*
             * Cannot occur since default charset is checked on global mail configuration initialization
             */
            LOG.error(e.getMessage(), e);
        }
        return addrs;
    }

    private static final Pattern PATTERN_REPLACE = Pattern.compile("([^\"]\\S+?)(\\s*)([;])(\\s*)");

    private static String replaceWithComma(final String addressList) {
        final Matcher m = PATTERN_REPLACE.matcher(addressList);
        if (m.find()) {
            final StringBuilder sb = new StringBuilder(addressList.length());
            int lastMatch = 0;
            do {
                sb.append(addressList.substring(lastMatch, m.start()));
                sb.append(m.group(1)).append(m.group(2)).append(',').append(m.group(4));
                lastMatch = m.end();
            } while (m.find());
            sb.append(addressList.substring(lastMatch));
            return sb.toString();
        }
        return addressList;
    }

    // private static final Pattern PAT_QUOTED = Pattern.compile("(^\")([^\"]+?)(\"$)");

    // private static final Pattern PAT_QUOTABLE_CHAR = Pattern.compile("[.,:;<>\"]");

    private final static String RFC822 = "()<>@,;:\\\".[]";

    /**
     * Quotes given personal part of an Internet address according to RFC 822 syntax if needed; otherwise the personal is returned
     * unchanged.
     * <p>
     * This method guarantees that the resulting string can be used to build an Internet address according to RFC 822 syntax so that the
     * <code>{@link InternetAddress#parse(String)}</code> constructor won't throw an instance of <code>{@link AddressException}</code>.
     * 
     * <pre>
     * final String quotedPersonal = quotePersonal(&quot;Doe, Jane&quot;);
     * 
     * final String buildAddr = quotedPersonal + &quot; &lt;someone@somewhere.com&gt;&quot;;
     * System.out.println(buildAddr);
     * // Plain Address: &quot;=?UTF-8?Q?Doe=2C_Jan=C3=A9?=&quot; &lt;someone@somewhere.com&gt;
     * 
     * final InternetAddress ia = new InternetAddress(buildAddr);
     * System.out.println(ia.toUnicodeString());
     * // Unicode Address: &quot;Doe, Jane&quot; &lt;someone@somewhere.com&gt;
     * </pre>
     * 
     * @param personal The personal's string representation
     * @return The properly quoted personal for building an Internet address according to RFC 822 syntax
     */
    public static String quotePersonal(final String personal) {
        return quotePhrase(personal, true);
    }

    /**
     * Quotes given phrase if needed.
     * 
     * @param phrase The phrase
     * @param encode <code>true</code> to encode phrase according to RFC 822 syntax if needed; otherwise <code>false</code>
     * @return The quoted phrase
     */
    public static String quotePhrase(final String phrase, final boolean encode) {
        if (null == phrase || phrase.length() == 0) {
            return phrase;
        }
        final char[] chars = phrase.toCharArray();
        final int len = chars.length;
        if ('"' == chars[0] && '"' == chars[len - 1]) {
            /*
             * Already quoted
             */
            return phrase;
        }
        boolean needQuoting = false;
        for (int i = 0; !needQuoting && i < len; i++) {
            final char c = chars[i];
            needQuoting = (c == '"' || c == '\\' || (c < 32 && c != '\r' && c != '\n' && c != '\t') || c >= 127 || RFC822.indexOf(c) >= 0);
        }
        try {
            if (!needQuoting) {
                return encode ? MimeUtility.encodeWord(phrase) : phrase;
            }
            final String replaced = phrase.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\\\"");
            return new StringBuilder(len + 2).append('"').append(encode ? MimeUtility.encodeWord(replaced) : replaced).append('"').toString();
        } catch (final UnsupportedEncodingException e) {
            LOG.error("Unsupported encoding in a message detected and monitored: \"" + e.getMessage() + '"', e);
            mailInterfaceMonitor.addUnsupportedEncodingExceptions(e.getMessage());
            return phrase;
        }
    }

    private static final int USED_CT = 14;

    /**
     * Folds specified <code>Content-Type</code> value.
     * 
     * @param contentDisposition The <code>Content-Type</code> value
     * @return The folded <code>Content-Type</code> value
     */
    public static String foldContentType(final String contentType) {
        return fold(USED_CT, contentType);
    }

    private static final int USED_CD = 21;

    /**
     * Folds specified <code>Content-Disposition</code> value.
     * 
     * @param contentDisposition The <code>Content-Disposition</code> value
     * @return The folded <code>Content-Disposition</code> value
     */
    public static String foldContentDisposition(final String contentDisposition) {
        return fold(USED_CD, contentDisposition);
    }

    /**
     * Folds a string at linear whitespace so that each line is no longer than 76 characters, if possible. If there are more than 76
     * non-whitespace characters consecutively, the string is folded at the first whitespace after that sequence. The parameter
     * <tt>used</tt> indicates how many characters have been used in the current line; it is usually the length of the header name.
     * <p>
     * Note that line breaks in the string aren't escaped; they probably should be.
     * 
     * @param used The characters used in line so far
     * @param foldMe The string to fold
     * @return The folded string
     */
    public static String fold(final int used, final String foldMe) {
        int end;
        char c;
        /*
         * Strip trailing spaces and newlines
         */
        for (end = foldMe.length() - 1; end >= 0; end--) {
            c = foldMe.charAt(end);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                break;
            }
        }
        String s;
        if (end != foldMe.length() - 1) {
            s = foldMe.substring(0, end + 1);
        } else {
            s = foldMe;
        }
        /*
         * Check if the string fits now
         */
        if (used + s.length() <= 76) {
            return s;
        }
        /*
         * Fold the string
         */
        final StringBuilder sb = new StringBuilder(s.length() + 4);
        char lastc = 0;
        int usedChars = used;
        while (usedChars + s.length() > 76) {
            int lastspace = -1;
            for (int i = 0; i < s.length(); i++) {
                if (lastspace != -1 && usedChars + i > 76) {
                    break;
                }
                c = s.charAt(i);
                if ((c == ' ' || c == '\t') && !(lastc == ' ' || lastc == '\t')) {
                    lastspace = i;
                }
                lastc = c;
            }
            if (lastspace == -1) {
                /*
                 * No space, use the whole thing
                 */
                sb.append(s);
                s = "";
                usedChars = 0;
                break;
            }
            sb.append(s.substring(0, lastspace));
            sb.append("\r\n");
            lastc = s.charAt(lastspace);
            sb.append(lastc);
            s = s.substring(lastspace + 1);
            usedChars = 1;
        }
        sb.append(s);
        return sb.toString();
    }

    /**
     * Unfolds a folded header. Any line breaks that aren't escaped and are followed by whitespace are removed.
     * 
     * @param headerLine The header line to unfold
     * @return The unfolded string
     */
    public static String unfold(final String headerLine) {
        if (null == headerLine) {
            return null;
        }
        StringBuilder sb = null;
        int i;
        if ((i = headerLine.indexOf('\r')) >= 0 || (i = headerLine.indexOf('\n')) >= 0) {
            /*-
             * Check folded encoded-words as per RFC 2047:
             *
             * An 'encoded-word' may not be more than 75 characters long, including
             * 'charset', 'encoding', 'encoded-text', and delimiters.  If it is
             * desirable to encode more text than will fit in an 'encoded-word' of
             * 75 characters, multiple 'encoded-word's (separated by CRLF SPACE) may
             * be used.
             *
             * In this case the SPACE character is not part of the header and should
             * be discarded.
             */
            String s;
            if (headerLine.indexOf("=?") < 0) {
                s = headerLine;
            } else {
                s = unfoldEncodedWords(headerLine);
                if ((i = s.indexOf('\r')) < 0 && (i = s.indexOf('\n')) < 0) {
                    return s;
                }
            }
            do {
                final int start = i;
                final int len = s.length();
                i++; // skip CR or NL
                if ((i < len) && (s.charAt(i - 1) == '\r') && (s.charAt(i) == '\n')) {
                    i++; // skip LF
                }
                if (start == 0 || s.charAt(start - 1) != '\\') {
                    char c;
                    /*
                     * If next line starts with whitespace, skip all of it
                     */
                    if ((i < len) && (((c = s.charAt(i)) == ' ') || (c == '\t'))) {
                        i++; // skip whitespace
                        while ((i < len) && (((c = s.charAt(i)) == ' ') || (c == '\t'))) {
                            i++;
                        }
                        if (sb == null) {
                            sb = new StringBuilder(s.length());
                        }
                        if (start != 0) {
                            sb.append(s.substring(0, start));
                            sb.append(' ');
                        }
                        s = s.substring(i);
                    } else {
                        /*
                         * It's not a continuation line, just leave it in
                         */
                        if (sb == null) {
                            sb = new StringBuilder(s.length());
                        }
                        sb.append(s.substring(0, i));
                        s = s.substring(i);
                    }
                } else {
                    /*
                     * There's a backslash at "start - 1", strip it out, but leave in the line break
                     */
                    if (sb == null) {
                        sb = new StringBuilder(s.length());
                    }
                    sb.append(s.substring(0, start - 1));
                    sb.append(s.substring(start, i));
                    s = s.substring(i);
                }
            } while ((i = s.indexOf('\r')) >= 0 || (i = s.indexOf('\n')) >= 0);
            sb.append(s);
            return sb.toString();
        }
        return headerLine;
    }

    private static final Pattern PAT_ENC_WORDS;

    static {
        final String regex = "(\\?=)" + "(?:\r?\n(?:\t| +))" + "(=\\?)";
        PAT_ENC_WORDS = Pattern.compile(regex);
        //final String regexEncodedWord = "(=\\?\\S+?\\?\\S+?\\?.+?\\?=)";
        //PAT_ENC_WORDS = Pattern.compile(regexEncodedWord + "(?:\r?\n(?:\t| +))" + regexEncodedWord);
    }

    /**
     * Unfolds encoded-words as per RFC 2047. When unfolding a non-encoded-word the preceding space character should not be stripped out,
     * but should when unfolding encoded-words.
     * <p>
     * &quot;...<br>
     * An 'encoded-word' may not be more than 75 characters long, including 'charset', 'encoding', 'encoded-text', and delimiters. If it is
     * desirable to encode more text than will fit in an 'encoded-word' of 75 characters, multiple 'encoded-word's (separated by CRLF SPACE)
     * may be used.&quot;
     * <p>
     * 
     * <pre>
     * Subject: =?UTF-8?Q?Kombatibilit=C3=A4t?=\r\n =?UTF-8?Q?sliste?=
     * </pre>
     * 
     * Should be unfolded to:
     * 
     * <pre>
     * Subject: =?UTF-8?Q?Kombatibilit=C3=A4t?==?UTF-8?Q?sliste?=
     * </pre>
     * 
     * @param encodedWords The possibly folded encoded-words
     * @return The unfolded encoded-words
     */
    private static String unfoldEncodedWords(final String encodedWords) {
        return PAT_ENC_WORDS.matcher(encodedWords).replaceAll("$1$2");
    }

    private static final int BUFSIZE = 8192; // 8K

    /**
     * Gets the matching header out of RFC 822 data input stream.
     * 
     * @param headerName The header name
     * @param inputStream The input stream
     * @param closeStream <code>true</code> to close the stream on finish; otherwise <code>false</code>
     * @return The value of first appeared matching header
     * @throws IOException If reading input stream fails
     */
    public static String extractHeader(final String headerName, final InputStream inputStream, final boolean closeStream) throws IOException {
        boolean close = closeStream;
        try {
            /*
             * Gather bytes until empty line, EOF or matching header found
             */
            final UnsynchronizedByteArrayOutputStream buffer = new UnsynchronizedByteArrayOutputStream(BUFSIZE);
            int start = 0;
            int i = -1;
            boolean firstColonFound = false;
            boolean found = false;
            while ((i = inputStream.read()) != -1) {
                int count = 0;
                while ((i == '\r') || (i == '\n')) {
                    if (!found) {
                        buffer.write(i);
                    }
                    if ((i == '\n')) {
                        if (found) {
                            i = inputStream.read();
                            if ((i != ' ') && (i != '\t')) {
                                /*
                                 * All read
                                 */
                                return new String(buffer.toByteArray(), com.openexchange.java.Charsets.US_ASCII);

                            }
                            /*
                             * Write previously ignored CRLF
                             */
                            buffer.write('\r');
                            buffer.write('\n');
                            /*
                             * Continue collecting header value
                             */
                            buffer.write(i);
                        }
                        if (++count >= 2) {
                            /*
                             * End of headers
                             */
                            return null;
                        }
                        i = inputStream.read();
                        if ((i != ' ') && (i != '\t')) {
                            /*
                             * No header continuation; start of a new header
                             */
                            start = buffer.size();
                            firstColonFound = false;
                        }
                        buffer.write(i);
                    }
                    i = inputStream.read();
                }
                buffer.write(i);
                if (!firstColonFound && (i == ':')) {
                    /*
                     * Found the first delimiting colon in header line
                     */
                    firstColonFound = true;
                    if ((new String(buffer.toByteArray(start, buffer.size() - start - 1), com.openexchange.java.Charsets.US_ASCII).equalsIgnoreCase(headerName))) {
                        /*
                         * Matching header
                         */
                        buffer.reset();
                        found = true;
                    }
                }
            }
            return null;
        } catch (final IOException e) {
            // Close on error
            close = true;
            throw e;
        } finally {
            if (close) {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Writes specified part's headers to given output stream.
     * 
     * @param p The part
     * @param os The output stream
     * @throws OXException If an I/O error occurs
     */
    public static void writeHeaders(final MailPart p, final OutputStream os) throws OXException {
        if (p instanceof MimeMailMessage) {
            writeHeaders(((MimeMailMessage) p).getMimeMessage(), os);
            return;
        }
        if (p instanceof MimeMailPart) {
            writeHeaders(((MimeMailPart) p).getPart(), os);
            return;
        }
        try {
            final LineOutputStream los;
            if (os instanceof LineOutputStream) {
                los = (LineOutputStream) os;
            } else {
                los = new LineOutputStream(os);
            }
            /*
             * Write headers
             */
            final StringBuilder sb = new StringBuilder(256);
            for (final Iterator<Entry<String, String>> it = p.getHeadersIterator(); it.hasNext();) {
                final Entry<String, String> entry = it.next();
                sb.setLength(0);
                sb.append(entry.getKey()).append(": ");
                sb.append(fold(sb.length(), entry.getValue()));
                los.writeln(sb);
            }
            /*
             * The CRLF separator between header and content
             */
            los.writeln();
            os.flush();
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Writes specified part's headers to given output stream.
     * 
     * @param p The part
     * @param os The output stream
     * @throws OXException If an error occurs
     */
    public static void writeHeaders(final Part p, final OutputStream os) throws OXException {
        if (p instanceof MimePart) {
            writeHeaders((MimePart) p, os);
            return;
        }
        try {
            final LineOutputStream los;
            if (os instanceof LineOutputStream) {
                los = (LineOutputStream) os;
            } else {
                los = new LineOutputStream(os);
            }
            /*
             * Write headers
             */
            @SuppressWarnings("unchecked")
            final
            Enumeration<Header> headers = p.getAllHeaders();
            final StringBuilder sb = new StringBuilder(256);
            while (headers.hasMoreElements()) {
                final Header header = headers.nextElement();
                sb.setLength(0);
                sb.append(header.getName()).append(": ");
                sb.append(fold(sb.length(), header.getValue()));
                los.writeln(sb);
            }
            /*
             * The CRLF separator between header and content
             */
            los.writeln();
            os.flush();
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Writes specified part's headers to given output stream.
     * 
     * @param p The part
     * @param os The output stream
     * @throws OXException If an error occurs
     */
    public static void writeHeaders(final MimePart p, final OutputStream os) throws OXException {
        try {
            final LineOutputStream los;
            if (os instanceof LineOutputStream) {
                los = (LineOutputStream) os;
            } else {
                los = new LineOutputStream(os);
            }
            /*
             * Write headers
             */
            for (@SuppressWarnings("unchecked") final Enumeration<String> hdrLines = p.getNonMatchingHeaderLines(null); hdrLines.hasMoreElements();) {
                los.writeln(hdrLines.nextElement());
            }
            /*
             * The CRLF separator between header and content
             */
            los.writeln();
            os.flush();
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        } catch (final IOException e) {
            throw MailExceptionCode.IO_ERROR.create(e, e.getMessage());
        }
    }

    private static final class LineOutputStream extends FilterOutputStream {

        private static final byte[] newline = { (byte) '\r', (byte) '\n' };

        protected LineOutputStream(final OutputStream out) {
            super(out);
        }

        protected void writeln(final CharSequence s) throws IOException {
            out.write(getBytes(s));
            out.write(newline);
        }

        protected void writeln() throws IOException {
            out.write(newline);
        }

        private static byte[] getBytes(final CharSequence s) {
            if (null == s) {
                return new byte[0];
            }
            final int len = s.length();
            final byte[] bytes = new byte[len];
            for (int i = 0; i < len; i++) {
                bytes[i] = (byte) s.charAt(i);
            }
            return bytes;
        }
    }

    private static final String X_ORIGINAL_HEADERS = "x-original-headers";

    /**
     * Drops invalid "X-Original-Headers" header from RFC822 source file.
     * 
     * @param file The file
     * @param newTempFile The new file (to write cleansed content to)
     * @return The resulting file
     */
    public static File dropInvalidHeaders(final File file, final File newTempFile) {
        InputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            out = new BufferedOutputStream(new FileOutputStream(newTempFile));
            {
                @SuppressWarnings("resource")
				final LineReaderInputStream instream = new LineReaderInputStreamAdaptor(in, -1);
                int lineCount = 0;
                final ByteArrayBuffer linebuf = new ByteArrayBuffer(64);
                final FieldBuilder fieldBuilder = new DefaultFieldBuilder(-1);
                boolean endOfHeader = false;
                while (!endOfHeader) {
                    fieldBuilder.reset();
                    for (;;) {
                        // If there's still data stuck in the line buffer
                        // copy it to the field buffer
                        int len = linebuf.length();
                        if (len > 0) {
                            fieldBuilder.append(linebuf);
                        }
                        linebuf.clear();
                        if (instream.readLine(linebuf) == -1) {
                            endOfHeader = true;
                            break;
                        }
                        len = linebuf.length();
                        if (len > 0 && linebuf.byteAt(len - 1) == '\n') {
                            len--;
                        }
                        if (len > 0 && linebuf.byteAt(len - 1) == '\r') {
                            len--;
                        }
                        if (len == 0) {
                            // empty line detected
                            endOfHeader = true;
                            break;
                        }
                        lineCount++;
                        if (lineCount > 1) {
                            final int ch = linebuf.byteAt(0);
                            if (ch != CharsetUtil.SP && ch != CharsetUtil.HT) {
                                // new header detected
                                break;
                            }
                        }
                    }
                    final RawField rawfield = fieldBuilder.build();
                    if (rawfield != null && !X_ORIGINAL_HEADERS.equalsIgnoreCase(rawfield.getName())) {
                        final ByteArrayBuffer buffer = fieldBuilder.getRaw();
                        out.write(buffer.buffer(), 0, buffer.length());
                    }
                } // End of Headers
            }
            // Write rest
            final int l = 2048;
            final byte[] buf = new byte[l];
            for (int read; (read = in.read(buf, 0, l)) > 0;) {
                out.write(buf, 0, read);
            }
            out.flush();
            return newTempFile;
        } catch (final Exception e) {
            return file;
        } finally {
            Streams.close(in, out);
        }
    }

}

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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.ajax.requesthandler.converters.preview;

import static com.openexchange.java.Charsets.toAsciiBytes;
import static com.openexchange.java.Charsets.toAsciiString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.binary.Base64;
import com.openexchange.ajax.container.IFileHolder;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.ajax.requesthandler.ResultConverter;
import com.openexchange.ajax.requesthandler.cache.CachedResource;
import com.openexchange.ajax.requesthandler.cache.ResourceCache;
import com.openexchange.ajax.requesthandler.cache.ResourceCaches;
import com.openexchange.conversion.DataProperties;
import com.openexchange.conversion.SimpleData;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.mail.mime.MimeTypes;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.utils.DisplayMode;
import com.openexchange.preview.ContentTypeChecker;
import com.openexchange.preview.PreviewDocument;
import com.openexchange.preview.PreviewOutput;
import com.openexchange.preview.PreviewService;
import com.openexchange.preview.cache.CachedPreviewDocument;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.Task;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link AbstractPreviewResultConverter}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractPreviewResultConverter implements ResultConverter {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractPreviewResultConverter.class);

    private static final Charset UTF8 = Charsets.UTF_8;
    private static final byte[] DELIM = new byte[] { '\r', '\n' };

    /**
     * The <code>"view"</code> parameter.
     */
    protected static final String PARAMETER_VIEW = "view";

    /**
     * The <code>"edit"</code> parameter.
     */
    protected static final String PARAMETER_EDIT = "edit";

    /**
     * Initializes a new {@link AbstractPreviewResultConverter}.
     */
    protected AbstractPreviewResultConverter() {
        super();
    }

    @Override
    public String getInputFormat() {
        return "file";
    }

    @Override
    public void convert(final AJAXRequestData requestData, final AJAXRequestResult result, final ServerSession session, final Converter converter) throws OXException {
        IFileHolder fileHolder = null;
        try {
            // Check cache first
            final ResourceCache resourceCache;
            {
                final ResourceCache tmp = ResourceCaches.getResourceCache();
                resourceCache = null == tmp ? null : (tmp.isEnabledFor(session.getContextId(), session.getUserId()) ? tmp : null);
            }
            final String eTag = requestData.getETag();
            final boolean isValidEtag = !Strings.isEmpty(eTag);
            if (null != resourceCache && isValidEtag && AJAXRequestDataTools.parseBoolParameter("cache", requestData, true)) {
                final String cacheKey = ResourceCaches.generatePreviewCacheKey(eTag, requestData);
                final CachedResource cachedPreview = resourceCache.get(cacheKey, 0, session.getContextId());
                if (null != cachedPreview) {
                    /*
                     * Get content according to output format
                     */
                    final byte[] bytes;
                    {
                        final InputStream in = cachedPreview.getInputStream();
                        if (null == in) {
                            bytes = cachedPreview.getBytes();
                        } else {
                            bytes = Streams.stream2bytes(in);
                        }
                    }
                    /*
                     * Convert meta data to a map
                     */
                    final Map<String, String> map = new HashMap<String, String>(4);
                    map.put("resourcename", cachedPreview.getFileName());
                    map.put("content-type", cachedPreview.getFileType());
                    // Decode contents
                    final List<String> contents;
                    {
                        final int[] computedFailure = computeFailure(DELIM);
                        int prev = 0;
                        int pos;
                        if ((pos = indexOf(bytes, DELIM, prev, computedFailure)) >= 0) {
                            // Multiple contents
                            contents = new LinkedList<String>();
                            final ByteArrayOutputStream baos = new ByteArrayOutputStream(8192 << 1);
                            do {
                                baos.reset();
                                prev = pos + DELIM.length;
                                pos = indexOf(bytes, DELIM, prev, computedFailure);
                                if (pos >= 0) {
                                    baos.write(bytes, prev, pos);
                                } else {
                                    baos.write(bytes, prev, bytes.length);
                                }
                                contents.add(new String(Base64.decodeBase64(toAsciiBytes(toAsciiString(baos.toByteArray()))), UTF8));
                            } while (pos >= 0);
                        } else {
                            // Single content
                            contents = Collections.singletonList(new String(Base64.decodeBase64(toAsciiBytes(toAsciiString(bytes))), UTF8));
                        }
                    }
                    // Set preview document
                    result.setResultObject(new CachedPreviewDocument(contents, map), getOutputFormat());
                    return;
                }
            }

            // No cached preview available
            {
                final Object resultObject = result.getResultObject();
                if (!(resultObject instanceof IFileHolder)) {
                    throw AjaxExceptionCodes.UNEXPECTED_RESULT.create(IFileHolder.class.getSimpleName(), null == resultObject ? "null" : resultObject.getClass().getSimpleName());
                }
                fileHolder = (IFileHolder) resultObject;
            }

            // Check file holder's content
            if (0 == fileHolder.getLength()) {
                throw AjaxExceptionCodes.UNEXPECTED_ERROR.create("File holder has not content, hence no preview can be generated.");
            }

            // Obtain preview document
            final PreviewDocument previewDocument;
            {
                InputStream stream = fileHolder.getStream();
                final Ref<InputStream> ref = new Ref<InputStream>();
                if (streamIsEof(stream, null)) {
                    Streams.close(stream, fileHolder);
                    throw AjaxExceptionCodes.UNEXPECTED_ERROR.create("File holder has not content, hence no preview can be generated.");
                }
                stream = ref.getValue();

                final PreviewService previewService = ServerServiceRegistry.getInstance().getService(PreviewService.class);

                final DataProperties dataProperties = new DataProperties(4);
                dataProperties.put(DataProperties.PROPERTY_CONTENT_TYPE, getContentType(fileHolder, previewService instanceof ContentTypeChecker ? (ContentTypeChecker) previewService : null));
                dataProperties.put(DataProperties.PROPERTY_DISPOSITION, fileHolder.getDisposition());
                dataProperties.put(DataProperties.PROPERTY_NAME, fileHolder.getName());
                dataProperties.put(DataProperties.PROPERTY_SIZE, Long.toString(fileHolder.getLength()));

                int pages = -1;
                if (requestData.containsParameter("pages")) {
                    pages = requestData.getIntParameter("pages");
                }
                previewDocument = previewService.getPreviewFor(new SimpleData<InputStream>(stream, dataProperties), getOutput(), session, pages);
                // Put to cache
                if (null != resourceCache && isValidEtag && AJAXRequestDataTools.parseBoolParameter("cache", requestData, true)) {
                    final List<String> content = previewDocument.getContent();
                    if (null != content) {
                        final int size = content.size();
                        if (size > 0) {
                            final String cacheKey = ResourceCaches.generatePreviewCacheKey(eTag, requestData);
                            final byte[] bytes;
                            if (1 == content.size()) {
                                bytes = toAsciiBytes(toAsciiString(Base64.encodeBase64(content.get(0).getBytes(UTF8))));
                            } else {
                                final ByteArrayOutputStream baos = Streams.newByteArrayOutputStream(8192 << 1);
                                baos.write(toAsciiBytes(toAsciiString(Base64.encodeBase64(content.get(0).getBytes(UTF8)))));
                                final byte[] delim = DELIM;
                                for (int i = 1; i < size; i++) {
                                    baos.write(delim);
                                    baos.write(toAsciiBytes(toAsciiString(Base64.encodeBase64(content.get(i).getBytes(UTF8)))));
                                }
                                bytes = baos.toByteArray();
                            }
                            final String fileName = fileHolder.getName();
                            final String fileType = fileHolder.getContentType();
                            // Specify task
                            final Task<Void> task = new AbstractTask<Void>() {
                                @Override
                                public Void call() {
                                    try {
                                        final CachedResource preview = new CachedResource(bytes, fileName, fileType, bytes.length);
                                        resourceCache.save(cacheKey, preview, 0, session.getContextId());
                                    } catch (OXException e) {
                                        LOG.warn("Could not cache preview.", e);
                                    }

                                    return null;
                                }
                            };
                            // Acquire thread pool service
                            final ThreadPoolService threadPool = ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
                            if (null == threadPool) {
                                final Thread thread = Thread.currentThread();
                                boolean ran = false;
                                task.beforeExecute(thread);
                                try {
                                    task.call();
                                    ran = true;
                                    task.afterExecute(null);
                                } catch (final Exception ex) {
                                    if (!ran) {
                                        task.afterExecute(ex);
                                    }
                                    // Else the exception occurred within
                                    // afterExecute itself in which case we don't
                                    // want to call it again.
                                    throw (ex instanceof OXException ? (OXException) ex : AjaxExceptionCodes.UNEXPECTED_ERROR.create(
                                        ex,
                                        ex.getMessage()));
                                }
                            } else {
                                threadPool.submit(task);
                            }
                        }
                    }
                }
            }
            if (requestData.getIntParameter("save") == 1) {
                // TODO:
//                /*-
//                 * Preview document should be saved.
//                 * We set the request format to file and return a FileHolder
//                 * containing the preview document.
//                 */
//                requestData.setFormat("file");
//                final byte[] documentBytes = previewDocument.getContent().getBytes();
//                final InputStream is = new ByteArrayInputStream(documentBytes);
//                final String contentType = previewDocument.getMetaData().get("content-type");
//                final String fileName = previewDocument.getMetaData().get("resourcename");
//                final FileHolder responseFileHolder = new FileHolder(is, documentBytes.length, contentType, fileName);
//                result.setResultObject(responseFileHolder, "file");
            } else {
                result.setResultObject(previewDocument, getOutputFormat());
            }
        } catch (final IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Streams.close(fileHolder);
        }
    }

    private static final Set<String> BOOLS = new HashSet<String>(Arrays.asList("true", "yes", "y", "on", "1"));

    /**
     * Parses specified value to a <code>boolean</code>:<br>
     * <code>true</code> if given value is not <code>null</code> and equals ignore-case to one of the values "true", "yes", "y", "on", or "1".
     *
     * @param value The value to parse
     * @return The parsed <code>boolean</code> value
     */
    protected static boolean parseBool(final String value) {
        if (null == value) {
            return false;
        }
        return BOOLS.contains(value.trim().toLowerCase(Locale.US));
    }

    private static final Set<String> INVALIDS = MimeTypes.INVALIDS;

    /**
     * Gets the checked MIME type from given file.
     *
     * @param fileHolder The file
     * @param checker The optional checker
     * @return The checked MIME type
     */
    protected static String getContentType(final IFileHolder fileHolder, final ContentTypeChecker checker) {
        String contentType = fileHolder.getContentType();
        if (Strings.isEmpty(contentType)) {
            // Determine Content-Type by file name
            return MimeType2ExtMap.getContentType(fileHolder.getName());
        }
        // Cut to base type & sanitize
        contentType = sanitizeContentType(getLowerCaseBaseType(contentType));
        contentType = MimeTypes.checkedMimeType(contentType, fileHolder.getName(), INVALIDS);
        if (INVALIDS.contains(contentType) || (null != checker && !checker.isValid(contentType))) {
            // Determine Content-Type by file name
            contentType = MimeType2ExtMap.getContentType(fileHolder.getName());
        }
        return contentType == null ? "application/octet-stream" : contentType;
    }

    private static String sanitizeContentType(final String contentType) {
        if (null == contentType) {
            return null;
        }
        try {
            return new ContentType(contentType).getBaseType();
        } catch (final OXException e) {
            return contentType;
        }
    }

    private static String getLowerCaseBaseType(final String contentType) {
        if (null == contentType) {
            return null;
        }
        final int pos = contentType.indexOf(';');
        return Strings.toLowerCase(pos > 0 ? contentType.substring(0, pos) : contentType).trim();
    }

    private static final String VIEW_RAW = "raw";

    private static final String VIEW_TEXT = "text";

    private static final String VIEW_TEXT_NO_HTML_ATTACHMENT = "textNoHtmlAttach";

    private static final String VIEW_HTML = "html";

    private static final String VIEW_HTML_BLOCKED_IMAGES = "noimg";

    /**
     * Detects display mode dependent on passed arguments.
     *
     * @param modifyable Whether content is intended being modified by client
     * @param view The view parameter
     * @param usm The user settings
     * @return The display mode
     */
    protected static DisplayMode detectDisplayMode(final boolean modifyable, final String view, final UserSettingMail usm) {
        if (null == view) {
            return modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
        }
        final DisplayMode displayMode;
        if (VIEW_RAW.equals(view)) {
            displayMode = DisplayMode.RAW;
        } else if (VIEW_TEXT_NO_HTML_ATTACHMENT.equals(view)) {
            usm.setDisplayHtmlInlineContent(false);
            usm.setSuppressHTMLAlternativePart(true);
            displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
        } else if (VIEW_TEXT.equals(view)) {
            usm.setDisplayHtmlInlineContent(false);
            displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
        } else if (VIEW_HTML.equals(view)) {
            usm.setDisplayHtmlInlineContent(true);
            usm.setAllowHTMLImages(true);
            displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
        } else if (VIEW_HTML_BLOCKED_IMAGES.equals(view)) {
            usm.setDisplayHtmlInlineContent(true);
            usm.setAllowHTMLImages(false);
            displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
        } else {
            LOG.warn("Unknown value in parameter {}: {}. Using user's mail settings as fallback.", PARAMETER_VIEW, view);
            displayMode = modifyable ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
        }
        return displayMode;
    }

    /**
     * Gets the desired output format.
     *
     * @return The output format
     */
    public abstract PreviewOutput getOutput();

    /**
     * Finds the first occurrence of the pattern in the text.
     */
    private int indexOf(final byte[] data, final byte[] pattern, final int[] computeFailure) {
        return indexOf(data, pattern, 0, computeFailure);
    }

    /**
     * Finds the first occurrence of the pattern in the text.
     */
    private int indexOf(final byte[] data, final byte[] pattern, final int fromIndex, final int[] computedFailure) {
        final int[] failure = null == computedFailure ? computeFailure(pattern) : computedFailure;
        int j = 0;
        final int dLen = data.length;
        if (dLen == 0) {
            return -1;
        }
        final int pLen = pattern.length;
        for (int i = fromIndex; i < dLen; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) {
                j++;
            }
            if (j == pLen) {
                return i - pLen + 1;
            }
        }
        return -1;
    }

    /**
     * Computes the failure function using a boot-strapping process, where the pattern is matched against itself.
     */
    private int[] computeFailure(final byte[] pattern) {
        final int length = pattern.length;
        final int[] failure = new int[length];
        int j = 0;
        for (int i = 1; i < length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }

    /**
     * Checks if passed stream signals EOF.
     *
     * @param in The stream to check
     * @param ref The stream reference
     * @return <code>true</code> if passed stream signals EOF; otherwise <code>false</code>
     * @throws IOException If an I/O error occurs
     */
    protected static boolean streamIsEof(final InputStream in, final Ref<InputStream> ref) throws IOException {
        if (null == in) {
            return true;
        }
        final PushbackInputStream pin = Streams.pushbackInputStreamFor(in);
        final int read = pin.read();
        if (read < 0) {
            return true;
        }
        pin.unread(read);
        ref.setValue(pin);
        return false;
    }

    /** Simple reference class */
    protected static final class Ref<V> {

        private V value;

        Ref() {
            super();
            this.value = null;
        }

        /**
         * Gets the value
         *
         * @return The value
         */
        V getValue() {
            return value;
        }

        /**
         * Sets the value
         *
         * @param value The value to set
         */
        Ref<V> setValue(final V value) {
            this.value = value;
            return this;
        }
    }

}

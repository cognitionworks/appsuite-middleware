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

package com.openexchange.snippet;

import static com.openexchange.java.Strings.isEmpty;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.openexchange.config.cascade.ConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.mail.mime.utils.ImageMatcher;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.openexchange.session.Session;
import com.openexchange.snippet.DefaultAttachment.InputStreamProvider;
import com.openexchange.snippet.internal.Services;

/**
 * {@link SnippetProcessor}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class SnippetProcessor {

    private static class InputStreamProviderImpl implements InputStreamProvider {

        private final ManagedFile managedFile;

        /**
         * Initializes a new {@link InputStreamProviderImpl}.
         *
         * @param mf
         */
        protected InputStreamProviderImpl(ManagedFile managedFile) {
            super();
            this.managedFile = managedFile;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                return managedFile.getInputStream();
            } catch (OXException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                throw new IOException(e);
            }
        }
    }

    private final Session session;
    private final Pattern pattern;

    /**
     * Initializes a new {@link SnippetProcessor}.
     *
     * @param session
     * @param ctx
     */
    public SnippetProcessor(Session session) {
        super();
        this.session = session;
        pattern = Pattern.compile("(?i)src=\"[^\"]*\"");
    }

    /**
     * Process the images in the snippet, extracts them and convert them to attachments
     *
     * @param snippet
     * @throws OXException
     */
    public void processImages(final Snippet snippet) throws OXException {
        String content = snippet.getContent();
        if (isEmpty(content)) {
            return;
        }

        ConfigViewFactory configViewFactory = Services.getService(ConfigViewFactory.class);
        ConfigView configView = configViewFactory.getView(session.getUserId(), session.getContextId());

        ConfigProperty<Integer> maxImageLimitConf = configView.property("com.openexchange.mail.signature.maxImageLimit", Integer.class);

        final Integer maxImageLimit;
        if (maxImageLimitConf.isDefined()) {
            maxImageLimit = maxImageLimitConf.get();
        } else {
            // Defaults to 3 images
            maxImageLimit = 3;
        }

        final long maxImageSize;
        {
            ConfigProperty<Double> misConf = configView.property("com.openexchange.mail.signature.maxImageSize", Double.class);
            final double mis;
            if (misConf.isDefined()) {
                mis = misConf.get();
            } else {
                // Defaults to 1 MB
                mis = (1d);
            }
            maxImageSize = (long) (Math.pow(1024, 2) * mis);
        }

        Map<String, String> imageTags = new HashMap<String, String>(maxImageLimit);

        final ManagedFileManagement mfm = Services.getService(ManagedFileManagement.class);
        final ImageMatcher m = ImageMatcher.matcher(content);
        int count = 0;
        while (m.find()) {
            final String imageTag = m.group();
            if (MimeMessageUtility.isValidImageUri(imageTag)) {
                if (!imageTag.contains("picture?uid")) {
                    final String id = m.getManagedFileId();
                    imageTags.put(id, imageTag);
                }
                count++;
            }
        }

        if (count > maxImageLimit) {
            throw SnippetExceptionCodes.MAXIMUM_IMAGES_COUNT.create(maxImageLimit);
        }

        for (String id : imageTags.keySet()) {
            final ManagedFile mf = mfm.getByID(id);

            if (mf.getSize() > maxImageSize) {
                throw SnippetExceptionCodes.MAXIMUM_IMAGE_SIZE.create(maxImageSize);
            }

            DefaultAttachment att = new DefaultAttachment();
            att.setContentDisposition(mf.getContentDisposition());
            att.setContentType(mf.getContentType());
            att.setId(mf.getID());
            att.setSize(mf.getSize());
            att.setStreamProvider(new InputStreamProviderImpl(mf));
            att.setFilename(mf.getFileName());

            DefaultSnippet ds = (DefaultSnippet) snippet;
            ds.addAttachment(att);

            final String url = mf.constructURL(session);
            String imageTag = imageTags.get(id);
            String replacement = pattern.matcher(imageTag).replaceAll(Matcher.quoteReplacement("src=\"" + url + "\""));
            content = content.replace(imageTag, replacement);
            ds.setContent(content);
        }
    }
}

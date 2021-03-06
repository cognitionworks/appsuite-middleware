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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.file.storage.infostore;

import java.util.function.Function;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.Document;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.infostore.internal.InfostoreDocument;
import com.openexchange.groupware.infostore.DocumentAndMetadata;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.tools.iterator.ConvertingSearchIterator;
import com.openexchange.tools.iterator.SearchIterator;

/**
 * {@link FileConverter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.5
 */
public class FileConverter implements Function<DocumentMetadata, File> {

    /**
     * Initializes a new {@link FileConverter}.
     */
    public FileConverter() {
        super();
    }

    @Override
    public File apply(DocumentMetadata metadata) {
        return getFile(metadata);
    }

    public File getFile(DocumentMetadata metadata) {
        return null == metadata ? null : new InfostoreFile(metadata);
    }

    public Document getFileDocument(DocumentAndMetadata metadataDocument) {
        return new InfostoreDocument(metadataDocument, this);
    }

    public TimedResult<File> getFileTimedResult(TimedResult<DocumentMetadata> metadataTimedResult) {
        return new TimedResult<File>() {

            @Override
            public SearchIterator<File> results() throws OXException {
                return getFileSearchIterator(metadataTimedResult.results());
            }

            @Override
            public long sequenceNumber() throws OXException {
                return metadataTimedResult.sequenceNumber();
            }
        };
    }

    public Delta<File> getFileDelta(Delta<DocumentMetadata> metadataDelta) {
        return new Delta<File>() {

            @Override
            public SearchIterator<File> results() throws OXException {
                return getFileSearchIterator(metadataDelta.results());
            }

            @Override
            public long sequenceNumber() throws OXException {
                return metadataDelta.sequenceNumber();
            }

            @Override
            public SearchIterator<File> getNew() {
                return getFileSearchIterator(metadataDelta.getNew());
            }

            @Override
            public SearchIterator<File> getModified() {
                return getFileSearchIterator(metadataDelta.getModified());
            }

            @Override
            public SearchIterator<File> getDeleted() {
                return getFileSearchIterator(metadataDelta.getDeleted());
            }
        };
    }

    public SearchIterator<File> getFileSearchIterator(SearchIterator<DocumentMetadata> metadataSearchIterator) {
        return null == metadataSearchIterator ? null : new ConvertingSearchIterator<DocumentMetadata, File>(metadataSearchIterator, this);
    }

    public DocumentMetadata getMetadata(File file) throws OXException {
        return new FileMetadata(file);
    }

}

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

package com.openexchange.find.basic.drive;

import static com.openexchange.java.Autoboxing.I2i;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.davekoelle.AlphanumComparator;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.composition.FileID;
import com.openexchange.file.storage.composition.FolderID;
import com.openexchange.file.storage.infostore.InfostoreFile;
import com.openexchange.file.storage.infostore.ToInfostoreTermVisitor;
import com.openexchange.find.Document;
import com.openexchange.find.SearchRequest;
import com.openexchange.find.basic.Services;
import com.openexchange.find.drive.FileDocument;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.InfostoreSearchEngine;
import com.openexchange.groupware.infostore.search.SearchTerm;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.groupware.tools.chunk.ChunkPerformer;
import com.openexchange.groupware.tools.chunk.ListPerformable;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.session.ServerSession;


/**
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.6.0
 */
public class InfostoreSearcher {

    /*
     * If we need to do chunk loading only HARD_LIMIT messages will be retrieved and sorted
     * to avoid OOM situations. For very long result sets this will lead to missing results
     * when paging exceeds this limit.
     */
    private static final int HARD_LIMIT = 2000;

    /*
     * Max. number of folders to search in at a time. If more folders need to be considered,
     * chunk loading is performed.
     */
    private static final int MAX_FOLDERS = 1000;

    private final SearchTerm<?> infostoreTerm;
    private List<Integer> folderIDs;
    private Metadata[] fields;
    private final int start;
    private final int size;
    private final ServerSession session;
    private final InfostoreSearchEngine searchEngine;

    public InfostoreSearcher(final SearchRequest searchRequest, final ServerSession session, final com.openexchange.file.storage.search.SearchTerm<?> searchTerm, final List<Integer> folderIDs, final Metadata[] fields) throws OXException {
        super();
        this.folderIDs = folderIDs;
        this.fields = fields;
        this.session = session;
        start = searchRequest.getStart();
        size = searchRequest.getSize();
        searchEngine = Services.getInfostoreSearchEngine();
        if (null == searchEngine) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(InfostoreFacade.class.getName());
        }
        ToInfostoreTermVisitor visitor = new ToInfostoreTermVisitor();
        searchTerm.visit(visitor);
        infostoreTerm = visitor.getInfostoreTerm();
    }

    public List<Document> search() throws OXException {
        if (folderIDs.size() > MAX_FOLDERS) {
            return performChunkedSearch();
        } else {
            SearchIterator<DocumentMetadata> it = null;
            try {
                it = searchEngine.search(session, infostoreTerm, I2i(folderIDs), fields, Metadata.TITLE_LITERAL, InfostoreSearchEngine.ASC, start, start + size);
                List<Document> results = new ArrayList<Document>(100);
                while (it.hasNext()) {
                    results.add(new FileDocument(new FindInfostoreFile(it.next())));
                }

                return results;
            } finally {
                SearchIterators.close(it);
            }
        }
    }

    private List<Document> performChunkedSearch() throws OXException {
        final List<Document> results = new ArrayList<Document>(HARD_LIMIT);
        Set<Metadata> extendedFieldSet = new HashSet<Metadata>();
        Collections.addAll(extendedFieldSet, fields);
        extendedFieldSet.add(Metadata.TITLE_LITERAL);
        extendedFieldSet.add(Metadata.FILENAME_LITERAL);
        final Metadata[] extendedFields = extendedFieldSet.toArray(new Metadata[extendedFieldSet.size()]);
        final double chunks = Math.ceil((double) folderIDs.size() / MAX_FOLDERS);
        final int limitPerChunk = (int) Math.ceil(HARD_LIMIT / chunks);
        ChunkPerformer.perform(folderIDs, 0, MAX_FOLDERS, new ListPerformable<Integer>() {
            @Override
            public void perform(List<Integer> subList) throws OXException {
                SearchIterator<DocumentMetadata> it = null;
                try {
                    it = searchEngine.search(session, infostoreTerm, I2i(subList), extendedFields, Metadata.TITLE_LITERAL, InfostoreSearchEngine.ASC, 0, limitPerChunk);
                    while (it.hasNext()) {
                        results.add(new FileDocument(new FindInfostoreFile(it.next())));
                    }
                } finally {
                    SearchIterators.close(it);
                }
            }
        });

        if (start > results.size()) {
            return Collections.emptyList();
        }

        final AlphanumComparator alphanumComparator = new AlphanumComparator(session.getUser().getLocale());
        Collections.sort(results, new Comparator<Document>() {
            @Override
            public int compare(Document o1, Document o2) {
                return alphanumComparator.compare(getTitle(o1), getTitle(o2));
            }

            private String getTitle(Document d) {
                FileDocument fd = (FileDocument) d;
                String t = fd.getFile().getTitle();
                if (t == null) {
                    t = fd.getFile().getFileName();
                }
                if (t == null) {
                    t = "";
                }
                return t;
            }
        });

        int toIndex = results.size();
        if (start + size > 0 && start + size < results.size()) {
            toIndex = start + size;
        }
        return results.subList(start, toIndex);
    }

    private static final class FindInfostoreFile extends InfostoreFile {


        private final String fileId;

        private final String folderId;

        private final String originalFileId;

        private final String originalFolderId;

        /**
         * Initializes a new {@link FindInfostoreFile}.
         * @param document
         */
        public FindInfostoreFile(DocumentMetadata document) {
            super(document);
            fileId = new FileID(FileID.INFOSTORE_SERVICE_ID, FileID.INFOSTORE_ACCOUNT_ID, Long.toString(document.getFolderId()), Integer.toString(document.getId())).toUniqueID();
            folderId = new FolderID(FileID.INFOSTORE_SERVICE_ID, FileID.INFOSTORE_ACCOUNT_ID, Long.toString(document.getFolderId())).toUniqueID();
            originalFileId = new FileID(FileID.INFOSTORE_SERVICE_ID, FileID.INFOSTORE_ACCOUNT_ID, Long.toString(document.getOriginalFolderId()), Integer.toString(document.getOriginalId())).toUniqueID();
            originalFolderId = new FolderID(FileID.INFOSTORE_SERVICE_ID, FileID.INFOSTORE_ACCOUNT_ID, Long.toString(document.getOriginalFolderId())).toUniqueID();
        }

        @Override
        public String getId() {
            return fileId;
        }

        @Override
        public String getFolderId() {
            return folderId;
        }

        @Override
        public String getOriginalId() {
            return originalFileId;
        }

        @Override
        public String getOriginalFolderId() {
            return originalFolderId;
        }

        @Override
        public void setId(String id) {
            throw new UnsupportedOperationException("IDs are read only for search results!");
        }

        @Override
        public void setFolderId(String folderId) {
            throw new UnsupportedOperationException("IDs are read only for search results!");
        }

        @Override
        public void setOriginalId(String id) {
            throw new UnsupportedOperationException("IDs are read only for search results!");
        }

        @Override
        public void setOriginalFolderId(String id) {
            throw new UnsupportedOperationException("IDs are read only for search results!");
        }

    }

}

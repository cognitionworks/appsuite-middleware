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

import static com.openexchange.find.basic.drive.Constants.QUERY_FIELDS;
import static com.openexchange.java.Autoboxing.I2i;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.composition.IDBasedFolderAccessFactory;
import com.openexchange.file.storage.infostore.ToInfostoreTermVisitor;
import com.openexchange.file.storage.registry.FileStorageServiceRegistry;
import com.openexchange.file.storage.search.AndTerm;
import com.openexchange.file.storage.search.SearchTerm;
import com.openexchange.find.AutocompleteRequest;
import com.openexchange.find.AutocompleteResult;
import com.openexchange.find.Document;
import com.openexchange.find.Module;
import com.openexchange.find.SearchRequest;
import com.openexchange.find.SearchResult;
import com.openexchange.find.basic.Services;
import com.openexchange.find.common.FolderTypeDisplayItem;
import com.openexchange.find.common.FormattableDisplayItem;
import com.openexchange.find.drive.DriveFacetType;
import com.openexchange.find.drive.DriveStrings;
import com.openexchange.find.drive.FileDocument;
import com.openexchange.find.drive.FileSizeDisplayItem;
import com.openexchange.find.drive.FileSizeDisplayItem.Size;
import com.openexchange.find.drive.FileTypeDisplayItem;
import com.openexchange.find.facet.ActiveFacet;
import com.openexchange.find.facet.Facet;
import com.openexchange.find.facet.FacetValue;
import com.openexchange.find.facet.FieldFacet;
import com.openexchange.find.facet.Filter;
import com.openexchange.find.spi.AbstractModuleSearchDriver;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.InfostoreSearchEngine;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link BasicInfostoreDriver}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since 7.6.0
 */
public class BasicInfostoreDriver extends AbstractModuleSearchDriver {

    private final Metadata[] fields = new Metadata[] {
        Metadata.ID_LITERAL, Metadata.MODIFIED_BY_LITERAL, Metadata.LAST_MODIFIED_LITERAL, Metadata.FOLDER_ID_LITERAL,
        Metadata.TITLE_LITERAL, Metadata.FILENAME_LITERAL, Metadata.FILE_MIMETYPE_LITERAL, Metadata.FILE_SIZE_LITERAL,
        Metadata.VERSION_LITERAL, Metadata.LOCKED_UNTIL_LITERAL };

    /**
     * Initializes a new {@link BasicInfostoreDriver}.
     */
    public BasicInfostoreDriver() {
        super();
    }

    @Override
    public Module getModule() {
        return Module.DRIVE;
    }

    @Override
    public boolean isValidFor(ServerSession session) throws OXException {
        if (!session.getUserConfiguration().hasInfostore()) {
            return false;
        }
        FileStorageServiceRegistry registry = Services.getFileStorageServiceRegistry();
        if (null == registry) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(FileStorageServiceRegistry.class.getName());
        }
        List<FileStorageService> services = registry.getAllServices();
        for (FileStorageService service : services) {
            List<FileStorageAccount> accounts = service.getAccountManager().getAccounts(session);
            if (accounts.size() == 0 || accounts.size() > 1) {
                return false;
            }
            if (!"com.openexchange.infostore".equals(service.getId())) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public SearchResult search(SearchRequest searchRequest, ServerSession session) throws OXException {
        int userId = session.getUserId();
        InfostoreSearchEngine searchEngine = Services.getInfostoreSearchEngine();
        if (null == searchEngine) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(InfostoreFacade.class.getName());
        }
        IDBasedFolderAccessFactory folderAccessFactory = Services.getIdBasedFolderAccessFactory();
        if (null == folderAccessFactory) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(IDBasedFolderAccessFactory.class.getName());
        }
        SearchTerm<?> term = null;
        List<SearchTerm<?>> terms = null;
        List<Integer> folderIds = new ArrayList<Integer>();
        List<ActiveFacet> facets = searchRequest.getActiveFacets();
        if (facets.size() > 1) {
            terms = new ArrayList<SearchTerm<?>>(facets.size());
        }
        for (ActiveFacet facet : facets) {
            Filter filter = facet.getFilter();
            if (filter.getFields().contains(Constants.FIELD_FOLDER_TYPE)) {
                String query = "";
                for (int i = 0; i < filter.getFields().size(); i++) {
                    if (filter.getFields().get(i).equals(Constants.FIELD_FOLDER_TYPE)) {
                        query = filter.getQueries().get(i);
                        break;
                    }
                }
                OXFolderAccess access = new OXFolderAccess(session.getContext());
                FolderObject defaultFolder = access.getDefaultFolder(userId, FolderObject.INFOSTORE);
                folderIds.add(defaultFolder.getObjectID());
                List<Integer> subfolders = new LinkedList<Integer>();
                if (defaultFolder.hasSubfolders()) {
                    try {
                        subfolders.addAll(defaultFolder.getSubfolderIds(true, session.getContext()));
                    } catch (SQLException e) {
                        // should not happen?
                    }
                }
                if (query.equals(FolderTypeDisplayItem.Type.PUBLIC.getIdentifier())) {
                    for (int folder : subfolders) {
                        if (access.getFolderType(folder, userId) != FolderObject.PUBLIC) {
                            folderIds.add(folder);
                        }
                    }
                }
                if (query.equals(FolderTypeDisplayItem.Type.PRIVATE.getIdentifier())) {
                    for (int folder : subfolders) {
                        if (access.getFolderType(folder, userId) != FolderObject.PRIVATE) {
                            folderIds.add(folder);
                        }
                    }
                }
                if (query.equals(FolderTypeDisplayItem.Type.SHARED.getIdentifier())) {
                    for (int folder : subfolders) {
                        if (access.getFolderType(folder, userId) != FolderObject.SHARED) {
                            folderIds.add(folder);
                        }
                    }
                }

            }
            term = Utils.termFor(filter);
            if (null != terms) {
                terms.add(term);
            }
        }
        if (null != terms) {
            term = new AndTerm(terms);
        }
        if (null == term) {
            term = prepareSearchTerm(searchRequest.getQueries(), searchRequest.getFilters());
        }

        final String folderId = searchRequest.getFolderId();
        if (folderIds.size() == 0 && null != folderId) {
            folderIds = Collections.singletonList(Integer.valueOf(folderId));
        } else {
            if (folderId != null) {
                folderIds.add(Integer.valueOf(folderId));
            }
        }
        ToInfostoreTermVisitor visitor = new ToInfostoreTermVisitor();
        term.visit(visitor);

        SearchIterator<DocumentMetadata> it = null;
        try {
            final int start = searchRequest.getStart();
            it = searchEngine.search(
                I2i(folderIds),
                visitor.getInfstoreTerm(),
                fields,
                Metadata.TITLE_LITERAL,
                0,
                start,
                start + searchRequest.getSize(),
                session.getContext(),
                session.getUser(),
                session.getUserPermissionBits());
            final List<Document> results = new LinkedList<Document>();
            while (it.hasNext()) {
                final DocumentMetadata doc = it.next();
                results.add(new FileDocument(Utils.documentMetadata2File(doc)));
            }
            return new SearchResult(-1, start, results, searchRequest.getActiveFacets());
        } finally {
            SearchIterators.close(it);
        }
    }

    @Override
    protected String getFormatStringForGlobalFacet() {
        return DriveStrings.FACET_GLOBAL;
    }

    @Override
    protected AutocompleteResult doAutocomplete(AutocompleteRequest autocompleteRequest, ServerSession session) throws OXException {

        String prefix = autocompleteRequest.getPrefix();

        // List of supported facets
        final List<Facet> facets = new LinkedList<Facet>();

        if (!prefix.isEmpty()) {
            // Add field factes
            FieldFacet filenameFacet = new FieldFacet(DriveFacetType.FILE_NAME, new FormattableDisplayItem(
                DriveStrings.SEARCH_IN_FILE_NAME,
                prefix), Field.FILENAME.getName(), prefix);
            FieldFacet descriptionFacet = new FieldFacet(DriveFacetType.FILE_DESCRIPTION, new FormattableDisplayItem(
                DriveStrings.SEARCH_IN_FILE_DESC,
                prefix), Field.DESCRIPTION.getName(), prefix);
            FieldFacet contentFacet = new FieldFacet(DriveFacetType.FILE_CONTENT, new FormattableDisplayItem(
                DriveStrings.SEARCH_IN_FILE_CONTENT,
                prefix), Field.CONTENT.getName(), prefix);
            facets.add(filenameFacet);
            facets.add(descriptionFacet);
            facets.add(contentFacet);
        }
        // Add static file type facet
        final List<FacetValue> fileTypes = new ArrayList<FacetValue>(6);
        final String fieldFileType = Constants.FIELD_FILE_TYPE;
        fileTypes.add(new FacetValue(FileTypeDisplayItem.Type.AUDIO.getIdentifier(), new FileTypeDisplayItem(
            DriveStrings.FILE_TYPE_AUDIO,
            FileTypeDisplayItem.Type.AUDIO), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFileType),
            FileTypeDisplayItem.Type.AUDIO.getIdentifier())));
        fileTypes.add(new FacetValue(FileTypeDisplayItem.Type.DOCUMENTS.getIdentifier(), new FileTypeDisplayItem(
            DriveStrings.FILE_TYPE_DOCUMENTS,
            FileTypeDisplayItem.Type.DOCUMENTS), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFileType),
            FileTypeDisplayItem.Type.DOCUMENTS.getIdentifier())));
        fileTypes.add(new FacetValue(FileTypeDisplayItem.Type.IMAGES.getIdentifier(), new FileTypeDisplayItem(
            DriveStrings.FILE_TYPE_IMAGES,
            FileTypeDisplayItem.Type.IMAGES), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFileType),
            FileTypeDisplayItem.Type.IMAGES.getIdentifier())));
        fileTypes.add(new FacetValue(FileTypeDisplayItem.Type.OTHER.getIdentifier(), new FileTypeDisplayItem(
            DriveStrings.FILE_TYPE_OTHER,
            FileTypeDisplayItem.Type.OTHER), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFileType),
            FileTypeDisplayItem.Type.OTHER.getIdentifier())));
        fileTypes.add(new FacetValue(FileTypeDisplayItem.Type.VIDEO.getIdentifier(), new FileTypeDisplayItem(
            DriveStrings.FILE_TYPE_VIDEO,
            FileTypeDisplayItem.Type.VIDEO), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFileType),
            FileTypeDisplayItem.Type.VIDEO.getIdentifier())));
        final Facet fileTypeFacet = new Facet(DriveFacetType.FILE_TYPE, fileTypes);
        facets.add(fileTypeFacet);

        // Add static folder type facets
        String fieldFolderType = Constants.FIELD_FOLDER_TYPE;
        final List<FacetValue> folderTypes = new ArrayList<FacetValue>(4);
        folderTypes.add(new FacetValue(FolderTypeDisplayItem.Type.PUBLIC.getIdentifier(), new FolderTypeDisplayItem(
            DriveStrings.FOLDER_TYPE_PUBLIC,
            FolderTypeDisplayItem.Type.PUBLIC), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFolderType),
            FolderTypeDisplayItem.Type.PUBLIC.getIdentifier())));
        folderTypes.add(new FacetValue(FolderTypeDisplayItem.Type.PRIVATE.getIdentifier(), new FolderTypeDisplayItem(
            DriveStrings.FOLDER_TYPE_PRIVATE,
            FolderTypeDisplayItem.Type.PRIVATE), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFolderType),
            FolderTypeDisplayItem.Type.PRIVATE.getIdentifier())));
        folderTypes.add(new FacetValue(FolderTypeDisplayItem.Type.SHARED.getIdentifier(), new FolderTypeDisplayItem(
            DriveStrings.FOLDER_TYPE_SHARED,
            FolderTypeDisplayItem.Type.SHARED), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFolderType),
            FolderTypeDisplayItem.Type.SHARED.getIdentifier())));
        final Facet folderTypeFacet = new Facet(DriveFacetType.FOLDER_TYPE, folderTypes);
        facets.add(folderTypeFacet);

        // Add static file size facet
        List<FacetValue> fileSize = new ArrayList<FacetValue>(5);
        String fieldFileSize = Constants.FIELD_FILE_SIZE;
        fileSize.add(new FacetValue(
            FileSizeDisplayItem.Size.MB1.getSize(),
            new FileSizeDisplayItem(DriveStrings.FACET_FILE_SIZE, Size.MB1),
            FacetValue.UNKNOWN_COUNT,
            new Filter(Collections.singletonList(fieldFileSize), FileSizeDisplayItem.Size.MB1.getSize())));
        fileSize.add(new FacetValue(FileSizeDisplayItem.Size.MB10.getSize(), new FileSizeDisplayItem(
            DriveStrings.FACET_FILE_SIZE,
            Size.MB10), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFileSize),
            FileSizeDisplayItem.Size.MB10.getSize())));
        fileSize.add(new FacetValue(FileSizeDisplayItem.Size.MB100.getSize(), new FileSizeDisplayItem(
            DriveStrings.FACET_FILE_SIZE,
            Size.MB100), FacetValue.UNKNOWN_COUNT, new Filter(
            Collections.singletonList(fieldFileSize),
            FileSizeDisplayItem.Size.MB100.getSize())));
        fileSize.add(new FacetValue(
            FileSizeDisplayItem.Size.GB1.getSize(),
            new FileSizeDisplayItem(DriveStrings.FACET_FILE_SIZE, Size.GB1),
            FacetValue.UNKNOWN_COUNT,
            new Filter(Collections.singletonList(fieldFileSize), FileSizeDisplayItem.Size.GB1.getSize())));
        Facet fileSizeFacet = new Facet(DriveFacetType.FILE_SIZE, fileSize);
        facets.add(fileSizeFacet);

        return new AutocompleteResult(facets);
    }

    private static SearchTerm<?> prepareSearchTerm(final List<String> queries, final List<Filter> filters) throws OXException {
        final SearchTerm<?> queryTerm = prepareQueryTerm(queries);
        final SearchTerm<?> filterTerm = prepareFilterTerm(filters);
        if (filterTerm == null || queryTerm == null) {
            return (filterTerm == null) ? queryTerm : filterTerm;
        }
        return new AndTerm(Arrays.<SearchTerm<?>> asList(queryTerm, filterTerm));
    }

    private static SearchTerm<?> prepareQueryTerm(final List<String> queries) throws OXException {
        if (queries == null || queries.isEmpty()) {
            return null;
        }
        for (String query : queries) {
            query.trim();
        }

        return Utils.termFor(QUERY_FIELDS, queries);
    }

    private static SearchTerm<?> prepareFilterTerm(final List<Filter> filters) throws OXException {
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        final int size = filters.size();
        if (size == 1) {
            return Utils.termFor(filters.get(0));
        }

        final List<SearchTerm<?>> terms = new ArrayList<SearchTerm<?>>(size);
        for (final Filter filter : filters) {
            terms.add(Utils.termFor(filter));
        }
        return new AndTerm(terms);
    }

//    private List<DocumentMetadata> getAutocompleteFiles(ServerSession session, AutocompleteRequest request) throws OXException {
//        InfostoreSearchEngine searchEngine = Services.getInfostoreSearchEngine();
//        if (null == searchEngine) {
//            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(InfostoreSearchEngine.class.getName());
//        }
//
//        // Compose term
//        String prefix = request.getPrefix();
//        int limit = request.getLimit();
//        List<SearchTerm<?>> terms = new LinkedList<SearchTerm<?>>();
//        terms.add(new TitleTerm(prefix, true, true));
//        terms.add(new FileNameTerm(prefix, true, true));
//        terms.add(new DescriptionTerm(prefix, true, true));
//        SearchTerm<List<SearchTerm<?>>> orTerm = new OrTerm(terms);
//        ToInfostoreTermVisitor visitor = new ToInfostoreTermVisitor();
//        orTerm.visit(visitor);
//
//        // Fire search
//        SearchIterator<DocumentMetadata> it = null;
//        try {
//            it = searchEngine.search(
//                new int[0],
//                visitor.getInfstoreTerm(),
//                fields,
//                Metadata.TITLE_LITERAL,
//                0,
//                0,
//                limit,
//                session.getContext(),
//                session.getUser(),
//                session.getUserPermissionBits());
//            List<DocumentMetadata> docs = new LinkedList<DocumentMetadata>();
//            while (it.hasNext()) {
//                DocumentMetadata doc = it.next();
//                docs.add(doc);
//            }
//            return docs;
//        } finally {
//            SearchIterators.close(it);
//        }
//    }

}

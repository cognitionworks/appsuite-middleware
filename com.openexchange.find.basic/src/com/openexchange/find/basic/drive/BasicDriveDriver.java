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

import static com.openexchange.find.basic.drive.Utils.prepareSearchTerm;
import static com.openexchange.find.facet.Facets.newSimpleBuilder;
import static com.openexchange.java.SimpleTokenizer.tokenize;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.AccountAware;
import com.openexchange.file.storage.CapabilityAware;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageCapability;
import com.openexchange.file.storage.FileStorageCapabilityTools;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.FileStorageFileAccess.SortDirection;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStoragePermission;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.composition.FolderID;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.file.storage.composition.IDBasedFolderAccess;
import com.openexchange.file.storage.composition.IDBasedFolderAccessFactory;
import com.openexchange.file.storage.registry.FileStorageServiceRegistry;
import com.openexchange.file.storage.search.SearchTerm;
import com.openexchange.file.storage.search.TitleTerm;
import com.openexchange.find.AbstractFindRequest;
import com.openexchange.find.AutocompleteRequest;
import com.openexchange.find.AutocompleteResult;
import com.openexchange.find.Document;
import com.openexchange.find.Module;
import com.openexchange.find.SearchRequest;
import com.openexchange.find.SearchResult;
import com.openexchange.find.basic.Services;
import com.openexchange.find.common.CommonConstants;
import com.openexchange.find.common.CommonFacetType;
import com.openexchange.find.common.CommonStrings;
import com.openexchange.find.common.FolderType;
import com.openexchange.find.drive.DriveFacetType;
import com.openexchange.find.drive.DriveStrings;
import com.openexchange.find.drive.FileDocument;
import com.openexchange.find.facet.ActiveFacet;
import com.openexchange.find.facet.DefaultFacet;
import com.openexchange.find.facet.Facet;
import com.openexchange.find.facet.FacetValue;
import com.openexchange.find.facet.Facets;
import com.openexchange.find.facet.Filter;
import com.openexchange.find.spi.AbstractModuleSearchDriver;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.folderstorage.database.contentType.InfostoreContentType;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.MimeType2ExtMap;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link BasicDriveDriver}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.6.0
 */
public class BasicDriveDriver extends AbstractModuleSearchDriver {

    private static final Set<FolderType> FOLDER_TYPES = EnumSet.noneOf(FolderType.class);
    static {
        FOLDER_TYPES.add(FolderType.SHARED);
        FOLDER_TYPES.add(FolderType.PUBLIC);
    }

    private static final List<Field> DEFAULT_FIELDS = new ArrayList<Field>(10);
    static {
        Collections.addAll(DEFAULT_FIELDS,
            Field.FOLDER_ID, Field.META, Field.ID, Field.LAST_MODIFIED,
            Field.TITLE, Field.FILENAME, Field.FILE_MIMETYPE, Field.FILE_SIZE,
            Field.LOCKED_UNTIL, Field.MODIFIED_BY, Field.VERSION);
    }

    /**
     * Initializes a new {@link BasicDriveDriver}.
     */
    public BasicDriveDriver() {
        super();
    }

    @Override
    public boolean isValidFor(final ServerSession session) {
        return session.getUserConfiguration().hasInfostore();
    }

    @Override
    protected Set<FolderType> getSupportedFolderTypes(ServerSession session) {
        UserPermissionBits userPermissionBits = session.getUserPermissionBits();
        if (userPermissionBits.hasFullSharedFolderAccess()) {
            return ALL_FOLDER_TYPES;
        }

        Set<FolderType> types = EnumSet.noneOf(FolderType.class);
        types.add(FolderType.PRIVATE);
        types.add(FolderType.PUBLIC);
        return types;
    }

    @Override
    public SearchResult doSearch(final SearchRequest searchRequest, final ServerSession session) throws OXException {
        IDBasedFileAccessFactory fileAccessFactory = Services.getIdBasedFileAccessFactory();
        if (null == fileAccessFactory) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(IDBasedFileAccessFactory.class.getName());
        }

        IDBasedFolderAccessFactory folderAccessFactory = Services.getIdBasedFolderAccessFactory();
        if (null == folderAccessFactory) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(IDBasedFolderAccessFactory.class.getName());
        }

        // Create file access
        IDBasedFileAccess fileAccess = fileAccessFactory.createAccess(session);
        IDBasedFolderAccess folderAccess = folderAccessFactory.createAccess(session);

        // Folder identifier
        String folderId = searchRequest.getFolderId();

        // Fields
        int start = searchRequest.getStart();
        List<Field> fields = DEFAULT_FIELDS;
        int[] columns = searchRequest.getColumns().getIntColumns();
        if (columns.length > 0) {
            fields = Field.get(columns);
        }

        // Search by term only if supported
        if (supportsSearchByTerm(session, fileAccess, searchRequest)) {

            // Yield search term from search request
            SearchTerm<?> term = prepareSearchTerm(searchRequest);
            if (term == null) {
                term = new TitleTerm("*", true, true);
            }

            // Search...
            List<String> folderIds = determineFolderIDs(searchRequest, session, folderAccess);

            SearchIterator<File> it = null;
            try {
                it = fileAccess.search(folderIds, term, fields, Field.TITLE, SortDirection.DEFAULT, start, start + searchRequest.getSize());
                List<Document> results = new LinkedList<Document>();
                while (it.hasNext()) {
                    results.add(new FileDocument(it.next()));
                }
                return new SearchResult(-1, start, results, searchRequest.getActiveFacets());
            } finally {
                SearchIterators.close(it);
                fileAccess.finish();
                folderAccess.finish();
            }
        }

        // Search by simple pattern as fallback
        final List<String> queries = searchRequest.getQueries();
        final String pattern = null != queries && 0 < queries.size() ? queries.get(0) : "*";
        List<File> files = new LinkedList<File>();
        final SearchIterator<File> it = null;
        try {
            files = iterativeSearch(fileAccess, folderAccess, folderId, pattern, fields, start, start + searchRequest.getSize());
            //            it = fileAccess.search(pattern, fields, folderId, File.Field.TITLE, SortDirection.DEFAULT, start, start + searchRequest.getSize());
            //            while (it.hasNext()) {
            //                files.add(it.next());
            //            }
        } finally {
            SearchIterators.close(it);
            fileAccess.finish();
            folderAccess.finish();
        }

        // Filter according to file type facet if defined
        final String fileType = extractFileType(searchRequest.getActiveFacets(DriveFacetType.FILE_TYPE));
        if (null != fileType) {
            files = filter(files, fileType);
        }
        final List<Document> results = new ArrayList<Document>(files.size());
        for (final File file : files) {
            results.add(new FileDocument(file));
        }
        return new SearchResult(-1, start, results, searchRequest.getActiveFacets());
    }

    private List<String> determineFolderIDs(SearchRequest searchRequest, ServerSession session, IDBasedFolderAccess folderAccess) throws OXException {
        List<String> folderIDs;
        FolderType folderType = searchRequest.getFolderType();
        String requestFolderId = searchRequest.getFolderId();
        if (requestFolderId == null) {
            if (folderType == null) {
                folderIDs = Collections.emptyList();
            } else {
                switch (folderType) {
                    // Probably no other file storage despite infostore will ever implement folder types.
                    // For performance reasons we therefore limit the folder-lookup to infostore folders.

                    case PUBLIC:
                    {
                        folderIDs = findSubfolders(Integer.toString(FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID), folderAccess);
                        break;
                    }

                    case SHARED:
                    {
                        folderIDs = findSubfolders(Integer.toString(FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID), folderAccess);
                        break;
                    }

                    default:
                    {
                        FolderService folderService = Services.getFolderService();
                        FolderServiceDecorator decorator = new FolderServiceDecorator();
                        decorator.put("altNames", Boolean.TRUE.toString());
                        decorator.setLocale(session.getUser().getLocale());
                        UserizedFolder privateInfostoreFolder = folderService.getDefaultFolder(session.getUser(), "1", InfostoreContentType.getInstance(), session, decorator);
                        folderIDs = findSubfolders(privateInfostoreFolder.getID(), folderAccess);
                        break;
                    }
                }
            }
        } else {
            folderIDs = findSubfolders(requestFolderId, folderAccess);
        }

        return folderIDs;
    }

    private List<File> iterativeSearch(final IDBasedFileAccess fileAccess, final IDBasedFolderAccess folderAccess, final String startingId, final String pattern, final List<Field> fields, final int start, final int end)
    {
        try {
            //get all Folders
            final List<String> folders = findSubfolders(startingId, folderAccess);
            folders.size();
            //search in all folders
            final List<File> files = new ArrayList<File>(30);
            SearchIterator<File> it = null;
            for (final String folderId : folders)
            {
                it = fileAccess.search(pattern, fields, folderId, File.Field.TITLE, SortDirection.DEFAULT, start, end);
                while (it.hasNext()) {
                    files.add(it.next());
                }
            }
            Collections.sort(files, SortDirection.DEFAULT.comparatorBy(File.Field.TITLE));
            return files;
        } catch (final OXException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Return all visible and readable folders that are below given folder, including that folder itself.
     *
     * @return
     * @throws OXException
     */
    private List<String> findSubfolders(String folderId, IDBasedFolderAccess folderAccess) throws OXException {
        List<String> folderIds = new LinkedList<>();
        FileStorageFolder folder = folderAccess.getFolder(folderId);
        findSubfoldersRecursive(folder, folderAccess, folderIds);
        return folderIds;
    }

    private void findSubfoldersRecursive(FileStorageFolder folder, IDBasedFolderAccess folderAccess, List<String> folderIds) throws OXException {
        FileStoragePermission permission = folder.getOwnPermission();
        if (permission == null || permission.getReadPermission() >= FileStoragePermission.READ_OWN_OBJECTS) {
            folderIds.add(folder.getId());
        }

        FileStorageFolder[] fileStorageFolderIds = folderAccess.getSubfolders(folder.getId(), true);
        if (fileStorageFolderIds != null && fileStorageFolderIds.length > 0) {
            for (FileStorageFolder f : fileStorageFolderIds) {
                findSubfoldersRecursive(f, folderAccess, folderIds);
            }
        }
    }

    /**
     * Extracts the file type used in the filter of the supplied active facets.
     *
     * @param fileyTypeFacts The active facets holding the defined file type
     * @return The file type, or <code>null</code> if there is none
     */
    private static String extractFileType(final List<ActiveFacet> facets) {
        if (null != facets && 0 < facets.size() && null != facets.get(0)) {
            final ActiveFacet facet = facets.get(0);
            if (DriveFacetType.FILE_TYPE.equals(facet.getType()) && null != facet.getFilter() && null != facet.getFilter().getQueries() &&
                0 < facet.getFilter().getQueries().size() && null != facet.getFilter().getQueries().get(0)) {
                return facet.getFilter().getQueries().get(0);
            }
        }
        return null;
    }

    /**
     * Filters a list of files based on a specific file type.
     *
     * @param files The files to filter
     * @param fileType The file type identifier
     * @return The filtered list
     */
    private static List<File> filter(final List<File> files, final String fileType) {
        if (null != files && 0 < files.size()) {
            /*
             * determine patterns to check the MIME type against
             */
            List<String> patterns;
            boolean negate;
            if (FileType.OTHER.getIdentifier().equals(fileType)) {
                negate = true;
                patterns = new ArrayList<String>();
                final String[] typesToNegate = new String[] {
                    FileType.AUDIO.getIdentifier(), FileType.IMAGES.getIdentifier(), FileType.DOCUMENTS.getIdentifier(), FileType.VIDEO.getIdentifier()
                };
                for (final String typeToNegate : typesToNegate) {
                    patterns.addAll(getPatternsForFileType(typeToNegate));
                }
            } else {
                negate = false;
                patterns = getPatternsForFileType(fileType);
            }
            /*
             * filter files
             */
            final Iterator<File> iterator = files.iterator();
            while (iterator.hasNext()) {
                final File file = iterator.next();
                if (matchesAny(file, patterns)) {
                    if (negate) {
                        iterator.remove();
                    }
                } else if (false == negate) {
                    iterator.remove();
                }
            }
        }
        return files;
    }

    /**
     * Gets a value indicating whether the supplied file's MIME type matches any of the specified patterns.
     *
     * @param file The file to check
     * @param patterns The patterns to check the file's MIME type against
     * @return <code>true</code> if the file's MIME type matches at least one of the supplied patterns, <code>false</code>, otherwise
     */
    private static boolean matchesAny(final File file, final List<String> patterns) {
        final String mimeType = null != file.getFileMIMEType() ? file.getFileMIMEType() : MimeType2ExtMap.getContentType(file.getFileName());
        for (final String regex : patterns) {
            if (Pattern.matches(regex, mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates patterns to match against filenames based on the file types defined by the supplied active facets.
     *
     * @param fileyType The file type to get the patterns for
     * @return The patterns, or an empty array if there are none
     */
    private static List<String> getPatternsForFileType(final String fileType) {
        String[] wildcardPatterns;
        if (FileType.DOCUMENTS.getIdentifier().equals(fileType)) {
            wildcardPatterns = Constants.FILETYPE_PATTERNS_DOCUMENTS;
        } else if (FileType.VIDEO.getIdentifier().equals(fileType)) {
            wildcardPatterns = Constants.FILETYPE_PATTERNS_VIDEO;
        } else if (FileType.AUDIO.getIdentifier().equals(fileType)) {
            wildcardPatterns = Constants.FILETYPE_PATTERNS_AUDIO;
        } else if (FileType.IMAGES.getIdentifier().equals(fileType)) {
            wildcardPatterns = Constants.FILETYPE_PATTERNS_IMAGES;
        } else {
            wildcardPatterns = new String[0];
        }
        final List<String> patterns = new ArrayList<String>(wildcardPatterns.length);
        for (final String wildcardPattern : wildcardPatterns) {
            patterns.add(Strings.wildcardToRegex(wildcardPattern));
        }
        return patterns;
    }

    @Override
    public AutocompleteResult doAutocomplete(final AutocompleteRequest autocompleteRequest, final ServerSession session) throws OXException {
        // The auto-complete prefix
        String prefix = autocompleteRequest.getPrefix();

        List<Facet> facets = new LinkedList<Facet>();

        boolean supportsSearchByTerm = supportsSearchByTerm(session, autocompleteRequest);
        int minimumSearchCharacters = ServerConfig.getInt(ServerConfig.Property.MINIMUM_SEARCH_CHARACTERS);
        if (Strings.isNotEmpty(prefix) && prefix.length() >= minimumSearchCharacters) {
            List<String> prefixTokens = tokenize(prefix, minimumSearchCharacters);
            if (prefixTokens.isEmpty()) {
                prefixTokens = Collections.singletonList(prefix);
            }

            facets.add(newSimpleBuilder(CommonFacetType.GLOBAL)
                .withSimpleDisplayItem(prefix)
                .withFilter(Filter.of(Constants.FIELD_GLOBAL, prefixTokens))
                .build());

            if (supportsSearchByTerm) {
                facets.add(newSimpleBuilder(DriveFacetType.FILE_NAME)
                    .withFormattableDisplayItem(DriveStrings.SEARCH_IN_FILE_NAME, prefix)
                    .withFilter(Filter.of(Constants.FIELD_FILE_NAME, prefixTokens))
                    .build());
                facets.add(newSimpleBuilder(DriveFacetType.FILE_DESCRIPTION)
                    .withFormattableDisplayItem(DriveStrings.SEARCH_IN_FILE_DESC, prefix)
                    .withFilter(Filter.of(Constants.FIELD_FILE_DESC, prefixTokens))
                    .build());
            }
        }

        addFileTypeFacet(facets);

        if (supportsSearchByTerm) {
            addFileSizeFacet(facets);
            addDateFacet(facets);
        }

        return new AutocompleteResult(facets);
    }

    private void addFileTypeFacet(List<Facet> facets) {
        String fieldFileType = Constants.FIELD_FILE_TYPE;
        DefaultFacet fileTypeFacet = Facets.newExclusiveBuilder(DriveFacetType.FILE_TYPE)
            .addValue(FacetValue.newBuilder(FileType.AUDIO.getIdentifier())
                .withLocalizableDisplayItem(DriveStrings.FILE_TYPE_AUDIO)
                .withFilter(Filter.of(fieldFileType, FileType.AUDIO.getIdentifier()))
                .build())
            .addValue(FacetValue.newBuilder(FileType.DOCUMENTS.getIdentifier())
                .withLocalizableDisplayItem(DriveStrings.FILE_TYPE_DOCUMENTS)
                .withFilter(Filter.of(fieldFileType, FileType.DOCUMENTS.getIdentifier()))
                .build())
            .addValue(FacetValue.newBuilder(FileType.IMAGES.getIdentifier())
                .withLocalizableDisplayItem(DriveStrings.FILE_TYPE_IMAGES)
                .withFilter(Filter.of(fieldFileType, FileType.IMAGES.getIdentifier()))
                .build())
            .addValue(FacetValue.newBuilder(FileType.OTHER.getIdentifier())
                .withLocalizableDisplayItem(DriveStrings.FILE_TYPE_OTHER)
                .withFilter(Filter.of(fieldFileType, FileType.OTHER.getIdentifier()))
                .build())
            .addValue(FacetValue.newBuilder(FileType.VIDEO.getIdentifier())
                .withLocalizableDisplayItem(DriveStrings.FILE_TYPE_VIDEO)
                .withFilter(Filter.of(fieldFileType, FileType.VIDEO.getIdentifier()))
                .build())
            .build();
        facets.add(fileTypeFacet);
    }

    private void addFileSizeFacet(List<Facet> facets) {
        String fieldFileSize = Constants.FIELD_FILE_SIZE;
        facets.add(Facets.newExclusiveBuilder(DriveFacetType.FILE_SIZE)
            .addValue(FacetValue.newBuilder(FileSize.MB1.getSize())
                .withSimpleDisplayItem(FileSize.MB1.getSize())
                .withFilter(Filter.of(fieldFileSize, FileSize.MB1.getSize()))
                .build())
            .addValue(FacetValue.newBuilder(FileSize.MB10.getSize())
                .withSimpleDisplayItem(FileSize.MB10.getSize())
                .withFilter(Filter.of(fieldFileSize, FileSize.MB10.getSize()))
                .build())
            .addValue(FacetValue.newBuilder(FileSize.MB100.getSize())
                .withSimpleDisplayItem(FileSize.MB100.getSize())
                .withFilter(Filter.of(fieldFileSize, FileSize.MB100.getSize()))
                .build())
            .addValue(FacetValue.newBuilder(FileSize.GB1.getSize())
                .withSimpleDisplayItem(FileSize.GB1.getSize())
                .withFilter(Filter.of(fieldFileSize, FileSize.GB1.getSize()))
                .build())
            .build());
    }

    private void addDateFacet(List<Facet> facets) {
        String fieldDate = CommonConstants.FIELD_DATE;
        facets.add(Facets.newExclusiveBuilder(CommonFacetType.DATE)
            .addValue(FacetValue.newBuilder(CommonConstants.QUERY_LAST_WEEK)
                .withLocalizableDisplayItem(CommonStrings.LAST_WEEK)
                .withFilter(Filter.of(fieldDate, CommonConstants.QUERY_LAST_WEEK))
                .build())
            .addValue(FacetValue.newBuilder(CommonConstants.QUERY_LAST_MONTH)
                .withLocalizableDisplayItem(CommonStrings.LAST_MONTH)
                .withFilter(Filter.of(fieldDate, CommonConstants.QUERY_LAST_MONTH))
                .build())
            .addValue(FacetValue.newBuilder(CommonConstants.QUERY_LAST_YEAR)
                .withLocalizableDisplayItem(CommonStrings.LAST_YEAR)
                .withFilter(Filter.of(fieldDate, CommonConstants.QUERY_LAST_YEAR))
                .build())
            .build());
    }

    @Override
    public Module getModule() {
        return Module.DRIVE;
    }

    /**
     * Gets a value indicating whether the "search by term" capability is available based on the parameters of the supplied find request.
     *
     * @param session The current session
     * @param findRequest The find request
     * @return <code>true</code> if searching by term is supported, <code>false</code>, otherwise
     */
    private static boolean supportsSearchByTerm(final ServerSession session, final AbstractFindRequest findRequest) throws OXException {
        final IDBasedFileAccessFactory fileAccessFactory = Services.getIdBasedFileAccessFactory();
        if (null == fileAccessFactory) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(IDBasedFileAccessFactory.class.getName());
        }
        final IDBasedFileAccess fileAccess = fileAccessFactory.createAccess(session);
        try {
            return supportsSearchByTerm(session, fileAccess, findRequest);
        } finally {
            fileAccess.finish();
        }
    }

    /**
     * Gets a value indicating whether the "search by term" capability is available based on the parameters of the supplied find request.
     *
     * @param session The current session
     * @param fileAccess A reference to the ID based file access service
     * @param findRequest The find request
     * @return <code>true</code> if searching by term is supported, <code>false</code>, otherwise
     */
    private static boolean supportsSearchByTerm(final ServerSession session, final IDBasedFileAccess fileAccess, final AbstractFindRequest findRequest) throws OXException {
        /*
         * check capability of all concrete file storage if folder ID is specified
         */
        if (null != findRequest.getFolderId()) {
            final FolderID folderID = new FolderID(findRequest.getFolderId());
            return fileAccess.supports(folderID.getService(), folderID.getAccountId(), FileStorageCapability.SEARCH_BY_TERM);
        }
        /*
         * check capability of all available storages, otherwise
         */
        final FileStorageServiceRegistry registry = Services.getFileStorageServiceRegistry();
        if (null == registry) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(FileStorageServiceRegistry.class.getName());
        }
        for (final FileStorageService service : registry.getAllServices()) {
            // Determine accounts
            final List<FileStorageAccount> accounts = AccountAware.class.isInstance(service) ? ((AccountAware) service).getAccounts(session) : service.getAccountManager().getAccounts(session);

            // Check for support of needed capability
            for (final FileStorageAccount account : accounts) {
                final FileStorageAccountAccess accountAccess = service.getAccountAccess(account.getId(), session);

                boolean checkByInstance = true;
                if (accountAccess instanceof CapabilityAware) {
                    final Boolean supported = ((CapabilityAware) accountAccess).supports(FileStorageCapability.SEARCH_BY_TERM);
                    if (null != supported) {
                        if (false == supported.booleanValue()) {
                            return false;
                        }
                        checkByInstance = false;
                    }
                }

                if (checkByInstance) {
                    accountAccess.connect();
                    try {
                        final FileStorageFileAccess _fileAccess = accountAccess.getFileAccess();
                        if (false == FileStorageCapabilityTools.supports(_fileAccess, FileStorageCapability.SEARCH_BY_TERM)) {
                            return false;
                        }
                    } finally {
                        accountAccess.close();
                    }
                }
            }
        }
        return true;
    }

}

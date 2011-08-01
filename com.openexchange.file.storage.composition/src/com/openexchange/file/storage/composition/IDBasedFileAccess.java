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

package com.openexchange.file.storage.composition;

import java.io.InputStream;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageFileAccess.SortDirection;
import com.openexchange.groupware.results.Delta;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tx.TransactionAware;


/**
 * {@link IDBasedFileAccess}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public interface IDBasedFileAccess extends TransactionAware {
    /**
     * Find out whether the file with a given ID exists or not.
     * @param id The ID to check for
     * @param version The version to check for
     *
     * @return true when the file exists and is readable, false otherwise.
     * @throws OXException
     */
    public boolean exists(String id, int version) throws OXException;

    /**
     * Load the metadata about a file
     * @param id The id of the file
     * @param version The version number of the file. May pass in CURRENT_VERSION to load the current version
     * @return The File Metadata
     * @throws OXException
     */
    public File getFileMetadata(String id, int version) throws OXException;

    /**
     * Save the file metadata.
     * @param document The metadata to save
     * @param sequenceNumber The sequence number to catch concurrent modification. May pass UNDEFINED_SEQUENCE_NUMBER for new files or DISTANT_FUTURE to circumvent the check
     * @throws OXException
     */
    public void saveFileMetadata(File document, long sequenceNumber) throws OXException ; // No modifiedColumns means all columns

    /**
     * Save the file metadata.
     * @param document The metadata to save
     * @param sequenceNumber The sequence number to catch concurrent modification. May pass UNDEFINED_SEQUENCE_NUMBER for new files or DISTANT_FUTURE to circumvent the check
     * @param modifiedColumns The fields to save. All other fields will be ignored
     * @throws OXException
     */
    public void saveFileMetadata(File document, long sequenceNumber, List<File.Field> modifiedColumns) throws OXException ;

    /**
     * Copy a file from the source to the destination.
     * @param sourceId The file to copy
     * @param destFolderId The folder to copy into
     * @param update Optional updates to the copy. May be null
     * @param newData Optional new binary data. May be null
     * @param The fields to use from the update.
     * @throws OXException
     */
    public String copy(String sourceId, String destFolderId, File update, InputStream newData, List<File.Field> modifiedFields) throws OXException;

    /**
     * Load the documents content
     * @param id The id of the document
     * @param version The version of the document. Pass in CURRENT_VERSION for the current version of the document.
     * @return
     * @throws OXException
     */
    public InputStream getDocument(String id, int version) throws OXException;

    /**
     * Save the file metadata and binary content
     * @param document The metadata to save
     * @param data The binary content
     * @param sequenceNumber The sequence number to catch concurrent modification. May pass UNDEFINED_SEQUENCE_NUMBER for new files or DISTANT_FUTURE to circumvent the check
     * @throws OXException
     */
    public void saveDocument(File document, InputStream data, long sequenceNumber) throws OXException ;

    /**
     * Save the file metadata.
     * @param document The metadata to save
     * @param data The binary content
     * @param sequenceNumber The sequence number to catch concurrent modification. May pass DISTANT_FUTURE to circumvent the check
     * @param modifiedColumns The fields to save. All other fields will be ignored
     * @throws OXException
     */
    public void saveDocument(File document, InputStream data, long sequenceNumber, List<File.Field> modifiedColumns) throws OXException ;

    /**
     * Remove all documents in the given folder.
     * @param folderId The folder to clear
     * @param sequenceNumber The sequence number to catch concurrent modification. May pass DISTANT_FUTURE to circumvent the check
     * @throws OXException
     */
    public void removeDocument(String folderId, long sequenceNumber) throws OXException;

    /**
     * Removes the documents with the given IDs from the folder. Documents ids that could not be removed due to an edit-delete conflict are returned.
     * @param ids TODO
     * @param sequenceNumber The sequence number to catch concurrent modification. May pass DISTANT_FUTURE to circumvent the check
     * @return
     * @throws OXException
     */
    public List<String> removeDocument(List<String> ids, long sequenceNumber) throws OXException;

    /**
     * Remove a certain version of a file
     * @param id The file id whose version is to be removed
     * @param versions The versions to be remvoed. The versions that couldn't be removed are returned again.
     * @return
     * @throws OXException
     */
    public int[] removeVersion(String id, int[] versions) throws OXException;

    /**
     * Unlocks a given file.
     * @param id The file to unlock
     * @throws OXException
     */
    public void unlock(String id) throws OXException;

    /**
     * Locks a given file for the given duration (in milliseconds)
     * @param id The file to lock
     * @param diff The duration in milliseconds
     * @throws OXException
     */
    public void lock(String id, long diff) throws OXException;

    /**
     * Updates a files sequence number
     * @param id The file whose sequence number should be updated
     * @throws OXException
     */
    public void touch(String id) throws OXException;

    /**
     * List a folders content
     * @param folderId The folder whose contents to list
     * @return
     * @throws OXException
     */
    public TimedResult<File> getDocuments(String folderId) throws OXException;

    /**
     * List a folders content loading only the columns given
     * @param folderId The folder whose contents to list
     * @param columns The fields to load
     * @return
     * @throws OXException
     */
    public TimedResult<File> getDocuments(String folderId, List<File.Field> columns) throws OXException;

    /**
     * List a folders content loading only the columns given and sorting by a certain field either ascendingly or descendingly
     * @param folderId The folder whose contents to list
     * @param columns The columns to load
     * @param sort The field to sort by
     * @param order The sorting direction
     * @return
     * @throws OXException
     */
    public TimedResult<File> getDocuments(String folderId, List<File.Field> columns, File.Field sort, SortDirection order) throws OXException;

    /**
     * List all versions of a document
     * @param id The documents id
     * @return
     * @throws OXException
     */
    public TimedResult<File> getVersions(String id) throws OXException;

    /**
     * List all versions of a document loading the given columns
     * @param id The documents id
     * @param columns The columns to load
     * @return
     * @throws OXException
     */
    public TimedResult<File> getVersions(String id, List<File.Field> columns) throws OXException;

    /**
     * List all versions of a document loading the given columns sorted according to the given field in a given order
     * @param id The documents id
     * @param columns The columns to load
     * @return
     * @throws OXException
     */
    public TimedResult<File> getVersions(String id, List<File.Field> columns, File.Field sort, SortDirection order) throws OXException;

    /**
     * Load the document metadata with the given ids
     * @param ids TODO
     * @param columns The fields to load
     * @return
     * @throws OXException
     */
    public TimedResult<File> getDocuments(List<String> ids, List<File.Field> columns) throws OXException;

    /**
     * Get changes in a given folder since a certain sequence number
     * @param folderId The folder to examine
     * @param updateSince The sequence number to check against
     * @param columns The columns to load
     * @param ignoreDeleted Whether to check for file deletion as well.
     * @return
     * @throws OXException
     */
    public Delta<File> getDelta(String folderId, long updateSince, List<File.Field> columns, boolean ignoreDeleted) throws OXException;

    /**
     * Get changes in a given folder since a certain sequence number
     * @param folderId The folder to examine
     * @param updateSince The sequence number to check against
     * @param columns The columns to load
     * @param sort The field to sort by
     * @param order The sorting direction
     * @param ignoreDeleted
     * @return
     * @throws OXException
     */
    public Delta<File> getDelta(String folderId, long updateSince, List<File.Field> columns, File.Field sort, SortDirection order, boolean ignoreDeleted) throws OXException;

    /**
     * Search for a given file.
     *
     * @param query The search query
     * @param cols Which fields to load
     * @param folderId In which folder to search. Pass ALL_FOLDERS to search in all folders.
     * @param sort Which field to sort by. May be null.
     * @param order The order in which to sort
     * @param start A start index (inclusive) for the search results. Useful for paging.
     * @param end An end index (exclusive) for the search results. Useful for paging.
     * @return
     * @throws OXException
     */
    public SearchIterator<File> search(String query, List<File.Field> cols, String folderId, File.Field sort, SortDirection order, int start, int end) throws OXException;

}

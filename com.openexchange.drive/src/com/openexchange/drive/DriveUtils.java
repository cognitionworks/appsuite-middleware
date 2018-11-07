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

package com.openexchange.drive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.drive.actions.AbstractAction;
import com.openexchange.drive.actions.DownloadFileAction;
import com.openexchange.drive.actions.EditFileAction;
import com.openexchange.drive.actions.ErrorFileAction;
import com.openexchange.drive.comparison.ServerFileVersion;
import com.openexchange.drive.internal.PathNormalizer;
import com.openexchange.drive.internal.SyncSession;
import com.openexchange.drive.management.DriveConfig;
import com.openexchange.drive.storage.DriveStorage;
import com.openexchange.drive.sync.RenameTools;
import com.openexchange.drive.sync.SimpleFileVersion;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.composition.FileID;
import com.openexchange.file.storage.composition.FolderID;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.quota.QuotaExceptionCodes;

/**
 * {@link DriveUtils}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DriveUtils {

    /**
     * Constructs the identifier for the supplied file, setting all relevant properties in the {@link FileID} object, especially the
     * folder ID field is populated with the (non-unique) identifier of the parent folder if not already done by the {@link FileID}
     * constructor.
     *
     * @param file The file to generate the ID for
     * @return The file ID
     * @throws IllegalArgumentException if the supplied file lacks the required properties
     */
    public static FileID getFileID(File file) {
        if (null == file.getId() || null == file.getFolderId()) {
            throw new IllegalArgumentException("File- and folder IDs  are required");
        }
        FileID fileID = new FileID(file.getId());
        FolderID folderID = new FolderID(file.getFolderId());
        if (null == fileID.getFolderId()) {
            fileID.setFolderId(folderID.getFolderId());
        }
        return fileID;
    }

    /**
     * Gets a value indicating whether the supplied path is invalid, i.e. it contains illegal characters or is not supported for
     * other reasons.
     *
     * @param path The path to check
     * @return <code>true</code> if the path is considered invalid, <code>false</code>, otherwise
     * @throws OXException
     */
    public static boolean isInvalidPath(String path) throws OXException {
        if (Strings.isEmpty(path)) {
            return true; // no empty paths
        }
        if (false == DriveConstants.PATH_VALIDATION_PATTERN.matcher(path).matches()) {
            return true; // no invalid paths
        }
        for (String pathSegment : DriveStorage.split(path)) {
            if (DriveConstants.MAX_PATH_SEGMENT_LENGTH < pathSegment.length()) {
                return true; // no too long paths
            }
        }
        return false;
    }

    /**
     * Gets a value indicating whether the supplied path is ignored, i.e. it is excluded from synchronization by definition.
     *
     * @param session The sync session
     * @param path The path to check
     * @return <code>true</code> if the path is considered to be ignored, <code>false</code>, otherwise
     * @throws OXException
     */
    public static boolean isIgnoredPath(SyncSession session, String path) throws OXException {
        if (DriveConstants.TEMP_PATH.equalsIgnoreCase(path)) {
            return true; // no temp path
        }
        if (DriveConfig.getInstance().getExcludedDirectoriesPattern().matcher(path).matches()) {
            return true; // no (server-side) excluded paths
        }
        List<DirectoryPattern> directoryExclusions = session.getDriveSession().getDirectoryExclusions();
        if (null != directoryExclusions && 0 < directoryExclusions.size()) {
            for (DirectoryPattern pattern : directoryExclusions) {
                if (pattern.matches(path)) {
                    return true; // no (client-side) excluded paths
                }
            }
        }
        if (session.getStorage().hasTrashFolder()) {
            FileStorageFolder trashFolder = session.getStorage().getTrashFolder();
            String trashPath = session.getStorage().getPath(trashFolder.getId());
            if (null != trashPath && trashPath.equals(path)) {
                return true; // no trash path
            }
        }
        return false;
    }

    /**
     * Gets a value indicating whether the supplied filename is invalid, i.e. it contains illegal characters or is not supported for
     * other reasons.
     *
     * @param fileName The filename to check
     * @return <code>true</code> if the filename is considered invalid, <code>false</code>, otherwise
     */
    public static boolean isInvalidFileName(String fileName) {
        if (Strings.isEmpty(fileName)) {
            return true; // no empty filenames
        }
        if (false == DriveConstants.FILENAME_VALIDATION_PATTERN.matcher(fileName).matches()) {
            return true; // no invalid filenames
        }
        if (DriveConstants.MAX_PATH_SEGMENT_LENGTH < fileName.length()) {
            return true; // no too long filenames
        }
        return false;
    }

    /**
     * Gets a value indicating whether the supplied filename is ignored, i.e. it is excluded from synchronization by definition. Only
     * static / global exclusions are considered in this check.
     *
     * @param fileName The filename to check
     * @return <code>true</code> if the filename is considered to be ignored, <code>false</code>, otherwise
     */
    public static boolean isIgnoredFileName(String fileName) {
        if (fileName.endsWith(DriveConstants.FILEPART_EXTENSION)) {
            return true; // no temporary upload files
        }
        if (DriveConfig.getInstance().getExcludedFilenamesPattern().matcher(fileName).matches()) {
            return true; // no (server-side) excluded files
        }
        return false;
    }

    /**
     * Gets a value indicating whether the supplied filename is ignored, i.e. it is excluded from synchronization by definition. Static /
     * global exclusions are considered, as well as client-defined filters based on path and filename.
     *
     * @param session The drive session
     * @param path The directory path, relative to the root directory
     * @param fileName The filename to check
     * @return <code>true</code> if the filename is considered to be ignored, <code>false</code>, otherwise
     * @throws OXException
     */
    public static boolean isIgnoredFileName(DriveSession session, String path, String fileName) throws OXException {
        if (isIgnoredFileName(fileName)) {
            return true;
        }
        List<FilePattern> fileExclusions = session.getFileExclusions();
        if (null != fileExclusions && 0 < fileExclusions.size()) {
            for (FilePattern pattern : fileExclusions) {
                if (pattern.matches(path, fileName)) {
                    return true; // no (client-side) excluded files
                }
            }
        }
        return false;
    }

    /**
     * Handles a 'quota-exceeded' situation that happened when a new file version was saved by generating an appropriate sequence of
     * actions to inform the client and instruct him to put the affected file into quarantine.
     *
     * @param session The sync session
     * @param quotaException The quota exception that occurred
     * @param path The path where the new file version was tried to be saved
     * @param originalVersion The original version if it was an update to an existing file, or <code>null</code>, otherwise
     * @param newVersion The new file version that was tried to be saved
     * @return A sequence of file actions the client should execute in order to handle the 'quota-exceeded' situation
     * @throws OXException
     */
    public static List<AbstractAction<FileVersion>> handleQuotaExceeded(SyncSession session, OXException quotaException, String path,
        FileVersion originalVersion, FileVersion newVersion) throws OXException {
        List<AbstractAction<FileVersion>> actionsForClient = new ArrayList<AbstractAction<FileVersion>>();
        /*
         * quota reached
         */
        OXException quotaReachedException = DriveExceptionCodes.QUOTA_REACHED.create(quotaException, (Object[])null);
        if (null != originalVersion) {
            /*
             * upload should have replaced an existing file, let client first rename it's file and mark as error with quarantine flag...
             */
            String alternativeName = RenameTools.findRandomAlternativeName(originalVersion.getName());
            FileVersion renamedVersion = new SimpleFileVersion(alternativeName, originalVersion.getChecksum());
            actionsForClient.add(new EditFileAction(newVersion, renamedVersion, null, path, false));
            actionsForClient.add(new ErrorFileAction(newVersion, renamedVersion, null, path, quotaReachedException, true));
            /*
             * ... then download the server version afterwards
             */
            ServerFileVersion serverFileVersion = ServerFileVersion.valueOf(originalVersion, path, session);
            actionsForClient.add(new DownloadFileAction(session, null, serverFileVersion, null, path));
        } else {
            /*
             * upload of new file, mark as error with quarantine flag
             */
            actionsForClient.add(new ErrorFileAction(null, newVersion, null, path, quotaReachedException, true));
        }
        return actionsForClient;
    }

    /**
     * Gets a value indicating whether the supplied exception indicates a 'quota-exceeded' exception or not.
     *
     * @param e The exception to check
     * @return <code>true</code> if the exception indicates a 'quota-exceeded' exception, <code>false</code>, otherwise
     */
    public static boolean indicatesQuotaExceeded(OXException e) {
        return "FLS-0024".equals(e.getErrorCode()) || FileStorageExceptionCodes.QUOTA_REACHED.equals(e) ||
            DriveExceptionCodes.QUOTA_REACHED.equals(e) || "SMARTDRIVEFILE_STORAGE-0008".equals(e.getErrorCode()) ||
            QuotaExceptionCodes.QUOTA_EXCEEDED.equals(e) || QuotaExceptionCodes.QUOTA_EXCEEDED_FILES.equals(e)
        ;
    }

    /**
     * Gets a value indicating whether the supplied exception indicates an unrecoverable failed save operation, e.g. due to an invalid
     * path name not supported by the underlying storage backend.
     *
     * @param e The exception to check
     * @return <code>true</code> if the exception indicates a failed saved exception, <code>false</code>, otherwise
     */
    public static boolean indicatesFailedSave(OXException e) {
        return "IFO-0100".equals(e.getErrorCode()) || "IFO-2103".equals(e.getErrorCode()) ||
            "FLD-0092".equals(e.getErrorCode()) || "FLD-0064".equals(e.getErrorCode())|| FileStorageExceptionCodes.DENIED_MIME_TYPE.equals(e);
    }

    /**
     * Gets a set of the normalized names of all supplied folders.
     *
     * @param folders The subfolders to get the names for
     * @return The normalied folder names
     */
    public static Set<String> getNormalizedFolderNames(Collection<FileStorageFolder> folders) {
        if (null == folders || 0 == folders.size()) {
            return Collections.emptySet();
        }
        Set<String> folderNames = new HashSet<String>(folders.size());
        for (FileStorageFolder folder : folders) {
            folderNames.add(PathNormalizer.normalize(folder.getName()));
        }
        return folderNames;
    }

    /**
     * Gets a set of the normalized names of all supplied files.
     *
     * @param file The files to get the names for
     * @param lowercase <code>true</code> to make them lowercase, <code>false</code>, otherwise
     * @return The normalized file names
     */
    public static Set<String> getNormalizedFileNames(Collection<File> files, boolean lowercase) {
        if (null == files || 0 == files.size()) {
            return Collections.emptySet();
        }
        Set<String> fileNames = new HashSet<String>(files.size());
        for (File file : files) {
            String normalizedName = PathNormalizer.normalize(file.getFileName());
            if (lowercase) {
                normalizedName = normalizedName.toLowerCase();
            }
            fileNames.add(normalizedName);
        }
        return fileNames;
    }

    /**
     * Checks that a (client-supplied) content type is allowed, throwing an appropriate exception in case validation is not passed.
     * 
     * @param contentType The content type to check
     * @return The checked content type string
     * @throws OXException {@link FileStorageExceptionCodes#DENIED_MIME_TYPE} if content type validation fails
     */
    public static String checkContentType(String contentType) throws OXException {
        if (Strings.isEmpty(contentType)) {
            return null;
        }
        ContentType checkdContentType;
        try {
            checkdContentType = new ContentType(contentType, true);
        } catch (Exception e) {
            throw FileStorageExceptionCodes.DENIED_MIME_TYPE.create(e); // MIME type could not be safely parsed
        }
        if (checkdContentType.contains("multipart/") || checkdContentType.containsBoundaryParameter()) {
            throw FileStorageExceptionCodes.DENIED_MIME_TYPE.create(); // deny weird MIME types
        }
        return checkdContentType.toString();
    }

    private DriveUtils() {
        super();
    }
}

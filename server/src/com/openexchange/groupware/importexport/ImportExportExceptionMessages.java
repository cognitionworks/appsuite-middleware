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

package com.openexchange.groupware.importexport;

import com.openexchange.exceptions.LocalizableStrings;

/**
 * {@link ImportExportExceptionMessages}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ImportExportExceptionMessages implements LocalizableStrings {

    public static final String CANNOT_EXPORT_MSG = "Could not export the folder %1$s in the format %2$s.";

    public static final String LOADING_CONTACTS_FAILED_MSG = "Could not load contacts";

    public static final String UTF8_ENCODE_FAILED_MSG = "Could not encode as UTF-8";

    public static final String NO_DATABASE_CONNECTION_MSG = "Can not get connection to database.";

    public static final String SQL_PROBLEM_MSG = "Invalid SQL Query: %s";

    public static final String LOADING_FOLDER_FAILED_MSG = "Could not load folder %s";

    public static final String ICAL_EMITTER_SERVICE_MISSING_MSG = "The necessary iCal emitter service is missing.";

    public static final String NUMBER_FAILED_MSG = "Parsing %1$s to a number failed.";

    public static final String ICAL_CONVERSION_FAILED_MSG = "Conversion to iCal failed.";

    public static final String VCARD_CONVERSION_FAILED_MSG = "Conversion to vCard failed.";

    public static final String CANNOT_IMPORT_MSG = "Can not import the format %2$s into folder %1$s.";

    public static final String CALENDAR_DISABLED_MSG = "Module calendar not enabled for user, cannot import appointments.";

    public static final String TASKS_DISABLED_MSG = "Module tasks not enabled for user, cannot import tasks.";

    public static final String ICAL_PARSER_SERVICE_MISSING_MSG = "The necessary iCal parser service is missing.";

    public static final String RESOURCE_HARD_CONFLICT_MSG = "Failed importing appointment due to hard conflicting resource.";

    public static final String WARNINGS_MSG = "Warnings when importing file: %i warnings";

    public static final String UNKNOWN_VCARD_FORMAT_MSG = "Could not recognize format of the following data: %s";

    public static final String CONTACTS_DISABLED_MSG = "Module contacts not enabled for user, cannot import contacts.";

    public static final String NO_VCARD_FOUND_MSG = "No VCard to import found.";

    public static final String VCARD_PARSING_PROBLEM_MSG = "Problem while parsing the vcard, reason: %s";

    public static final String VCARD_CONVERSION_PROBLEM_MSG = "Problem while converting the vcard to a contact, reason: %s";

    public static final String ONLY_ONE_FOLDER_MSG = "Can only import into one folder at a time.";

    public static final String NOT_FOUND_FIELD_MSG = "Could not find the following fields %s";

    public static final String NO_VALID_CSV_COLUMNS_MSG = "Could not translate a single column title. Is this a valid CSV file?";

    public static final String NO_FIELD_IMPORTED_MSG = "Could not translate a single field of information, did not insert entry %s.";

    public static final String NO_FIELD_FOR_NAMING_MSG = "No field can be found that could be used to name contacts in this file: no name, no company nor e-mail.";

    public static final String NO_FIELD_FOR_NAMING_IN_LINE_MSG = "No field was set that might give the contact in line %s a display name: no name, no company nor e-mail.";

    public static final String IOEXCEPTION_MSG = "Could not read InputStream as string";

    public static final String BROKEN_CSV_MSG = "Broken CSV file: Lines have different number of cells, line #1 has %d, line #%d has %d. Is this really a CSV file?";

    public static final String DATA_AFTER_LAST_LINE_MSG = "Illegal state: Found data after presumed last line.";

    public static final String NO_IMPORTER_MSG = "Cannot find an importer for format %s into folders %s";

    public static final String NO_EXPORTER_MSG = "Cannot find an exporter for folder %s to format %s";

    public static final String NO_TYPES_CONSTANT_MSG = "Cannot translate id=%d to a constant from Types.";

    public static final String NO_FOLDEROBJECT_CONSTANT_MSG = "Cannot translate id=%d to a constant from FolderObject.";

    public static final String ONLY_ONE_FILE_MSG = "Can only handle one file, not %s";

    public static final String UNKNOWN_FORMAT_MSG = "Unknown format: %s";

    public static final String EMPTY_FILE_MSG = "Empty file uploaded.";

    public static final String FILE_NOT_EXISTS_MSG = "The file you selected does not exist.";

    // Invalid date format detected: "%1$s". Ignoring value.
    public static final String INVALID_DATE_MSG = "Invalid date format detected: \"%1$s\". Ignoring value.";

    //  Ignoring invalid value for field "%1$s": %2$s
    public static final String IGNORE_FIELD_MSG = "Ignoring invalid value for field \"%1$s\": %2$s";

    private ImportExportExceptionMessages() {
        super();
    }
}

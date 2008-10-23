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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.admin.contextrestore.rmi.exceptions;

import java.util.IllegalFormatException;

/**
 * OXContextRestore exception class
 * 
 */
public class OXContextRestoreException extends Exception {

    /**
     * For serialization
     */
    private static final long serialVersionUID = 2597458638173191174L;
    
    public enum Code {
        /**
         * The version tables are incompatible
         */
        VERSION_TABLES_INCOMPATIBLE("The version tables are incompatible"),
        
        /**
         * No version information found in dump
         */
        NO_VERSION_INFORMATION_FOUND("No version information found in dump"),
        
        /**
         * Couldn't convert pool value
         */
        COULD_NOT_CONVERT_POOL_VALUE("Couldn't convert pool value"),
        
        /**
         * No entries in version table
         */
        NO_ENTRIES_IN_VERSION_TABLE("No entries in version table"),
        
        /**
         * Error during database operation: %s
         */
        DATABASE_OPERATION_ERROR("Error during database operation: %s"),
        
        /**
         * Error during rollback: %s
         */
        ROLLBACK_ERROR("Error during rollback: %s"),
        
        /**
         * No values found for the filestore in the database
         */
        NO_FILESTORE_VALUE("No values found for the filestore in the database"),

        /**
         * None of the files contained a configdb database dump
         */
        NO_CONFIGDB_FOUND("None of the files contained a configdb database dump, but this is essential for the restore process"),

        /**
         * None of the files contained a userdata database dump, but this is essential for the restore process
         */
        NO_USER_DATA_DB_FOUND("None of the files contained a userdata database dump, but this is essential for the restore process"),

        /**
         * One of the given filenames cannot be found
         */
        FILE_NOT_FOUND("One of the given filenames cannot be found"),
        
        /**
         * An IO Exception has occurred, see the log files for details
         */
        IO_EXCEPTION("An IO Exception has occurred, see the log files for details"),
        
        /**
         * The context id wasn't found in the context_server2db_pool table
         */
        CONTEXT_NOT_FOUND_IN_POOL_MAPPING("The context id wasn't found in the context_server2db_pool table");
        
        private final String text;

        /**
         * @param text
         */
        private Code(String text) {
            this.text = text;
        }

        public final String getText() {
            return text;
        }
    }

    private String[] msgArgs;
    
    /**
     * Default constructor
     * 
     */
    public OXContextRestoreException(final Code code) {
        super(code.getText());
    }

    /**
     * Constructor with parameters
     * 
     */
    public OXContextRestoreException(final Code code, final String... msgArgs) {
        super(code.getText());
        this.msgArgs = msgArgs;
    }

    @Override
    public String toString() {
        if (null != this.msgArgs) {
            final String message = super.getMessage();
            if (null != message) {
                try {
                    return String.format(message, (Object[]) this.msgArgs);
                } catch (final IllegalFormatException e) {
                    System.err.println("Illegal message format:" + e.getMessage());
                }
            }
            return super.toString();
        } else {
            return super.toString();
        }
    }

    @Override
    public String getMessage() {
        if (null != this.msgArgs) {
            final String message = super.getMessage();
            if (null != message) {
                try {
                    return String.format(message, (Object[]) this.msgArgs);
                } catch (final IllegalFormatException e) {
                    System.err.println("Illegal message format:" + e.getMessage());
                }
            }
            return super.getMessage();
        } else {
            return super.getMessage();
        }

    }
    
}

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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.gdpr.dataexport;


/**
 * {@link DataExportConstants} - Provides constants for data export.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class DataExportConstants {

    /**
     * Initializes a new {@link DataExportConstants}.
     */
    private DataExportConstants() {
        super();
    }

    /**
     * The minimum file size for generated result files (512 MB).
     */
    public static final long MINIMUM_FILE_SIZE = 536870912L;

    /**
     * The default value for the max. file size for generated result files (512 MB).
     */
    public static final long DFAULT_MAX_FILE_SIZE = 1073741824L;

    /**
     * The default value for max. time to live for a completed data export (<code>1209600000</code> two weeks in milliseconds).
     */
    public static final long DEFAULT_MAX_TIME_TO_LIVE = 1209600000L; // Two weeks in milliseconds

    /**
     * The default value for the frequency to check for available data export tasks (<code>300000</code> five minutes in milliseconds).
     */
    public static final long DEFAULT_CHECK_FOR_TASKS_FREQUENCY = 300000L; // 5 minutes in milliseconds

    /**
     * The default value for the frequency to check for aborted data export tasks (<code>120000</code> 2 minutes in milliseconds).
     */
    public static final long DEFAULT_CHECK_FOR_ABORTED_TASKS_FREQUENCY = 120000L; // 2 minutes in milliseconds

    /**
     * The default value for the expiration time for an in-processing data export task (<code>600000</code> 10 minutes in milliseconds).
     */
    public static final long DEFAULT_EXPIRATION_TIME = 600000L; // 10 minutes in milliseconds

    /**
     * The default value for allowed number of concurrent data export tasks (<code>1</code>).
     */
    public static final int DEFAULT_NUMBER_OF_CONCURRENT_TASKS = 1;

    /**
     * The default value for max. allowed fail count for a certain work item associated with a data export task (<code>4</code>).
     */
    public static final int DEFAULT_MAX_FAIL_COUNT_FOR_WORK_ITEM = 4;

}

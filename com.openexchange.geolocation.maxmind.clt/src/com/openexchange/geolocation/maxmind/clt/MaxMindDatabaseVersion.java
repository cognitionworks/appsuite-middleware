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

package com.openexchange.geolocation.maxmind.clt;

import com.openexchange.geolocation.clt.DatabaseVersion;

/**
 * {@link MaxMindDatabaseVersion} - Defines the amount of fields for every supported Ip2Location database
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public enum MaxMindDatabaseVersion implements DatabaseVersion {
    CITY(10, 14, "GeoLite2-City-CSV", "GeoIP2-City-CSV"),
    COUNTRY(6, 7, "GeoLite2-Country-CSV", "GeoIP2-Country-CSV");

    private final int ipBlocksNumberOfFields;
    private final int ipLocationsNumberOfFields;
    private final String liteName;
    private final String name;

    /**
     * Initialises a new {@link MaxMindDatabaseVersion}.
     */
    private MaxMindDatabaseVersion(int ipBlocksNumberOfFields, int ipLocationsNumberOfFields, String liteName, String name) {
        this.ipBlocksNumberOfFields = ipBlocksNumberOfFields;
        this.ipLocationsNumberOfFields = ipLocationsNumberOfFields;
        this.liteName = liteName;
        this.name = name;
    }

    /**
     * Returns the number of the IPBlocks' fields
     * 
     * @return the number of the IPBlocks' fields
     */
    @Override
    public int getNumberOfFields() {
        return ipBlocksNumberOfFields;
    }

    /**
     * Returns the number of fields of the IP locations file
     * 
     * @return the number of fields of the IP locations file
     */
    public int getNumberOfIPLocationsFields() {
        return ipLocationsNumberOfFields;
    }

    @Override
    public String getLiteName() {
        return liteName;
    }

    @Override
    public String getName() {
        return name;
    }
}

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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.chronos;

/**
 * {@link CalendarUser}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 * @see <a href="https://tools.ietf.org/html/rfc5545#section-3.3.3">RFC 5545, section 3.3.3</a>
 */
public class CalendarUser {

    String uri;
    String commonName;
    int entity;
    String sentBy;
    CalendarUserType cuType;

    /**
     * Gets the uri
     *
     * @return The uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the uri
     *
     * @param uri The uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Gets the commonName
     *
     * @return The commonName
     */
    public String getCommonName() {
        return commonName;
    }

    /**
     * Sets the commonName
     *
     * @param commonName The commonName to set
     */
    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    /**
     * Gets the entity
     *
     * @return The entity
     */
    public int getEntity() {
        return entity;
    }

    /**
     * Sets the entity
     *
     * @param entity The entity to set
     */
    public void setEntity(int entity) {
        this.entity = entity;
    }

    /**
     * Gets the sentBy
     *
     * @return The sentBy
     */
    public String getSentBy() {
        return sentBy;
    }

    /**
     * Sets the sentBy
     *
     * @param sentBy The sentBy to set
     */
    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    /**
     * Gets the cuType
     *
     * @return The cuType
     */
    public CalendarUserType getCuType() {
        return cuType;
    }

    /**
     * Sets the cuType
     *
     * @param cuType The cuType to set
     */
    public void setCuType(CalendarUserType cuType) {
        this.cuType = cuType;
    }

}

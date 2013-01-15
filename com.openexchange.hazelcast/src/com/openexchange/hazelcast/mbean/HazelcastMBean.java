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

package com.openexchange.hazelcast.mbean;

import java.util.List;
import javax.management.MBeanException;

/**
 * {@link HazelcastMBean}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface HazelcastMBean {

    public static final String HAZELCAST_DOMAIN = "com.openexchange.hazelcast";

    public static final String MAP_NAME = "ox-jmx-test-map";

    /**
     * Removes names element.
     *
     * @param name The name
     */
    void remove(String name);

    /**
     * Puts named element.
     *
     * @param name The name
     * @param value The value
     * @throws MBeanException If put fails
     */
    void put(String name, String value) throws MBeanException;

    /**
     * Gets names element
     *
     * @param name The name
     * @return The associated value or <code>null</code>
     */
    String get(String name);

    /**
     * Gets the list of (possible) cluster members.
     *
     * @return The members
     * @throws MBeanException
     */
    List<String> listMembers() throws MBeanException;

    /**
     * Adds a member to the list of (possible) cluster members.
     *
     * @param member The IP address or hostname of the member to add
     * @throws MBeanException
     */
    void addMember(String member) throws MBeanException;

    /**
     * Removes a member from the list of (possible) cluster members.
     *
     * @param member The IP address or hostname of the member to add
     * @throws MBeanException
     */
    void removeMember(String member) throws MBeanException;

    /**
     * Gets a list of the actual cluster members.
     *
     * @return The members
     * @throws MBeanException
     */
    List<String> listClusterMembers() throws MBeanException;

}

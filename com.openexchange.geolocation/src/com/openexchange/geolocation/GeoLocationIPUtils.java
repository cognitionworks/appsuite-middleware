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

package com.openexchange.geolocation;

import static com.openexchange.java.Autoboxing.L;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.openexchange.exception.OXException;
import com.openexchange.geolocation.exceptions.GeoLocationExceptionCodes;
import com.openexchange.geolocation.exceptions.NotConvertibleException;
import com.openexchange.java.Strings;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv6.IPv6Address;

/**
 * {@link GeoLocationIPUtils}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public final class GeoLocationIPUtils {

    /**
     * The first octet multiplier/divisor for the IP address, i.e. <code>16777216</code>
     */
    private static final long O1 = (long) Math.pow(2, 24);
    /**
     * The second octet multiplier/divisor for the IP address, i.e. <code>65536</code>
     */
    private static final long O2 = (long) Math.pow(2, 16);
    /**
     * The second octet multiplier/divisor for the IP address, i.e. <code>256</code>
     */
    private static final long O3 = (long) Math.pow(2, 8);

    /**
     * Converts the specified IPv4 address to its integer representation
     *
     * @param ipAddress The IPv4 address to convert
     * @return The number representation of the IPv4 address
     * @throws OXException if the specified IPv4 address is invalid
     * @throws NotConvertibleException if the address is IPv6 and cannot be converted to IPv4.
     */
    public static long convertIp(String ipAddress) throws OXException, NotConvertibleException {
        validate(ipAddress);
        ipAddress = convertToIPv4(ipAddress);
        String[] split = ipAddress.split("\\.");
        return Long.parseLong(split[0]) * O1 + Long.parseLong(split[1]) * O2 + Long.parseLong(split[2]) * O3 + Long.parseLong(split[3]);
    }

    /**
     * Converts the specified integer to an IP address
     *
     * @param ipAddress The integer version of the IP address
     * @return The IP address as a string
     * @throws OXException if the specified integer did not yield a valid IPv4 address
     */
    public static String convertIp(long ipAddress) throws OXException {
        long p1 = (ipAddress / O1) % O3;
        long p2 = (ipAddress / O2) % O3;
        long p3 = (ipAddress / O3) % O3;
        long p4 = ipAddress % O3;
        String ipStr = Strings.concat(".", L(p1), L(p2), L(p3), L(p4));
        validate(ipStr);
        return ipStr;
    }

    /**
     * Determines whether the specified IP address is valid
     *
     * @param ipAddress The IP address to validate
     * @throws OXException If the specified IP address is invalid
     */
    private static void validate(String ipAddress) throws OXException {
        try {
            InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            throw GeoLocationExceptionCodes.UNABLE_TO_RESOLVE_HOST.create(e, ipAddress);
        }
    }

    /**
     * Checks if the specified IP address is an IPv6 address and if possible converts it
     * to an IPv4. Otherwise, it throws an exception
     *
     * @param ipAddress The address to check and possibly convert
     * @return The IPv4 version of the specified address.
     * @throws NotConvertibleException if the address is IPv6 and cannot be converted to IPv4.
     */
    private static String convertToIPv4(String ipAddress) throws NotConvertibleException {
        IPAddressString stringAddress = new IPAddressString(ipAddress);
        if (stringAddress.isIPv4()) {
            return ipAddress;
        }
        IPAddress address = stringAddress.getAddress();
        if (address == null) {
            throw new NotConvertibleException(ipAddress);
        }
        IPv6Address ipv6 = address.toIPv6();
        if (false == ipv6.isIPv4Convertible()) {
            throw new NotConvertibleException(ipAddress);
        }
        return ipv6.toIPv4().toNormalizedString();
    }
}

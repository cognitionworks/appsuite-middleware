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

package com.openexchange.dav.mixins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.openexchange.chronos.ResourceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.dav.DAVProtocol;
import com.openexchange.group.Group;
import com.openexchange.java.Strings;
import com.openexchange.resource.Resource;
import com.openexchange.user.User;
import com.openexchange.webdav.protocol.helpers.SingleXMLPropertyMixin;

/**
 * {@link CalendarUserAddressSet}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class CalendarUserAddressSet extends SingleXMLPropertyMixin {

    private final List<String> addresses;

    /**
     * Initializes a new {@link CalendarUserAddressSet}.
     *
     * @param contextID The context identifier
     * @param user The user
     * @param configViewFactory The configuration view
     */
    public CalendarUserAddressSet(int contextID, User user, ConfigViewFactory configViewFactory) {
        this(getAddresses(contextID, user, configViewFactory));
    }

    /**
     * Initializes a new {@link CalendarUserAddressSet}.
     *
     * @param contextID The context identifier
     * @param group The group
     * @param configViewFactory The configuration view
     */
    public CalendarUserAddressSet(int contextID, Group group, ConfigViewFactory configViewFactory) {
        this(Arrays.asList(PrincipalURL.forGroup(group.getIdentifier(), configViewFactory), ResourceId.forGroup(contextID, group.getIdentifier())));
    }

    /**
     * Initializes a new {@link CalendarUserAddressSet}.
     *
     * @param contextID The context identifier
     * @param resource The resource
     * @param configViewFactory The configuration view
     */
    public CalendarUserAddressSet(int contextID, Resource resource, ConfigViewFactory configViewFactory) {
        this(getAddresses(contextID, resource, configViewFactory));
    }

    private static List<String> getAddresses(int contextID, Resource resource, ConfigViewFactory configViewFactory) {
        List<String> addresses = new ArrayList<String>(3);
        if (Strings.isNotEmpty(resource.getMail())) {
            addresses.add(CalendarUtils.getURI(resource.getMail()));
        }
        addresses.add(PrincipalURL.forResource(resource.getIdentifier(), configViewFactory));
        addresses.add(ResourceId.forResource(contextID, resource.getIdentifier()));
        return addresses;
    }

    private static List<String> getAddresses(int contextID, User user, ConfigViewFactory configViewFactory) {
        List<String> addresses = new ArrayList<String>(3);
        if (Strings.isNotEmpty(user.getMail())) {
            addresses.add(CalendarUtils.getURI(user.getMail()));
        }
        if (null != user.getAliases()) {
            for (String alias : user.getAliases()) {
                if (Strings.isNotEmpty(alias)) {
                    String address = CalendarUtils.getURI(alias);
                    if (false == addresses.contains(address)) {
                        addresses.add(address);
                    }
                }
            }
        }
        addresses.add(PrincipalURL.forUser(user.getId(), configViewFactory));
        addresses.add(ResourceId.forUser(contextID, user.getId()));
        return addresses;
    }

    /**
     * Initializes a new {@link CalendarUserAddressSet}.
     *
     * @param addresses The possible calendar user address URLs
     */
    private CalendarUserAddressSet(List<String> addresses) {
        super(DAVProtocol.CAL_NS.getURI(), "calendar-user-address-set");
        this.addresses = addresses;
    }

    @Override
    protected String getValue() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String address : addresses) {
            stringBuilder.append("<D:href>").append(address).append("</D:href>");
        }
        return stringBuilder.toString();
    }

}

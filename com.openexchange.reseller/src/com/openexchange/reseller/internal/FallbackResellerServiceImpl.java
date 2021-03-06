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

package com.openexchange.reseller.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.openexchange.exception.OXException;
import com.openexchange.reseller.ResellerService;
import com.openexchange.reseller.data.ResellerAdmin;
import com.openexchange.reseller.data.ResellerCapability;
import com.openexchange.reseller.data.ResellerConfigProperty;
import com.openexchange.reseller.data.ResellerTaxonomy;

/**
 * {@link FallbackResellerServiceImpl}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.3
 */
public class FallbackResellerServiceImpl implements ResellerService {

    private static ResellerAdmin DEFAULT;
    static {
        DEFAULT = ResellerAdmin.builder().name("default").build();
    }

    /**
     * Initialises a new {@link FallbackResellerServiceImpl}.
     */
    public FallbackResellerServiceImpl() {
        super();
    }

    @Override
    public ResellerAdmin getReseller(int cid) throws OXException {
        return DEFAULT;
    }

    @Override
    public ResellerAdmin getResellerByName(String resellerName) throws OXException {
        return DEFAULT;
    }

    @Override
    public ResellerAdmin getResellerById(int resellerId) throws OXException {
        return DEFAULT;
    }

    @Override
    public List<ResellerAdmin> getResellerAdminPath(int cid) throws OXException {
        return getAll();
    }

    @Override
    public List<ResellerAdmin> getSubResellers(int parentId) throws OXException {
        return getAll();
    }

    @Override
    public List<ResellerAdmin> getAll() {
        return ImmutableList.of(DEFAULT);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Set<ResellerCapability> getCapabilities(int resellerId) throws OXException {
        return ImmutableSet.of();
    }

    @Override
    public ResellerConfigProperty getConfigProperty(int resellerId, String key) {
        return null;
    }

    @Override
    public Map<String, ResellerConfigProperty> getAllConfigProperties(int resellerId) {
        return ImmutableMap.of();
    }

    @Override
    public Map<String, ResellerConfigProperty> getConfigProperties(int resellerId, Set<String> keys) {
        return ImmutableMap.of();
    }

    @Override
    public Set<ResellerTaxonomy> getTaxonomies(int resellerId) throws OXException {
        return ImmutableSet.of();
    }

    @Override
    public Set<ResellerCapability> getCapabilitiesByContext(int contextId) throws OXException {
        return ImmutableSet.of();
    }

    @Override
    public ResellerConfigProperty getConfigPropertyByContext(int contextId, String key) throws OXException {
        return null;
    }

    @Override
    public Map<String, ResellerConfigProperty> getAllConfigPropertiesByContext(int contextId) throws OXException {
        return ImmutableMap.of();
    }

    @Override
    public Map<String, ResellerConfigProperty> getConfigPropertiesByContext(int contextId, Set<String> keys) throws OXException {
        return ImmutableMap.of();
    }

    @Override
    public Set<ResellerTaxonomy> getTaxonomiesByContext(int contextId) throws OXException {
        return ImmutableSet.of();
    }
}

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

package com.openexchange.advertisement.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.advertisement.AdvertisementConfigService;
import com.openexchange.advertisement.AdvertisementPackageService;
import com.openexchange.advertisement.osgi.Services;
import com.openexchange.advertisement.services.TaxonomyTypesAdvertisementConfigService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Reloadable;
import com.openexchange.exception.OXException;
import com.openexchange.reseller.ResellerService;
import com.openexchange.reseller.data.ResellerAdmin;

/**
 * {@link AdvertisementPackageServiceImpl}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.3
 */
public class AdvertisementPackageServiceImpl implements AdvertisementPackageService, Reloadable {

    private static final Logger LOG = LoggerFactory.getLogger(AdvertisementConfigService.class);
    private static final String CONFIG_PREFIX = "com.openexchange.advertisement.";
    private static final String CONFIG_SUFFIX = ".packageScheme";

    private ConcurrentHashMap<String, AdvertisementConfigService> map;

    /**
     * Initializes a new {@link AdvertisementPackageServiceImpl}.
     * 
     * @param map
     */
    public AdvertisementPackageServiceImpl(ConfigurationService configService) {
        super();
        reloadConfiguration(configService);
    }

    @Override
    public AdvertisementConfigService getScheme(String reseller) {
        if (map == null) {
            return null;
        }
        return map.get(reseller);
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        ResellerService resellerService = Services.getService(ResellerService.class);
        try {
            List<ResellerAdmin> reseller = resellerService.getAll();
            map = new ConcurrentHashMap<>(reseller.size());
            for(ResellerAdmin res: reseller){
                String strScheme = configService.getProperty(CONFIG_PREFIX+res.getName()+CONFIG_SUFFIX);
                if(strScheme==null){
                    //fallback to reseller id
                    strScheme = configService.getProperty(CONFIG_PREFIX+res.getId()+CONFIG_SUFFIX);
                    
                    if(strScheme==null){
                        //fallback to gloabl
                        strScheme="Global";
                    }
                }
                
                PackageScheme scheme = PackageScheme.valueOf(strScheme);
                AdvertisementConfigService adsService = null;
                
                switch(scheme){
                    case AccessCombinations:
                    case TaxonomyTypes:
                        adsService = TaxonomyTypesAdvertisementConfigService.getInstance();
                    case Global:
                    default:
                        
                }
                map.put(res.getName(), adsService);
            }
        } catch (OXException e) {
            LOG.error("Error while reloading configuration: " + e.getMessage());
        }
    }

    @Override
    public Map<String, String[]> getConfigFileNames() {
        return null;
    }

}

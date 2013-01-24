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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.apps.manifests.json.osgi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.osgi.util.tracker.ServiceTracker;

import com.openexchange.ajax.requesthandler.osgiservice.AJAXModuleActivator;
import com.openexchange.apps.manifests.ComputedServerConfigValueService;
import com.openexchange.apps.manifests.ServerConfigMatcherService;
import com.openexchange.apps.manifests.json.ManifestActionFactory;
import com.openexchange.apps.manifests.json.values.UIVersion;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.conversion.simple.SimpleConverter;
import com.openexchange.java.Streams;
import com.openexchange.java.StringAllocator;
import com.openexchange.log.LogFactory;

/**
 * {@link ManifestJSONActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ManifestJSONActivator extends AJAXModuleActivator {
    private static final Log LOG = LogFactory.getLog(ManifestJSONActivator.class);


	@Override
	protected Class<?>[] getNeededServices() {
		return new Class<?>[]{ConfigurationService.class, CapabilityService.class, SimpleConverter.class};
	}

	@Override
	protected void startBundle() throws Exception {
	    
	    UIVersion.UIVERSION = context.getBundle().getVersion().toString();
	    
		final ServiceTracker<ServerConfigMatcherService, ServerConfigMatcherService> matcherTracker = track(ServerConfigMatcherService.class);
		final ServiceTracker<ComputedServerConfigValueService, ComputedServerConfigValueService> computedValueTracker = track(ComputedServerConfigValueService.class);

		registerModule(new ManifestActionFactory(this, readManifests(), new ServerConfigServicesLookup() {

			@Override
			public List<ServerConfigMatcherService> getMatchers() {
				List<ServerConfigMatcherService> services = new ArrayList<ServerConfigMatcherService>();
				Object[] tracked = matcherTracker.getServices();
				if (tracked == null) {
					return services;
				}
				for(Object service: tracked) {
					services.add((ServerConfigMatcherService) service);
				}
				return services;
			}

			@Override
			public List<ComputedServerConfigValueService> getComputed() {
				List<ComputedServerConfigValueService> services = new ArrayList<ComputedServerConfigValueService>();
				Object[] tracked = computedValueTracker.getServices();
				if (tracked == null) {
					return services;
				}
				for(Object service: tracked) {
					services.add((ComputedServerConfigValueService) service);
				}
				return services;
			}
		}), "apps/manifests");

		openTrackers();
	}

    private JSONArray readManifests() {
        ConfigurationService conf = getService(ConfigurationService.class);
        String property = conf.getProperty("com.openexchange.apps.manifestPath");
        if (null == property) {
            property = conf.getProperty("com.openexchange.apps.path");
            if (null == property) {
                return new JSONArray(0);
            }
            property += "/manifests";
        }

        JSONArray array = new JSONArray();
        for(String path: property.split(":")) {
            File file = new File(path);
            if (file.exists()) {
                for (File f : file.listFiles()) {
                    read(f, array);
                }
            }
        }
        return array;
    }

    private void read(File f, JSONArray array) {
        BufferedReader r = null;
        StringAllocator b = new StringAllocator();
        try {
            r = new BufferedReader(new FileReader(f));
            int c = -1;
            while ((c = r.read()) != -1) {
                b.append((char) c);
            }
            JSONArray fileContent = new JSONArray(b.toString());
            for (int i = 0, size = fileContent.length(); i < size; i++) {
                array.put(fileContent.get(i));
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            Streams.close(r);
        }
    }

}

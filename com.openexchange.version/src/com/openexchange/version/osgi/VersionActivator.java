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

package com.openexchange.version.osgi;

import java.util.Dictionary;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.version.ServerVersion;
import com.openexchange.version.Version;
import com.openexchange.version.VersionService;
import com.openexchange.version.internal.VersionServiceImpl;

/**
 * Reads version and build number from the bundle manifest.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
@SuppressWarnings("deprecation")
public class VersionActivator extends HousekeepingActivator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VersionActivator.class);

    public VersionActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return EMPTY_CLASSES;
    }

    @Override
    protected void startBundle() throws Exception {
        LOG.info("Starting bundle com.openexchange.version");
        Dictionary<String, String> headers = context.getBundle().getHeaders();
        String version = headers.get("OXVersion");
        if (null == version) {
            throw new Exception("Can not read version from bundle manifest " + context.getBundle().getSymbolicName());
        }
        String buildNumber = headers.get("OXRevision");
        if (null == buildNumber) {
            throw new Exception("Can not read buildNumber from bundle manifest.");
        }
        String date = headers.get("OXBuildDate");
        if (null == date) {
            throw new Exception("Can not read build date from bundle manifest.");
        }
        Version instance = Version.getInstance();
        instance.setNumbers(new ServerVersion(version, buildNumber));
        instance.setBuildDate(date);
        VersionService versionService = new VersionServiceImpl(date, new ServerVersion(version, buildNumber));
        registerService(VersionService.class, versionService);
        LOG.info(VersionServiceImpl.NAME + ' ' + versionService.getVersionString());
        LOG.info("(c) OX Software GmbH , Open-Xchange GmbH");

    }

    @Override
    protected void stopBundle() throws Exception {
        LOG.info("Stopping bundle com.openexchange.version");
        super.stopBundle();
    }
}

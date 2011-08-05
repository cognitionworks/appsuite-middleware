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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.mdns.internal;

import java.util.List;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import com.openexchange.exception.OXException;
import com.openexchange.mdns.MDNSService;
import com.openexchange.mdns.MDNSServiceEntry;

/**
 * {@link MDNSCommandProvider} - The {@link CommandProvider command provider} to output MDNS status.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MDNSCommandProvider implements CommandProvider {

    private final MDNSService mdnsService;

    /**
     * Initializes a new {@link MDNSCommandProvider}.
     *
     * @param registry
     */
    public MDNSCommandProvider(final MDNSService mdnsService) {
        super();
        this.mdnsService = mdnsService;
    }

    public Object _mdnsServices(final CommandInterpreter intp) {
        /*
         * Check service identifier
         */
        final String serviceId = intp.nextArgument();
        final StringBuilder sb = new StringBuilder(256);
        final List<MDNSServiceEntry> services;
        try {
            services = mdnsService.listByService(null == serviceId ? "openexchange.service.messaging" : serviceId);
        } catch (final OXException e) {
            intp.print(sb.append("Error: ").append(e.getMessage()).toString());
            return null;
        }
        sb.setLength(0);
        intp.print(sb.append("---Tracked services of \"").append(null == serviceId ? "openexchange.service.messaging" : serviceId).append(
            "\" ---\n").toString());
        for (final MDNSServiceEntry mdnsServiceEntry : services) {
            sb.setLength(0);
            sb.append("\n\tUUID: ").append(mdnsServiceEntry.getId()).append("\n\t");
            sb.append("Address: ").append(mdnsServiceEntry.getAddress()).append("\n\t");
            sb.append("Port: ").append(mdnsServiceEntry.getPort()).append('\n');
            intp.print(sb.toString());
        }
        /*
         * Return
         */
        return null;
    }

    @Override
    public String getHelp() {
        final StringBuilder builder = new StringBuilder(256).append("---Output tracked hosts of specified service---\n\t");
        builder.append("mdnsServices <service-id> - Output tracked hosts. Specify the service identifier; by default \"openexchange.service.messaging\".\n");
        return builder.toString();
    }

}

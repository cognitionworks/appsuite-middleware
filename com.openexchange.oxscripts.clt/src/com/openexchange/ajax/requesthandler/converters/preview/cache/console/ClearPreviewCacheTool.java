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

package com.openexchange.ajax.requesthandler.converters.preview.cache.console;

import java.rmi.RemoteException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import com.openexchange.ajax.requesthandler.converters.preview.cache.rmi.ResourceCacheRMIService;
import com.openexchange.auth.rmi.RemoteAuthenticator;
import com.openexchange.cli.AbstractRmiCLI;

/**
 * {@link ClearPreviewCacheTool} - Serves <code>clearpreviewcache</code> command-line tool.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public final class ClearPreviewCacheTool extends AbstractRmiCLI<Void> {

    private static final String SYNTAX = "clearpreviewcache [[[-c <contextId>] | [-a]] -A <masterAdmin | contextAdmin> -P <masterAdminPassword | contextAdminPassword> [-p <RMI-Port>] [-s <RMI-Server]] | [-h]";
    private static final String FOOTER = "The options -c/--context and -a/--all are mutually exclusive.";

    private Integer contextId;

    /**
     * Prevent instantiation from outside.
     */
    private ClearPreviewCacheTool() {
        super();
    }

    /**
     * Main method for starting from console.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        new ClearPreviewCacheTool().execute(args);
    }

    @Override
    protected void administrativeAuth(String login, String password, CommandLine cmd, RemoteAuthenticator authenticator) throws RemoteException {
        if (contextId == null) {
            authenticator.doAuthentication(login, password);
        } else {
            authenticator.doAuthentication(login, password, contextId.intValue());
        }
    }

    @Override
    protected void addOptions(Options options) {
        OptionGroup group = new OptionGroup();
        group.addOption(createArgumentOption("c", "context", "contextId", "Required. The context identifier", true));
        group.addOption(createSwitch("a", "all", "Required. The flag to signal that contexts shall be processed. Hence option -c/--context is then obsolete.", true));
        options.addOptionGroup(group);
    }

    @Override
    protected Void invoke(Options options, CommandLine cmd, String optRmiHostName) throws Exception {
        boolean error = true;
        try {
            ResourceCacheRMIService rmiService = getRmiStub(optRmiHostName, ResourceCacheRMIService.RMI_NAME);
            if (null == contextId) {
                rmiService.clear();
                System.out.println("All cache entries cleared.");
            } else {
                rmiService.clearFor(contextId.intValue());
                System.out.println("All cache entries cleared for context " + contextId.intValue());
            }
            error = false;
        } catch (RemoteException e) {
            final String errMsg = e.getMessage();
            System.out.println(errMsg == null ? "An error occurred." : errMsg);
        } catch (Exception e) {
            final String errMsg = e.getMessage();
            System.out.println(errMsg == null ? "An error occurred." : errMsg);
        } finally {
            if (error) {
                System.exit(1);
            }
        }
        return null;

    }

    @Override
    protected Boolean requiresAdministrativePermission() {
        return Boolean.TRUE;
    }

    @Override
    protected void checkOptions(CommandLine cmd) {
        if (cmd.hasOption('a')) {
            contextId = null;
            return;
        }

        if (cmd.hasOption('c')) {
            String contextVal = cmd.getOptionValue('c');
            try {
                contextId = Integer.valueOf(contextVal.trim());
            } catch (NumberFormatException e) {
                System.err.println("Cannot parse '" + contextVal + "' as a context id");
                printHelp();
                System.exit(1);
            }
            return;
        }

        System.out.println("Either parameter 'context' or parameter 'all' is required.");
        printHelp();
        System.exit(1);
    }

    @Override
    protected String getFooter() {
        return FOOTER;
    }

    @Override
    protected String getName() {
        return SYNTAX;
    }
}

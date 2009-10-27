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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.admin.contextrestore.console;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.BasicCommandlineOptions;
import com.openexchange.admin.console.CLIIllegalOptionValueException;
import com.openexchange.admin.console.CLIOption;
import com.openexchange.admin.console.CLIParseException;
import com.openexchange.admin.console.CLIUnknownOptionException;
import com.openexchange.admin.console.AdminParser.NeededQuadState;
import com.openexchange.admin.contextrestore.rmi.OXContextRestoreInterface;
import com.openexchange.admin.contextrestore.rmi.exceptions.OXContextRestoreException;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.MissingOptionException;
import com.openexchange.admin.rmi.exceptions.StorageException;

public class Restore extends BasicCommandlineOptions {
	
    CLIOption filenameOption = null;
    CLIOption dryRun = null;
    
    public static void main(final String[] args) {
        final Restore restore = new Restore();
        restore.start(args);
    }

    protected final void setOptions(final AdminParser parser) {
        setDefaultCommandLineOptions(parser);
        filenameOption = setShortLongOpt(parser, 'f', "filename","Comma-separated list of filenames with full path", true, NeededQuadState.needed);
        dryRun = setShortLongOpt(parser, 'n', "dry-run","Activate this option if do not want to apply the changes to the database", false, NeededQuadState.needed);
    }

    public Restore() {
    }
    
    public void start(final String[] args) {
        final AdminParser parser = new AdminParser("restorecontext");    
        
        setOptions(parser);
        
        // parse the command line
        try {
            parser.ownparse(args);
            
            final Context ctx = contextparsing(parser);
            
            final Credentials auth = credentialsparsing(parser);
            
            // get rmi ref
            final OXContextRestoreInterface oxrestore = getContextRestoreInterface();

            
            final String filenames = (String) parser.getOptionValue(filenameOption);

            final String[] filenamearray = filenames.split(",");
            if (null == parser.getOptionValue(dryRun)) {
                System.out.println(oxrestore.restore(ctx, filenamearray, auth, false));
            } else {
                System.out.println(oxrestore.restore(ctx, filenamearray, auth, true));
            }
            
            sysexit(0);
        } catch (CLIParseException e) {
            printError("Unable to parse the command line: " + e.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_UNABLE_TO_PARSE);
        } catch (final CLIIllegalOptionValueException e) {
            printError("Illegal option value : " + e.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_ILLEGAL_OPTION_VALUE);
        } catch (final CLIUnknownOptionException e) {
            printError("Unrecognized options on the command line: " + e.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_UNKNOWN_OPTION);
        } catch (final MissingOptionException e) {
            printError(e.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_MISSING_OPTION);
        } catch (final MalformedURLException e) {
            printServerException(e, parser);
            sysexit(1);
        } catch (final RemoteException e) {
            printServerException(e, parser);
            sysexit(SYSEXIT_REMOTE_ERROR);
        } catch (final NotBoundException e) {
            printServerException(e, parser);
            sysexit(1);
        } catch (final InvalidDataException e) {
            printServerException(e, parser);
            sysexit(SYSEXIT_INVALID_DATA);
        } catch (final InvalidCredentialsException e) {
            printServerException(e, parser);
            sysexit(SYSEXIT_INVALID_CREDENTIALS);
        } catch (final StorageException e) {
            printServerException(e, parser);
            sysexit(SYSEXIT_SERVERSTORAGE_ERROR);
        } catch (final OXContextRestoreException e) {
            printServerException(e, parser);
            sysexit(1);
        } catch (final DatabaseUpdateException e) {
            printServerException(e, parser);
            sysexit(1);
        }
    }

    private OXContextRestoreInterface getContextRestoreInterface() throws MalformedURLException, RemoteException, NotBoundException {
        return (OXContextRestoreInterface) Naming.lookup(RMI_HOSTNAME + OXContextRestoreInterface.RMI_NAME);
    }

}

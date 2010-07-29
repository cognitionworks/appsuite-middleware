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
package com.openexchange.admin.console.context;

import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import au.com.bytecode.opencsv.CSVReader;
import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.AdminParser.NeededQuadState;
import com.openexchange.admin.console.context.extensioninterfaces.ContextConsoleCreateInterface;
import com.openexchange.admin.rmi.OXLoginInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.dataobjects.UserModuleAccess;
import com.openexchange.admin.rmi.exceptions.ContextExistsException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.StorageException;

public abstract class CreateCore extends ContextAbstraction {

    protected void setOptions(final AdminParser parser) {
        setCsvImport(parser);
        setDefaultCommandLineOptions(parser);
        setContextNameOption(parser, NeededQuadState.notneeded);
        setMandatoryOptions(parser);
        
        setLanguageOption(parser);
        setTimezoneOption(parser);

        setContextQuotaOption(parser, true);
        
        setFurtherOptions(parser);
    }
    
    protected final void commonfunctions(final AdminParser parser, final String[] args) {
        setOptions(parser);
        setExtensionOptions(parser, ContextConsoleCreateInterface.class);

        try {
            Context ctx = null;
            Credentials auth = null;
            // create user obj
            User usr = null;
            try {
                parser.ownparse(args);
                
                ctx = contextparsing(parser);
                
                parseAndSetContextName(parser, ctx);
                
                auth = credentialsparsing(parser);
                
                usr = new User();
                
                // fill user obj with mandatory values from console
                parseAndSetMandatoryOptionsinUser(parser, usr);
                // fill user obj with mandatory values from console
                final String tz = (String) parser.getOptionValue(this.timezoneOption);
                if (null != tz) {
                    usr.setTimezone(tz);
                }
                
                final String languageoptionvalue = (String) parser.getOptionValue(this.languageOption);
                if (languageoptionvalue != null) {
                    usr.setLanguage(languageoptionvalue);
                }
                
                parseAndSetContextQuota(parser, ctx);
                
                parseAndSetExtensions(parser, ctx, auth);
            } catch (final RuntimeException e) {
                printError(null, null, e.getClass().getSimpleName() + ": " + e.getMessage(), parser);
                sysexit(1);
            }
            final String filename = (String) parser.getOptionValue(parser.getCsvImportOption());

            if (null != filename) {
                csvparsing(filename, auth);
            } else {
                ctxid = maincall(parser, ctx, usr, auth).getId();
            }
        } catch (final Exception e) {
            printErrors((null != ctxid) ? String.valueOf(ctxid) : null, null, e, parser);
        }

        try {
            displayCreatedMessage((null != ctxid) ? String.valueOf(ctxid) : null, null, parser);
        } catch (final RuntimeException e) {
            printError(null, null, e.getClass().getSimpleName() + ": " + e.getMessage(), parser);
            sysexit(1);
        }
        sysexit(0);
    }

    protected void csvparsing(final String filename, final Credentials auth) throws NotBoundException, IOException, InvalidDataException, ParseException, StorageException, InvalidCredentialsException {
        // First check if we can login with the given credentials. Otherwise there's no need to continue
        final OXLoginInterface oxlgn = (OXLoginInterface) Naming.lookup(RMI_HOSTNAME +OXLoginInterface.RMI_NAME);
        oxlgn.login(auth);
        
        final CSVReader reader = new CSVReader(new FileReader(filename), ',', '"');
        int[] idarray = csvParsingCommon(filename, reader);
        int linenumber = 2;
        String [] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            // nextLine[] is an array of values from the line
            final Context context = getContext(nextLine, idarray);
            final User adminuser = getUser(nextLine, idarray);
            final int i = idarray[AccessCombinations.ACCESS_COMBI_NAME.getIndex()];
            try {
                final Context createdCtx;
                if (-1 != i) {
                    // create call
                    createdCtx = simpleMainCall(context, adminuser, nextLine[i], auth);
                } else {
                    final UserModuleAccess moduleacess = getUserModuleAccess(nextLine, idarray);
                    if (!NO_RIGHTS_ACCESS.equals(moduleacess)) {
                        // with module access
                        createdCtx = simpleMainCall(context, adminuser, moduleacess, auth);
                    } else {
                        // without module access
                        createdCtx = simpleMainCall(context, adminuser, auth);
                    }
                    
                }
                System.out.println("Context " + createdCtx.getId() + " successfully created");
            } catch (final StorageException e) {
                System.err.println("Failed to create context " + getContextIdOrLine(context, linenumber) + ": " + e);
            } catch (final InvalidCredentialsException e) {
                System.err.println("Failed to create context " + getContextIdOrLine(context, linenumber) + ": " + e);
            } catch (final ContextExistsException e) {
                System.err.println("Failed to create context " + getContextIdOrLine(context, linenumber) + ": " + e);
            }
            linenumber++;
        }

    }

    public void prepareConstantsMap() {
        super.prepareConstantsMap();
        for (final ContextConstants value : ContextConstants.values()) {
            this.constantsMap.put(value.getString(), value);
        }
    }

    /**
     * Returns the length of all constants
     * 
     * @return
     */
    protected int getConstantsLength() {
        return super.getConstantsLength() + ContextConstants.values().length;
    }

    private String getContextIdOrLine(final Context context, final int linenumber) {
        final Integer id = context.getId();
        if (null != id) {
            return id.toString();
        } else {
            return "in line " + linenumber;
        }
    }

    protected abstract Context simpleMainCall(final Context ctx, final User usr, final String accessCombiName, final Credentials auth) throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, ContextExistsException; 
    
    protected abstract Context simpleMainCall(final Context ctx, final User usr, final UserModuleAccess access, final Credentials auth) throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, ContextExistsException; 
    
    protected abstract Context simpleMainCall(final Context ctx, final User usr, final Credentials auth) throws MalformedURLException, RemoteException, NotBoundException, StorageException, InvalidCredentialsException, InvalidDataException, ContextExistsException; 
    
    protected abstract Context maincall(final AdminParser parser, Context ctx, User usr, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, InvalidDataException, MalformedURLException, NotBoundException, ContextExistsException, NoSuchContextException;
        
    protected abstract void setFurtherOptions(final AdminParser parser);

    public void checkRequired(final int[] idarray) throws InvalidDataException {
        super.checkRequired(idarray);
        for (final ContextConstants value : ContextConstants.values()) {
            if (value.isRequired()) {
                if (-1 == idarray[value.getIndex()]) {
                    throw new InvalidDataException("The required column \"" + value.getString() + "\" is missing");
                }
            }
        }

    }
    
}

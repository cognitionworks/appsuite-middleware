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

package com.openexchange.admin.reseller.console;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.CmdLineParser.IllegalOptionValueException;
import com.openexchange.admin.console.CmdLineParser.UnknownOptionException;
import com.openexchange.admin.reseller.rmi.OXResellerInterface;
import com.openexchange.admin.reseller.rmi.dataobjects.ResellerAdmin;
import com.openexchange.admin.reseller.rmi.exceptions.OXResellerException;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.MissingOptionException;
import com.openexchange.admin.rmi.exceptions.StorageException;


/**
 * @author choeger
 *
 */
public class Delete extends ResellerAbstraction {

    protected final void setOptions(final AdminParser parser) {
        setNameAndIdOptions(parser);
    }

    /**
     * 
     */
    public Delete() {
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        final Delete delete = new Delete();
        delete.start(args);
    }

    public void start(final String[] args) {
        final AdminParser parser = new AdminParser("deleteadmin");    
        
        setOptions(parser);

        
        // parse the command line
        try {
            parser.ownparse(args);

            final Credentials auth = credentialsparsing(parser);
            final ResellerAdmin adm = parseDeleteOptions(parser);

            final OXResellerInterface rsi = getResellerInterface();
        
            rsi.delete(adm, auth);
            
        } catch (IllegalOptionValueException e) {
            printError("Illegal option value : " + e.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_ILLEGAL_OPTION_VALUE);
        } catch (UnknownOptionException e) {
            printError("Unrecognized options on the command line: " + e.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_UNKNOWN_OPTION);
        } catch (MissingOptionException e) {
            printError(e.getMessage(), parser);
            parser.printUsage();
            sysexit(SYSEXIT_MISSING_OPTION);
        } catch (MalformedURLException e) {
            printServerException(e, parser);
            sysexit(1);
        } catch (RemoteException e) {
            printServerException(e, parser);
            sysexit(SYSEXIT_REMOTE_ERROR);
        } catch (NotBoundException e) {
            printServerException(e, parser);
            sysexit(1);
        } catch (InvalidDataException e) {
            printServerException(e, parser);
            sysexit(SYSEXIT_INVALID_DATA);
        } catch (StorageException e) {
            printServerException(e, parser);
            sysexit(SYSEXIT_SERVERSTORAGE_ERROR);
        } catch (InvalidCredentialsException e) {
            printServerException(e, parser);
            sysexit(SYSEXIT_INVALID_CREDENTIALS);
        } catch (OXResellerException e) {
            printServerException(e, parser);
            sysexit(1);
        }
    }
}

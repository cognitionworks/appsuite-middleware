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
package com.openexchange.admin.lib.console.util.filestore;

import java.net.URISyntaxException;
import java.rmi.Naming;
import java.util.ArrayList;
import com.openexchange.admin.lib.console.AdminParser;
import com.openexchange.admin.lib.rmi.OXUtilInterface;
import com.openexchange.admin.lib.rmi.dataobjects.Credentials;
import com.openexchange.admin.lib.rmi.dataobjects.Filestore;
import com.openexchange.admin.lib.rmi.exceptions.InvalidDataException;

/**
 * 
 * @author d7,cutmasta
 * 
 */
public class ListFilestore extends FilestoreAbstraction {

    public ListFilestore(final String[] args2) {

        final AdminParser parser = new AdminParser("listfilestore");

        setOptions(parser);
        setCSVOutputOption(parser);
        try {
            parser.ownparse(args2);

            final Credentials auth = credentialsparsing(parser);

            // get rmi ref
            final OXUtilInterface oxutil = (OXUtilInterface) Naming.lookup(RMI_HOSTNAME +OXUtilInterface.RMI_NAME);

            String searchpattern = "*";
            if (parser.getOptionValue(this.searchOption) != null) {
                searchpattern = (String) parser.getOptionValue(this.searchOption);
            }
            // Setting the options in the dataobject

            final Filestore[] filestores = oxutil.listFilestore(searchpattern, auth);

            if (null != parser.getOptionValue(this.csvOutputOption)) {
                precsvinfos(filestores);
            } else {
                sysoutOutput(filestores);
            }

            sysexit(0);
        } catch (final Exception e) {
            printErrors(null, ctxid, e, parser);
        }
    }

    private void sysoutOutput(final Filestore[] filestores) throws InvalidDataException, URISyntaxException {
        final ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
        for (final Filestore filestore : filestores) {
            data.add(makeCSVData(filestore, false));
        }
        
        //doOutput(new String[] { "3r", "35l", "7r", "8r", "7r", "7r", "7r" },
        doOutput(new String[] { "r", "l", "r", "r", "r", "r", "r" },
                 new String[] { "id", "path", "size", "reserved", "used", "maxctx", "curctx" }, data);
    }

    private void precsvinfos(final Filestore[] filestores) throws URISyntaxException, InvalidDataException {
        // needed for csv output, KEEP AN EYE ON ORDER!!!
        final ArrayList<String> columns = new ArrayList<String>();
        columns.add("id");
        columns.add("uri");
        columns.add("size");
        columns.add("reserved");
        columns.add("used");
        columns.add("maxcontexts");
        columns.add("currentcontexts");
        // Needed for csv output
        final ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();

        for (final Filestore filestore : filestores) {
            data.add(makeCSVData(filestore, true));
        }

        doCSVOutput(columns, data);
    }

    public static void main(final String args[]) {
        new ListFilestore(args);
    }

    private void setOptions(final AdminParser parser) {
        setDefaultCommandLineOptionsWithoutContextID(parser);
        setSearchOption(parser);
    }

    private ArrayList<String> makeCSVData(final Filestore fstore, final boolean csv) throws URISyntaxException {
        final ArrayList<String> rea_data = new ArrayList<String>();

        rea_data.add(fstore.getId().toString());

        rea_data.add(fstore.getUrl());

        if (fstore.getSize() != null) {
            rea_data.add(fstore.getSize().toString());
        } else {
            rea_data.add(null);
        }

        if (fstore.getReserved() != null) {
            rea_data.add(fstore.getReserved().toString());
        } else {
            rea_data.add(null);
        }
        
        if (fstore.getUsed() != null) {
            rea_data.add(fstore.getUsed().toString());
        } else {
            rea_data.add(null);
        }
        
        if (fstore.getMaxContexts() != null) {
            rea_data.add(fstore.getMaxContexts().toString());
        } else {
            rea_data.add(null);
        }

        if (fstore.getCurrentContexts() != null) {
            rea_data.add(fstore.getCurrentContexts().toString());
        } else {
            rea_data.add(null);
        }
        
        return rea_data;
    }
    
    @Override
    protected final String getObjectName() {
        return "filestores";
    }
}

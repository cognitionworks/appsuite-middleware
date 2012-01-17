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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.admin.usermove.console;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.AdminParser.NeededQuadState;
import com.openexchange.admin.console.CLIOption;
import com.openexchange.admin.console.ObjectNamingAbstraction;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.usermove.rmi.OXUserMoveInterface;

public class UserMove extends ObjectNamingAbstraction {

    protected static final char OPT_NAME_FROM_CONTEXT_SHORT='c';
    protected static final String OPT_NAME_FROM_CONTEXT_LONG="srccontextid";
    protected static final String OPT_NAME_FROM_CONTEXT_DESCRIPTION="The id of the source context";
    protected static final char OPT_NAME_TO_CONTEXT_SHORT='d';
    protected static final String OPT_NAME_TO_CONTEXT_LONG="destcontextid";
    protected static final String OPT_NAME_TO_CONTEXT_DESCRIPTION="The id of the destination context";
    protected static final char OPT_NAME_USER_SHORT='u';
    protected static final String OPT_NAME_USER_LONG="userid";
    protected static final String OPT_NAME_USER_DESCRIPTION="The id of the user which should be moved";

    protected CLIOption fromContextOption = null;
    protected CLIOption toContextOption = null;
    protected CLIOption userOption = null;

    @Override
    protected String getObjectName() {
        return "user";
    }

    protected OXUserMoveInterface getUserMoveInterface() throws MalformedURLException, RemoteException, NotBoundException{
        return (OXUserMoveInterface) Naming.lookup(RMI_HOSTNAME + OXUserMoveInterface.RMI_NAME);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        final UserMove usermove = new UserMove();
        usermove.start(args);
    }

    public void start(final String[] args) {
        final AdminParser parser = new AdminParser("usermove");

        setOptions(parser);


        // parse the command line
        try {
            parser.ownparse(args);

            final Credentials auth = credentialsparsing(parser);
            final OXUserMoveInterface rsi = getUserMoveInterface();

            final User user = userParsing(parser, userOption);
            final Context src = contextParsing(parser, fromContextOption);
            final Context dest = contextParsing(parser, toContextOption);

            final User result = rsi.moveUser(user, src, dest, auth);
            displayMovedMessage(user.getId(), result.getId(), src.getId(), dest.getId(), parser);
            sysexit(0);
        } catch (final Exception e) {
            printErrors(null, null, e, parser);
            sysexit(1);
        }
    }

    protected final void displayMovedMessage(final Integer srcuserid, final Integer destuserid, final Integer srcctxid, final Integer destcontext, final AdminParser parser) {
        final StringBuilder sb = new StringBuilder(getObjectName());
        if (null != srcuserid) {
            sb.append(" ");
            sb.append(srcuserid);
        }
        sb.append(" moved");
        if (null != srcctxid) {
            sb.append(" from context ");
            sb.append(srcctxid);
        }
        if (null != destcontext) {
            sb.append(" to context ");
            sb.append(destcontext);
        }
        if (null != destuserid) {
            sb.append(" with new user id ");
            sb.append(destuserid);
        }
        if( null != parser && parser.checkNoNewLine()) {
            final String output = sb.toString().replace("\n", "");
            System.out.println(output);
        } else {
            System.out.println(sb.toString());
        }

    }


    private final Context contextParsing(final AdminParser parser, final CLIOption option) {
        final Context ctx = new Context();

        if (parser.getOptionValue(option) != null) {
            final Integer contextId = Integer.valueOf((String) parser.getOptionValue(option));
            ctx.setId(contextId);
        }
        return ctx;
    }

    private final User userParsing(final AdminParser parser, final CLIOption option) {
        final User user = new User();

        if (parser.getOptionValue(option) != null) {
            final Integer userid = Integer.valueOf((String) parser.getOptionValue(option));
            user.setId(userid);
        }
        return user;
    }


    private void setOptions(AdminParser parser) {
        setDefaultCommandLineOptionsWithoutContextID(parser);
        this.fromContextOption = setShortLongOpt(parser,OPT_NAME_FROM_CONTEXT_SHORT, OPT_NAME_FROM_CONTEXT_LONG, OPT_NAME_FROM_CONTEXT_DESCRIPTION, true, NeededQuadState.needed);
        this.toContextOption = setShortLongOpt(parser,OPT_NAME_TO_CONTEXT_SHORT, OPT_NAME_TO_CONTEXT_LONG, OPT_NAME_TO_CONTEXT_DESCRIPTION, true, NeededQuadState.needed);
        this.userOption = setShortLongOpt(parser,OPT_NAME_USER_SHORT, OPT_NAME_USER_LONG, OPT_NAME_USER_DESCRIPTION, true, NeededQuadState.needed);
    }


}

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

package com.openexchange.index.solr.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import com.openexchange.exception.OXException;
import com.openexchange.index.solr.IndexServer;


/**
 * {@link IndexTestTool}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class IndexTestTool {

    
    public static IndexServer createIndexServer() throws OXException {
        final IndexServer indexServer = new IndexServer();
        indexServer.setUrl("http://1.2.3.4:8005");
        indexServer.setConnectionTimeout(23);
        indexServer.setMaxConnectionsPerHost(54);
        indexServer.setSoTimeout(46);

        return indexServer;
    }
    
    public static void createInactiveCoreEntry(final Connection con, final int cid, final int uid, final int module) throws Exception {
        PreparedStatement stmt = null;
        stmt = con.prepareStatement("INSERT INTO solrCores (cid, uid, module, active, core, server) VALUES (?, ?, ?, ?, ?, ?)");
        int i = 1;
        stmt.setInt(i++, cid);
        stmt.setInt(i++, uid);
        stmt.setInt(i++, module);
        stmt.setInt(i++, 0);
        stmt.setNull(i++, java.sql.Types.VARCHAR);
        stmt.setNull(i, java.sql.Types.INTEGER);
        stmt.executeUpdate();
    }
    
    public static void createIndexFileEntry(final Connection con, final int cid, final int uid, final int module, final String indexFile) throws Exception {
        PreparedStatement stmt = null;
        stmt = con.prepareStatement("INSERT INTO solrIndexFiles (cid, uid, module, indexFile) VALUES (?, ?, ?, ?)");
        int i = 1;
        stmt.setInt(i++, cid);
        stmt.setInt(i++, uid);
        stmt.setInt(i++, module);
        stmt.setString(i, indexFile);
        stmt.executeUpdate();
    }

}

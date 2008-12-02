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
package com.openexchange.groupware.update.tasks;

import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.groupware.update.Schema;
import com.openexchange.groupware.update.exception.Classes;
import com.openexchange.groupware.update.exception.UpdateExceptionFactory;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.OXExceptionSource;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.OXThrowsMultiple;
import com.openexchange.database.Database;
import com.openexchange.tools.update.Tools;
import com.openexchange.tools.update.ForeignKey;
import com.openexchange.tools.sql.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
@OXExceptionSource(classId = Classes.UPDATE_TASK, component = EnumComponent.UPDATE)
public class ClearOrphanedInfostoreDocuments implements UpdateTask {

    private static final Log LOG = LogFactory.getLog(ClearOrphanedInfostoreDocuments.class);
  private static final UpdateExceptionFactory EXCEPTION = new UpdateExceptionFactory(ClearOrphanedInfostoreDocuments.class);


    public int addedWithVersion() {
        return 0;  //Todo
    }

    public int getPriority() {
        return 0;  //Todo
    }

     @OXThrowsMultiple(category = { AbstractOXException.Category.CODE_ERROR },
        desc = { "" },
        exceptionId = { 1 },
        msg = { "An SQL error occurred: %1$s." }
    )
    public void perform(Schema schema, int contextId) throws AbstractOXException {
        PreparedStatement select = null;
        PreparedStatement delete = null;
        PreparedStatement addKey = null;
        Connection con = null;
        ResultSet rs = null;
        try {
            LOG.info("Clearing orphaned infostore document versions");
            con = Database.get(contextId, true);

            List<ForeignKey> keys = ForeignKey.getForeignKeys(con, "infostore_document");
            ForeignKey fk = new ForeignKey("infostore_document", "infostore_id", "infostore", "id");
          
            if( keys.contains(fk)) {
                LOG.info("Foreign Key "+fk+" exists. Skipping Update Task.");
                return;
            }

            con.setAutoCommit(false);
            select = con.prepareStatement("SELECT doc.cid, doc.infostore_id, doc.version_number, doc.file_store_location FROM infostore_document AS doc LEFT JOIN infostore AS info ON info.cid = doc.cid AND info.id = doc.infostore_id WHERE info.id IS NULL");
            delete  = con.prepareStatement("DELETE FROM infostore_document WHERE cid = ? AND infostore_id = ? AND version_number = ?");
            addKey = con.prepareStatement("ALTER TABLE infostore_document ADD FOREIGN KEY (cid, infostore_id) REFERENCES infostore (cid, id)");
            rs = select.executeQuery();

            int counter = 0;
            while(rs.next()) {
                int cid = rs.getInt(1);
                int id = rs.getInt(2);
                int version = rs.getInt(3);
                String fileStoreLocation = rs.getString(4);

                delete.setInt(1, cid);
                delete.setInt(2, id);
                delete.setInt(3, version);
                delete.executeUpdate();

                Tools.removeFile(cid, fileStoreLocation, con);
                counter++;
            }
            LOG.info("Cleared "+counter+" orphaned documents");

            LOG.info("Adding foreign key: "+fk);

            // Need one with CID as well, so FK system doesn't work.
            addKey.executeUpdate();

            LOG.info("Clearing orphaned documents from del_infostore_document table.");

            select.close();
            delete.close();
            addKey.close();

            select = con.prepareStatement("SELECT doc.cid, doc.infostore_id, doc.version_number FROM del_infostore_document AS doc LEFT JOIN del_infostore AS info ON info.cid = doc.cid AND info.id = doc.infostore_id WHERE info.id IS NULL");
            delete  = con.prepareStatement("DELETE FROM del_infostore_document WHERE cid = ? AND infostore_id = ? AND version_number = ?");
            addKey = con.prepareStatement("ALTER TABLE del_infostore_document ADD FOREIGN KEY (cid, infostore_id) REFERENCES del_infostore (cid, id)");
            rs.close();

            rs = select.executeQuery();
            
            counter = 0;
            while(rs.next()) {
                int cid = rs.getInt(1);
                int id = rs.getInt(2);
                int version = rs.getInt(3);
            
                delete.setInt(1, cid);
                delete.setInt(2, id);
                delete.setInt(3, version);
                delete.executeUpdate();
                counter++;
            }
            LOG.info("Cleared "+counter+" orphaned documents in del_tables");

            LOG.info("Adding foreign key: "+new ForeignKey("del_infostore_document", "infostore_id", "del_infostore", "id"));

            addKey.executeUpdate();

            con.commit();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException e1) {
                // IGNORE
            }
            LOG.error(e.getMessage(),e);
            throw EXCEPTION.create(1, e.toString());
        } finally {
            DBUtils.closeSQLStuff(rs, select);
            DBUtils.closeSQLStuff(null, delete);
            DBUtils.closeSQLStuff(null, addKey);
            DBUtils.autocommit(con);
            Database.back(contextId, true, con);
            
        }
    }
}

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

package com.openexchange.groupware.update.tasks;

import static com.openexchange.tools.sql.DBUtils.closeSQLStuff;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.update.Schema;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.sql.DBUtils;
import com.openexchange.tools.update.Tools;

/**
 * Remove orphaned links and attachments for contacts.
 * @author <a href="mailto:ben.pahne@open-xchange.com">Ben Pahne</a>
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ContactsRepairLinksAttachments implements UpdateTask {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ContactsRepairLinksAttachments.class);

    public ContactsRepairLinksAttachments() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int addedWithVersion() {
        return 17;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return UpdateTask.UpdateTaskPriority.NORMAL.priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perform(final Schema schema, final int contextId) throws OXException {
        final Connection con = Database.getNoTimeout(contextId, true);
        try {
            con.setAutoCommit(false);
            correctContacts(con);
            correctLinks(con);
            correctAttachments(con);
            con.commit();
        } catch (final SQLException e) {
            DBUtils.rollback(con);
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            DBUtils.autocommit(con);
            if (con != null) {
                Database.backNoTimeout(contextId, true, con);
            }
        }
    }

    private void correctContacts(final Connection con) throws SQLException {
        final String sql = "SELECT c.intfield01,c.cid,c.pflag FROM prg_contacts c LEFT JOIN oxfolder_tree f ON c.fid=f.fuid AND c.cid=f.cid "
            + "WHERE f.fuid is NULL";
        Statement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.createStatement();
            result = stmt.executeQuery(sql);
            Context ctx = null;
            while (result.next()) {
                boolean delete = false;
                int pos = 1;
                final int id = result.getInt(pos++);
                final int cid = result.getInt(pos++);
                final int pflag = result.getInt(pos++);
                try {
                    ctx = ContextStorage.getInstance().loadContext(cid);
                    if (pflag == 0) {
                        try {
                            moveContactToAdmin(con, ctx, id);
                        } catch (final OXException e) {
                            LOG.info("Failed moving contact {} to admin in context {}. Removing contact.", id, cid, e);
                            delete = true;
                        } catch (final Exception e) {
                            LOG.info("Failed moving contact {} to admin in context {}. Removing contact.", id, cid, e);
                            delete = true;
                        }
                    } else {
                        LOG.info("Removing private contact {} in context {} because its folder does not exist anymore.", id, cid);
                        delete = true;
                    }
                } catch (final OXException ce) {
                    LOG.info("Removing contact {} in context {} because context does not exist anymore.", id, cid);
                    delete = true;
                }
                if (delete) {
                    deleteContact(con, cid, id);
                }
            }
        } finally {
            closeSQLStuff(result, stmt);
        }
    }

    private void moveContactToAdmin(final Connection con, final Context ctx, final int id) throws SQLException, OXException {
        String sql = "UPDATE prg_contacts SET changed_from = ?, created_from = ?, changing_date = ?, fid = ? "
            + "WHERE intfield01 = ? and cid = ?";
        int folderId = new OXFolderAccess(con, ctx).getDefaultFolder(ctx.getMailadmin(), FolderObject.CONTACT).getObjectID();
        PreparedStatement stmt = null;
        try {
            LOG.info("Trying to move contact {} to admin in context {}.", id, ctx.getContextId());
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, ctx.getMailadmin());
            stmt.setInt(2, ctx.getMailadmin());
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setInt(4, folderId);
            stmt.setInt(5, id);
            stmt.setInt(6, ctx.getContextId());
            stmt.execute();
        } finally {
            closeSQLStuff(null, stmt);
        }
    }

    private void deleteContact(final Connection con, final int cid, final int id) throws SQLException {
        final String sql = "DELETE FROM prg_contacts WHERE cid=? AND intfield01=?";
        PreparedStatement stmt2 = null;
        try {
            stmt2 = con.prepareStatement(sql);
            stmt2.setInt(1, cid);
            stmt2.setInt(2, id);
            stmt2.execute();
        } finally {
            closeSQLStuff(null, stmt2);
        }
    }

    public boolean checkContactExistence(final Connection con, final int id,
        final int cid) throws SQLException {
        final String sql = "SELECT intfield01 FROM prg_contacts WHERE cid=? AND intfield01=?";
        ResultSet result = null;
        PreparedStatement ps = null;
        boolean exists = false;
        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, cid);
            ps.setInt(2, id);
            result = ps.executeQuery();
            while (result.next()) {
                final int chk = result.getInt(1);
                if (chk == id) {
                    exists = true;
                }
            }
        } finally {
            closeSQLStuff(result, ps);
        }
        return exists;
    }

    public void correctLinks(final Connection con) throws SQLException {
        final String sql = "SELECT firstid,firstmodule,secondid,secondmodule,cid "
            + "FROM prg_links WHERE firstmodule=? OR secondmodule=?";
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, Types.CONTACT);
            ps.setInt(2, Types.CONTACT);
            result = ps.executeQuery();
            while (result.next()) {
                int pos = 1;
                final int id1 = result.getInt(pos++);
                final int mod1 = result.getInt(pos++);
                final int id2 = result.getInt(pos++);
                final int mod2 = result.getInt(pos++);
                final int cid = result.getInt(pos++);
                boolean deleteit = false;
                if (mod1 == Types.CONTACT) {
                    if (!checkContactExistence(con, id1, cid)) {
                        deleteit = true;
                    }
                }
                if (mod2 == Types.CONTACT && !deleteit) {
                    if (!checkContactExistence(con, id2, cid)) {
                        deleteit = true;
                    }
                }
                if (deleteit) {
                    deleteLink(cid, con, id1, id2, mod1, mod2);
                }
            }
        } finally {
            closeSQLStuff(result, ps);
        }
    }

    private void deleteLink(final int cid, final Connection con,
        final int id1, final int id2, final int mod1, final int mod2) throws SQLException {
        LOG.info("Deleting orphaned link in context {}.", cid);
        final String sql = "DELETE FROM prg_links WHERE firstid=? AND secondid=?"
            + " AND firstmodule=? AND secondmodule=? AND cid=?";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            int pos = 1;
            ps.setInt(pos++, id1);
            ps.setInt(pos++, id2);
            ps.setInt(pos++, mod1);
            ps.setInt(pos++, mod2);
            ps.setInt(pos++, cid);
            ps.executeUpdate();
        } finally {
            closeSQLStuff(null, ps);
        }
    }

    public void correctAttachments(final Connection con) throws OXException {
        final String sql = "SELECT a.cid,a.id,a.filename FROM prg_attachment a "
            + "LEFT JOIN prg_contacts c ON a.attached=c.intfield01 AND a.cid=c.cid "
            + "WHERE a.module=? AND c.intfield01 IS NULL";
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, Types.CONTACT);
            result = ps.executeQuery();
            while (result.next()) {
                int pos = 1;
                final int cid = result.getInt(pos++);
                final int attachId = result.getInt(pos++);
                final String filename = result.getString(pos++);
                deleteAttachments(cid, con, attachId, filename);
            }
        } catch (final SQLException e) {
            throw UpdateExceptionCodes.SQL_PROBLEM.create(e, e.getMessage());
        } finally {
            closeSQLStuff(result, ps);
        }
    }

    private final void deleteAttachments(final int cid, final Connection con, final int id, final String filename) throws SQLException {
        LOG.info("Deleting orphaned attachment {} in context {}.", id, cid);
        try {
            Tools.removeFile(cid, filename);
        } catch (final OXException e) {
            LOG.info("Context is already removed. Assuming its files are removed, too.");
        }
        final String sql = "DELETE FROM prg_attachment WHERE cid=? AND id=?";
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, cid);
            ps.setInt(2, id);
            ps.executeUpdate();
        } finally {
            closeSQLStuff(null, ps);
        }
    }
}

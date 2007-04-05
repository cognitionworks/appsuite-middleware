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
package com.openexchange.admin.storage.mysqlStorage;

import com.openexchange.admin.exceptions.PoolException;
import com.openexchange.admin.properties.AdminProperties;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Group;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.storage.interfaces.OXToolStorageInterface;
import com.openexchange.admin.storage.sqlStorage.OXGroupSQLStorage;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.groupware.IDGenerator;
import com.openexchange.groupware.contexts.ContextException;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFailedException;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.server.DBPoolingException;
import com.openexchange.tools.oxfolder.OXFolderAdminHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author d7
 * 
 */
public class OXGroupMySQLStorage extends OXGroupSQLStorage {

    private final static Log log = LogFactory.getLog(OXGroupMySQLStorage.class);

    public OXGroupMySQLStorage() {
    }

    private void changeLastModifiedOnGroup(final int context_id, final int group_id, final Connection write_ox_con) throws SQLException {

        PreparedStatement prep_edit_group = null;
        try {
            prep_edit_group = write_ox_con.prepareStatement("UPDATE groups SET lastModified=? WHERE cid=? AND id=?;");
            prep_edit_group.setLong(1, System.currentTimeMillis());
            prep_edit_group.setInt(2, context_id);
            prep_edit_group.setInt(3, group_id);
            prep_edit_group.executeUpdate();
            prep_edit_group.close();
        } finally {
            try {
                if (prep_edit_group != null) {
                    prep_edit_group.close();
                }
            } catch (final SQLException ee) {
                log.error("SQL Error", ee);
            }
        }

    }

    @Override
    public void addMember(final Context ctx, final int grp_id, final int[] member_ids) throws StorageException {
        Connection con = null;
        PreparedStatement prep_add_member = null;
        final int context_id = ctx.getIdAsInt().intValue();
        try {

            con = cache.getWRITEConnectionForContext(context_id);
            con.setAutoCommit(false);

            for (final int member_id : member_ids) {
                prep_add_member = con.prepareStatement("INSERT INTO groups_member VALUES (?,?,?);");
                prep_add_member.setInt(1, context_id);
                prep_add_member.setInt(2, grp_id);
                prep_add_member.setInt(3, member_id);
                prep_add_member.executeUpdate();
                prep_add_member.close();
            }

            // set last modified on group
            changeLastModifiedOnGroup(context_id, grp_id, con);
            OXUserMySQLStorage oxu = new OXUserMySQLStorage();
            for (final int element : member_ids) {
                oxu.changeLastModified(element, ctx, con);
            }
            
            // let the groupware api know that the group has changed
            OXFolderAdminHelper.propagateGroupModification(grp_id, con, con, context_id); 
            
            con.commit();
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback addmember operation", ecp);
            }
            throw new StorageException(sql);
        } catch (final PoolException e) {
            log.error("Pool Error", e);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback addmember operation", ecp);
            }
            throw new StorageException(e);
        } finally {
            try {
                prep_add_member.close();
            } catch (final SQLException e) {
                log.error("SQL Error", e);
            }
            try {
                cache.pushOXDBWrite(context_id, con);
            } catch (final PoolException e) {
                log.error("Error pushing ox write connection to pool! ", e);
            }
        }
    }

    @Override
    public void removeMember(final Context ctx, final int grp_id, final int[] member_ids) throws StorageException {
        Connection con = null;
        PreparedStatement prep_del_member = null;
        final int context_id = ctx.getIdAsInt().intValue();
        try {
            con = cache.getWRITEConnectionForContext(context_id);
            con.setAutoCommit(false);

            for (final int element : member_ids) {
                prep_del_member = con.prepareStatement("DELETE FROM groups_member WHERE cid=? AND id=? AND member=?;");
                prep_del_member.setInt(1, context_id);
                prep_del_member.setInt(2, grp_id);
                prep_del_member.setInt(3, element);
                prep_del_member.executeUpdate();
                prep_del_member.close();
            }

            // set last modified
            changeLastModifiedOnGroup(context_id, grp_id, con);
            final OXToolStorageInterface tool = OXToolStorageInterface.getInstance();
            OXUserMySQLStorage oxu = new OXUserMySQLStorage();
            for (final int element : member_ids) {
                if (tool.existsUser(ctx, element)) {
                    // update last modified on user
                    oxu.changeLastModified(element, ctx, con);
                }
            }
            
            // let the groupware api know that the group has changed
            OXFolderAdminHelper.propagateGroupModification(grp_id, con, con, context_id); 
            
            con.commit();
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback addmember operation", ecp);
            }
            throw new StorageException(sql);
        } catch (final PoolException e) {
            log.error("Pool Error", e);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback addmember operation", ecp);
            }
            throw new StorageException(e);
        } finally {
            try {
                if (prep_del_member != null) {
                    prep_del_member.close();
                }
            } catch (final SQLException ee) {
                log.error("SQL Error", ee);
            }
            try {
                cache.pushOXDBWrite(context_id, con);
            } catch (final PoolException e) {
                log.error("Error pushing ox write connection to pool! ", e);
            }
        }
    }

    @Override
    public void change(final Context ctx, final Group grp) throws StorageException {
        Connection con = null;
        PreparedStatement prep_edit_group = null;
        final int context_id = ctx.getIdAsInt();
        try {
            con = cache.getWRITEConnectionForContext(context_id);
            con.setAutoCommit(false);
            final int group_id = grp.getId();
            final String identifier = grp.getName();
            if (null != identifier) {
                prep_edit_group = con.prepareStatement("UPDATE groups SET identifier=? WHERE cid=? AND id = ?");
                prep_edit_group.setString(1, identifier);
                prep_edit_group.setInt(2, context_id);
                prep_edit_group.setInt(3, group_id);
                prep_edit_group.executeUpdate();
                prep_edit_group.close();
            }

            final String displayName = grp.getDisplayname();
            if (null != displayName) {
                prep_edit_group = con.prepareStatement("UPDATE groups SET displayName=? WHERE cid=? AND id = ?");
                prep_edit_group.setString(1, displayName);
                prep_edit_group.setInt(2, context_id);
                prep_edit_group.setInt(3, group_id);
                prep_edit_group.executeUpdate();
                prep_edit_group.close();
            }

            // set last modified
            changeLastModifiedOnGroup(context_id, group_id, con);

            con.commit();
        } catch (final SQLException e) {
           log.error("SQL Error", e);
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (final SQLException ecp) {
                log.error("Error processing rollback of connection!", ecp);
            }
            throw new StorageException(e);
        } catch (final PoolException e) {
            log.error("Pool Error", e);
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (final SQLException ecp) {
                log.error("Error processing rollback of connection!", ecp);
            }
            throw new StorageException(e);
        } finally {
            try {
                if (prep_edit_group != null) {
                    prep_edit_group.close();
                }
            } catch (final SQLException e) {
                log.error("SQL Error", e);
            }

            try {
                if (con != null) {
                    cache.pushOXDBWrite(context_id, con);
                }
            } catch (final PoolException e) {
                log.error("Pool Error", e);
            }
        }
    }

    @Override
    public int create(final Context ctx, final Group grp) throws StorageException {
        int retval = -1;
        Connection con = null;
        PreparedStatement prep_insert = null;
        final int context_ID = ctx.getIdAsInt().intValue();
        try {
            con = cache.getWRITEConnectionForContext(context_ID);
            con.setAutoCommit(false);
            final String identifier = grp.getName();

            final String displayName = grp.getDisplayname();
            final int groupID = IDGenerator.getId(context_ID, com.openexchange.groupware.Types.PRINCIPAL, con);
            con.commit();
            
            int gid_number = -1;
            if(Integer.parseInt(prop.getGroupProp(AdminProperties.Group.GID_NUMBER_START,"-1"))>0){
                gid_number = IDGenerator.getId(context_ID, com.openexchange.groupware.Types.GID_NUMBER, con);
                con.commit();
            }
            
            
            prep_insert = con.prepareStatement("INSERT INTO groups (cid,id,identifier,displayName,lastModified) VALUES (?,?,?,?,?);");
            prep_insert.setInt(1, context_ID);
            prep_insert.setInt(2, groupID);
            prep_insert.setString(3, identifier);
            prep_insert.setString(4, displayName);
            prep_insert.setLong(5, System.currentTimeMillis());
            prep_insert.executeUpdate();
            prep_insert.close();
            
            if(gid_number!=-1){
                prep_insert = con.prepareStatement("UPDATE " +
                                "groups " +
                                "SET " +
                                "gidnumber = ? " +
                                "WHERE " +
                                "cid = ? " +
                                "AND " +
                                "id = ?");
                prep_insert.setInt(1, gid_number );
                prep_insert.setInt(2, context_ID);
                prep_insert.setInt(3, groupID);
                prep_insert.executeUpdate();
                prep_insert.close();
            }
                
            con.commit();
            
            retval = groupID;
            log.info("Group " + groupID + " created!");
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);
            try {
                con.rollback();
            } catch (final SQLException ec) {
                log.error("Error rollback configdb connection", ec);
            }
            throw new StorageException(sql);
        } catch (final PoolException e) {
            log.error("Pool Error", e);
            try {
                con.rollback();
            } catch (final SQLException ec) {
                log.error("Error rollback configdb connection", ec);
            }
            throw new StorageException(e);
        } finally {
            try {
                if (prep_insert != null) {
                    prep_insert.close();
                }
            } catch (final SQLException e) {
                prep_insert = null;
            }

            try {
                if(con!=null){
                    cache.pushOXDBWrite(context_ID, con);
                }
            } catch (final PoolException e) {
                log.error("Pool Error", e);
            }
        }

        return retval;
    }

    public void delete(final Context ctx, final Group[] grps) throws StorageException {
        Connection con = null;
        PreparedStatement prep_del_members = null;
        PreparedStatement prep_del_group = null;
        final int context_id = ctx.getIdAsInt();
        try {
            final OXToolStorageInterface tool = OXToolStorageInterface.getInstance();
            
            tool.existsGroup(ctx, grps);

            con = cache.getWRITEConnectionForContext(context_id);
            con.setAutoCommit(false);
            for (final Group grp : grps) {
                final int grp_id = grp.getId();
//              let the groupware api know that the group will be deleted
                OXFolderAdminHelper.propagateGroupModification(grp_id, con, con, context_id); 
                
                final DeleteEvent delev = new DeleteEvent(this, grp_id, DeleteEvent.TYPE_GROUP, context_id);
                AdminCache.delreg.fireDeleteEvent(delev, con, con);

                prep_del_members = con.prepareStatement("DELETE FROM groups_member WHERE cid=? AND id=?");
                prep_del_members.setInt(1, context_id);
                prep_del_members.setInt(2, grp_id);
                prep_del_members.executeUpdate();
                prep_del_members.close();

                createRecoveryData(grp_id, context_id, con);

                prep_del_group = con.prepareStatement("DELETE FROM groups WHERE cid=? AND id=?");
                prep_del_group.setInt(1, context_id);
                prep_del_group.setInt(2, grp_id);
                prep_del_group.executeUpdate();
                prep_del_group.close();
            }
            
            
            
            con.commit();
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback ox db write connection ", ecp);
            }
            throw new StorageException(sql);
        } catch (final PoolException e) {
            log.error("Pool Error", e);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback ox db write connection ", ecp);
            }
            throw new StorageException(e);
        } catch (final LdapException e) {
            log.error("LDAP Error", e);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback ox db write connection ", ecp);
            }
            throw new StorageException(e);
        } catch (final DBPoolingException e) {
            log.error("DBPooling Error", e);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback ox db write connection ", ecp);
            }
            throw new StorageException(e);
        } catch (final DeleteFailedException e) {
            log.error("Delete Error", e);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback ox db write connection ", ecp);
            }
            throw new StorageException(e);
        } catch (final ContextException e) {
            log.error("Context Error", e);
            try {
                con.rollback();
            } catch (final SQLException ecp) {
                log.error("Error rollback ox db write connection ", ecp);
            }
            throw new StorageException(e);
        } finally {
            try {
                if (prep_del_members != null) {
                    prep_del_members.close();
                }
            } catch (final SQLException e) {
                prep_del_members = null;
            }

            try {
                if (prep_del_group != null) {
                    prep_del_group.close();
                }
            } catch (final SQLException e) {
                prep_del_group = null;
            }

            try {
                if(con!=null){
                    cache.pushOXDBWrite(context_id, con);
                }
            } catch (final PoolException e) {
                log.error("Pool Error", e);
            }
        }
    }

    @Override
    public int[] getMembers(final Context ctx, final int grp_id) throws StorageException {
        int[] retval = null;
        Connection con = null;
        PreparedStatement prep_list = null;
        final int context_id = ctx.getIdAsInt();
        try {

            con = cache.getREADConnectionForContext(context_id);

            prep_list = con.prepareStatement("SELECT member FROM groups_member WHERE groups_member.cid = ? AND groups_member.id = ?;");
            prep_list.setInt(1, context_id);
            prep_list.setInt(2, grp_id);
            final ResultSet rs = prep_list.executeQuery();
            final ArrayList<Integer> ids = new ArrayList<Integer>();
            while (rs.next()) {
                ids.add(rs.getInt("member"));
            }

            // Convert to int[] unfortunately there's no standard method
            retval = new int[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                retval[i] = ids.get(i);
            }
        } catch (final PoolException e) {
            log.error("Pool Error", e);            
            throw new StorageException(e);
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);            
            throw new StorageException(sql);
        } finally {
            try {
                if (prep_list != null) {
                    prep_list.close();
                }
            } catch (final SQLException e) {
                log.error("SQL Error", e);
            }

            try {
                if(con!=null){
                    cache.pushOXDBRead(context_id, con);
                }
            } catch (final PoolException e) {
                log.error("Pool Error", e);
            }
        }

        return retval;
    }
    
    private static Group get(final Context ctx,final Group grp,Connection con) throws StorageException{
    	Group retval = null;        
        PreparedStatement prep_list = null;
        final int context_ID = ctx.getIdAsInt();
        try {

            prep_list = con.prepareStatement("SELECT cid,id,identifier,displayName FROM groups WHERE groups.cid = ? AND groups.id = ?");
            prep_list.setInt(1, context_ID);
            prep_list.setInt(2, grp.getId());
            final ResultSet rs = prep_list.executeQuery();

            while (rs.next()) {
                // int cid = rs.getInt("cid");
                final int id = rs.getInt("id");
                final String ident = rs.getString("identifier");
                final String disp = rs.getString("displayName");
                retval = new Group(id, ident, disp);
            }
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);            
            throw new StorageException(sql);       
        } finally {
            try {
                if (prep_list != null) {
                    prep_list.close();
                }
            } catch (final SQLException e) {
                log.error("SQL Error", e);
            }
        }

        return retval;
    }

    @Override
    public Group get(final Context ctx, final Group grp) throws StorageException {
        
        Connection con = null;
        
        try {
        	
            con = cache.getREADConnectionForContext(ctx.getIdAsInt().intValue());

            return get(ctx, grp, con);        
        } catch (final PoolException e) {
            log.error("Pool Error", e);            
            throw new StorageException(e);
        } finally {
            try {
                if (con != null) {
                    cache.pushOXDBRead(ctx.getIdAsInt().intValue(), con);
                }
            } catch (final PoolException e) {
                log.error("Pool Error", e);
            }
        }
    }

    @Override
    public Group[] list(final Context ctx, final String pattern) throws StorageException {
        
        Connection con = null;
        PreparedStatement prep_list = null;
        ResultSet rs = null;
        final int context_id = ctx.getIdAsInt();
        try {
            String pattern_temp = null;
            if (pattern != null) {
                pattern_temp = pattern.replace('*', '%');
            }

            con = cache.getREADConnectionForContext(context_id);

            prep_list = con.prepareStatement("SELECT cid,id,identifier,displayName FROM groups WHERE groups.cid = ? AND (identifier like ? OR displayName like ?)");
            prep_list.setInt(1, context_id);
            prep_list.setString(2, pattern_temp);
            prep_list.setString(3, pattern_temp);
            rs = prep_list.executeQuery();

            final ArrayList<Group> list = new ArrayList<Group>();
            while (rs.next()) {
                // int cid = rs.getInt("cid");
                final int id = rs.getInt("id");
                final String ident = rs.getString("identifier");
                final String disp = rs.getString("displayName");
                // data.put(I_OXGroup.CID,cid);
                list.add(new Group(id, ident, disp));
            }
            return (Group[])list.toArray(new Group[list.size()]);
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);            
            throw new StorageException(sql);
        } catch (final PoolException e) {
            log.error("Pool Error", e);            
            throw new StorageException(e);
        } finally {
            try {
                rs.close();
            } catch (final SQLException ex) {
                log.error("Error closing Resultset!", ex);
            }
            try {
                if (prep_list != null) {
                    prep_list.close();
                }
            } catch (final SQLException e) {
                log.error("SQL Error", e);
            }

            try {
                if(con!=null){
                    cache.pushOXDBRead(context_id, con);
                }
            } catch (final PoolException e) {
                log.error("Pool Error", e);
            }
        }

        
    }

    @Override
    public void deleteRecoveryData(final Context ctx, final int group_id, final Connection con) throws StorageException {
        // delete from del_groups table
        PreparedStatement del_st = null;
        final int context_id = ctx.getIdAsInt();
        try {
            del_st = con.prepareStatement("DELETE from del_groups WHERE id = ? AND cid = ?");
            del_st.setInt(1, group_id);
            del_st.setInt(2, context_id);
            del_st.executeUpdate();
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);
            try {
                con.rollback();
            } catch (final SQLException ec) {
                log.error("Error rollback configdb connection", ec);
            }
            throw new StorageException(sql);
        } finally {
            try {
                if (del_st != null) {
                    del_st.close();
                }
            } catch (final SQLException e) {
                log.error("Error closing prepared statement!", e);
            }
        }
    }

    @Override
    public void deleteAllRecoveryData(final Context ctx, final Connection con) throws StorageException {
        // delete from del_groups table
        PreparedStatement del_st = null;
        final int context_id = ctx.getIdAsInt();
        try {
            del_st = con.prepareStatement("DELETE from del_groups WHERE cid = ?");
            del_st.setInt(1, context_id);
            del_st.executeUpdate();
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);
            try {
                con.rollback();
            } catch (final SQLException ec) {
                log.error("Error rollback configdb connection", ec);
            }
            throw new StorageException(sql);
        } finally {
            try {
                if (del_st != null) {
                    del_st.close();
                }
            } catch (final SQLException e) {
                log.error("Error closing prepared statement!", e);
            }
        }
    }

    private void createRecoveryData(final int group_id, final int context_id, final Connection write_ox_con) throws SQLException {
        PreparedStatement del_st = null;
        ResultSet rs = null;
        try {
            del_st = write_ox_con.prepareStatement("SELECT identifier,displayName FROM groups WHERE id = ? AND cid = ?");
            del_st.setInt(1, group_id);
            del_st.setInt(2, context_id);
            rs = del_st.executeQuery();
            String ident = null;
            String disp = null;

            if (rs.next()) {
                ident = rs.getString("identifier");
                disp = rs.getString("displayName");
            }
            del_st.close();
            rs.close();

            del_st = write_ox_con.prepareStatement("" + "INSERT into del_groups (id,cid,lastModified,identifier,displayName) VALUES (?,?,?,?,?)");
            del_st.setInt(1, group_id);
            del_st.setInt(2, context_id);
            del_st.setLong(3, System.currentTimeMillis());
            del_st.setString(4, ident);
            del_st.setString(5, disp);
            del_st.executeUpdate();
            del_st.close();

        } finally {
            try {
                if (del_st != null) {
                    del_st.close();
                }
            } catch (final SQLException e) {
                log.error("Error closing prepared statement!", e);
            }
        }
    }

	@Override
	public Group[] getGroupsForUser(Context ctx, User usr) throws StorageException {
		
		Connection con = null;
        PreparedStatement prep_list = null;  
        try {

            con = cache.getREADConnectionForContext(ctx.getIdAsInt().intValue());
            // fetch all group ids the user is member of
            prep_list = con.prepareStatement("SELECT id FROM groups_member WHERE cid = ? AND member = ?");
            prep_list.setInt(1, ctx.getIdAsInt().intValue());
            prep_list.setInt(2, usr.getId().intValue());
            
            final ResultSet rs = prep_list.executeQuery();
            ArrayList<Group> grplist = new ArrayList<Group>();
            while (rs.next()) {
            	grplist.add(get(ctx, new Group(rs.getInt("id")), con));
            }
            return grplist.toArray(new Group[grplist.size()]);
        } catch (final SQLException sql) {
            log.error("SQL Error", sql);            
            throw new StorageException(sql);
        } catch (final PoolException e) {
            log.error("Pool Error", e);            
            throw new StorageException(e);
        } finally {
            try {
                if (prep_list != null) {
                    prep_list.close();
                }
            } catch (final SQLException e) {
                log.error("SQL Error", e);
            }

            try {
                if (con != null) {
                    cache.pushOXDBRead(ctx.getIdAsInt().intValue(), con);
                }
            } catch (final PoolException e) {
                log.error("Pool Error", e);
            }
        }
		
	}

}

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

package com.openexchange.groupware.infostore.webdav;

import static com.openexchange.tools.sql.DBUtils.getStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.tx.DBService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.impl.IDGenerator;
import com.openexchange.groupware.infostore.InfostoreExceptionCodes;
import com.openexchange.groupware.ldap.User;

public abstract class LockManagerImpl<T extends Lock> extends DBService implements LockManager{

    private static final String PAT_TABLENAME = "%%tablename%%";

    private String INSERT = "INSERT INTO %%tablename%% (entity, timeout, scope, type, ownerDesc, cid, userid, id %%additional_fields%% ) VALUES (?, ?, ?, ?, ?, ?, ?, ? %%additional_question_marks%%)";
    private String DELETE = "DELETE FROM %%tablename%% WHERE cid = ? AND id = ? ";
    private String REASSIGN = "UPDATE %%tablename%% SET userid = ? WHERE userid = ? and cid = ?";
    private String FIND_BY_ENTITY = "SELECT entity, timeout, scope, type, ownerDesc, cid, userid, id %%additional_fields%% FROM %%tablename%% WHERE entity IN %%entity_ids%% and cid = ? ";
    private String EXISTS_BY_ENTITY = "SELECT 1 FROM %%tablename%% WHERE entity IN %%entity_ids%% and cid = ? ";
    private String DELETE_BY_ENTITY = "DELETE FROM %%tablename%% WHERE cid = ? AND entity = ?";
    private String UPDATE_BY_ID = "UPDATE %%tablename%% SET timeout = ? , scope = ?, type = ? , ownerDesc = ? %%additional_updates%% WHERE id = ? AND cid = ?";

    private final List<LockExpiryListener> expiryListeners = new ArrayList<LockExpiryListener>();

    public LockManagerImpl(final String tablename) {
        this(null, tablename);
    }

    public LockManagerImpl(final DBProvider provider, final String tablename) {
        setProvider(provider);
        initTablename(tablename);
    }

    private void initTablename(final String tablename) {
        INSERT = INSERT.replaceAll(PAT_TABLENAME, tablename);
        INSERT = initAdditionalINSERT(INSERT);
        DELETE = DELETE.replaceAll(PAT_TABLENAME, tablename);

        FIND_BY_ENTITY = FIND_BY_ENTITY.replaceAll(PAT_TABLENAME, tablename);
        FIND_BY_ENTITY = initAdditionalFIND_BY_ENTITY(FIND_BY_ENTITY);

        EXISTS_BY_ENTITY = EXISTS_BY_ENTITY.replaceAll(PAT_TABLENAME, tablename);


        DELETE_BY_ENTITY = DELETE_BY_ENTITY.replaceAll(PAT_TABLENAME, tablename);

        UPDATE_BY_ID = UPDATE_BY_ID.replaceAll(PAT_TABLENAME, tablename);
        UPDATE_BY_ID = initAdditionalUPDATE_BY_ID(UPDATE_BY_ID);

        REASSIGN = REASSIGN.replaceAll(PAT_TABLENAME, tablename);
    }


    private String initAdditionalUPDATE_BY_ID(final String query) {
        return query.replaceAll("%%additional_updates%%","");
    }

    protected String initAdditionalINSERT(final String insert) {
        return insert.replaceAll("%%additional_fields%%","").replaceAll("%%additional_question_marks%%", "");
    }

    protected String initAdditionalFIND_BY_ENTITY(final String findByEntity) {
        return findByEntity.replaceAll("%%additional_fields%%", "");
    }

    protected int getType() {
        return com.openexchange.groupware.Types.WEBDAV; // FIXME
    }

    protected abstract T newLock();

    protected void fillLock(final T lock, final ResultSet rs) throws SQLException {
        lock.setId(rs.getInt("id"));
        lock.setOwner(rs.getInt("userid"));
        final int scopeNum = rs.getInt("scope");
        for(final Scope scope : Scope.values()) {
            if(scopeNum == scope.ordinal()) {
                lock.setScope(scope);
            }
        }
        lock.setType(Type.WRITE);
        final long timeout =  (rs.getLong("timeout") - System.currentTimeMillis());
        lock.setTimeout(timeout);
        lock.setOwnerDescription(rs.getString("ownerDesc"));
        lock.setEntity(rs.getInt("entity"));
    }

    protected int createLockForceId(final int entity, final int id, final long timeout, final Scope scope, final Type type, final String ownerDesc,
            final Context ctx, final User user, final Object...additional) throws OXException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getWriteConnection(ctx);
            stmt = con.prepareStatement(INSERT);
            long tm = 0;
            if(timeout != INFINITE) {
                tm = System.currentTimeMillis()+timeout;
                // Previously Infinite Locks exceed long range if ms counter increased by 1 since loading
                if(tm<0) {
                    tm = Long.MAX_VALUE;
                }
            } else {
                tm = Long.MAX_VALUE;
            }
            set(1, stmt, additional, Integer.valueOf(entity), Long.valueOf(tm), Integer.valueOf(scope.ordinal()), Integer.valueOf(type.ordinal()), ownerDesc, Integer.valueOf(ctx.getContextId()), Integer.valueOf(user.getId()), Integer.valueOf(id));
            stmt.executeUpdate();
            return id;
        } catch (final SQLException x) {
            throw InfostoreExceptionCodes.SQL_PROBLEM.create(x, getStatement(stmt));
        } catch (final OXException e) {
            throw e;
        } finally {
            close(stmt, null);
            releaseWriteConnection(ctx, con);
        }
    }

    protected int createLock(final int entity, final long timeout, final Scope scope, final Type type, final String ownerDesc,
            final Context ctx, final User user, final Object...additional) throws OXException {
        try {
            return createLockForceId(entity, IDGenerator.getId(ctx, getType()), timeout, scope, type, ownerDesc, ctx, user, additional);
        } catch (final SQLException e) {
            throw InfostoreExceptionCodes.NEW_ID_FAILED.create(e);
        }
    }

    protected void updateLock(final int lockId, final long timeout, final Scope scope, final Type type, final String ownerDesc, final Context ctx, final Object...additional) throws OXException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getWriteConnection(ctx);
            stmt = con.prepareStatement(UPDATE_BY_ID);
            long tm = 0;
            if(timeout != INFINITE) {
                tm = System.currentTimeMillis()+timeout;
            } else {
                tm = Long.MAX_VALUE;
            }
            final int index = set(1, stmt, additional, Long.valueOf(tm), Integer.valueOf(scope.ordinal()), Integer.valueOf(type.ordinal()), ownerDesc);
            set(index, stmt, null, Integer.valueOf(lockId), Integer.valueOf(ctx.getContextId()));
            stmt.executeUpdate();
        } catch (final SQLException x) {
            throw InfostoreExceptionCodes.SQL_PROBLEM.create(x, getStatement(stmt));
        } catch (final OXException e) {
            throw e;
        } finally {
            close(stmt, null);
            releaseWriteConnection(ctx, con);
        }
    }

    protected void removeLock(final int id, final Context ctx) throws OXException {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getWriteConnection(ctx);
            stmt = con.prepareStatement(DELETE);
            set(1, stmt, null, Integer.valueOf(ctx.getContextId()), Integer.valueOf(id));
            stmt.executeUpdate();
        } catch (final SQLException x) {
            throw InfostoreExceptionCodes.SQL_PROBLEM.create(x, getStatement(stmt));
        } catch (final OXException e) {
            throw e;
        } finally {
            close(stmt, null);
            releaseWriteConnection(ctx, con);
        }
    }

    public Map<Integer,List<T>> findLocksByEntity(final List<Integer> entities, final Context ctx) throws OXException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            final StringBuilder entityIds = new StringBuilder().append('(');
            entityIds.append(join(entities));
            entityIds.append(')');

            con = getReadConnection(ctx);
            stmt = con.prepareStatement(FIND_BY_ENTITY.replaceAll("%%entity_ids%%", entityIds.toString()));
            set(1, stmt, null, Integer.valueOf(ctx.getContextId()));
            rs = stmt.executeQuery();
            final Map<Integer, List<T>> locks = new HashMap<Integer, List<T>>();
            final Set<Integer> entitySet = new HashSet<Integer>(entities);

            while(rs.next()) {
                final int entity = rs.getInt("entity");
                entitySet.remove(Integer.valueOf(entity));
                List<T> lockList = locks.get(Integer.valueOf(entity));
                if(null == lockList) {
                    lockList = new ArrayList<T>();
                    locks.put(Integer.valueOf(entity), lockList);
                }

                final T lock = newLock();
                fillLock(lock, rs);
                if(lock.getTimeout()<1){
                    removeLock(lock.getId(), ctx);
                    lockExpired(lock);
                } else {
                    lockList.add(lock);
                }
            }
            for(final Integer entity : entitySet) {
                locks.put(entity, new ArrayList<T>());
            }
            return locks;
        } catch (final SQLException x) {
            throw InfostoreExceptionCodes.SQL_PROBLEM.create(x, getStatement(stmt));
        } catch (final OXException e) {
            throw e;
        } finally {
            close(stmt, null);
            releaseReadConnection(ctx, con);
        }
    }

    public boolean existsLockForEntity(final List<Integer> entities, final Context ctx) throws OXException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            final StringBuilder entityIds = new StringBuilder().append('(');
            entityIds.append(join(entities));
            entityIds.append(')');

            con = getReadConnection(ctx);
            stmt = con.prepareStatement(EXISTS_BY_ENTITY.replaceAll("%%entity_ids%%", entityIds.toString()));
            set(1, stmt, null, Integer.valueOf(ctx.getContextId()));
            rs = stmt.executeQuery();
            return rs.next();

        } catch (final SQLException x) {
            throw InfostoreExceptionCodes.SQL_PROBLEM.create(x, getStatement(stmt));
        } catch (final OXException e) {
            throw e;
        } finally {
            close(stmt, null);
            releaseReadConnection(ctx, con);
        }
    }

    public void reassign(final Context ctx, final int from, final int to) throws OXException {
        Connection writeCon = null;
        PreparedStatement stmt = null;
        try {
            writeCon = getWriteConnection(ctx);
            stmt = writeCon.prepareStatement(REASSIGN);
            stmt.setInt(1, to);
            stmt.setInt(2, from);
            stmt.setInt(3, ctx.getContextId());
            stmt.executeUpdate();
        } catch (final SQLException x) {
            throw InfostoreExceptionCodes.SQL_PROBLEM.create(x, getStatement(stmt));
        } catch (final OXException e) {
            throw e;
        } finally {
            close(stmt, null);
            releaseWriteConnection(ctx, writeCon);
        }
    }

    protected void removeAllFromEntity(final int entity, final Context ctx) throws OXException {
        Connection writeCon = null;
        PreparedStatement stmt = null;
        try {
            writeCon = getWriteConnection(ctx);
            stmt = writeCon.prepareStatement(DELETE_BY_ENTITY);
            stmt.setInt(1, ctx.getContextId());
            stmt.setInt(2, entity);
            stmt.executeUpdate();
        } catch (final SQLException x) {
            throw InfostoreExceptionCodes.SQL_PROBLEM.create(x, getStatement(stmt));
        } catch (final OXException e) {
            throw e;
        } finally {
            close(stmt, null);
            releaseWriteConnection(ctx, writeCon);
        }
    }

    protected CharSequence join(final List<Integer> entities) {
        final StringBuilder b = new StringBuilder();
        for(final int entity : entities) { b.append(entity); b.append(", "); }
        b.setLength(b.length()-2);
        return b;
    }

    protected final int set(int index, final PreparedStatement stmt, final Object[] additional, final Object...values) throws SQLException {
        for(final Object o : values) {
            stmt.setObject(index++,o);
        }
        if(null == additional) {
            return index;
        }
        for(final Object o : additional) {
            stmt.setObject(index++,o);
        }
        return index;
    }

    public void addExpiryListener(final LockExpiryListener listener) {
        expiryListeners.add( listener );
    }

    protected void lockExpired(final Lock lock) throws OXException {
        for (final LockExpiryListener listener : expiryListeners) {
            listener.lockExpired(lock);
        }
    }

}

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

package com.openexchange.database.tx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.database.DBPoolingException;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.provider.DBProviderUser;
import com.openexchange.database.provider.RequestDBProvider;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.tools.exceptions.LoggingLogic;
import com.openexchange.tools.sql.DBUtils;
import com.openexchange.tx.TransactionAware;
import com.openexchange.tx.TransactionException;
import com.openexchange.tx.TransactionExceptionCodes;
import com.openexchange.tx.Undoable;
import com.openexchange.tx.UndoableAction;

public abstract class DBService implements TransactionAware, DBProviderUser, DBProvider {

    private static final Log LOG = LogFactory.getLog(DBService.class);
    private static final LoggingLogic LL = LoggingLogic.getLoggingLogic(DBService.class);

    private RequestDBProvider provider;

    private final ThreadLocal<ThreadState> txState = new ThreadLocal<ThreadState>();

    private static final class ThreadState {
        public List<Undoable> undoables = new ArrayList<Undoable>();
        public boolean preferWriteCon;
        public Set<Connection> writeCons = new HashSet<Connection>();
    }

    public DBProvider getProvider() {
        return this.provider;
    }

    public void setProvider(final DBProvider provider) {
        this.provider = new RequestDBProvider(provider);
        this.provider.setTransactional(true);
    }

    public DBService() {
        super();
    }

    public DBService(final DBProvider provider) {
        super();
        setProvider(provider);
    }

    public Connection getReadConnection(final Context ctx) throws DBPoolingException {
        if(txState.get() != null && txState.get().preferWriteCon) {
            return getWriteConnection(ctx);
        }
        return provider.getReadConnection(ctx);
    }

    public Connection getWriteConnection(final Context ctx) throws DBPoolingException {
        final Connection writeCon = provider.getWriteConnection(ctx);
        if(txState.get() != null && txState.get().preferWriteCon) {
            txState.get().writeCons.add(writeCon);
            return writeCon;
        } else if(txState.get() != null){
            txState.get().preferWriteCon = true;
        }

        return writeCon;
    }

    public void releaseReadConnection(final Context ctx, final Connection con) {
        if(txState.get() != null && txState.get().preferWriteCon && txState.get().writeCons.contains(con)){
            releaseWriteConnection(ctx,con);
            return;
        }
        provider.releaseReadConnection(ctx, con);
    }

    public void releaseWriteConnection(final Context ctx, final Connection con) {
        if(txState.get() != null && txState.get().preferWriteCon) {
            txState.get().writeCons.remove(con);
        }
        provider.releaseWriteConnection(ctx, con);
    }

    public void commitDBTransaction() throws DBPoolingException {
        provider.commit();
    }

    public void commitDBTransaction(final Undoable undo) throws DBPoolingException {
        provider.commit();
        addUndoable(undo);
    }

    public void rollbackDBTransaction() throws DBPoolingException {
        provider.rollback();
    }

    public void startDBTransaction() throws DBPoolingException {
        provider.startTransaction();
    }

    public void finishDBTransaction() throws DBPoolingException {
        provider.finish();
    }

    public void startTransaction() throws TransactionException {
        txState.set(new ThreadState());
    }

    public void finish() throws TransactionException {
        try {
            provider.finish();
        } catch (DBPoolingException e) {
            throw new TransactionException(e);
        }
        txState.set(null);
    }

    public void rollback() throws TransactionException {
        final List<Undoable> failed = new ArrayList<Undoable>();
        final List<Undoable> undos = new ArrayList<Undoable>(txState.get().undoables);
        Collections.reverse(undos);
        for(final Undoable undo : undos) {
            try {
                undo.undo();
            } catch (final AbstractOXException x) {
                LOG.fatal(x.getMessage(),x);
                failed.add(undo);
            }
        }
        if (failed.size() != 0) {
            final TransactionException exception = TransactionExceptionCodes.NO_COMPLETE_ROLLBACK.create();
            if (LOG.isFatalEnabled()) {
                final StringBuilder explanations = new StringBuilder();
                for(final Undoable undo : failed) {
                    explanations.append(undo.error());
                    explanations.append("\n");
                }
                LOG.fatal(explanations.toString(),exception);
            }
            throw exception;
        }
    }

    public void commit() throws TransactionException {
        // Nothing to do.
    }

    public void setRequestTransactional(final boolean transactional) {
        provider.setRequestTransactional(transactional);
    }


    public void setCommitsTransaction(final boolean mustCommit) {
        provider.setCommitsTransaction(false);
    }

    protected void close(final PreparedStatement stmt, final ResultSet rs) {
        DBUtils.closeSQLStuff(rs, stmt);
    }

    protected void addUndoable(final Undoable undo) {
        if(null == txState.get() || null == txState.get().undoables) {
            return;
        }
        txState.get().undoables.add(undo);
    }

    protected void perform(final UndoableAction action, final boolean dbTransaction) throws AbstractOXException {
        try {
            if(dbTransaction) {
                startDBTransaction();
            }
            action.perform();
            if(dbTransaction) {
                commitDBTransaction(action);
            } else {
                addUndoable(action);
            }
        } catch (final AbstractOXException e) {
            if(dbTransaction) {
                try {
                    rollbackDBTransaction();
                } catch (final DBPoolingException x) {
                    LL.log(x);
                }
            }
            throw e;
        } finally {
            if(dbTransaction) {
                try {
                    finishDBTransaction();
                } catch (final DBPoolingException x) {
                    LL.log(x);
                }
            }
        }
    }

    @Deprecated
    public void setTransactional(final boolean tx) {
        // Nothing to do.
    }

    public boolean inTransaction(){
        return txState.get() != null;
    }
}

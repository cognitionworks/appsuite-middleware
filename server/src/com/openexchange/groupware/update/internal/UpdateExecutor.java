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

package com.openexchange.groupware.update.internal;

import java.sql.SQLException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.database.DBPoolingException;
import com.openexchange.databaseold.Database;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.update.PerformParameters;
import com.openexchange.groupware.update.ProgressState;
import com.openexchange.groupware.update.SchemaException;
import com.openexchange.groupware.update.SchemaStore;
import com.openexchange.groupware.update.SchemaUpdateState;
import com.openexchange.groupware.update.UpdateException;
import com.openexchange.groupware.update.UpdateExceptionCodes;
import com.openexchange.groupware.update.UpdateTask;
import com.openexchange.groupware.update.UpdateTaskCollection;
import com.openexchange.groupware.update.UpdateTaskV2;

/**
 * {@link UpdateExecutor}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public final class UpdateExecutor {

    private static final Log LOG = LogFactory.getLog(UpdateExecutor.class);

    private static final SchemaStore store = SchemaStore.getInstance();

    private SchemaUpdateState state;

    private final int contextId;

    private List<UpdateTask> tasks;

    public UpdateExecutor(SchemaUpdateState state, int contextId, List<UpdateTask> tasks) {
        super();
        this.state = state;
        this.contextId = contextId;
        this.tasks = tasks;
    }

    public void execute() throws UpdateException {
        boolean unlock = false;
        try {
            lockSchema();
            // Lock successfully obtained, thus remember to unlock
            unlock = true;
            // Remove affected contexts and kick active sessions
            removeContexts();
            if (null == tasks) {
                state = store.getSchema(contextId);
                // Get filtered & sorted list of update tasks
                tasks = UpdateTaskCollection.getInstance().getFilteredAndSortedUpdateTasks(state);
            }
            // Perform updates
            for (UpdateTask task : tasks) {
                String taskName = task.getClass().getSimpleName();
                boolean success = false;
                try {
                    LOG.info("Starting update task " + taskName + " on schema " + state.getSchema() + ".");
                    if (task instanceof UpdateTaskV2) {
                        ProgressState logger = new ProgressStatusImpl(taskName, state.getSchema());
                        PerformParameters params = new PerformParametersImpl(state, contextId, logger);
                        ((UpdateTaskV2) task).perform(params);
                    } else {
                        task.perform(state, contextId);
                    }
                    success = true;
                } catch (AbstractOXException e) {
                    LOG.error(e.getMessage(), e);
                }
                if (success) {
                    LOG.info("Update task " + taskName + " on schema " + state.getSchema() + " done.");
                } else {
                    LOG.info("Update task " + taskName + " on schema " + state.getSchema() + " failed.");
                }
                addExecutedTask(task.getClass().getName(), success);
            }
            LOG.info("Finished updating schema " + state.getSchema());
        } catch (SchemaException e) {
            final Throwable cause = e.getCause();
            unlock = (null != cause) && (cause instanceof SQLException);
            throw new UpdateException(e);
        } catch (UpdateException e) {
            throw e;
        } catch (Throwable t) {
            throw UpdateExceptionCodes.UPDATE_FAILED.create(t, state.getSchema(), t.getMessage());
        } finally {
            try {
                if (unlock) {
                    unlockSchema();
                }
                // Remove contexts from cache if they are cached during update process.
                removeContexts();
            } catch (SchemaException e) {
                throw new UpdateException(e);
            }
        }
    }

    private final void lockSchema() throws SchemaException {
        store.lockSchema(state, contextId);
    }

    private final void unlockSchema() throws SchemaException {
        store.unlockSchema(state, contextId);
    }

    private final void addExecutedTask(String taskName, boolean success) throws SchemaException {
        store.addExecutedTask(contextId, taskName, success);
    }

    private final void removeContexts() throws UpdateException {
        ContextStorage contextStorage = ContextStorage.getInstance();
        try {
            int[] contextIds = Database.getContextsInSameSchema(contextId);
            for (final int cid : contextIds) {
                contextStorage.invalidateContext(cid);
            }
        } catch (DBPoolingException e) {
            throw new UpdateException(e);
        } catch (ContextException e) {
            throw new UpdateException(e);
        }
    }
}

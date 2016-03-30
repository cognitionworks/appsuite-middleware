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

package com.openexchange.drive.storage.execute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.openexchange.drive.DriveVersion;
import com.openexchange.drive.actions.AbstractAction;
import com.openexchange.drive.internal.SyncSession;
import com.openexchange.drive.storage.StorageOperation;
import com.openexchange.exception.OXException;

/**
 * {@link AbstractActionExecutor}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public abstract class AbstractActionExecutor<T extends DriveVersion> implements ActionExecutor<T> {

    protected final SyncSession session;
    protected final boolean transactional;

    private List<AbstractAction<T>> newActionsForClient;

    /**
     * Initializes a new {@link AbstractActionExecutor}.
     *
     * @param session The session
     * @param transactional <code>true</code> to operate in a transactional mode, <code>false</code>, otherwise
     */
    protected AbstractActionExecutor(SyncSession session, boolean transactional) {
        super();
        this.session = session;
        this.transactional = transactional;
    }

    /**
     * Adds a new action the client should execute as a result of the executed server actions.
     *
     * @param action The action to add.
     */
    protected void addNewActionForClient(AbstractAction<T> action) {
        if (null == newActionsForClient) {
            newActionsForClient = new ArrayList<AbstractAction<T>>();
        }
        newActionsForClient.add(action);
    }

    /**
     * Adds multiple new actions the client should execute as a result of the executed server actions.
     *
     * @param actions The actions to add.
     */
    protected void addNewActionsForClient(Collection<? extends AbstractAction<T>> actions) {
        if (null == newActionsForClient) {
            newActionsForClient = new ArrayList<AbstractAction<T>>();
        }
        newActionsForClient.addAll(actions);
    }

    @Override
    public List<AbstractAction<T>> getNewActionsForClient() {
        return newActionsForClient;
    }

    @Override
    public void execute(List<AbstractAction<T>> actions) throws OXException {
        if (null == actions || 0 == actions.size()) {
            return;
        }
        for (final AbstractAction<T> action : actions) {
            if (transactional) {
                session.getStorage().wrapInTransaction(new StorageOperation<Void>() {

                    @Override
                    public Void call() throws OXException {
                        execute(action);
                        return null;
                    }
                });
            } else {
                execute(action);
            }
        }
    }

    protected abstract void execute(AbstractAction<T> action) throws OXException;

}

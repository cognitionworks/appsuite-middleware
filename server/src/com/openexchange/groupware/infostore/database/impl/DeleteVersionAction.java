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

package com.openexchange.groupware.infostore.database.impl;

import java.sql.SQLException;
import java.util.List;

import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.OXExceptionSource;
import com.openexchange.groupware.OXThrows;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.infostore.Classes;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreExceptionFactory;

@OXExceptionSource(
		classId = Classes.COM_OPENEXCHANGE_GROUPWARE_INFOSTORE_DATABASE_IMPL_DELETEVERSIONACTION,
		component = EnumComponent.INFOSTORE
)
public class DeleteVersionAction extends AbstractDocumentListAction {

	private static final InfostoreExceptionFactory EXCEPTIONS = new InfostoreExceptionFactory(DeleteVersionAction.class);
	
	
	@OXThrows(
			category = Category.CODE_ERROR,
			desc = "An invalid SQL Query was sent to the server",
			exceptionId = 0,
			msg = "Invalid SQL Query : %s")
	@Override
	protected void undoAction() throws AbstractOXException {
		if(getDocuments().size()==0) {
			return;
		}
		final UpdateBlock[] updates = new UpdateBlock[getDocuments().size()+1];
		int i = 0;
		for(final DocumentMetadata doc : getDocuments()) {
			updates[i++] = new Update(getQueryCatalog().getVersionInsert()) {

				@Override
				public void fillStatement() throws SQLException {
					fillStmt(stmt,getQueryCatalog().getVersionFields(),doc,Integer.valueOf(getContext().getContextId()));
				}
				
			};
		}
		updates[i] = new Update(getQueryCatalog().getVersionDelete(InfostoreQueryCatalog.Table.DEL_INFOSTORE_DOCUMENT, getDocuments())){

			@Override
			public void fillStatement() throws SQLException {
				stmt.setInt(1, getContext().getContextId());
			}
			
		};
		
		try {
			doUpdates(updates);
		} catch (final UpdateException e) {
			throw EXCEPTIONS.create(0, e.getSQLException(), e.getStatement());
		}
	}

	@OXThrows(
			category = Category.CODE_ERROR,
			desc = "An invalid SQL Query was sent to the server",
			exceptionId = 1,
			msg = "Invalid SQL Query : %s")
	public void perform() throws AbstractOXException {
		if(getDocuments().size()==0) {
			return;
		}

        List<DocumentMetadata>[] slices = getSlices();

        final UpdateBlock[] updates = new UpdateBlock[getDocuments().size()+slices.length+1];


        updates[0] = new Update(getQueryCatalog().getVersionDelete(InfostoreQueryCatalog.Table.DEL_INFOSTORE_DOCUMENT,getDocuments())) {
			@Override
			public void fillStatement() throws SQLException {
				stmt.setInt(1, getContext().getContextId());
			}
		};
		
		int i = 1;
		for(final DocumentMetadata doc : getDocuments()) {
			updates[i++] = new Update(getQueryCatalog().getDelVersionInsert()) {

				@Override
				public void fillStatement() throws SQLException {
					fillStmt(stmt,getQueryCatalog().getVersionFields(),doc,Integer.valueOf(getContext().getContextId()));
				}
				
			};
		}
        for(int j = 0; j < slices.length; j++) {
            updates[i++] = new Update(getQueryCatalog().getVersionDelete(InfostoreQueryCatalog.Table.INFOSTORE_DOCUMENT, slices[j])){

                @Override
                public void fillStatement() throws SQLException {
                    stmt.setInt(1, getContext().getContextId());
                }

            };
	    }

		try {
			doUpdates(updates);
		} catch (final UpdateException e) {
			throw EXCEPTIONS.create(1, e.getSQLException(), e.getStatement());
		}
		
	}

    private int batchSize = 1000;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    private List<DocumentMetadata>[] getSlices() {
        List<DocumentMetadata> documents = getDocuments();
        boolean addOne = (0 != (documents.size() % batchSize));
        int numberOfSlices = documents.size() / batchSize;
        if(addOne) { numberOfSlices += 1; }
        
        List<DocumentMetadata>[] slices = new List[numberOfSlices];

        int max = documents.size();
        for(int i = 0; i < numberOfSlices; i++) {
            int start = i * batchSize;
            int end = i+1 * batchSize;
            if(end > max) { end = max; };
            List<DocumentMetadata> slice = documents.subList(start, end);
            slices[i] = slice;

        }

        return slices;
    }


    @Override
	protected Object[] getAdditionals(final DocumentMetadata doc) {
		return null;
	}
}

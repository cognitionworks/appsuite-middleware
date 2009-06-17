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

package com.openexchange.publish.sql;

import static com.openexchange.sql.grammar.Constant.ASTERISK;
import static com.openexchange.sql.schema.Tables.publications;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import com.openexchange.groupware.tx.TransactionException;
import com.openexchange.publish.Publication;
import com.openexchange.publish.PublicationErrorMessage;
import com.openexchange.publish.PublicationException;
import com.openexchange.sql.builder.StatementBuilder;
import com.openexchange.sql.grammar.EQUALS;
import com.openexchange.sql.grammar.SELECT;


/**
 * {@link PublicationSQLTest}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public class PublicationSQLTest extends AbstractPublicationSQLStorageTest {
    public void testRemember() throws Exception {
        storage.rememberPublication(pub1);
        assertTrue("Id should be greater 0", pub1.getId() > 0);
        publicationsToDelete.add(pub1.getId());
        
        SELECT select = new SELECT(ASTERISK).
        FROM(publications).
        WHERE(new EQUALS("id", pub1.getId()).
            AND(new EQUALS("cid", ctx.getContextId())).
            AND(new EQUALS("user_id", pub1.getUserId())).
            AND(new EQUALS("entity", pub1.getEntityId())).
            AND(new EQUALS("module", pub1.getModule())).
            AND(new EQUALS("target_id", pub1.getTarget().getId())));
        
        assertResult(new StatementBuilder().buildCommand(select));
    }
    
    public void testForget() throws Exception {
        storage.rememberPublication(pub1);
        assertTrue("Id should be greater 0", pub1.getId() > 0);
        publicationsToDelete.add(pub1.getId());
        
        storage.forgetPublication(pub1);
        
        SELECT select = new SELECT(ASTERISK).
        FROM(publications).
        WHERE(new EQUALS("id", pub1.getId()).
            AND(new EQUALS("cid", ctx.getContextId())).
            AND(new EQUALS("user_id", pub1.getUserId())).
            AND(new EQUALS("entity", pub1.getEntityId())).
            AND(new EQUALS("module", pub1.getModule())).
            AND(new EQUALS("target_id", pub1.getTarget().getId())));
        
        assertNoResult(new StatementBuilder().buildCommand(select));
    }
    
    public void testListGet1() throws Exception {
        removePublicationsForTarget(pub1.getTarget().getId());
        
        storage.rememberPublication(pub1);
        assertTrue("Id should be greater 0", pub1.getId() > 0);
        publicationsToDelete.add(pub1.getId());
        storage.rememberPublication(pub2);
        assertTrue("Id should be greater 0", pub2.getId() > 0);
        publicationsToDelete.add(pub2.getId());
        
        List<Publication> publications = storage.getPublications(ctx, pub1.getTarget().getId());
        
        assertEquals("Number of publications for this target is not correct.", 2, publications.size());
        
        for (Publication publication : publications) {
            if (publication.getId() == pub1.getId()) {
                assertEquals(pub1, publication);
            } else if (publication.getId() == pub2.getId()) {
                assertEquals(pub2, publication);
            } else {
                fail("Found unexpected publication");
            }
        }
    }
    
    public void testListGet2() throws Exception {
        removePublicationsForEntity(entityId1, module1);
        
        storage.rememberPublication(pub1);
        assertTrue("Id should be greater 0", pub1.getId() > 0);
        publicationsToDelete.add(pub1.getId());
        storage.rememberPublication(pub2);
        assertTrue("Id should be greater 0", pub2.getId() > 0);
        publicationsToDelete.add(pub2.getId());
        
        List<Publication> publications = storage.getPublications(ctx, module1, entityId1);
        
        assertEquals("Number of publications for this entity is not correct.", 2, publications.size());
        
        for (Publication publication : publications) {
            if (publication.getId() == pub1.getId()) {
                assertEquals(pub1, publication);
            } else if (publication.getId() == pub2.getId()) {
                assertEquals(pub2, publication);
            } else {
                fail("Found unexpected publication");
            }
        }
    }
    
    public void testGet() throws Exception {
        storage.rememberPublication(pub1);
        assertTrue("Id should be greater 0", pub1.getId() > 0);
        publicationsToDelete.add(pub1.getId());
        
        Publication publication = storage.getPublication(ctx, pub1.getId());
        
        assertEquals(pub1, publication);
    }
    
    public void testSearch() throws Exception {
        storage.rememberPublication(pub1);
        assertTrue("Id should be greater 0", pub1.getId() > 0);
        publicationsToDelete.add(pub1.getId());
        storage.rememberPublication(pub2);
        assertTrue("Id should be greater 0", pub2.getId() > 0);
        publicationsToDelete.add(pub2.getId());
        
        Collection<Publication> publications = storage.search(ctx, pub1.getTarget().getId(), pub1.getConfiguration());
        assertEquals("Number of expected publications is not correct.", 1, publications.size());
        Publication foundPublication = publications.iterator().next();
        assertEquals(pub1, foundPublication);
    }
    
    public void testUpdate() throws Exception {
        storage.rememberPublication(pub1);
        assertTrue("Id should be greater 0", pub1.getId() > 0);
        publicationsToDelete.add(pub1.getId());
        pub2.setId(pub1.getId());
        pub2.setModule("newModule");
        pub2.setEntityId("3546");
        storage.updatePublication(pub2);
        assertEquals("Id should not changed", pub1.getId(), pub2.getId());
        
        SELECT select = new SELECT(ASTERISK).
        FROM(publications).
        WHERE(new EQUALS("id", pub1.getId()).
            AND(new EQUALS("cid", ctx.getContextId())).
            AND(new EQUALS("user_id", pub2.getUserId())).
            AND(new EQUALS("entity", "3546")).
            AND(new EQUALS("module", "newModule")).
            AND(new EQUALS("target_id", pub2.getTarget().getId())));
        
        assertResult(new StatementBuilder().buildCommand(select));
    }
    
    public void testIDCheckDuringRemember() throws Exception {
        pub1.setId(123);
        try {
            storage.rememberPublication(pub1);
            publicationsToDelete.add(pub1.getId());
            fail("Exception expected");
        } catch (PublicationException e) {
            assertEquals("Wrong error code", PublicationErrorMessage.IDGiven.getDetailNumber(), e.getDetailNumber());
        }
    }
    
    public void testDeleteAllPublicationsOfOneUser() throws PublicationException, TransactionException, SQLException{
        storage.rememberPublication(pub1);
        storage.deletePublicationsOfUser(userId, ctx);
        SELECT select = new SELECT(ASTERISK).FROM(publications).WHERE( new EQUALS("user_id", userId).AND( new EQUALS("cid", ctx.getContextId()) ) );
        assertNoResult(new StatementBuilder().buildCommand(select));
    }

}

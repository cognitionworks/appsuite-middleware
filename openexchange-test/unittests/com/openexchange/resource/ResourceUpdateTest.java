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

package com.openexchange.resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.openexchange.database.Database;
import com.openexchange.groupware.Init;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.resource.internal.ResourceCreate;
import com.openexchange.resource.internal.ResourceUpdate;
import com.openexchange.server.impl.DBPoolingException;
import com.openexchange.test.AjaxInit;

import junit.framework.TestCase;

/**
 * {@link ResourceUpdateTest}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class ResourceUpdateTest extends TestCase {

	private Context ctx;

	private User user;

	private User admin;

	/**
	 * Initializes a new {@link ResourceUpdateTest}
	 */
	public ResourceUpdateTest() {
		super();
	}

	/**
	 * Initializes a new {@link ResourceUpdateTest}
	 * 
	 * @param name
	 *            The test's name
	 */
	public ResourceUpdateTest(final String name) {
		super(name);
	}

	private static Context resolveContext(final String ctxStr) throws Exception {
		try {
			int pos = -1;
			final String c = (pos = ctxStr.indexOf('@')) > -1 ? ctxStr.substring(pos + 1) : ctxStr;
			return ContextStorage.getStorageContext(ContextStorage.getInstance().getContextId(c));
		} catch (final Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	private static User resolveUser(final String user, final Context ctx) throws Exception {
		try {
			int pos = -1;
			final String u = (pos = user.indexOf('@')) > -1 ? user.substring(0, pos) : user;
			return UserStorage.getInstance().getUser(UserStorage.getInstance().getUserId(u, ctx), ctx);
		} catch (final Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	@Override
	protected void setUp() throws Exception {
		try {
			/*
			 * Init
			 */
			Init.startServer();
			/*
			 * Init test environment
			 */
			final String login = AjaxInit.getAJAXProperty("login");
			ctx = resolveContext(login);
			user = resolveUser(login, ctx);
			admin = UserStorage.getInstance().getUser(ctx.getMailadmin(), ctx);
		} catch (final Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	protected void tearDown() throws Exception {
		Init.stopServer();
	}

	public void testResourceUpdate() {
		int id = -1;
		try {
			final Resource resource = createDummyResource(admin, ctx);
			id = resource.getIdentifier();
			assertTrue("Invalid ID detected: " + id + ". ID has not been properly set through creation", id != -1);
			assertTrue("Invalid last-modified detected: " + resource.getLastModified()
					+ ". Last-modified timestamp has not been properly set through creation", resource
					.getLastModified() != null
					&& resource.getLastModified().getTime() < System.currentTimeMillis());
			final long beforeLastModified = resource.getLastModified().getTime();
			/*
			 * Try to update
			 */
			resource.setSimpleName("Foobar-12334");
			resource.setDisplayName("The Foobar Display Name");
			resource.setAvailable(false);
			new ResourceUpdate(admin, ctx, resource).perform();
			/*
			 * Load via storage API
			 */
			Resource storageVersion = ResourceStorage.getInstance().getResource(id, ctx);
			/*
			 * Compare values
			 */
			assertTrue("Invalid last-modified timestamp", resource.getLastModified().getTime() > beforeLastModified
					&& resource.getLastModified().getTime() == storageVersion.getLastModified().getTime());
			assertTrue("Simple name has not been properly updated", resource.getSimpleName().equals(
					storageVersion.getSimpleName()));
			assertTrue("Display name has not been properly updated", resource.getDisplayName().equals(
					storageVersion.getDisplayName()));
			assertTrue("Availability has not been properly updated", resource.isAvailable() == storageVersion
					.isAvailable());
			System.out.println("Resource successfully updated with ID " + id);
			/*
			 * Try to update
			 */
			resource.setDisplayName("The Foobar Display Name qwertz");
			resource.setMail("foobar@somewhere.org");
			resource.setAvailable(true);
			new ResourceUpdate(admin, ctx, resource).perform();
			/*
			 * Load via storage API
			 */
			storageVersion = ResourceStorage.getInstance().getResource(id, ctx);
			/*
			 * Compare values
			 */
			assertTrue("Invalid last-modified timestamp", resource.getLastModified().getTime() > beforeLastModified
					&& resource.getLastModified().getTime() == storageVersion.getLastModified().getTime());
			assertTrue("Simple name has not been properly updated", resource.getSimpleName().equals(
					storageVersion.getSimpleName()));
			assertTrue("Display name has not been properly updated", resource.getDisplayName().equals(
					storageVersion.getDisplayName()));
			assertTrue("Availability has not been properly updated", resource.isAvailable() == storageVersion
					.isAvailable());
			System.out.println("Resource successfully updated with ID " + id);
		} catch (final ResourceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final LdapException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			deleteResource(id, ctx.getContextId());
		}

	}

	public void testResourceCreationFail001() {
		int id = -1;
		try {
			final Resource resource = createDummyResource(admin, ctx);
			id = resource.getIdentifier();
			
			resource.setSimpleName("öäüß");
			new ResourceUpdate(admin, ctx, resource).perform();

			fail("Update succeeded with invalid string identifier");
		} catch (final ResourceException e) {
			System.out.println("Update failed with invalid string identifier: " + e.getMessage());
		} finally {
			deleteResource(id, ctx.getContextId());
		}

	}

	public void testResourceCreationFail002() {
		int id = -1;
		try {
			final Resource resource = createDummyResource(admin, ctx);
			id = resource.getIdentifier();
			
			resource.setMail("mytestresourcesomewhere.com");
			new ResourceUpdate(admin, ctx, resource).perform();

			fail("Update succeeded with invalid email address");
		} catch (final ResourceException e) {
			System.out.println("Update failed with invalid email address: " + e.getMessage());
		} finally {
			deleteResource(id, ctx.getContextId());
		}

	}

	public void testResourceFail006() {
		int id = -1;
		try {
			final Resource resource = createDummyResource(admin, ctx);
			id = resource.getIdentifier();
			
			resource.setSimpleName("foobarfoobar");
			new ResourceUpdate(user, ctx, resource).perform();

			fail("Creation succeeded with non-admin caller");
		} catch (final ResourceException e) {
			System.out.println("Creation failed with non-admin caller: " + e.getMessage());
		} finally {
			deleteResource(id, ctx.getContextId());
		}

	}

	public void testResourceFail007() {
		int id = -1;
		try {
			final Resource resource = createDummyResource(admin, ctx);
			id = resource.getIdentifier();
			
			resource.setSimpleName(null);
			new ResourceUpdate(user, ctx, resource).perform();

			fail("Creation succeeded with missing mandatory field");
		} catch (final ResourceException e) {
			System.out.println("Creation failed with missing mandatory field: " + e.getMessage());
		} finally {
			deleteResource(id, ctx.getContextId());
		}

	}

	private static final Resource createDummyResource(final User admin, final Context ctx) throws ResourceException {
		final Resource resource = new Resource();
		resource.setAvailable(true);
		resource.setDescription("My test resource");
		resource.setDisplayName("MyTestResource");
		resource.setMail("mytestresource@somewhere.com");
		resource.setSimpleName("M-T-R");
		new ResourceCreate(admin, ctx, resource).perform();
		return resource;
	}

	private static final String SQL_DELETE = "DELETE FROM resource WHERE cid = ? AND id = ?";

	private static final void deleteResource(final int id, final int cid) {
		if (-1 == id) {
			return;
		}
		final Connection writeCon;
		try {
			writeCon = Database.get(cid, true);
		} catch (final DBPoolingException e) {
			e.printStackTrace();
			return;
		}
		PreparedStatement stmt = null;
		try {
			stmt = writeCon.prepareStatement(SQL_DELETE);
			stmt.setInt(1, cid);
			stmt.setInt(2, id);
			stmt.executeUpdate();

			System.out.println("Temporary resource with ID " + id + " successfully deleted");
		} catch (final SQLException e) {
			e.printStackTrace();
		} finally {
			if (null != stmt) {
				try {
					stmt.close();
				} catch (final SQLException e) {
				}
				stmt = null;
			}
			Database.back(cid, true, writeCon);
		}

	}
}

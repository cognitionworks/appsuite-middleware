package com.openexchange.webdav.infostore.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.infostore.webdav.InfostoreWebdavFactory;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.server.impl.DBPoolingException;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXFolderManager;
import com.openexchange.tools.oxfolder.OXFolderManagerImpl;
import com.openexchange.webdav.protocol.DummySessionHolder;
import com.openexchange.webdav.protocol.TestWebdavFactoryBuilder;
import com.openexchange.webdav.protocol.WebdavException;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavResource;

// Bug #9109
public class DropBoxScenarioTest extends TestCase{
	
	
	private Context ctx;
	private String user1;
	private String user2;
	
	
	private InfostoreWebdavFactory factory = null;
	
	WebdavPath dropBox = null;
	
	List<WebdavPath> clean = new ArrayList<WebdavPath>();
	
	@Override
	public void setUp() throws Exception {
		
        user1 = "thorben";
        user2 = "francisco"; //FIXME
        
		TestWebdavFactoryBuilder.setUp();

		final ContextStorage ctxstor = ContextStorage.getInstance();
        final int contextId = ctxstor.getContextId("defaultcontext");
        ctx = ctxstor.getContext(contextId);

        factory = (InfostoreWebdavFactory) TestWebdavFactoryBuilder.buildFactory();
        factory.beginRequest();
		try {
		
			switchUser(user1);
			createDropBox();
			
		} catch (final Exception x) {
			tearDown();
			throw x;
		}
	}
	
	@Test public void testAddToDropBox(){
		try {
			switchUser(user2);
			
			WebdavResource res = factory.resolveResource(dropBox.dup().append("testFile"));
			res.putBodyAndGuessLength(new ByteArrayInputStream(new byte[]{1,2,3,4,5,6,7,8,9,10}));
			clean.add(dropBox.dup().append("testFile"));
			res.create();
			
			switchUser(user1);
			
			res = factory.resolveResource(dropBox.dup().append("testFile"));
			final InputStream is = res.getBody();
			
			for(int i = 0; i < 10; i++) {
				assertEquals(i+1, is.read());
			}
			assertEquals(-1, is.read());
			is.close();
			
		} catch (final LdapException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final DBPoolingException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final OXException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final WebdavException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Override
	public void tearDown() throws Exception {
		try {
			switchUser(user1);
			for(final WebdavPath url : clean) {
				factory.resolveResource(url).delete();
			}
		} finally {
			factory.endRequest(200);
			TestWebdavFactoryBuilder.tearDown();
		}
	}
	
	
	private void switchUser(final String username) throws LdapException, DBPoolingException, OXException, SQLException {
		factory.endRequest(200);
		factory.setSessionHolder(new DummySessionHolder(username, ctx));
		factory.beginRequest();
	}
	
	private void createDropBox() throws OXException, WebdavException, ContextException {
		final Session session = factory.getSessionHolder().getSessionObject();
		final OXFolderManager mgr = new OXFolderManagerImpl(session);
		final OXFolderAccess acc = new OXFolderAccess(ContextStorage.getInstance().getContext(session.getContextId()));
		
		final FolderObject fo = acc.getDefaultFolder(session.getUserId(), FolderObject.INFOSTORE);
		
		final FolderObject newFolder = new FolderObject();
		newFolder.setFolderName("Drop Box");
		newFolder.setParentFolderID(fo.getObjectID());
		newFolder.setType(FolderObject.PUBLIC);
		newFolder.setModule(FolderObject.INFOSTORE);
		
		final ArrayList<OCLPermission> perms = new ArrayList<OCLPermission>();
		
		// User is Admin and can read, write or delete everything
		OCLPermission perm = new OCLPermission();
		perm.setEntity(session.getUserId());
		perm.setFolderAdmin(true);
		perm.setFolderPermission(OCLPermission.ADMIN_PERMISSION);
		perm.setReadObjectPermission(OCLPermission.READ_ALL_OBJECTS);
		perm.setWriteObjectPermission(OCLPermission.WRITE_ALL_OBJECTS);
		perm.setDeleteObjectPermission(OCLPermission.DELETE_ALL_OBJECTS);
		perm.setGroupPermission(false);
		perms.add(perm);
		
		
		// Everybody can create objects, but may not read, write or delete
		
		perm = new OCLPermission();
		perm.setEntity(OCLPermission.ALL_GROUPS_AND_USERS);
		perm.setFolderAdmin(false);
		perm.setFolderPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER);
		perm.setReadObjectPermission(OCLPermission.NO_PERMISSIONS);
		perm.setWriteObjectPermission(OCLPermission.NO_PERMISSIONS);
		perm.setDeleteObjectPermission(OCLPermission.NO_PERMISSIONS);
		perm.setGroupPermission(true);
		perms.add(perm);
		
		newFolder.setPermissions(perms);
		
		mgr.createFolder(newFolder, true, System.currentTimeMillis());
		dropBox = new WebdavPath(fo.getFolderName(), newFolder.getFolderName());
		
		clean.add(factory.resolveCollection(dropBox).getUrl());
	}
}

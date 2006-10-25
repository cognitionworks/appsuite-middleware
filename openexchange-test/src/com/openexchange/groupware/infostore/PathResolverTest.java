package com.openexchange.groupware.infostore;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.TestCase;

import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.groupware.Init;
import com.openexchange.groupware.UserConfiguration;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.ContextImpl;
import com.openexchange.groupware.infostore.database.impl.DatabaseImpl;
import com.openexchange.groupware.infostore.database.impl.DocumentMetadataImpl;
import com.openexchange.groupware.infostore.paths.impl.PathResolverImpl;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tx.DBPoolProvider;
import com.openexchange.groupware.tx.DBProvider;
import com.openexchange.server.OCLPermission;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.sessiond.SessionObjectWrapper;
import com.openexchange.tools.oxfolder.OXFolderAction;
import com.openexchange.tools.oxfolder.OXFolderLogicException;
import com.openexchange.tools.oxfolder.OXFolderPermissionException;

public class PathResolverTest extends TestCase {

	private DBProvider provider = new DBPoolProvider();
	private Database database = new DatabaseImpl(provider);
	private PathResolver pathResolver = new PathResolverImpl(provider, database);
	
	private int root;
	
	private int id8;
	private int id7;
	private int id6;
	private int id5;
	private int id4;
	private int id3;
	private int id2;
	private int id;
	
	private int type = FolderObject.PUBLIC;
	
	SessionObject session;
	private Context ctx = null;
	private User user;
	private UserConfiguration userConfig;
	
	public void setUp() throws Exception {
		Init.initDB();
		
		ctx = getContext();
		
		session = SessionObjectWrapper.createSessionObject(UserStorage.getInstance(ctx).getUserId(getUsername()), ctx, "gnitzelgnatzel");
		user = session.getUserObject();
		userConfig = session.getUserConfiguration();
		
		findRoot();
		
		pathResolver.startTransaction();
		id = mkdir(root, "this");
		id2 = mkdir(id, "is");
		id3 = mkdir(id2, "a");
		id4 = mkdir(id3, "nice");
		id5 = mkdir(id4, "path");
		id6 = touch(id5,"document.txt");
		id7 = mkdir(id5,"path");
		id8 = mkdir(id7,"path");
		mkdir(id8,"path");
		
	}
	
	private Context getContext() {
		ContextImpl ctxI = new ContextImpl(1);
		ctxI.setFilestoreId(5);
		ctxI.setFileStorageQuota(Long.MAX_VALUE);
		return ctxI;
	}

	public void tearDown() throws Exception {
		pathResolver.finish();
		rmdir(id8);
		rmdir(id7);
		rmdir(id5);
		rmdir(id4);
		rmdir(id3);
		rmdir(id2);
		rmdir(id);
		Init.stopDB();
	}

	private void findRoot() throws Exception {
		/*Connection con = null;
		try {
			con = provider.getReadConnection(ctx);
			for(int i : FolderObject.getSubfolderIds(FolderObject.SYSTEM_INFOSTORE_FOLDER_ID, ctx, con)) {
				FolderObject f = FolderCacheManager.getInstance().getFolderObject(i, ctx);
				if(f == null)
					f = FolderCacheManager.getInstance().loadFolderObject(i, ctx, con);
				System.out.println(f.getFolderName());
				if(f.getFolderName().toLowerCase().contains(getUsername().toLowerCase())) {
					root = f.getObjectID();
					return;
				}
			}
			throw new IllegalStateException("Can't find users infostore folder!");
		} finally {
			provider.releaseReadConnection(ctx, con);
		}*/
		root = 136;
	}

	private String getUsername() {
		return "francisco"; //FIXME
	}

	public void testResolvePathDocument() throws Exception {
		Resolved resolved = pathResolver.resolve(root, "/this/is/a/nice/path/document.txt", ctx, user, userConfig);
		assertTrue(resolved.isDocument());
		assertFalse(resolved.isFolder());
		assertEquals(id6, resolved.getId());
		
		resolved = pathResolver.resolve(id2, "a/nice/path/document.txt", ctx, user, userConfig);
		assertTrue(resolved.isDocument());
		assertFalse(resolved.isFolder());
		assertEquals(id6, resolved.getId());
	}

	public void testResolvePathFolder() throws Exception {
		Resolved resolved = pathResolver.resolve(root, "/this/is/a/nice/path", ctx, user, userConfig);
		assertFalse(resolved.isDocument());
		assertTrue(resolved.isFolder());
		assertEquals(id5, resolved.getId());
		
		resolved = pathResolver.resolve(id2, "a/nice/path", ctx, user, userConfig);
		assertFalse(resolved.isDocument());
		assertTrue(resolved.isFolder());
		assertEquals(id5, resolved.getId());
	}

	public void testGetPathDocument() throws Exception {
		String path = pathResolver.getPathForDocument(root, id6, ctx, user, userConfig);
		assertEquals("/this/is/a/nice/path/document.txt", path);
	}

	public void testGetPathFolder() throws Exception {
		String path = pathResolver.getPathForFolder(root, id5, ctx, user, userConfig);
		assertEquals("/this/is/a/nice/path", path);
	
	}
	
	public void testNotExists() throws Exception {
		try {
			pathResolver.resolve(root, "/i/dont/exist", ctx, user, userConfig);
			fail("Expected OXObjectNotFoundException");
		} catch (OXObjectNotFoundException x) {
			assertTrue(true);
		}
	}
	
	private int mkdir(int parent, String name) throws SQLException, OXFolderPermissionException, Exception {
	
		OXFolderAction oxfa = new OXFolderAction(session);
		FolderObject folder = new FolderObject();
		folder.setFolderName(name);
		folder.setParentFolderID(parent);
		folder.setType(type);
		folder.setModule(FolderObject.INFOSTORE);
		
		OCLPermission perm = new OCLPermission();
		perm.setEntity(user.getId());
		perm.setFolderAdmin(true);
		perm.setFolderPermission(OCLPermission.ADMIN_PERMISSION);
		perm.setReadObjectPermission(OCLPermission.ADMIN_PERMISSION);
		perm.setWriteObjectPermission(OCLPermission.ADMIN_PERMISSION);
		perm.setDeleteObjectPermission(OCLPermission.ADMIN_PERMISSION);
		perm.setGroupPermission(false);
		
		// All others may read and write
		
		OCLPermission perm2 = new OCLPermission();
		perm2.setEntity(OCLPermission.ALL_GROUPS_AND_USERS);
		perm2.setFolderPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER);
		perm2.setReadObjectPermission(OCLPermission.READ_ALL_OBJECTS);
		perm2.setWriteObjectPermission(OCLPermission.WRITE_ALL_OBJECTS);
		perm2.setDeleteObjectPermission(OCLPermission.DELETE_ALL_OBJECTS);
		
		folder.setPermissionsAsArray(new OCLPermission[]{perm, perm2});
		
		Connection writeCon = null;
		try {
			writeCon = provider.getWriteConnection(ctx);
		} finally {
			if (writeCon != null)
				provider.releaseWriteConnection(ctx, writeCon);
		}
		oxfa.createFolder(folder, session, true, writeCon, writeCon, false);
		return folder.getObjectID();
	}
	
	private int touch(int parent, String filename) throws Exception {
		DocumentMetadata m = new DocumentMetadataImpl();
		m.setFolderId(parent);
		m.setFileName(filename);
		m.setId(Database.NEW);
		database.startTransaction();
		
		try {
			database.saveDocument(m, new ByteArrayInputStream(new byte[10]), Long.MAX_VALUE, session);
			database.commit();
		} catch (Exception x) {
			database.rollback();
			throw x;
		} finally {
			database.finish();	
		}
		return m.getId();
	}
	
	private void rmdir(int id) throws SQLException, OXFolderPermissionException, OXFolderLogicException, Exception {
		OXFolderAction oxfa = new OXFolderAction(session);
		oxfa.deleteFolder(id, session, true, Long.MAX_VALUE);
	}

}

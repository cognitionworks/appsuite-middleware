package com.openexchange.tools.oxfolder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.Init;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.ContextStorage;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFailedException;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.server.DBPool;
import com.openexchange.server.DBPoolingException;
import com.openexchange.server.OCLPermission;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.sessiond.SessionObjectWrapper;

public class OXFolderDeleteListenerTest extends TestCase {
	SessionObject session = null;
	int myInfostoreFolder = 0;
	
	int userWhichWillBeDeletedId = 0;
	int userWhichWillRemainId = 0;
	int contextAdminId = 0;
	
	OXFolderAccess oxfa = null;
	OXFolderManager oxma = null;
	
	List<FolderObject> clean = new LinkedList<FolderObject>();
	
	public void setUp() throws Exception {
		Init.initDB();
		ContextStorage.init();
		Context ctx = ContextStorage.getInstance().getContext(1);
		userWhichWillBeDeletedId = UserStorage.getInstance(ctx).getUserId("francisco");
		userWhichWillRemainId = UserStorage.getInstance(ctx).getUserId("thorben");
		contextAdminId = ctx.getMailadmin(); // TODO
		
		session = SessionObjectWrapper.createSessionObject(userWhichWillBeDeletedId, ctx, "Blubb");
		
		oxfa = new OXFolderAccess(ctx);
		oxma = new OXFolderManagerImpl(session);
		
		myInfostoreFolder = oxfa.getDefaultFolder(session.getUserObject().getId(), FolderObject.INFOSTORE).getObjectID();
	}
	
	public void tearDown() throws Exception {
		for(FolderObject fo : clean) {
			oxma.deleteFolder(fo, false, System.currentTimeMillis());
		}
		Init.stopDB();
	}
	
	// Bug 7503
	public void testPublicFolderTransferPermissionsToAdmin() throws OXException, LdapException, DBPoolingException, DeleteFailedException, SQLException{
		
		FolderObject testFolder = createPublicInfostoreSubfolderWithAdmin(myInfostoreFolder, userWhichWillBeDeletedId);
		clean.add(testFolder);
		
		testFolder = assignAdminPermissions(testFolder, userWhichWillRemainId);
		
		simulateUserDelete(userWhichWillBeDeletedId);
		
		checkUserInPermissionsOfFolder(testFolder.getObjectID(), userWhichWillRemainId);
		checkUserInPermissionsOfFolder(testFolder.getObjectID(), contextAdminId);
		
	}

	public void checkUserInPermissionsOfFolder(int folderId, int user) throws OXException {
		FolderObject fo = oxfa.getFolderObject(folderId);
		for(OCLPermission ocl : fo.getPermissions()) {
			if(ocl.getEntity() == user) {
				return;
			}
		}
		fail("Can't find permission for user "+user+" for folder "+fo.getFolderName()+" ("+fo.getObjectID()+")");
	}

	public void simulateUserDelete(int deleteMe) throws LdapException, DBPoolingException, DeleteFailedException, SQLException {
		DeleteEvent delEvent = new DeleteEvent(this, deleteMe, DeleteEvent.TYPE_USER,session.getContext());
		
		Connection con = null;
		try {
			con = DBPool.pickupWriteable(session.getContext());
			new OXFolderDeleteListener().deletePerformed(delEvent, con, con);
		} finally {
			DBPool.closeWriterSilent(session.getContext(), con);
		}
		
	}

	public FolderObject assignAdminPermissions(FolderObject folder, int user) throws OXException {
		final OCLPermission ocl = new OCLPermission();
		ocl.setEntity(user);
		ocl.setAllPermission(OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION);
		ocl.setGroupPermission(false);
		ocl.setFolderAdmin(true);
		
		OCLPermission[] oldPerms = folder.getPermissionsAsArray();
		OCLPermission[] newPerms = new OCLPermission[oldPerms.length+1];
		System.arraycopy(oldPerms, 0, newPerms, 0, oldPerms.length);
		newPerms[oldPerms.length] = ocl;
		
		folder.setPermissionsAsArray(newPerms);
		
		return oxma.updateFolder(folder, true, System.currentTimeMillis());
	}

	public FolderObject createPublicInfostoreSubfolderWithAdmin(int parentFolder, int folderAdmin) throws OXException {
		FolderObject fo = new FolderObject();
		fo.setFolderName(""+System.currentTimeMillis());
		fo.setParentFolderID(parentFolder);
		fo.setModule(FolderObject.INFOSTORE);
		fo.setType(FolderObject.PUBLIC);
		final OCLPermission ocl = new OCLPermission();
		ocl.setEntity(folderAdmin);
		ocl.setAllPermission(OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION);
		ocl.setGroupPermission(false);
		ocl.setFolderAdmin(true);
		fo.setPermissionsAsArray(new OCLPermission[] { ocl });
		
		FolderObject created = oxma.createFolder(fo, true, System.currentTimeMillis());
		return created;
	}

}

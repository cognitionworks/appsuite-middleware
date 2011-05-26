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

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.api2.OXException;
import com.openexchange.cache.impl.FolderCacheManager;
import com.openexchange.cache.impl.FolderCacheNotEnabledException;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.WebdavFolderAliases;
import com.openexchange.groupware.infostore.database.impl.InfostoreSecurity;
import com.openexchange.groupware.infostore.webdav.URLCache.Type;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.sessiond.impl.SessionHolder;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.oxfolder.OXFolderException;
import com.openexchange.tools.oxfolder.OXFolderIteratorSQL;
import com.openexchange.tools.oxfolder.OXFolderManager;
import com.openexchange.tools.oxfolder.OXFolderPermissionException;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.webdav.protocol.Protocol;
import com.openexchange.webdav.protocol.WebdavCollection;
import com.openexchange.webdav.protocol.WebdavFactory;
import com.openexchange.webdav.protocol.WebdavLock;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.WebdavProperty;
import com.openexchange.webdav.protocol.WebdavProtocolException;
import com.openexchange.webdav.protocol.WebdavResource;
import com.openexchange.webdav.protocol.Protocol.Property;
import com.openexchange.webdav.protocol.impl.AbstractCollection;

public class FolderCollection extends AbstractCollection implements OXWebdavResource {

	private static final Log LOG = LogFactory.getLog(FolderCollection.class);
	private final InfostoreWebdavFactory factory;
	private WebdavPath url;
	private final PropertyHelper propertyHelper;
	private final SessionHolder sessionHolder;
	private final FolderLockHelper lockHelper;

	private FolderObject folder;
	private int id;
	private boolean exists;
	private boolean loaded;
	private final DBProvider provider;
	private final Set<OXWebdavResource> children = new HashSet<OXWebdavResource>();
	
	private boolean loadedChildren;
	private ArrayList<OCLPermission> overrideNewACL;
    private final InfostoreSecurity security;

    private final WebdavFolderAliases aliases;
    
    public FolderCollection(final WebdavPath url, final InfostoreWebdavFactory factory) {
		this(url,factory,null);
	}
	
	public FolderCollection(final WebdavPath url, final InfostoreWebdavFactory factory, final FolderObject folder) {
		this.url = url;
		this.factory = factory;
		this.sessionHolder = factory.getSessionHolder();
		this.propertyHelper = new PropertyHelper(factory.getFolderProperties(), sessionHolder, url);
		this.lockHelper = new FolderLockHelper(factory.getFolderLockManager(), sessionHolder, url);
        this.security = factory.getSecurity();
        this.provider = factory.getProvider();
        this.aliases = factory.getAliases();
        if(folder!=null) {
			setId(folder.getObjectID());
			this.folder = folder;
			this.loaded = true;
			this.exists = true;
		}
	}

	@Override
	public void delete() throws WebdavProtocolException {
		if(!exists) {
			return;
		}
//		OXFolderManager oxma = new OXFolderManagerImpl(getSession());
//		OXFolderAction oxfa = new OXFolderAction(getSession());
		Connection con = null;
		try {
			con = provider.getWriteConnection(getSession().getContext());
			final OXFolderManager oxma = OXFolderManager.getInstance(getSession(), con, con);
			oxma.deleteFolder(new FolderObject(id), true, System.currentTimeMillis());
			//oxfa.deleteFolder(id, getSession(),con, con, true,System.currentTimeMillis()); // FIXME
			exists = false;
			factory.removed(this);
		} catch (final OXFolderException x) {
			if(isPermissionException(x)) {
			    throw new WebdavProtocolException(x, url, HttpServletResponse.SC_FORBIDDEN);
			}
			throw new WebdavProtocolException(x, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);		
		} catch (final Exception e) {
		    throw new WebdavProtocolException(e, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} finally {
			if(con != null) {
				provider.releaseWriteConnection(getSession().getContext(), con);
			}
		}
		final Set<OXWebdavResource> set = new HashSet<OXWebdavResource>(children);
		for(final OXWebdavResource res : set) {
			res.removedParent();
		}
	}
	
	private WebdavResource mergeTo(final FolderCollection to, final boolean move, final boolean overwrite) throws WebdavProtocolException {

		
		final int lengthUrl = getUrl().size();

		for(final WebdavResource res : getChildren()) {
			final WebdavPath toUrl = to.getUrl().dup().append(res.getUrl().subpath(lengthUrl));
			if(move) {
				res.move(toUrl, false, overwrite);
			} else {
				res.copy(toUrl, false, overwrite);
			}

		}
		
		return this;
	}
	
	@Override
	public WebdavResource move(final WebdavPath dest, final boolean noroot, final boolean overwrite) throws WebdavProtocolException {
		final FolderCollection coll = (FolderCollection) factory.resolveCollection(dest);
		if(coll.exists()) {
			if(overwrite) {
				loadFolder();
				final ArrayList<OCLPermission> override = new ArrayList<OCLPermission>();
				for(final OCLPermission perm : folder.getPermissions()) {
					override.add(perm.deepClone());
				}
				coll.loadFolder();
				coll.folder.setPermissions(override);
				coll.save();
			}
			final WebdavResource moved = mergeTo(coll, true, overwrite);
			delete();
			return moved;
		}
		loadFolder();
		final String name = dest.name();
		final int parentId =  ((OXWebdavResource) coll.parent()).getId();
		

        folder.setFolderName(name);
		folder.setParentFolderID(parentId);
		
		
		invalidate();
		factory.invalidate(url, id, Type.COLLECTION);
		factory.invalidate(dest, id, Type.COLLECTION);
		
		
		url = dest;
		save();
		try {
			lockHelper.deleteLocks();
		} catch (final OXException e) {
			throw new WebdavProtocolException(getUrl(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return this;
	}
	
	

	@Override
	public WebdavResource copy(final WebdavPath dest, final boolean noroot, final boolean overwrite) throws WebdavProtocolException {
		final FolderCollection coll = (FolderCollection) factory.resolveCollection(dest);
		if(coll.exists()) {
			if (overwrite) {
				final ArrayList<OCLPermission> override = new ArrayList<OCLPermission>();
				loadFolder();
				for(final OCLPermission perm : folder.getPermissions()) {
					override.add(perm.deepClone());
				}
				coll.loadFolder();
				coll.folder.setPermissions(override);
				coll.save();
			}
		} else {
			loadFolder();
			final ArrayList<OCLPermission> override = new ArrayList<OCLPermission>();
			for(final OCLPermission perm : folder.getPermissions()) {
				override.add(perm.deepClone());
			}
			coll.overrideNewACL = override;
			coll.create();
			copyProperties(coll);
		}
		return mergeTo(coll, false, overwrite);
	}

	private void copyProperties(final FolderCollection coll) throws WebdavProtocolException {
		for(final WebdavProperty prop : internalGetAllProps()) {
			coll.putProperty(prop);
		}
	}

	@Override
	protected WebdavCollection parent() throws WebdavProtocolException {
		if(url != null) {
			return super.parent();
		}
		loadFolder();
		return (WebdavCollection) factory.getCollections(Arrays.asList(Integer.valueOf(folder.getParentFolderID()))).iterator().next();
	}

	private void invalidate() {
		for(final OXWebdavResource res : children) {
			Type t = Type.RESOURCE;
			if(res.isCollection()) {
				((FolderCollection) res).invalidate();
				t = Type.COLLECTION;
			}
			factory.invalidate(res.getUrl(), res.getId(), t);
		}
	}

	@Override
	protected void internalDelete(){
		throw new IllegalStateException("Should be called only by superclass");
	}

	@Override
	protected WebdavFactory getFactory() {
		return factory;
	}

	@Override
	protected List<WebdavProperty> internalGetAllProps() throws WebdavProtocolException {
		return propertyHelper.getAllProps();
	}

	@Override
	protected WebdavProperty internalGetProperty(final String namespace, final String name) throws WebdavProtocolException {
		return propertyHelper.getProperty(namespace, name);
	}

	@Override
	protected void internalPutProperty(final WebdavProperty prop) throws WebdavProtocolException {
		propertyHelper.putProperty(prop);
	}

	@Override
	protected void internalRemoveProperty(final String namespace, final String name) throws WebdavProtocolException {
		propertyHelper.removeProperty(namespace, name);
	}

	@Override
	protected boolean isset(final Property p) {
		if (p.getId() == Protocol.GETCONTENTLANGUAGE || p.getId() == Protocol.GETCONTENTLENGTH || p.getId() == Protocol.GETETAG) {
			return false;
		}
		return !propertyHelper.isRemoved(new WebdavProperty(p.getNamespace(), p.getName()));
	}

	@Override
	public void setCreationDate(final Date date) throws WebdavProtocolException {
		folder.setCreationDate(date);
	}

	public List<WebdavResource> getChildren() throws WebdavProtocolException {
		loadChildren();
		return new ArrayList<WebdavResource>(children);
	}
	public void create() throws WebdavProtocolException {
		if(exists) {
		    throw new WebdavProtocolException(WebdavProtocolException.Code.DIRECTORY_ALREADY_EXISTS, getUrl(), HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}
		save();
		exists=true;
		factory.created(this);
	}

	public boolean exists() throws WebdavProtocolException {
		return exists;
	}

	public Date getCreationDate() throws WebdavProtocolException {
		loadFolder();
		return folder.getCreationDate();
	}

	public String getDisplayName() throws WebdavProtocolException {
		loadFolder();
		return getFolderName(folder);
	}

	public Date getLastModified() throws WebdavProtocolException {
		loadFolder();
		return folder.getLastModified();
	}

	public WebdavLock getLock(final String token) throws WebdavProtocolException {
		final WebdavLock lock = lockHelper.getLock(token);
		if(lock != null) {
			return lock;
		}
		return findParentLock(token);
	}

	public List<WebdavLock> getLocks() throws WebdavProtocolException {
		final List<WebdavLock> lockList =  getOwnLocks();
		addParentLocks(lockList);
		return lockList;
	}

	public WebdavLock getOwnLock(final String token) throws WebdavProtocolException {
		return lockHelper.getLock(token);
	}

	public List<WebdavLock> getOwnLocks() throws WebdavProtocolException {
		return lockHelper.getAllLocks();
	}

	public String getSource() throws WebdavProtocolException { 
		// IGNORE
		return null;
	}

	public WebdavPath getUrl() {
		if(url == null) {
			initUrl();
		}
		return url;
	}

	public void lock(final WebdavLock lock) throws WebdavProtocolException {
		lockHelper.addLock(lock);
	}
	
	public void save() throws WebdavProtocolException {
		try {
			dumpToDB();
            if(propertyHelper.mustWrite()) {
                final ServerSession session = getSession();
                final EffectivePermission perm = security.getFolderPermission(getId(),session.getContext(), UserStorage.getStorageUser(session.getUserId(), session.getContext()),
					UserConfigurationStorage.getInstance().getUserConfigurationSafe(session.getUserId(),
							session.getContext()));
                if(!perm.isFolderAdmin()) {
                    throw new WebdavProtocolException(WebdavProtocolException.Code.NO_WRITE_PERMISSION, getUrl(), HttpServletResponse.SC_FORBIDDEN);
                }
            }
            propertyHelper.dumpPropertiesToDB();
			lockHelper.dumpLocksToDB();
		} catch (final WebdavProtocolException x) {
			throw x;
		} catch (final Exception x) {
		    throw new WebdavProtocolException(x, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public void setDisplayName(final String displayName) throws WebdavProtocolException {
		//loadFolder();
		//folder.setFolderName(displayName);
		//changedFields.add(FolderObject.FOLDER_NAME);
		//FIXME
	}

	public void unlock(final String token) throws WebdavProtocolException {
		lockHelper.removeLock(token);
	}

	public void setId(final int id) {
		this.id = id;
		this.propertyHelper.setId(id);
		this.lockHelper.setId(id);
	}

	public void setExists(final boolean b) {
		this.exists = b;
	}
		
	private void loadFolder() throws WebdavProtocolException {
		if(loaded) {
			return;
		}
		loaded = true;
		if(!exists) {
			folder = new FolderObject();
			return;
		}
		Connection readCon = null;
		final Context ctx = getSession().getContext();
		try {
			readCon = provider.getReadConnection(ctx);
			if(FolderCacheManager.isEnabled()) {
				folder = FolderCacheManager.getInstance().getFolderObject(id, false, ctx, readCon); // FIXME be smarter here
			} else {
				
				folder = FolderObject.loadFolderObjectFromDB(id, ctx, readCon);
				
			}
		} catch (final FolderCacheNotEnabledException e) {
			LOG.error("",e);
		} catch (final Exception e) {
		    throw new WebdavProtocolException(e, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} finally {
			provider.releaseReadConnection(ctx, readCon);
		}
	}
	
	private void dumpToDB() throws WebdavProtocolException {
		//OXFolderAction oxfa = new OXFolderAction(getSession());
		if(exists) {
			if(folder == null) {
				return;
			}
			folder.setLastModified(new Date());
			folder.setModifiedBy(getSession().getUserId()); // Java train of death
			initParent(folder);
			final ServerSession session = getSession();
			final Context ctx = session.getContext();
			
			Connection writeCon = null;
			
			try {
				
				writeCon = provider.getWriteConnection(ctx);
				final OXFolderManager oxma = OXFolderManager.getInstance(getSession(), writeCon, writeCon);
				oxma.updateFolder(folder, true, System.currentTimeMillis());
				//oxfa.updateMoveRenameFolder(folder, session, true, folder.getLastModified().getTime(), writeCon, writeCon);
			} catch (final OXFolderException x) {
				if(isPermissionException(x)) {
				    throw new WebdavProtocolException(x, url, HttpServletResponse.SC_FORBIDDEN);
				}
				throw new WebdavProtocolException(x, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} catch (final OXFolderPermissionException e) {
			    throw new WebdavProtocolException(e, url, HttpServletResponse.SC_FORBIDDEN);
			} catch (final Exception e) {
			    throw new WebdavProtocolException(e, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} finally {
				provider.releaseWriteConnection(ctx, writeCon);
			}
		} else {
			if(folder == null) {
				folder = new FolderObject();
			}
			initDefaultAcl(folder);
			initDefaultFields(folder);
			
			final ServerSession session = getSession();
			final Context ctx = session.getContext();
			
			Connection writeCon = null;
			
			try {
				writeCon = provider.getWriteConnection(ctx);
				final OXFolderManager oxma = OXFolderManager.getInstance(getSession(), writeCon, writeCon);
				folder = oxma.createFolder(folder, true, System.currentTimeMillis());
				//oxfa.createFolder(folder, session, true, writeCon, writeCon, true);
				setId(folder.getObjectID());
			} catch (final OXFolderException x) {
				if (isPermissionException(x)) {
				    throw new WebdavProtocolException(x, url, HttpServletResponse.SC_FORBIDDEN);
				}
				throw new WebdavProtocolException(x, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} catch (final OXFolderPermissionException e) {
			    throw new WebdavProtocolException(e, url, HttpServletResponse.SC_FORBIDDEN);
			} catch (final Exception e) {
			    throw new WebdavProtocolException(e, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} finally {
				provider.releaseWriteConnection(ctx, writeCon);
			}
		}
		
	}
	
	private boolean isPermissionException(final OXFolderException x) {
		return Category.PERMISSION.equals(x.getCategory());
	}

	private void initDefaultFields(final FolderObject folder) throws WebdavProtocolException {
		initParent(folder);
		folder.setType(FolderObject.PUBLIC);
		folder.setModule(FolderObject.INFOSTORE);
		if (folder.getFolderName() == null || folder.getFolderName().length() == 0) {
			//if(url.contains("/")) {
				folder.setFolderName(url.name());
			//}
		}
        folder.removeObjectID();
    }
	
	private void initParent(final FolderObject folder) throws WebdavProtocolException{
		try {
			final FolderCollection parent = (FolderCollection) parent();
			if(!parent.exists()) {
				throw new WebdavProtocolException(getUrl(), HttpServletResponse.SC_CONFLICT);
			}
			folder.setParentFolderID(parent.id);	
		} catch (final ClassCastException x) {
			throw new WebdavProtocolException(getUrl(), HttpServletResponse.SC_CONFLICT);
		}
		
	}

	private void initDefaultAcl(final FolderObject folder) throws WebdavProtocolException {
		
		final List<OCLPermission> copyPerms;
		
		if(this.overrideNewACL == null) {
			final FolderCollection parent = (FolderCollection) parent();
			parent.loadFolder();
			final FolderObject parentFolder = parent.folder;
			if(FolderObject.SYSTEM_MODULE == parentFolder.getType()) {
			    copyPerms = Collections.emptyList(); 
			} else {
	            copyPerms = parentFolder.getPermissions();          
			}
		} else {
			copyPerms = this.overrideNewACL;
		}
		
		final ArrayList<OCLPermission> newPerms = new ArrayList<OCLPermission>();

		final User owner = UserStorage.getStorageUser(getSession().getUserId(), getSession().getContext());
		
		for(final OCLPermission perm : copyPerms) {
			if(perm.getEntity() != owner.getId()){
				newPerms.add(perm.deepClone());
			} 
		}
		
		
		// Owner has all permissions
		final OCLPermission perm = new OCLPermission();
		perm.setEntity(owner.getId());
		perm.setFolderAdmin(true);
		perm.setFolderPermission(OCLPermission.ADMIN_PERMISSION);
		perm.setReadObjectPermission(OCLPermission.READ_ALL_OBJECTS);
		perm.setWriteObjectPermission(OCLPermission.WRITE_ALL_OBJECTS);
		perm.setDeleteObjectPermission(OCLPermission.DELETE_ALL_OBJECTS);
		perm.setGroupPermission(false);
		newPerms.add(perm);
		
		// All others may read and write
		
		/*OCLPermission perm2 = new OCLPermission();
		perm2.setFolderPermission(OCLPermission.CREATE_SUB_FOLDERS);
		perm2.setEntity(OCLPermission.ALL_GROUPS_AND_USERS);
		perm2.setReadObjectPermission(OCLPermission.READ_ALL_OBJECTS);
		perm2.setWriteObjectPermission(OCLPermission.WRITE_ALL_OBJECTS);
		perm2.setDeleteObjectPermission(OCLPermission.DELETE_ALL_OBJECTS); */
		folder.setPermissions(newPerms);
	}

	private void loadChildren() throws WebdavProtocolException {
		if(loadedChildren || !exists) {
			return;
		}
		loadedChildren = true;
		try {
			if(folder==null) {
				loadFolder();
			}
			final ServerSession session = getSession();
			final User user = UserStorage.getStorageUser(session.getUserId(), session.getContext());
			final UserConfiguration userConfig = UserConfigurationStorage.getInstance().getUserConfigurationSafe(session.getUserId(), session.getContext());
			final Context ctx = session.getContext();
			
			final SearchIterator<FolderObject> iter = OXFolderIteratorSQL.getVisibleSubfoldersIterator(id, user.getId(),user.getGroups(), ctx, userConfig, new Timestamp(0));
			//final SearchIterator iter = OXFolderTools.getVisibleSubfoldersIterator(id, user.getId(),user.getGroups(), ctx, userConfig, new Timestamp(0));
			

			while(iter.hasNext()) {
				final FolderObject folder = iter.next();
				final WebdavPath newUrl = getUrl().dup().append(getFolderName(folder));
                children.add(new FolderCollection(newUrl, factory, folder));
			}
			
			//children.addAll(factory.getCollections(folder.getSubfolderIds(true, getSession().getContext())));
			children.addAll(factory.getResourcesInFolder(this, folder.getObjectID()));
		} catch (final Exception e) {
		    throw new WebdavProtocolException(e, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		// Duplicates?
	}

	public int getId() {
		return id;
	}

	public void initUrl() {
		if(id == FolderObject.SYSTEM_INFOSTORE_FOLDER_ID) {
			url = new WebdavPath();
			return;
		}
		try {
			url = parent().getUrl().dup().append(getDisplayName());
		} catch (final WebdavProtocolException e) {
			if (LOG.isErrorEnabled()) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	public void registerChild(final OXWebdavResource resource) {
		children.add(resource);
	}
	
	public void unregisterChild(final OXWebdavResource resource) {
		children.remove(resource);
	}

	public int getParentId() throws WebdavProtocolException {
		if(exists) {
			loadFolder();
			return folder.getParentFolderID();
		}
		final WebdavPath url = getUrl();
		return ((OXWebdavResource) factory.resolveCollection(url.parent())).getId();
	}

	public void removedParent() throws WebdavProtocolException {
		exists = false;
		factory.removed(this);
		for(final OXWebdavResource res : children) { res.removedParent(); }
	}

	public void transferLock(final WebdavLock lock) throws WebdavProtocolException {
		try {
			lockHelper.transferLock(lock);
		} catch (final OXException e) {
		    throw new WebdavProtocolException(e, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	@Override
	public String toString(){
		return super.toString()+" :"+id;
	}

    public String getFolderName(final FolderObject folder) {
        if(aliases != null) {
            final String alias = aliases.getAlias(folder.getObjectID());
            if(alias != null) {
                return alias;
            }
        }
        return folder.getFolderName();
    }

    public EffectivePermission getEffectivePermission() throws WebdavProtocolException {
        loadFolder();
        final ServerSession session = getSession();

        final UserConfiguration userConfig = UserConfigurationStorage.getInstance().getUserConfigurationSafe(session.getUserId(), session.getContext());
        final Context ctx = session.getContext();

        Connection con = null;
        try {
            con = provider.getReadConnection(ctx);
            return folder.getEffectiveUserPermission(session.getUserId(), userConfig, con);
        } catch (final Exception e) {
            throw new WebdavProtocolException(e, url, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {

            if (con != null) {
                provider.releaseReadConnection(ctx, con);
            }
        }

    }

    public boolean isRoot() {
        return id == FolderObject.SYSTEM_INFOSTORE_FOLDER_ID;
    }

     private ServerSession getSession() {
        return new ServerSessionAdapter(sessionHolder.getSessionObject(), sessionHolder.getContext());
    }
}

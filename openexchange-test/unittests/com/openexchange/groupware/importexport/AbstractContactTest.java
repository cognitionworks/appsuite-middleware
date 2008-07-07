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

package com.openexchange.groupware.importexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.mail.Session;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.openexchange.api2.ContactSQLInterface;
import com.openexchange.api2.OXException;
import com.openexchange.api2.RdbContactSQLInterface;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.Init;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.container.DataObject;
import com.openexchange.groupware.container.FolderChildObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.contexts.impl.ContextImpl;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.importexport.exceptions.ImportExportException;
import com.openexchange.groupware.ldap.Credentials;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.server.impl.DBPoolingException;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.sessiond.impl.SessionObject;
import com.openexchange.test.AjaxInit;
import com.openexchange.tools.oxfolder.OXFolderException;
import com.openexchange.tools.oxfolder.OXFolderManager;
import com.openexchange.tools.oxfolder.OXFolderManagerImpl;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionFactory;

/**
 * Basis for folder tests: Creates a folder and deletes it after testing.
 * 
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb' Prinz</a>
 *
 */
public class AbstractContactTest {
	
	public static class TestSession extends SessionObject {
		
		/**
		 * This class is needed to fake permissions for different modules.
		 * @param sessionid
		 */
		public TestSession(final String sessionid) {
			super(sessionid);
		}
		
		public SessionObject delegateSessionObject;
		public UserConfiguration delegateUserConfiguration;
		
		@Override
		public void closingOperations() {
			delegateSessionObject.closingOperations();
		}
		@Override
		public boolean equals(final Object obj) {
			return delegateSessionObject.equals(obj);
		}
		@Override
		public int getContextId() {
			return delegateSessionObject.getContextId();
		}
		@Override
		public Date getCreationtime() {
			return delegateSessionObject.getCreationtime();
		}
		@Override
		public Credentials getCredentials() {
			return delegateSessionObject.getCredentials();
		}
		@Override
		public String getHost() {
			return delegateSessionObject.getHost();
		}
		@Override
		public String getLanguage() {
			return delegateSessionObject.getLanguage();
		}
		@Override
		public long getLifetime() {
			return delegateSessionObject.getLifetime();
		}
		@Override
		public String getLocalIp() {
			return delegateSessionObject.getLocalIp();
		}
		@Override
		public String getLoginName() {
			return delegateSessionObject.getLoginName();
		}
		@Override
		public Session getMailSession() {
			return delegateSessionObject.getMailSession();
		}
		@Override
		public String getPassword() {
			return delegateSessionObject.getPassword();
		}
		@Override
		public String getRandomToken() {
			return delegateSessionObject.getRandomToken();
		}
		@Override
		public String getSecret() {
			return delegateSessionObject.getSecret();
		}
		@Override
		public String getSessionID() {
			return delegateSessionObject.getSessionID();
		}
		@Override
		public Date getTimestamp() {
			return delegateSessionObject.getTimestamp();
		}
		public UserConfiguration getUserConfiguration() {
			return delegateUserConfiguration;
		}
		@Override
		public String getUserlogin() {
			return delegateSessionObject.getUserlogin();
		}
		@Override
		public String getUsername() {
			return delegateSessionObject.getUsername();
		}
		@Override
		public int hashCode() {
			return delegateSessionObject.hashCode();
		}
		@Override
		public void setContextId(final int id) {
			delegateSessionObject.setContextId(id);
		}
		@Override
		public void setCreationtime(final Date creationtime) {
			delegateSessionObject.setCreationtime(creationtime);
		}
		@Override
		public void setCredentials(final Credentials cred) {
			delegateSessionObject.setCredentials(cred);
		}
		@Override
		public void setHost(final String host) {
			delegateSessionObject.setHost(host);
		}
		@Override
		public void setLanguage(final String language) {
			delegateSessionObject.setLanguage(language);
		}
		@Override
		public void setLifetime(final long lifetime) {
			delegateSessionObject.setLifetime(lifetime);
		}
		@Override
		public void setLocalIp(final String localip) {
			delegateSessionObject.setLocalIp(localip);
		}
		@Override
		public void setLoginName(final String loginName) {
			delegateSessionObject.setLoginName(loginName);
		}
		@Override
		public void setMailSession(final Session mailSession) {
			delegateSessionObject.setMailSession(mailSession);
		}
		@Override
		public void setPassword(final String password) {
			delegateSessionObject.setPassword(password);
		}
		@Override
		public void setRandomToken(final String randomToken) {
			delegateSessionObject.setRandomToken(randomToken);
		}
		@Override
		public void setSecret(final String secret) {
			delegateSessionObject.setSecret(secret);
		}
		@Override
		public void setTimestamp(final Date timestamp) {
			delegateSessionObject.setTimestamp(timestamp);
		}
		@Override
		public void setUserlogin(final String userlogin) {
			delegateSessionObject.setUserlogin(userlogin);
		}
		@Override
		public void setUsername(final String username) {
			delegateSessionObject.setUsername(username);
		}
		@Override
		public String toString() {
			return delegateSessionObject.toString();
		}
	}
	
	protected static final int[] POSSIBLE_FIELDS = {
			DataObject.OBJECT_ID,
			DataObject.CREATED_BY,
			DataObject.CREATION_DATE,
			DataObject.LAST_MODIFIED,
			DataObject.MODIFIED_BY,
			FolderChildObject.FOLDER_ID,
	//		CommonObject.PRIVATE_FLAG,
	//		CommonObject.CATEGORIES,
			ContactObject.GIVEN_NAME,
			ContactObject.SUR_NAME,
			ContactObject.ANNIVERSARY,
			ContactObject.ASSISTANT_NAME,
			ContactObject.BIRTHDAY,
			ContactObject.BRANCHES,
			ContactObject.BUSINESS_CATEGORY,
			ContactObject.CATEGORIES,
			ContactObject.CELLULAR_TELEPHONE1,
			ContactObject.CELLULAR_TELEPHONE2,
			ContactObject.CITY_BUSINESS,
			ContactObject.CITY_HOME,
			ContactObject.CITY_OTHER,
			ContactObject.COMMERCIAL_REGISTER,
			ContactObject.COMPANY,
			ContactObject.COUNTRY_BUSINESS,
			ContactObject.COUNTRY_HOME,
			ContactObject.COUNTRY_OTHER,
			ContactObject.DEPARTMENT,
			ContactObject.DISPLAY_NAME,
	//		ContactObject.DISTRIBUTIONLIST,
			ContactObject.EMAIL1,
			ContactObject.EMAIL2,
			ContactObject.EMAIL3,
			ContactObject.EMPLOYEE_TYPE,
			ContactObject.FAX_BUSINESS,
			ContactObject.FAX_HOME,
			ContactObject.FAX_OTHER,
	//		ContactObject.FILE_AS,
			ContactObject.FOLDER_ID,
			ContactObject.GIVEN_NAME,
	//		ContactObject.IMAGE1,
	//		ContactObject.IMAGE1_CONTENT_TYPE,
			ContactObject.INFO,
			ContactObject.INSTANT_MESSENGER1,
			ContactObject.INSTANT_MESSENGER2,
	//		ContactObject.LINKS,
			ContactObject.MANAGER_NAME,
			ContactObject.MARITAL_STATUS,
			ContactObject.MIDDLE_NAME,
			ContactObject.NICKNAME,
			ContactObject.NOTE,
			ContactObject.NUMBER_OF_CHILDREN,
			ContactObject.NUMBER_OF_EMPLOYEE,
			ContactObject.POSITION,
			ContactObject.POSTAL_CODE_BUSINESS,
			ContactObject.POSTAL_CODE_HOME,
			ContactObject.POSTAL_CODE_OTHER,
	//		ContactObject.PRIVATE_FLAG,
			ContactObject.PROFESSION,
			ContactObject.ROOM_NUMBER,
			ContactObject.SALES_VOLUME,
			ContactObject.SPOUSE_NAME,
			ContactObject.STATE_BUSINESS,
			ContactObject.STATE_HOME,
			ContactObject.STATE_OTHER,
			ContactObject.STREET_BUSINESS,
			ContactObject.STREET_HOME,
			ContactObject.STREET_OTHER,
			ContactObject.SUFFIX,
			ContactObject.TAX_ID,
			ContactObject.TELEPHONE_ASSISTANT,
			ContactObject.TELEPHONE_BUSINESS1,
			ContactObject.TELEPHONE_BUSINESS2,
			ContactObject.TELEPHONE_CALLBACK,
			ContactObject.TELEPHONE_CAR,
			ContactObject.TELEPHONE_COMPANY,
			ContactObject.TELEPHONE_HOME1,
			ContactObject.TELEPHONE_HOME2,
			ContactObject.TELEPHONE_IP,
			ContactObject.TELEPHONE_ISDN,
			ContactObject.TELEPHONE_OTHER,
			ContactObject.TELEPHONE_PAGER,
			ContactObject.TELEPHONE_PRIMARY,
			ContactObject.TELEPHONE_RADIO,
			ContactObject.TELEPHONE_TELEX,
			ContactObject.TELEPHONE_TTYTDD,
			ContactObject.TITLE,
			ContactObject.URL,
			ContactObject.USERFIELD01,
			ContactObject.USERFIELD02,
			ContactObject.USERFIELD03,
			ContactObject.USERFIELD04,
			ContactObject.USERFIELD05,
			ContactObject.USERFIELD06,
			ContactObject.USERFIELD07,
			ContactObject.USERFIELD08,
			ContactObject.USERFIELD09,
			ContactObject.USERFIELD10,
			ContactObject.USERFIELD11,
			ContactObject.USERFIELD12,
			ContactObject.USERFIELD13,
			ContactObject.USERFIELD14,
			ContactObject.USERFIELD15,
			ContactObject.USERFIELD16,
			ContactObject.USERFIELD17,
			ContactObject.USERFIELD18,
			ContactObject.USERFIELD19,
			ContactObject.USERFIELD20,
			ContactObject.DEFAULT_ADDRESS};

	public static ServerSession sessObj;
	public static int userId;
	public static int contextId;
	public static int folderId;
	public String DISPLAY_NAME1 = ContactTestData.DISPLAY_NAME1;
	public String NAME1 = ContactTestData.NAME1;
	public String EMAIL1 = ContactTestData.EMAIL1;
	public String DISPLAY_NAME2 = ContactTestData.DISPLAY_NAME2;
	public String NAME2 = ContactTestData.NAME2;
	public String EMAIL2 = ContactTestData.EMAIL2;
	public static Importer imp;
	public Format defaultFormat;

	public static int createTestFolder(final int type, final ServerSession sessObj,final Context ctx, final String folderTitle) throws DBPoolingException, SQLException, LdapException, OXFolderException {
		final User user = UserStorage.getInstance().getUser(sessObj.getUserId(), ctx);
		final FolderObject fo = new FolderObject();
		fo.setFolderName(folderTitle);
		fo.setParentFolderID(FolderObject.SYSTEM_PRIVATE_FOLDER_ID);
		fo.setModule(type);
		fo.setType(FolderObject.PRIVATE);
		final OCLPermission ocl = new OCLPermission();
		ocl.setEntity(user.getId());
		ocl.setAllPermission(OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION);
		ocl.setGroupPermission(false);
		ocl.setFolderAdmin(true);
		fo.setPermissionsAsArray(new OCLPermission[] { ocl });
		final OXFolderManager oxfa = new OXFolderManagerImpl(sessObj);
		int tempFolderId = -1;
		//deleting old folder if existing
		try {
			if(fo.exists(sessObj.getContext())){
				deleteTestFolder(fo.getObjectID());
			}
		} catch (final OXException e) {
			System.out.println("Could not find or delete old folder");
			e.printStackTrace();
		}
		//creating new folder
		try {
			tempFolderId = oxfa.createFolder(fo, true, System.currentTimeMillis()).getObjectID();
		} catch (final OXException e) {
			System.out.println("Could not create test folder");
			e.printStackTrace();
		}
		return tempFolderId; 
	}

	public static void deleteTestFolder(final int fuid) throws OXException {
		if(fuid < 0){
			return;
		}
		final OXFolderManager oxfa = new OXFolderManagerImpl(sessObj);
		final FolderObject fo = new FolderObject(fuid);
		if(fo.exists(sessObj.getContext())){
			oxfa.deleteFolder(fo, true, System.currentTimeMillis());
		}
	}

    @BeforeClass
	public static void initialize() throws Exception {
		Init.startServer();
		final UserStorage uStorage = UserStorage.getInstance();
        final Context ctx = ContextStorage.getInstance().getContext(ContextStorage.getInstance().getContextId("defaultcontext"));
        userId = uStorage.getUserId(AjaxInit.getAJAXProperty("login"), ctx);
	    sessObj = ServerSessionFactory.createServerSession(userId, 1, "csv-tests");
		userId = sessObj.getUserId();
		folderId = createTestFolder(FolderObject.CONTACT, sessObj, ctx, "csvContactTestFolder");
	}
	
	@AfterClass
	public static void debrief() throws AbstractOXException {
		deleteTestFolder(folderId);
        Init.stopServer();
    }

	public AbstractContactTest() {
		super();
	}

	protected List<ImportResult> importStuff(final String csv) throws ImportExportException, UnsupportedEncodingException{
		final InputStream is = new ByteArrayInputStream( csv.getBytes("UTF-8") );
		return imp.importData(sessObj, defaultFormat, is, _folders(), null);
	}
	
	protected boolean existsEntry(final int entryNumber) throws ContextException {
		final ContactSQLInterface contactSql = new RdbContactSQLInterface(sessObj);
		try {
			final ContactObject co = contactSql.getObjectById(entryNumber, folderId);
			return co != null;
		} catch (final OXException e) {
			return false;
		}
	}
	
	protected ContactObject getEntry(final int entryNumber) throws OXException, ContextException {
		final ContactSQLInterface contactSql = new RdbContactSQLInterface(sessObj);
		return contactSql.getObjectById(entryNumber, folderId);
	}
	
	protected List<String> _folders(){
		return Arrays.asList( Integer.toString(folderId) );
	}
	
	/**
	 * This method perform the import of a file with one one result.
	 * Kept for backward compatibility, calls <code>performMultipleEntryImport</code>
	 * 
	 * @param file Content of file as string
	 * @param format Format of the file
	 * @param folderObjectType Type of this folder, usually taken from FolderObject.
	 * @param foldername Name of the folder to be used for this tests
	 * @param errorExpected Is an error expected?
	 * @return
	 * @throws DBPoolingException
	 * @throws SQLException
	 * @throws ImportExportException
	 * @throws UnsupportedEncodingException
	 */
	protected ImportResult performOneEntryCheck(final String file, final Format format, final int folderObjectType, final String foldername,final Context ctx, final boolean errorExpected) throws DBPoolingException, SQLException, ImportExportException, UnsupportedEncodingException, LdapException, OXFolderException {
		return performMultipleEntryImport(file, format, folderObjectType, foldername, ctx, errorExpected).get(0);
	}
	
	/**
	 * This method performs an import of several entries
	 * 
	 * @param file The content of a file as string 
	 * @param format Format of the file given
	 * @param folderObjectType Type of this folder, usually taken from FolderObject.
	 * @param foldername Name of the folder to be used for this tests
	 * @param expectedErrors Are errors expected? Example: If you expect two ImportResults, which both report failure, write <code>true, true</code>.
	 * @return
	 * @throws DBPoolingException
	 * @throws SQLException
	 * @throws ImportExportException
	 * @throws UnsupportedEncodingException
	 */
	protected List<ImportResult> performMultipleEntryImport(final String file, final Format format, final int folderObjectType, final String foldername, final Context ctx, final Boolean... expectedErrors) throws DBPoolingException, SQLException, ImportExportException, UnsupportedEncodingException, LdapException, OXFolderException {
		folderId = createTestFolder(folderObjectType, sessObj,ctx, foldername);

		assertTrue("Can import?" ,  imp.canImport(sessObj, format, _folders(), null));

		final List<ImportResult> results = imp.importData(sessObj, format, new ByteArrayInputStream(file.getBytes("UTF-8")), _folders(), null);
		assertEquals("Correct number of results?", expectedErrors.length, results.size());
		
		for(int i = 0; i < expectedErrors.length; i++){
			assertEquals("Entry " +i+ " is as expected?" , expectedErrors[i], results.get(i).hasError());
		}
		return results;
	}
	
	/**
	 * Loads a user that is different from the usual user for testing
	 * 
	 * @return the user information of user_participant1 as defined in ajax.properties 
	 * @throws Exception
	 */
	public static User getUserParticipant() throws Exception{
		Init.startServer();
		final UserStorage uStorage = UserStorage.getInstance();
		final Context ctx = new ContextImpl(1);
		final int uid = uStorage.getUserId(AjaxInit.getAJAXProperty("user_participant1"), ctx);
		return uStorage.getUser(uid, ctx);
	}
}
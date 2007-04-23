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
package com.openexchange.admin.rmi;

import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.dataobjects.UserModuleAccess;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchUserException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This class defines the Open-Xchange API Version 2 for creating and
 * manipulating OX Users within an OX context.
 * 
 * @author cutmasta
 * 
 */
public interface OXUserInterface extends Remote {

    /**
     * RMI name to be used in RMI URL
     */
    public static final String RMI_NAME = "OXUser_V2";

    /**
     * Creates a new user within the given context.
     * 
     * @param context
     *            Context in which the new user will exist.
     * @param usrdata
     *            User containing user data.
     * @param auth
     *            Credentials for authenticating against server.
     * @param access
     *            UserModuleAccess containing module access for the user.
     * @return int containing the id of the new user.
     * 
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws DatabaseUpdateException 
     */
    public int create(Context ctx, User usrdata, UserModuleAccess access, Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException;

    /**
     * Manipulate user data within the given context.
     * 
     * @param context
     *            Context in which the new user will be modified.
     * @param usrdata
     *            User containing user data.
     * @param auth
     *            Credentials for authenticating against server.
     *            
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws DatabaseUpdateException 
     */
    public void change(Context ctx, User usrdata, Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException;

    /**
     * Delete user from given context.
     * 
     * @param context
     *            Context in which the new user will be deleted.
     * @param users
     *            user array containing user object.
     * @param auth
     *            Credentials for authenticating against server.
     *            
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws DatabaseUpdateException 
     */
    public void delete(final Context ctx, final User[] users, final Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException;

    /**
     * Delete user from given context.
     * 
     * @param context
     *            Context in which the new user will be deleted.
     * @param user
     *            user object.
     * @param auth
     *            Credentials for authenticating against server.
     *            
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws DatabaseUpdateException 
     */
    public void delete(final Context ctx, final User user, final Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException;

    /**
     * Retrieve the ModuleAccess for an user.
     * 
     * @param context
     *            Context
     * @param user_id
     *            int containing the user id.
     * @param auth
     *            Credentials for authenticating against server.
     * @return UserModuleAccess containing the module access rights.
     * 
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws DatabaseUpdateException 
     */
    public UserModuleAccess getModuleAccess(Context ctx, int user_id, Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException;

    /**
     * Manipulate user module access within the given context.
     * 
     * @param ctx
     *            Context object.
     * @param user_id
     *            int containing the user id.
     * @param moduleAccess
     *            UserModuleAccess containing module access.
     * @param auth
     *            Credentials for authenticating against server.
     *            
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws DatabaseUpdateException 
     */
    public void changeModuleAccess(Context ctx, int user_id, UserModuleAccess moduleAccess, Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException;

    /**
     * Retrieve all user ids for a given context.
     * 
     * @param ctx
     *            numerical context identifier
     * @param auth
     *            Credentials for authenticating against server.
     * @return int[] containing user ids.
     * 
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws DatabaseUpdateException 
     */
    public int[] getAll(Context ctx, Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException;

    /**
     * Retrieve user objects for a range of users by id
     * 
     * @deprecated  Use {@link #getData(Context,User[],Credential)} instead
     * @param ctx
     *            numerical context identifier
     * @param user_id
     *            int[] array containing user id(s)
     * @param auth
     *            Credentials for authenticating against server.
     * @return User[] containing result objects.
     * 
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws NoSuchUserException
     * @throws DatabaseUpdateException 
     */
    @Deprecated
    public User[] getData(Context ctx, int[] user_ids, Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, NoSuchUserException, DatabaseUpdateException;

    /**
     * Retrieve user objects for a range of users by id
     * 
     * @deprecated  Use {@link #getData(Context,User,Credential)} instead
     * @param ctx
     *            numerical context identifier
     * @param user_id
     *            int of the user id
     * @param auth
     *            Credentials for authenticating against server.
     * @return User containing result object.
     * 
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws NoSuchUserException 
     * @throws DatabaseUpdateException 
     */
    @Deprecated
    public User getData(Context ctx, int user_id, Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, NoSuchUserException, DatabaseUpdateException;

    /**
     * Retrieve user objects for a range of users by name
     * @see User.getUsername().
     * 
     * @param context
     *            Context object.
     * @param users
     *            User[] with users to get data for.
     * @param auth
     *            Credentials for authenticating against server.
     * @return User[] containing result objects.
     *
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws NoSuchUserException 
     * @throws DatabaseUpdateException 
     */
    public User[] getData(Context ctx, User[] users, Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, NoSuchUserException, DatabaseUpdateException;

    /**
     * Retrieve user objects for a range of users by name
     * @see User.getUsername().
     * 
     * @param context
     *            Context object.
     * @param user
     *            user object with user to get data for.
     * @param auth
     *            Credentials for authenticating against server.
     * @return User containing result object.
     *
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws NoSuchContextException If the context does not exist in the system.
     * @throws InvalidDataException If the data sent within the method contained invalid data.
     * @throws NoSuchUserException 
     * @throws DatabaseUpdateException 
     */
    public User getData(Context ctx, User user, Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, NoSuchUserException, DatabaseUpdateException;

}

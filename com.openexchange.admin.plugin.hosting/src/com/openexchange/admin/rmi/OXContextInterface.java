
package com.openexchange.admin.rmi;

import com.openexchange.admin.exceptions.OXContextException;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Database;
import com.openexchange.admin.rmi.dataobjects.Filestore;
import com.openexchange.admin.rmi.dataobjects.MaintenanceReason;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.ContextExistsException;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.NoSuchFilestoreException;
import com.openexchange.admin.rmi.exceptions.NoSuchReasonException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;


/**
 * This interface defines the Open-Xchange API Version 2 for creating and manipulating OX Contexts.
 *
 * @author cutmasta
 *
 */
public interface OXContextInterface extends Remote {

    /**
     * RMI name to be used in RMI URL
     */
    public static final String RMI_NAME = "OXContext_V2";

    /**
     * Create a new context.
     * @param auth Credentials for authenticating against server.
     * @param ctx Context object
     * @param admin_user User data of administrative user account for this context
     * @param quota_max maximum quota value, this context can use in the filestore (in MB)
     * @return Context object.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws ContextExistsException 
     */
    public Context create(Context ctx,User admin_user,long quota_max,Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException,InvalidDataException, ContextExistsException;

    /**
     * Delete a context.<br>
     * Note: Deleting a context will delete all data whitch the context include (all users, groups, appointments, ... )
     * @param auth Credentials for authenticating against server.
     * @param ctx Context object
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     * 
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws DatabaseUpdateException 
     * @throws InvalidDataException 
     */
    public void delete(Context ctx,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException, DatabaseUpdateException, InvalidDataException;

    /**
     * Change storage data of a context.
     * @param ctx Context object
     * @param filestore Filestore 
     * @param auth Credentials for authenticating against server.
     * @throws java.rmi.RemoteException General RMI Exception
     * 
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     * 
     * @throws com.openexchange.admin.rmi.exceptions.StorageException When an error in the subsystems occured.
     * 
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     */
    public void changeStorageData(Context ctx,Filestore filestore,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException,InvalidDataException;

    /**
     * Move all data of a context contained on the filestore to another filestore using
     * specified reason to disable context.
     *         <p>
     *         This method returns immediately and the data is going to be copied
     *         in the background. To query the progress and the result of the actual
     *         task, the AdminJobExecutor interface must be used.
     * @param ctx Context object
     * @param dst_filestore_id Id of the Filestore to move the context in.
     * @param reason ID of the maintenance reason for disabling the context while the move is in progress.
     * @param auth Credentials for authenticating against server.
     * @return Job id which can be used for retrieving progress information.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException Credentials for authenticating against server.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws NoSuchFilestoreException 
     * @throws NoSuchReasonException 
     * @throws OXContextException 
     */
    public String moveContextFilestore(Context ctx,Filestore dst_filestore_id, MaintenanceReason reason,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException,InvalidDataException, NoSuchFilestoreException, NoSuchReasonException, OXContextException;

    /**
     * Move all data of a context contained in a database to another database using
     * specified reason to disable context.
     * @param ctx Context object
     * @param dst_database_id ID of a registered Database to move all data of this context in.
     * @param reason ID of the maintenance reason for disabling the context while the move is in progress.
     * @param auth Credentials for authenticating against server.
     * @return String containing return queue id to query status of job.
     *         <p>
     *         This method returns immediately and the data is going to be copied
     *         in the background. To query the progress and the result of the actual
     *         task, the AdminJobExecutor interface must be used.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     * 
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * 
     * @throws StorageException When an error in the subsystems occured.
     * @throws DatabaseUpdateException 
     * @throws OXContextException 
     */
    public int moveContextDatabase(Context ctx,Database dst_database_id,MaintenanceReason reason,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException,InvalidDataException, DatabaseUpdateException, OXContextException;

    /**
     * Disable given context.<br>
     * Note: To disable a context you need a reason
     * @param ctx Context object.
     * @param reason MaintenanceReason
     * @param auth Credentials for authenticating against server.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException Credentials for authenticating against server.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws NoSuchReasonException 
     * @throws OXContextException 
     */
    public void disable(Context ctx, MaintenanceReason reason,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException,InvalidDataException, NoSuchReasonException, OXContextException;

    /**
     * Enable given context.
     * @param auth Credentials for authenticating against server.
     * @param ctx Context object.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidDataException 
     */
    public void enable(Context ctx,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException, InvalidDataException;

    /**
     * Search for contexts<br>
     * Use this for search a context or list all contexts.
     * @param auth Credentials for authenticating against server.
     * @param search_pattern Search pattern e.g "*mycontext*".
     * @return Contexts.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     */
    public Context[] search( String search_pattern,Credentials auth ) 
    throws RemoteException, StorageException, InvalidCredentialsException,InvalidDataException;

    /**
     * Disable all contexts.<br>
     * Note: To disable all contexts, you need a reason.
     * @param reason MaintenanceReason
     * @param auth Credentials for authenticating against server.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * 
     * @throws StorageException When an error in the subsystems occured.
     * @throws NoSuchReasonException 
     */
    public void disableAll(MaintenanceReason reason,Credentials auth ) 
    throws RemoteException, StorageException, InvalidCredentialsException,InvalidDataException, NoSuchReasonException;

    /**
     * Enable all contexts.
     * @param auth Credentials for authenticating against server.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws RemoteException General RMI Exception
     * 
     * @throws StorageException When an error in the subsystems occured.
     */
    public void enableAll(Credentials auth) 
    throws RemoteException, StorageException, InvalidCredentialsException;

    /**
     * Retrieves the context setup of given context.
     * @param auth Credentials for authenticating against server.
     * @param ctx Context object.
     * @return Context containing result object.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     * @throws RemoteException General RMI Exception
     * 
     * @throws StorageException When an error in the subsystems occured.
     * @throws InvalidDataException 
     */
    public Context getSetup(Context ctx,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException, InvalidDataException;

    
    /**
     * Get specified context details
     * @param ctx With context ID set.
     * @param auth Credentials for authenticating against server.
     * @return Data for the requested context.
     * @throws RemoteException
     * @throws InvalidCredentialsException
     * @throws NoSuchContextException
     * @throws StorageException
     * @throws InvalidDataException
     */
    public Context getContextSetup(Context ctx,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException, InvalidDataException;
    
    /**
     * Change specified context details
     * @param ctx Change context data.
     * @param auth Credentials for authenticating against server.
     * @throws RemoteException
     * @throws InvalidCredentialsException
     * @throws NoSuchContextException
     * @throws StorageException
     * @throws InvalidDataException
     */
    public void changeContextSetup(Context ctx,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException, InvalidDataException;
    
    
    /**
     * Change the database handle of the given context.
     * @param ctx Context object
     * @param db_handle Datbase informations for the change.
     * @param auth Credentials for authenticating against server.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     * 
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * 
     * @throws StorageException When an error in the subsystems occured.
     */
    public void changeDatabase(Context ctx,Database db_handle,Credentials auth) throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException,InvalidDataException;

    /**
     * Change the quota size of the given context.
     * @param ctx Context object.
     * @param quota_max Maximum quota for the context.
     * @param auth Credentials for authenticating against server.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.NoSuchContextException If the context does not exist in the system.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * @throws StorageException When an error in the subsystems occured.
     */
    public void changeQuota(Context ctx, long quota_max,Credentials auth) 
    throws RemoteException,InvalidCredentialsException,NoSuchContextException,StorageException,InvalidDataException;

    /**
     * Search for context on specified db.
     * @param db_host_url Database on which to search for contexts.
     * @param auth Credentials for authenticating against server.
     * @return Found contexts on the specified database.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * 
     * @throws StorageException When an error in the subsystems occured.
     */
    public Context[] searchByDatabase(Database db_host_url,Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException,InvalidDataException;

    /**
     * Search for context which store data on specified filestore
     * @param filestore_url Filestore
     * @param auth Credentials for authenticating against server.
     * @return Contexts found on this filestore.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidCredentialsException When the supplied credentials were not correct or invalid.
     * @throws com.openexchange.admin.rmi.exceptions.InvalidDataException If the data sent within the method contained invalid data.
     * @throws RemoteException General RMI Exception
     * 
     * @throws StorageException When an error in the subsystems occured.
     */
    public Context[] searchByFilestore(Filestore filestore_url,Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException,InvalidDataException;
}

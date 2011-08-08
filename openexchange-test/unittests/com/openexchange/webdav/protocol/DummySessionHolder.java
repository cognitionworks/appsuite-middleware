/**
 * 
 */
package com.openexchange.webdav.protocol;

import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.sessiond.impl.SessionHolder;
import com.openexchange.sessiond.impl.SessionObject;
import com.openexchange.sessiond.impl.SessionObjectWrapper;

public class DummySessionHolder implements SessionHolder{

	private SessionObject session = null;

	private final Context ctx;
	
	public DummySessionHolder(final String username, final Context ctx) throws OXException {
		session =  SessionObjectWrapper.createSessionObject(UserStorage.getInstance().getUserId(username, ctx)  , ctx,"12345");
		this.ctx = ctx;
	}
	
	public SessionObject getSessionObject() {
		return session;
	}

	public Context getContext() {
		return ctx;
	}

    /* (non-Javadoc)
     * @see com.openexchange.sessiond.impl.SessionHolder#getUser()
     */
    public User getUser() {
        // TODO Auto-generated method stub
        return null;
    }
	
}

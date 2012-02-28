/**
 *
 */
package com.openexchange.contact.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import junit.framework.TestCase;
import com.openexchange.contact.storage.registry.ContactStorageRegistry;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Init;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextImpl;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.sessiond.impl.SessionObject;
import com.openexchange.sessiond.impl.SessionObjectWrapper;
import com.openexchange.test.AjaxInit;

/**
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ContactStorageTest extends TestCase {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
            .getLog(ContactStorageTest.class);

    public final static int CONTEXT_ID = 424242669;

    private static boolean initialized = false;

    private static int resolveUser(String user, final Context ctx) throws Exception {
        try {
            int pos = -1;
            user = (pos = user.indexOf('@')) > -1 ? user.substring(0, pos) : user;
            final UserStorage uStorage = UserStorage.getInstance();
            return uStorage.getUserId(user, ctx);
        } catch (final Throwable t) {
            t.printStackTrace();
            System.exit(1);
            return -1;
        }
    }

    private SessionObject session;
    private Context ctx;
    private int userId;
    private final Collection<Contact> rememberedContacts = new ArrayList<Contact>();
    
    protected void rememberForCleanUp(final Contact contact) {
        final Contact rememberedContact = new Contact();
        rememberedContact.setObjectID(contact.getObjectID());
        rememberedContact.setParentFolderID(contact.getParentFolderID());
        this.rememberedContacts.add(rememberedContact);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (false == initialized) {
            Init.startServer();
        }
        ctx = new ContextImpl(CONTEXT_ID);
        userId = resolveUser(AjaxInit.getAJAXProperty("login"), ctx);
        session = SessionObjectWrapper.createSessionObject(userId, CONTEXT_ID, "thorben_session_id");
    }

    @Override
    protected void tearDown() throws Exception {
        if (null != this.rememberedContacts && 0 < rememberedContacts.size()) {
            for (Contact contact : rememberedContacts) {
                try {
                    this.getStorage().delete(getSession(), Integer.toString(contact.getParentFolderID()), 
                        Integer.toString(contact.getObjectID()), new Date(0));
                } catch (final Exception e) {
                    LOG.error("error cleaning up contact", e);
                }
            }            
        }        
        if (initialized) {
            initialized = false;
            Init.stopServer();
        }
        super.tearDown();
    }

    protected ContactStorage getStorage() throws OXException {
        final ContactStorageRegistry registry = ServerServiceRegistry.getInstance().getService(ContactStorageRegistry.class);
        return registry.getStorage(this.getSession(), null);
    }

    protected Session getSession() {
        return this.session;
    }


}

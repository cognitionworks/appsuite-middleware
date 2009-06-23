package com.openexchange.ajax.mail;

import java.io.IOException;

import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.xml.sax.SAXException;

import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.framework.Executor;
import com.openexchange.ajax.framework.AJAXClient.User;
import com.openexchange.ajax.mail.actions.AllRequest;
import com.openexchange.ajax.mail.actions.AllResponse;
import com.openexchange.ajax.mail.actions.SendRequest;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.tools.servlet.AjaxException;

/**
 * 
 * {@link AllRequestAndResponseTest} - tests the AllRequest and -Response 
 *
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 *
 */
public class AllRequestAndResponseTest extends AbstractMailTest {
	
	private static final Log LOG = LogFactory.getLog(AllTest.class);
	protected String folder;
	String mailObject_25kb;

    public AllRequestAndResponseTest(String name) throws ConfigurationException, AjaxException, IOException, SAXException, JSONException {
        super(name);
        this.client = new AJAXClient(User.User1);
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        folder = getSentFolder();
        /*
		 * Create JSON mail object
		 */
		mailObject_25kb = createSelfAddressed25KBMailObject().toString();
        clearFolder(folder);
    }

    @Override
    public void tearDown() throws Exception {
        clearFolder(folder);
        super.tearDown();
    }
    
    public void testAllResponseGetMailObjects() throws Exception {
    	
    	/*
		 * Insert <numOfMails> mails through a send request
		 */
		final int numOfMails = 1;
		LOG.info("Sending " + numOfMails + " mails to fill emptied INBOX");
		for (int i = 0; i < numOfMails; i++) {
		    getClient().execute(new SendRequest(mailObject_25kb));
			LOG.info("Sent " + (i + 1) + ". mail of " + numOfMails);
		}
    	
    	AllResponse allR = Executor.execute(getSession(), new AllRequest(
				getInboxFolder(), COLUMNS_DEFAULT_LIST, 0, null, true));
        if (allR.hasError()) {
            fail(allR.getException().toString());
        }
        MailMessage[] mailMessages = allR.getMailMessages(COLUMNS_DEFAULT_LIST);
        for (MailMessage mailMessage : mailMessages){
        	assertEquals("From is not equal", new InternetAddress(getSendAddress()) ,mailMessage.getFrom()[0]);
        	assertEquals("Subject is not equal", MAIL_SUBJECT ,mailMessage.getSubject());
        }
    }

}

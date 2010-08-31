package com.openexchange.mail.filter;

import junit.framework.Assert;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import com.openexchange.config.ConfigurationService;
import com.openexchange.mailfilter.ajax.SessionWrapper.Credentials;
import com.openexchange.mailfilter.ajax.actions.MailfilterAction;
import com.openexchange.mailfilter.ajax.exceptions.OXMailfilterException;
import com.openexchange.mailfilter.internal.MailFilterProperties;
import com.openexchange.mailfilter.services.MailFilterServletServiceRegistry;


public class MailfilterActionTest extends MailfilterAction {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Common.prepare(null, null);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetRightPasswordNothing() throws OXMailfilterException {
        final ConfigurationService config = MailFilterServletServiceRegistry.getServiceRegistry().getService(ConfigurationService.class);
        final String credsPW = "pw2";
        final Credentials creds = new Credentials("","",credsPW);
        try {
            getRightPassword(config, creds);
            Assert.fail("No exception thrown");
        } catch (final OXMailfilterException e) {
            Assert.assertEquals(OXMailfilterException.Code.NO_VALID_PASSWORDSOURCE.getMessage(), e.getOrigMessage());
        }
    }
    
    @Test
    public void testGetRightPasswordSession() throws OXMailfilterException {
        Common.simConfigurationService.stringProperties.put(MailFilterProperties.Values.SIEVE_PASSWORDSRC.property, MailFilterProperties.PasswordSource.SESSION.name);
        final ConfigurationService config = MailFilterServletServiceRegistry.getServiceRegistry().getService(ConfigurationService.class);
        final String credsPW = "pw2";
        final Credentials creds = new Credentials("","",credsPW);
        final String rightPassword = getRightPassword(config, creds);
        Assert.assertEquals("Password should be equal to \"" + credsPW + "\"", credsPW, rightPassword);
    }
    
    @Test
    public void testGetRightPasswordGlobalNoMasterPW() throws OXMailfilterException {
        Common.simConfigurationService.stringProperties.put(MailFilterProperties.Values.SIEVE_PASSWORDSRC.property, MailFilterProperties.PasswordSource.GLOBAL.name);
        final ConfigurationService config = MailFilterServletServiceRegistry.getServiceRegistry().getService(ConfigurationService.class);
        final String credsPW = "pw2";
        final Credentials creds = new Credentials("","",credsPW);
        try {
            getRightPassword(config, creds);
            Assert.fail("No exception thrown");
        } catch (final OXMailfilterException e) {
            Assert.assertEquals(OXMailfilterException.Code.NO_MASTERPASSWORD_SET.getMessage(), e.getOrigMessage());
        }
    }
    
    @Test
    public void testGetRightPasswordGlobal() throws OXMailfilterException {
        final String masterPW = "masterPW";
        Common.simConfigurationService.stringProperties.put(MailFilterProperties.Values.SIEVE_PASSWORDSRC.property, MailFilterProperties.PasswordSource.GLOBAL.name);
        Common.simConfigurationService.stringProperties.put(MailFilterProperties.Values.SIEVE_MASTERPASSWORD.property, masterPW);
        final ConfigurationService config = MailFilterServletServiceRegistry.getServiceRegistry().getService(ConfigurationService.class);
        final String credsPW = "pw2";
        final Credentials creds = new Credentials("","",credsPW);
        final String rightPassword = getRightPassword(config, creds);
        Assert.assertEquals("Password should be equal to \"" + masterPW + "\"", masterPW, rightPassword);
    }
    
}

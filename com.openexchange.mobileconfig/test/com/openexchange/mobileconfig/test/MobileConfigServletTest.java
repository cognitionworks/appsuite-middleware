package com.openexchange.mobileconfig.test;

import junit.framework.Assert;
import org.junit.Test;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.SimConfigurationService;
import com.openexchange.mobileconfig.MobileConfigServlet;
import com.openexchange.mobileconfig.configuration.ConfigurationException;
import com.openexchange.mobileconfig.configuration.Property;
import com.openexchange.mobileconfig.services.MobileConfigServiceRegistry;


@SuppressWarnings("serial")
public class MobileConfigServletTest extends MobileConfigServlet  {

    @Test
    public void testSplitUsernameAndDomain() throws ConfigurationException {
        final SimConfigurationService service = new SimConfigurationService();
        service.stringProperties.put(Property.DomainUser.getName(), "$USER@$DOMAIN");
        MobileConfigServiceRegistry.getServiceRegistry().addService(ConfigurationService.class, service);
        
        final String[] splitUsernameAndDomain = splitUsernameAndDomain("seppel@ox.de");
        Assert.assertEquals("Value at index 0 wrong", "seppel", splitUsernameAndDomain[0]);
        Assert.assertEquals("Value at index 1 wrong", "ox.de", splitUsernameAndDomain[1]);
    }

    @Test
    public void testSplitUsernameAndDomain2() throws ConfigurationException {
        final SimConfigurationService service = new SimConfigurationService();
        service.stringProperties.put(Property.DomainUser.getName(), "$DOMAIN@$USER");
        MobileConfigServiceRegistry.getServiceRegistry().addService(ConfigurationService.class, service);
        
        final String[] splitUsernameAndDomain = splitUsernameAndDomain("seppel@ox.de");
        Assert.assertEquals("Value at index 0 wrong", "ox.de", splitUsernameAndDomain[0]);
        Assert.assertEquals("Value at index 1 wrong", "seppel", splitUsernameAndDomain[1]);
    }

    @Test
    public void testSplitUsernameAndDomain3() throws ConfigurationException {
        final SimConfigurationService service = new SimConfigurationService();
        service.stringProperties.put(Property.DomainUser.getName(), "$DOMAIN|$USER");
        MobileConfigServiceRegistry.getServiceRegistry().addService(ConfigurationService.class, service);
        
        final String[] splitUsernameAndDomain = splitUsernameAndDomain("seppel@ox.de");
        Assert.assertEquals("Value at index 0 wrong", "seppel@ox.de", splitUsernameAndDomain[0]);
        Assert.assertEquals("Value at index 1 wrong", "defaultcontext", splitUsernameAndDomain[1]);
    }

    @Test
    public void testSplitUsernameAndDomain4() throws ConfigurationException {
        final SimConfigurationService service = new SimConfigurationService();
        service.stringProperties.put(Property.DomainUser.getName(), "$DOMAIN|$USER");
        MobileConfigServiceRegistry.getServiceRegistry().addService(ConfigurationService.class, service);
        
        final String[] splitUsernameAndDomain = splitUsernameAndDomain("seppel|ox.de");
        Assert.assertEquals("Value at index 0 wrong", "ox.de", splitUsernameAndDomain[0]);
        Assert.assertEquals("Value at index 1 wrong", "seppel", splitUsernameAndDomain[1]);
    }
    
    @Test
    public void testSplitUsernameAndDomain5() throws ConfigurationException {
        final SimConfigurationService service = new SimConfigurationService();
        service.stringProperties.put(Property.DomainUser.getName(), "$USER@$DOMAIN");
        MobileConfigServiceRegistry.getServiceRegistry().addService(ConfigurationService.class, service);
        
        final String[] splitUsernameAndDomain = splitUsernameAndDomain("seppel");
        Assert.assertEquals("Value at index 0 wrong", "seppel", splitUsernameAndDomain[0]);
        Assert.assertEquals("Value at index 1 wrong", "defaultcontext", splitUsernameAndDomain[1]);
    }
    
}

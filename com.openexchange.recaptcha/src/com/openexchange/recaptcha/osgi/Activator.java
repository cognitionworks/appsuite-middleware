package com.openexchange.recaptcha.osgi;

import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.recaptcha.ReCaptchaService;
import com.openexchange.recaptcha.ReCaptchaServlet;
import com.openexchange.recaptcha.impl.ReCaptchaServiceImpl;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.server.osgiservice.ServiceRegistry;

public class Activator extends DeferredActivator {
    
    private static final Log LOG = LogFactory.getLog(Activator.class);
    
    private static final String ALIAS = "/ajax/recaptcha";
    
    private ReCaptchaServlet servlet;

    private ServiceRegistration serviceRegistration;

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class, HttpService.class };
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
        registerServlet();
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
        unregisterServlet();
    }

    @Override
    protected void startBundle() throws Exception {
        final ServiceRegistry registry = ReCaptchaServiceRegistry.getInstance();
        registry.clearRegistry();
        final Class<?>[] classes = getNeededServices();
        for (int i = 0; i < classes.length; i++) {
            final Object service = getService(classes[i]);
            if (service != null) {
                registry.addService(classes[i], service);
            }
        }
        
        final ConfigurationService config = registry.getService(ConfigurationService.class);
        final Properties props = config.getFile("recaptcha.properties");
        final Properties options = config.getFile("recaptcha_options.properties");
        final ReCaptchaServiceImpl reCaptchaService = new ReCaptchaServiceImpl(props, options);
        serviceRegistration = context.registerService(ReCaptchaService.class.getName(), reCaptchaService, null);
        registry.addService(ReCaptchaService.class, reCaptchaService);
        
        registerServlet();
    }

    @Override
    protected void stopBundle() throws Exception {
        
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
        
        unregisterServlet();
        ReCaptchaServiceRegistry.getInstance().clearRegistry();
    }
    
    private void registerServlet() {
        final ServiceRegistry registry = ReCaptchaServiceRegistry.getInstance();
        final HttpService httpService = registry.getService(HttpService.class);
        if(servlet == null) {
            try {
                httpService.registerServlet(ALIAS, servlet = new ReCaptchaServlet(), null, null);
                LOG.info("reCAPTCHA Servlet registered.");
            } catch (final Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
    
    private void unregisterServlet() {
        final HttpService httpService = getService(HttpService.class);
        if(httpService != null && servlet != null) {
            httpService.unregister(ALIAS);
            servlet = null;
            LOG.info("reCAPTCHA Servlet unregistered.");
        }
    }

}

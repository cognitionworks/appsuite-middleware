
package com.openexchange.filemanagement.distributed.servlet.osgi;

import org.osgi.service.http.HttpService;
import com.openexchange.config.ConfigurationService;
import com.openexchange.filemanagement.ManagedFileManagement;
import com.openexchange.filemanagement.distributed.servlet.DistributedFileServlet;
import com.openexchange.osgi.HousekeepingActivator;

public class Activator extends HousekeepingActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { HttpService.class, ManagedFileManagement.class, ConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {

        HttpService service = getService(HttpService.class);
        service.registerServlet(DistributedFileServlet.PATH, new DistributedFileServlet(this), null, null);
    }

}

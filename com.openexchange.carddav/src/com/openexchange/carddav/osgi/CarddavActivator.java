package com.openexchange.carddav.osgi;

import org.apache.commons.logging.Log;
import org.osgi.service.http.HttpService;
import com.openexchange.carddav.servlet.CardDAV;
import com.openexchange.carddav.servlet.CarddavPerformer;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.contact.ContactService;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.user.UserService;
import com.openexchange.webdav.directory.PathRegistration;
import com.openexchange.webdav.protocol.osgi.OSGiPropertyMixin;

public class CarddavActivator extends HousekeepingActivator {

    private static final Log LOG = com.openexchange.log.Log.loggerFor(CarddavActivator.class);
    private static final Class<?>[] NEEDED = new Class[] { HttpService.class, FolderService.class, ConfigViewFactory.class, UserService.class, ContactService.class };

    private volatile OSGiPropertyMixin mixin;

    @Override
    protected Class<?>[] getNeededServices() {
        return NEEDED;
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            CardDAV.setServiceLookup(this);
            CarddavPerformer.setServices(this);

            getService(HttpService.class).registerServlet("/servlet/dav/carddav", new CardDAV(), null, null);

            CarddavPerformer performer = CarddavPerformer.getInstance();
            final OSGiPropertyMixin mixin = new OSGiPropertyMixin(context, performer);
            performer.setGlobalMixins(mixin);
            this.mixin = mixin;

            registerService(PathRegistration.class, new PathRegistration("carddav"));

            openTrackers();
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
    }

    @Override
    protected void stopBundle() throws Exception {
        final OSGiPropertyMixin mixin = this.mixin;
        if (null != mixin) {
            mixin.close();
            this.mixin = null;
        }
        super.stopBundle();
    }


}

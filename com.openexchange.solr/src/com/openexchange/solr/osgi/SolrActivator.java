
package com.openexchange.solr.osgi;

import java.rmi.Remote;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.osgi.framework.ServiceReference;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.openexchange.config.ConfigurationService;
import com.openexchange.database.CreateTableService;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.update.DefaultUpdateTaskProviderService;
import com.openexchange.groupware.update.UpdateTaskProviderService;
import com.openexchange.log.LogFactory;
import com.openexchange.login.LoginHandlerService;
import com.openexchange.management.ManagementService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.SimpleRegistryListener;
import com.openexchange.solr.SolrAccessService;
import com.openexchange.solr.SolrCoreConfigService;
import com.openexchange.solr.SolrMBean;
import com.openexchange.solr.SolrProperties;
import com.openexchange.solr.groupware.SolrCoreLoginHandler;
import com.openexchange.solr.groupware.SolrCoresCreateTableService;
import com.openexchange.solr.groupware.SolrCoresCreateTableTask;
import com.openexchange.solr.internal.DelegationSolrAccessImpl;
import com.openexchange.solr.internal.EmbeddedSolrAccessImpl;
import com.openexchange.solr.internal.RMISolrAccessImpl;
import com.openexchange.solr.internal.Services;
import com.openexchange.solr.internal.SolrCoreConfigServiceImpl;
import com.openexchange.solr.internal.SolrMBeanImpl;
import com.openexchange.solr.rmi.RMISolrAccessService;
import com.openexchange.threadpool.ThreadPoolService;

/**
 * {@link SolrActivator}
 * 
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class SolrActivator extends HousekeepingActivator {

    public static final String SOLR_NODE_MAP = "solrNodeMap";

    static Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(SolrActivator.class));

    private volatile DelegationSolrAccessImpl delegationAccess;

    private static RMISolrAccessService solrRMI;

    private SolrMBean solrMBean;

    private ObjectName solrMBeanName;

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigurationService.class, DatabaseService.class, ThreadPoolService.class, HazelcastInstance.class };
    }

    @Override
    protected void startBundle() throws OXException {
        Services.setServiceLookup(this);
        new CheckConfigDBTables(getService(DatabaseService.class)).checkTables();
        EmbeddedSolrAccessImpl embeddedAccess = new EmbeddedSolrAccessImpl();
        embeddedAccess.startUp();
        DelegationSolrAccessImpl accessService = this.delegationAccess = new DelegationSolrAccessImpl(embeddedAccess);
        registerService(SolrAccessService.class, accessService);
        addService(SolrAccessService.class, accessService);
        SolrCoreConfigServiceImpl coreService = new SolrCoreConfigServiceImpl();
        registerService(SolrCoreConfigService.class, coreService);
        addService(SolrCoreConfigService.class, coreService);
        solrRMI = new RMISolrAccessImpl(embeddedAccess);
        registerService(Remote.class, solrRMI);

        SolrCoresCreateTableService createTableService = new SolrCoresCreateTableService();
        registerService(CreateTableService.class, createTableService);
        registerService(UpdateTaskProviderService.class, new DefaultUpdateTaskProviderService(new SolrCoresCreateTableTask(
            createTableService)));
        // new SolrCoreStoresCreateTableTask()
        registerService(LoginHandlerService.class, new SolrCoreLoginHandler(embeddedAccess));
        registerMBean(coreService);
        HazelcastInstance hazelcast = getService(HazelcastInstance.class);

        ConfigurationService config = getService(ConfigurationService.class);
        boolean isSolrNode = config.getBoolProperty(SolrProperties.IS_NODE, false);
        if (isSolrNode) {
            IMap<String, Integer> solrNodes = hazelcast.getMap(SOLR_NODE_MAP);
            String memberAddress = hazelcast.getCluster().getLocalMember().getInetSocketAddress().getAddress().getHostAddress();
            solrNodes.put(memberAddress, new Integer(0));
        }
        openTrackers();
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();

        solrRMI = null;
        ManagementService managementService = Services.optService(ManagementService.class);
        if (managementService != null && solrMBeanName != null) {
            managementService.unregisterMBean(solrMBeanName);
            solrMBean = null;
        }

        DelegationSolrAccessImpl delegationAccess = this.delegationAccess;
        if (delegationAccess != null) {
            delegationAccess.shutDown();
            this.delegationAccess = null;
        }
    }

    private void registerMBean(SolrCoreConfigServiceImpl coreService) {
        try {
            solrMBeanName = new ObjectName(SolrMBean.DOMAIN, "name", "Solr Control");
            DelegationSolrAccessImpl delegationAccess = this.delegationAccess;
            solrMBean = new SolrMBeanImpl(delegationAccess, coreService);
            track(ManagementService.class, new SimpleRegistryListener<ManagementService>() {

                @Override
                public void added(ServiceReference<ManagementService> ref, ManagementService service) {
                    try {
                        service.registerMBean(solrMBeanName, solrMBean);
                    } catch (OXException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }

                @Override
                public void removed(ServiceReference<ManagementService> ref, ManagementService service) {
                    try {
                        service.unregisterMBean(solrMBeanName);
                    } catch (OXException e) {
                        LOG.warn(e.getMessage(), e);
                    }
                }
            });
        } catch (MalformedObjectNameException e) {
            LOG.error(e.getMessage(), e);
        } catch (NotCompliantMBeanException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}

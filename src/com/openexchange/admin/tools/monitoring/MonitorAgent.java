
package com.openexchange.admin.tools.monitoring;

import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.rmi.registry.LocateRegistry;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.LoggingMXBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author cutmasta
 */
public class MonitorAgent {
    
    private static Log log = LogFactory.getLog(MonitorAgent.class);
    private MBeanServer mbs = null;
    private JMXConnectorServer cs = null;
    
    
    private void openRmiRegistryPort() {
        try {
            LocateRegistry.createRegistry(getPort());
        } catch (Exception e) {
            log.error("Error creating Registry on port "+getPort());
        }
    }
    
    public MonitorAgent(int jmx_port) {
        setPort(jmx_port);
    }
    
    public void start() {
        try {
            
            // we will publish our infos via rmi
            openRmiRegistryPort();
            
            // get the platform mbean server
            initBeanServer();
            
            // OX beans
            registerOXBeans();
            
            // Java sys beans
            registerSystemBeans();
            
            // start the jmx connector server with given url
            startConnectors();
            
        } catch (MalformedObjectNameException e) {
            log.error("Error starting the JMX Server",e);
        } catch (InstanceAlreadyExistsException e) {
            log.error("Error starting the JMX Server",e);
        } catch (MBeanRegistrationException e) {
            log.error("Error starting the JMX Server",e);
        } catch (NotCompliantMBeanException e) {
            log.error("Error starting the JMX Server",e);
        } catch (MalformedURLException e) {
            log.error("Error starting the JMX Server",e);
        } catch (IOException e) {
            log.error("Error starting the JMX Server",e);
        }
    }
    
    public void stop() {
        try {
            stopConnectors();
            unregisterSystemBeans();
            unregisterOXBeans();
            releaseBeanServer();
        } catch (MalformedObjectNameException e) {
            log.error("Error stopping the JMX Server",e);
        } catch (NullPointerException e) {
            log.error("Error stopping the JMX Server",e);
        } catch (InstanceNotFoundException e) {
            log.error("Error stopping the JMX Server",e);
        } catch (MBeanRegistrationException e) {
            log.error("Error stopping the JMX Server",e);
        } catch (IOException e) {
            log.error("Error stopping the JMX Server",e);
        }

    }
    
    private void startConnectors() throws MalformedURLException, IOException{
        JMXServiceURL url = new JMXServiceURL(getServerURL());
        this.cs = JMXConnectorServerFactory.newJMXConnectorServer(url,null,mbs);
        this.cs.start();
        log.info("Admindaemon JMX server running on: "+getServerURL());
    }
    
    private void stopConnectors() throws IOException {
        this.cs.stop();
    }
    
    private void registerSystemBeans() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException{
        
        // logging bean for manipulating log options at runtime
        LoggingMXBean logi = LogManager.getLoggingMXBean();
        ObjectName logy = new ObjectName(LogManager.LOGGING_MXBEAN_NAME);
        mbs.registerMBean(logi,logy);
        
        // os bean
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        ObjectName ox = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        mbs.registerMBean(os,ox);
        
        // runtime bean
        RuntimeMXBean rx = ManagementFactory.getRuntimeMXBean();
        ox = new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
        mbs.registerMBean(rx,ox);
        
        // tret bean
        ThreadMXBean mb = ManagementFactory.getThreadMXBean();
        ox = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
        mbs.registerMBean(mb,ox);
        
        // memory beans
        MemoryMXBean mm = ManagementFactory.getMemoryMXBean();
        ox = new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);
        mbs.registerMBean(mm,ox);
        
        List mp = ManagementFactory.getMemoryPoolMXBeans();
        for(int a = 0;a<mp.size();a++){
            MemoryPoolMXBean mbbm = (MemoryPoolMXBean)mp.get(a);
            ox = new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE+",name="+mbbm.getName());
            mbs.registerMBean(mbbm,ox);
        }
        
        mp = ManagementFactory.getMemoryManagerMXBeans();
        for(int a = 0;a<mp.size();a++){
            MemoryManagerMXBean mbbm = (MemoryManagerMXBean)mp.get(a);
            ox = new ObjectName(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE+",name="+mbbm.getName());
            mbs.registerMBean(mbbm,ox);
        }
        
        //gc bean
        mp = ManagementFactory.getGarbageCollectorMXBeans();
        for(int a = 0;a<mp.size();a++){
            GarbageCollectorMXBean mbbm = (GarbageCollectorMXBean)mp.get(a);
            ox = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE+",name="+mbbm.getName());
            mbs.registerMBean(mbbm,ox);
        }        
        
        // compile bean
        CompilationMXBean cmp = ManagementFactory.getCompilationMXBean();
        ox = new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME);
        mbs.registerMBean(cmp,ox);
        
        // class load bean
        ClassLoadingMXBean cmx = ManagementFactory.getClassLoadingMXBean();
        ox = new ObjectName(ManagementFactory.CLASS_LOADING_MXBEAN_NAME);
        mbs.registerMBean(cmx,ox);
        
    }
    
    private void unregisterSystemBeans() throws MalformedObjectNameException, NullPointerException, InstanceNotFoundException, MBeanRegistrationException {
        ObjectName logy = new ObjectName(LogManager.LOGGING_MXBEAN_NAME);
        ObjectName ox = new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        ObjectName runtime = new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
        ObjectName thread = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
        ObjectName memory = new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);
        
        mbs.unregisterMBean(logy);
        mbs.unregisterMBean(ox);
        mbs.unregisterMBean(runtime);
        mbs.unregisterMBean(thread);
        mbs.unregisterMBean(memory);

        List mp = ManagementFactory.getMemoryPoolMXBeans();
        for(int a = 0;a<mp.size();a++){
            MemoryPoolMXBean mbbm = (MemoryPoolMXBean)mp.get(a);
            ox = new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE+",name="+mbbm.getName());
            mbs.unregisterMBean(ox);
        }
        
        mp = ManagementFactory.getMemoryManagerMXBeans();
        for(int a = 0;a<mp.size();a++){
            MemoryManagerMXBean mbbm = (MemoryManagerMXBean)mp.get(a);
            ox = new ObjectName(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE+",name="+mbbm.getName());
            mbs.unregisterMBean(ox);
        }
        
        //gc bean
        mp = ManagementFactory.getGarbageCollectorMXBeans();
        for(int a = 0;a<mp.size();a++){
            GarbageCollectorMXBean mbbm = (GarbageCollectorMXBean)mp.get(a);
            ox = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE+",name="+mbbm.getName());
            mbs.unregisterMBean(ox);
        }        
        
        // compile bean
        ox = new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME);
        mbs.unregisterMBean(ox);
        
        // class load bean
        ox = new ObjectName(ManagementFactory.CLASS_LOADING_MXBEAN_NAME);
        mbs.unregisterMBean(ox);
    }
    
    private void initBeanServer(){
        mbs = MBeanServerFactory.createMBeanServer();
    }
    
    private void releaseBeanServer() {
        MBeanServerFactory.releaseMBeanServer(mbs);
    }
    
    private void registerOXBeans() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException{
        ObjectName onj = new ObjectName("com.openexchange.admin.monitor:name=CallMonitor");
        Monitor mn = new Monitor();
        mbs.registerMBean(mn,onj);
    }    
    
    private void unregisterOXBeans() throws MalformedObjectNameException, NullPointerException, InstanceNotFoundException, MBeanRegistrationException {
        ObjectName onj = new ObjectName("com.openexchange.admin.monitor:name=CallMonitor");
        mbs.unregisterMBean(onj);
    }
    
    private int getPort(){
        return this.port;
    }
    private void setPort(int jmx_port){
        this.port = jmx_port;
    }
    private String getServerURL(){
        return "service:jmx:rmi:///jndi/rmi://localhost:"+getPort()+"/server";
    }
    private int port = 9998;
    
    
}

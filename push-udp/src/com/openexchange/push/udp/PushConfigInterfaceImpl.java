/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the Open-Xchange, Inc. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.push.udp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.config.ConfigurationService;

/**
 * PushConfigInterface
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.org">Sebastian Kauss</a>
 */

public class PushConfigInterfaceImpl extends AbstractConfigWrapper implements PushConfig {

    private boolean isPushEnabled = false;

    private Set<RemoteHostObject> remoteHost = new HashSet<RemoteHostObject>();

    private int registerTimeout = 3600000;

    private int outputQueueDelay = 120000;

    private int registerPort = 44335;

    private boolean isRegisterDistributionEnabled = false;

    private boolean isEventDistributionEnabled = false;

    private InetAddress senderAddress;

    private boolean multicastEnabled = false;

    private InetAddress multicastAddress;

    private InetAddress hostname;

    private int multicastPort = 0;

    private int remoteHostTimeOut = 3600000;

    private int remoteHostRefresh = 30000;

    private boolean isInit = false;

    private static final Log LOG = LogFactory.getLog(PushConfigInterfaceImpl.class);

    private static final boolean DEBUG = LOG.isDebugEnabled();

    public PushConfigInterfaceImpl(final ConfigurationService conf) {
        this(conf, false);
    }

    public PushConfigInterfaceImpl(final ConfigurationService conf, final boolean ignoreIsInit) {
        if (!ignoreIsInit && isInit) {
            return;
        }

        isPushEnabled = parseProperty(conf, "com.openexchange.push.udp.pushEnabled", isPushEnabled);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.pushEnabled=" + isPushEnabled);
        }

        String[] remoteAddressAndPort = null;
        remoteAddressAndPort = parseProperty(conf, "com.openexchange.push.udp.remoteHost", remoteAddressAndPort);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.remoteHost=" + Arrays.toString(remoteAddressAndPort));
        }

        if (remoteAddressAndPort != null) {
            for (final String element : remoteAddressAndPort) {
                final RemoteHostObject remoteHostObject = new RemoteHostObject();
                final String[] addressAndPort = element.split(":");
                try {
                    if (addressAndPort.length == 1) {
                        remoteHostObject.setHost(InetAddress.getByName(addressAndPort[0]));
                    } else if (addressAndPort.length >= 2) {
                        remoteHostObject.setHost(InetAddress.getByName(addressAndPort[0]));
                        remoteHostObject.setPort(Integer.parseInt(addressAndPort[1]));
                    }
                } catch (final UnknownHostException exc) {
                    LOG.error("problem with parsing remote host attribute: " + addressAndPort[0], exc);
                }

                remoteHost.add(remoteHostObject);
            }
        }

        registerTimeout = parseProperty(conf, "com.openexchange.push.udp.registerTimeout", registerTimeout);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.registerTimeout=" + registerTimeout);
        }

        outputQueueDelay = parseProperty(conf, "com.openexchange.push.udp.outputQueueDelay", outputQueueDelay);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.outputQueueDelay=" + outputQueueDelay);
        }

        registerPort = parseProperty(conf, "com.openexchange.push.udp.registerPort", registerPort);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.registerPort=" + registerPort);
        }

        isRegisterDistributionEnabled = parseProperty(
            conf,
            "com.openexchange.push.udp.registerDistributionEnabled",
            isRegisterDistributionEnabled);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.registerDistributionEnabled=" + isRegisterDistributionEnabled);
        }

        isEventDistributionEnabled = parseProperty(conf, "com.openexchange.push.udp.eventDistributionEnabled", isEventDistributionEnabled);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.eventDistributionEnabled=" + isEventDistributionEnabled);
        }

        String senderAddressString = null;
        senderAddressString = parseProperty(conf, "com.openexchange.push.udp.senderAddress", senderAddressString);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.senderAddress=" + senderAddressString);
        }

        try {
            if (senderAddressString != null) {
                senderAddress = InetAddress.getByName(senderAddressString);
            }
        } catch (final UnknownHostException exc) {
            LOG.error("problem with parsing sender address: " + senderAddressString, exc);
        }

        remoteHostTimeOut = parseProperty(conf, "com.openexchange.push.udp.remoteHostTimeOut", remoteHostTimeOut);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.remoteHostTimeOut=" + remoteHostTimeOut);
        }

        remoteHostRefresh = parseProperty(conf, "com.openexchange.push.udp.remoteHostRefresh", remoteHostRefresh);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.remoteHostRefresh=" + remoteHostRefresh);
        }

        multicastEnabled = parseProperty(conf, "com.openexchange.push.udp.multicastEnabled", multicastEnabled);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.multicastEnabled=" + multicastEnabled);
        }

        String multicastAddressString = null;
        multicastAddressString = parseProperty(conf, "com.openexchange.push.udp.multicastAddress", multicastAddressString);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.multicastAddress=" + multicastAddressString);
        }

        try {
            multicastAddress = InetAddress.getByName(multicastAddressString);
        } catch (final UnknownHostException exc) {
            LOG.error("problem with parsing multicast address: " + multicastAddressString, exc);
        }

        multicastPort = parseProperty(conf, "com.openexchange.push.udp.multicastPort", multicastPort);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.multicastPort=" + multicastPort);
        }

        String hostnameString = null;
        hostnameString = parseProperty(conf, "com.openexchange.push.udp.hostname", hostnameString);
        if (DEBUG) {
            LOG.debug("PushHandler property: com.openexchange.push.udp.hostname=" + hostnameString);
        }

        try {
            hostname = InetAddress.getByName(hostnameString);
        } catch (final UnknownHostException exc) {
            LOG.error("problem with parsing hostname: " + hostnameString, exc);
        }

        isInit = true;
    }

    public boolean isPushEnabled() {
        return isPushEnabled;
    }

    public void setPushEnabled(final boolean isPushEnabled) {
        this.isPushEnabled = isPushEnabled;
    }

    public Set<RemoteHostObject> getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(final Set<RemoteHostObject> remoteHost) {
        this.remoteHost = remoteHost;
    }

    public int getRegisterTimeout() {
        return registerTimeout;
    }

    public void setRegisterTimeout(final int registerTimeout) {
        this.registerTimeout = registerTimeout;
    }

    public int getRegisterPort() {
        return registerPort;
    }

    public void setRegisterPort(final int registerPort) {
        this.registerPort = registerPort;
    }

    public boolean isRegisterDistributionEnabled() {
        return isRegisterDistributionEnabled;
    }

    public void setRegisterDistributionEnabled(final boolean isRegisterDistributionEnabled) {
        this.isRegisterDistributionEnabled = isRegisterDistributionEnabled;
    }

    public boolean isEventDistributionEnabled() {
        return isEventDistributionEnabled;
    }

    public void setEventDistributionEnabled(final boolean isEventDistributionEnabled) {
        this.isEventDistributionEnabled = isEventDistributionEnabled;
    }

    public int getOutputQueueDelay() {
        return outputQueueDelay;
    }

    public void setOutputQueueDelay(final int outputQueueDelay) {
        this.outputQueueDelay = outputQueueDelay;
    }

    public InetAddress getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(final InetAddress senderAddress) {
        this.senderAddress = senderAddress;
    }

    public boolean isMultiCastEnabled() {
        return multicastEnabled;
    }

    public void setMultiCastEnabled(final boolean multicastEnabled) {
        this.multicastEnabled = multicastEnabled;
    }

    public int getMultiCastPort() {
        return multicastPort;
    }

    public InetAddress getMultiCastAddress() {
        return multicastAddress;
    }

    public void setMultiCastAddress(final InetAddress multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    public int getRemoteHostTimeOut() {
        return remoteHostTimeOut;
    }

    public void setRemoteHostTimeOut(final int remoteHostTimeOut) {
        this.remoteHostTimeOut = remoteHostTimeOut;
    }

    public int getRemoteHostRefresh() {
        return remoteHostRefresh;
    }

    public void setRemoteHostRefresh(final int remoteHostRefresh) {
        this.remoteHostRefresh = remoteHostRefresh;
    }

    public void setHostName(final InetAddress hostname) {
        this.hostname = hostname;
    }

    public InetAddress getHostName() {
        return hostname;
    }
}

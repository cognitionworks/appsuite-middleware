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

package com.openexchange.mdns.internal;

import static com.openexchange.java.util.UUIDs.getUnformattedString;
import java.net.InetAddress;
import java.util.UUID;
import com.openexchange.mdns.MDNSServiceEntry;

/**
 * {@link MDNSServiceEntryImpl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MDNSServiceEntryImpl implements MDNSServiceEntry {

    private final InetAddress address;

    private final String info;

    private final UUID id;

    private final String serviceId;

    private final int port;

    private final String type;

    private final int hash;

    /**
     * Initializes a new {@link MDNSServiceEntryImpl}.
     *
     * @param address The address
     * @param port The port
     * @param number The number
     * @param serviceId The service identifier
     * @param info The info
     * @param type The type
     */
    public MDNSServiceEntryImpl(final InetAddress address, final int port, final UUID id, final String serviceId, final String info, final String type) {
        super();
        this.address = address;
        this.port = port;
        this.id = id;
        this.serviceId = serviceId;
        this.info = info;
        this.type = type;
        hash = hashCode0();
    }

    private int hashCode0() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
        result = prime * result + port;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    public InetAddress getAddress() {
        return address;
    }

    public String getInfo() {
        return info;
    }

    public String getServiceId() {
        return serviceId;
    }

    public UUID getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return new StringBuilder(64).append("{id=").append(getUnformattedString(id)).append(", serviceId=").append(serviceId).append(
            ", address=").append(address.toString()).append(", port=").append(port).append('}').toString();
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MDNSServiceEntryImpl)) {
            return false;
        }
        final MDNSServiceEntryImpl other = (MDNSServiceEntryImpl) obj;
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (serviceId == null) {
            if (other.serviceId != null) {
                return false;
            }
        } else if (!serviceId.equals(other.serviceId)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

}

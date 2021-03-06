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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.saml.state;



/**
 * Default implementation of {@link AuthnRequestInfo}.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.6.1
 */
public class DefaultAuthnRequestInfo implements AuthnRequestInfo {

    private String requestId;

    private String domainName;

    private String loginPath;

    private String client;

    private String uriFragment;

    @Override
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String getDomainName() {
        return domainName;
    }

    @Override
    public String getLoginPath() {
        return loginPath;
    }

    @Override
    public String getClientID() {
        return client;
    }

    @Override
    public String getUriFragment() {
        return uriFragment;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    public void setClientID(String client) {
        this.client = client;
    }

    public void setUriFragment(String uriFragment) {
        this.uriFragment = uriFragment;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((client == null) ? 0 : client.hashCode());
        result = prime * result + ((domainName == null) ? 0 : domainName.hashCode());
        result = prime * result + ((loginPath == null) ? 0 : loginPath.hashCode());
        result = prime * result + ((requestId == null) ? 0 : requestId.hashCode());
        result = prime * result + ((uriFragment == null) ? 0 : uriFragment.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultAuthnRequestInfo other = (DefaultAuthnRequestInfo) obj;
        if (client == null) {
            if (other.client != null)
                return false;
        } else if (!client.equals(other.client))
            return false;
        if (domainName == null) {
            if (other.domainName != null)
                return false;
        } else if (!domainName.equals(other.domainName))
            return false;
        if (loginPath == null) {
            if (other.loginPath != null)
                return false;
        } else if (!loginPath.equals(other.loginPath))
            return false;
        if (requestId == null) {
            if (other.requestId != null)
                return false;
        } else if (!requestId.equals(other.requestId))
            return false;
        if (uriFragment == null) {
            if (other.uriFragment != null)
                return false;
        } else if (!uriFragment.equals(other.uriFragment))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DefaultAuthnRequestInfo [requestId=" + requestId + ", domainName=" + domainName
            + ", loginPath=" + loginPath + ", client=" + client + ", uriFragment=" + uriFragment + "]";
    }

}

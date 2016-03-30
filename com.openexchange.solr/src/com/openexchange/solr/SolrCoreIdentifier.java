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

package com.openexchange.solr;

import java.io.Serializable;
import com.openexchange.exception.OXException;

/**
 * {@link SolrCoreIdentifier}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class SolrCoreIdentifier implements Serializable {

    private static final long serialVersionUID = 562216889747741399L;

    private final int contextId;

    private final int userId;

    private final int module;

    /**
     * Initializes a new {@link SolrCoreIdentifier}.
     *
     * @param contextId
     * @param userId
     * @param module
     */
    public SolrCoreIdentifier(final int contextId, final int userId, final int module) {
        super();
        this.contextId = contextId;
        this.userId = userId;
        this.module = module;
    }

    public SolrCoreIdentifier(final String identifier) throws OXException {
        super();
        final int i1 = identifier.indexOf("sc_c");
        final int i2 = identifier.indexOf("_u");
        final int i3 = identifier.indexOf("_m");
        if (i1 == -1 || i2 == -1 || i3 == -1) {
            throw SolrExceptionCodes.IDENTIFIER_PARSE_ERROR.create(identifier);
        }

        try {
            contextId = Integer.parseInt(identifier.substring(i1 + 4, i2));
            userId = Integer.parseInt(identifier.substring(i2 + 2, i3));
            module = Integer.parseInt(identifier.substring(i3 + 2, identifier.length()));
        } catch (final NumberFormatException e) {
            throw SolrExceptionCodes.IDENTIFIER_PARSE_ERROR.create(identifier);
        }
    }

    /**
     * Gets the contextId
     *
     * @return The contextId
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Gets the userId
     *
     * @return The userId
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the module
     *
     * @return The module
     */
    public int getModule() {
        return module;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + contextId;
        result = prime * result + module;
        result = prime * result + userId;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SolrCoreIdentifier other = (SolrCoreIdentifier) obj;
        if (contextId != other.contextId) {
            return false;
        }
        if (module != other.module) {
            return false;
        }
        if (userId != other.userId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sc_c" + contextId + "_u" + userId + "_m" + module;
    }

}

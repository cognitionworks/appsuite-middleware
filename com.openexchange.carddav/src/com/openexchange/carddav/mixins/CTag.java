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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.carddav.mixins;

import org.apache.commons.logging.Log;

import com.openexchange.carddav.GroupwareCarddavFactory;
import com.openexchange.carddav.resources.CardDAVCollection;
import com.openexchange.exception.OXException;
import com.openexchange.log.LogFactory;
import com.openexchange.webdav.protocol.helpers.SingleXMLPropertyMixin;

/**
 * {@link CTag}
 * 
 * Specifies a "synchronization" token used to indicate when the contents of 
 * a calendar or scheduling Inbox or Outbox collection have changed.
 * 
 * Used by the Apple Addressbook client in Mac OS 10.6 for CardDAV purposes, too. 
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class CTag extends SingleXMLPropertyMixin {

	protected static final Log LOG = com.openexchange.log.Log.loggerFor(CTag.class);
	
	private final GroupwareCarddavFactory factory;
    private final CardDAVCollection collection;
    private long timestamp = -1;
    
    public CTag(GroupwareCarddavFactory factory, CardDAVCollection collection) {
        super("http://calendarserver.org/ns/", "getctag");
        this.factory = factory;
        this.collection = collection;
    }

    @Override
    protected String getValue() {
        return "http://www.open-xchange.com/carddav/ctag/" + getTimestamp();
    }
    
    private long getTimestamp() {
		if (-1 == this.timestamp) {
			try {
				String token = null;
				final String overrrideSyncToken = this.factory.getOverrideNextSyncToken();
				if (null != overrrideSyncToken && 0 < overrrideSyncToken.length()) {
					this.factory.setOverrideNextSyncToken(null);
					token = overrrideSyncToken;
				}
				if (null != token) {
					try {
						this.timestamp = Long.parseLong(token);
						LOG.debug("Overriding CTag property to '" + timestamp + "' for user '" + factory.getUser() + "'.");
					} catch (NumberFormatException e) {
						LOG.warn("Invalid sync token: '" + token + "'.");
					}
				}
				if (-1 == this.timestamp) {
					this.timestamp = this.collection.getLastModified().getTime();
				}
			} catch (OXException e) {
		        LOG.error(e.getMessage(), e);
		        this.timestamp = 0;
			}
		}
		return this.timestamp;
	}    
}

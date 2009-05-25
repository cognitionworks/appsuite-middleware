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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * PushDelayedObject
 * 
 * @author <a href="mailto:sebastian.kauss@netline-is.de">Sebastian Kauss</a>
 */

public class PushDelayedObject implements Delayed {

    private final long delay;

    private final AbstractPushObject abstractPushObject;

    private long creationTime;

    private final int hash;

    public PushDelayedObject(final long delay, final AbstractPushObject abstractPushObject) {
        this.delay = delay;
        this.abstractPushObject = abstractPushObject;
        creationTime = System.currentTimeMillis();
        final int prime = 31;
        int result = 1;
        result = prime * result + ((abstractPushObject == null) ? 0 : abstractPushObject.hashCode());
        hash = result;
    }

    public long getDelay(final TimeUnit timeUnit) {
        return (creationTime + delay) - System.currentTimeMillis();
    }

    public AbstractPushObject getPushObject() {
        return abstractPushObject;
    }

    public int compareTo(final Delayed delayed) {
        return 0;
    }

    public void updateTime() {
        creationTime = System.currentTimeMillis();
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
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PushDelayedObject)) {
            return false;
        }
        final PushDelayedObject other = (PushDelayedObject) obj;
        if (abstractPushObject == null) {
            if (other.abstractPushObject != null) {
                return false;
            }
        } else if (!abstractPushObject.equals(other.abstractPushObject)) {
            return false;
        }
        return true;
    }

}

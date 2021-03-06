
package com.openexchange.share.subscription;
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
 *    trademarks of the OX Software GmbH. group of companies.
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

/**
 * {@link ShareLinkState} - States to indicate a possible usage of the link
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.5
 */
public enum ShareLinkState {

    /**
     * State to indicate that the link belongs to a known share and is accessible.
     */
    SUBSCRIBED,

    /**
     * State to indicate that the link belongs to a known share but is not accessible at the moment because the remote
     * server indicates that credentials have been updated meanwhile.
     */
    CREDENTIALS_REFRESH,

    /**
     * State to indicate that the link is valid and belongs to a share that is not yet subscribed an can be added.
     */
    ADDABLE,

    /**
     * Similar to {@link #ADDABLE} but in addition the user needs to enter a password to add the share.
     */
    ADDABLE_WITH_PASSWORD,

    /**
     * State to indicate that the link belongs to a known share but is inaccessible at the moment.
     */
    INACCESSIBLE,

    /**
     * State to indicate that the link belongs to a known share but can no longer be accessed.
     */
    REMOVED,

    /**
     * State to indicate that the share link can't be resolved at all and thus can't be subscribed.
     */
    UNRESOLVABLE,

    /**
     * State to indicate that the subscription of the share is not supported,
     * i.e. a single file in an unknown folder is shared or the share belongs to
     * an anonymous guest
     * <p>
     * This state describes technical limitations
     */
    UNSUPPORTED,

    /**
     * State to indicate that the subscription of the link is not allowed,
     * i.e. when the share belongs not to the current user.
     * <p>
     * This state describes permissions limitations
     */
    FORBIDDEN,

    /**
     * State to indicate that the link belongs to a known share but is not subscribed at the moment.
     */
    UNSUBSCRIBED;

}

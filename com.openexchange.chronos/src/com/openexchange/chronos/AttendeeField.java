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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.chronos;

/**
 * {@link AttendeeField}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public enum AttendeeField {

    /**
     * The calendar user address of the attendee.
     */
    URI,
    /**
     * The common name of the attendee.
     */
    CN,
    /**
     * The internal identifier of the attendee.
     */
    ENTITY,
    /**
     * The calendar user who is acting on behalf of the attendee.
     */
    SENT_BY,
    /**
     * The calendar user type of the attendee.
     */
    CU_TYPE,
    /**
     * The participation role of the attendee.
     */
    ROLE,
    /**
     * The participation status of the attendee.
     */
    PARTSTAT,
    /**
     * The attendee's comment.
     */
    COMMENT,
    /**
     * The RSVP expectation of the attendee.
     */
    RSVP,
    /**
     * The identifier of the folder where the event is located in for the attendee.
     */
    FOLDER_ID,
    /**
     * The group- or list membership of the attendee.
     */
    MEMBER,
    /**
     * The e-mail address of the attendee.
     */
    EMAIL,
    /**
     * The attendee's time transparency of the event.
     */
    TRANSP,
    /**
     * Extended parameters of the attendee.
     */
    EXTENDED_PARAMETERS,

    ;
}

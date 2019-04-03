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



package com.openexchange.authentication.ucs.impl;

import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.authentication.LoginInfo;
import com.openexchange.authentication.ucs.common.UCSLookup;
import com.openexchange.exception.OXException;

/**
 *
 * Authentication Plugin for the UCS Server Product.
 * This Class implements the needed Authentication against an UCS LDAP Server:
 * 1. User enters following information on Loginscreen: username and password (NO CONTEXT, will be resolved by the LDAP Attribute)
 * 1a. Search for given "username"  (NOT with context) given by OX Loginmask with configured pattern and with configured LDAP BASE.
 * 2. If user is found, bind to LDAP Server with the found DN
 * 3. If BIND successfull, fetch the configured "context" Attribute and parse out the context name.
 * 4. Return context name and username to OX API!
 * 5. User is logged in!
 *
 * @author Manuel Kraft
 *
 */
public class UCSAuthentication implements AuthenticationService {

    private final UCSLookup ucsLookup;

    /**
     * Default constructor.
     *
     * @param configService The service to use
     */
    public UCSAuthentication(UCSLookup ucsLookup) {
        super();
        this.ucsLookup = ucsLookup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Authenticated handleLoginInfo(final LoginInfo loginInfo) throws OXException {
        return ucsLookup.handleLoginInfo(loginInfo);
    }

    @Override
    public Authenticated handleAutoLoginInfo(LoginInfo loginInfo) throws OXException {
        throw LoginExceptionCodes.NOT_SUPPORTED.create(UCSAuthentication.class.getName());
    }

}

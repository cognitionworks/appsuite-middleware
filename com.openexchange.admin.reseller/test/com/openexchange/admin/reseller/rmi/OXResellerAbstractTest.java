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

package com.openexchange.admin.reseller.rmi;

import com.openexchange.admin.reseller.rmi.dataobjects.ResellerAdmin;
import com.openexchange.admin.reseller.rmi.dataobjects.Restriction;
import com.openexchange.admin.rmi.AbstractTest;
import com.openexchange.admin.rmi.dataobjects.Credentials;

/**
 * @author choeger
 *
 */
public abstract class OXResellerAbstractTest extends AbstractTest {
    protected static final String TESTUSER = "testuser";
    protected static final String TESTCHANGEUSER = "testchange";
    protected static final String CHANGEDNAME = "changedchangedagain";
    protected static final String TESTRESTRICTIONUSER = "testwithrestriction";
    
    protected static Credentials DummyMasterCredentials(){
        return new Credentials("oxadminmaster","secret");
    }

    protected static Credentials ResellerFooCredentials() {
        return new Credentials("foo", "secret");
    }
    
    protected static Credentials ResellerBarCredentials() {
        return new Credentials("bar", "secret");
    }

    protected static ResellerAdmin FooAdminUser() {
        ResellerAdmin adm = TestAdminUser("foo", "Foo Admin");
        adm.setPassword("secret");
        return adm;
    }

    protected static ResellerAdmin BarAdminUser() {
        ResellerAdmin adm = TestAdminUser("bar", "Bar Admin");
        adm.setPassword("secret");
        return adm;
    }

    protected static ResellerAdmin TestAdminUser() {
        return TestAdminUser(TESTUSER, "Test Reseller Admin");
    }
    
    protected static ResellerAdmin TestAdminUser(final String name, final String displayname) {
        ResellerAdmin adm = new ResellerAdmin(name);
        adm.setDisplayname(displayname);
        adm.setPassword("secret");
        return adm;
    }

    protected static Restriction MaxContextRestriction() {
        return new Restriction(Restriction.MAX_CONTEXT,"10");
    }

    protected static Restriction MaxContextQuotaRestriction() {
        return new Restriction(Restriction.MAX_CONTEXT_QUOTA,"1000");
    }
}

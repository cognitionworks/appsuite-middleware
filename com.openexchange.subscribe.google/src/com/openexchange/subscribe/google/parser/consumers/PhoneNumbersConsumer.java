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

package com.openexchange.subscribe.google.parser.consumers;

import java.util.function.BiConsumer;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.extensions.PhoneNumber;
import com.openexchange.groupware.container.Contact;

/**
 * {@link PhoneNumbersConsumer} - Parses the contact's phone numbers. Note that google
 * can store an unlimited mount of phone numbers for a contact due to their different
 * data model (probably EAV). Our contacts API however can only store a handful, therefore
 * we only fetch the first seven we encounter.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public class PhoneNumbersConsumer implements BiConsumer<ContactEntry, Contact> {

    /**
     * Initialises a new {@link PhoneNumbersConsumer}.
     */
    public PhoneNumbersConsumer() {
        super();
    }

    @Override
    public void accept(ContactEntry t, Contact u) {
        if (!t.hasPhoneNumbers()) {
            return;
        }
        int count = 0;
        for (PhoneNumber pn : t.getPhoneNumbers()) {
            if (pn.getPrimary()) {
                u.setTelephonePrimary(pn.getPhoneNumber());
            }
            // Unfortunately we do not have enough information
            // about the type of the telephone number, nor we
            // can make an educated guess. So we simply fetching
            // as much as possible.
            switch (count++) {
                case 0:
                    u.setTelephoneOther(pn.getPhoneNumber());
                    break;
                case 1:
                    u.setTelephoneHome1(pn.getPhoneNumber());
                    break;
                case 2:
                    u.setTelephoneHome2(pn.getPhoneNumber());
                    break;
                case 3:
                    u.setTelephoneBusiness1(pn.getPhoneNumber());
                    break;
                case 4:
                    u.setTelephoneBusiness2(pn.getPhoneNumber());
                    break;
                case 5:
                    u.setTelephoneAssistant(pn.getPhoneNumber());
                    break;
                case 6:
                    u.setTelephoneCompany(pn.getPhoneNumber());
                    break;
                case 7:
                    u.setTelephoneCallback(pn.getPhoneNumber());
                    break;
                // Maybe add more?
                default:
                    return;
            }
        }
    }
}

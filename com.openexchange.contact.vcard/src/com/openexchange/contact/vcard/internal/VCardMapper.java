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

package com.openexchange.contact.vcard.internal;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.contact.vcard.VCardParameters;
import com.openexchange.contact.vcard.mapping.AddressMapping;
import com.openexchange.contact.vcard.mapping.AnniversaryMapping;
import com.openexchange.contact.vcard.mapping.BirthdayMapping;
import com.openexchange.contact.vcard.mapping.CategoriesMapping;
import com.openexchange.contact.vcard.mapping.ClassMapping;
import com.openexchange.contact.vcard.mapping.DistributionlistMapping;
import com.openexchange.contact.vcard.mapping.EMailMapping;
import com.openexchange.contact.vcard.mapping.ExtendedStringPropertyMapping;
import com.openexchange.contact.vcard.mapping.FormattedNameMapping;
import com.openexchange.contact.vcard.mapping.IMPPMapping;
import com.openexchange.contact.vcard.mapping.NicknameMapping;
import com.openexchange.contact.vcard.mapping.NoteMapping;
import com.openexchange.contact.vcard.mapping.OrganizationMapping;
import com.openexchange.contact.vcard.mapping.PhotoMapping;
import com.openexchange.contact.vcard.mapping.ProductIdMapping;
import com.openexchange.contact.vcard.mapping.RevisionMapping;
import com.openexchange.contact.vcard.mapping.RoleMapping;
import com.openexchange.contact.vcard.mapping.StructuredNameMapping;
import com.openexchange.contact.vcard.mapping.TelephoneMapping;
import com.openexchange.contact.vcard.mapping.TitleMapping;
import com.openexchange.contact.vcard.mapping.UIDMapping;
import com.openexchange.contact.vcard.mapping.URLMapping;
import com.openexchange.contact.vcard.mapping.VCardMapping;
import com.openexchange.groupware.container.Contact;
import ezvcard.VCard;

/**
 * {@link VCardMapper}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class VCardMapper {

    private final List<VCardMapping> mappings;

    /**
     * Initializes a new {@link VCardMapper}.
     */
    public VCardMapper() {
        super();
        mappings = new ArrayList<VCardMapping>();
        /*
         * ADR
         */
        mappings.add(new AddressMapping());
        /*
         * AGENT
         */
        // not mapped
        /*
         * ANNIVERSARY
         */
        mappings.add(new AnniversaryMapping());
        /*
         * BIRTHDAY
         */
        mappings.add(new BirthdayMapping());
        /*
         * BIRTHPLACE
         */
        // not mapped
        /*
         * CALADRURI
         */
        // not mapped
        /*
         * CALURI
         */
        // not mapped
        /*
         * CATEGORIES
         */
        mappings.add(new CategoriesMapping());
        /*
         * CLASS
         */
        mappings.add(new ClassMapping());
        /*
         * CLIENTPIDMAP
         */
        // not mapped
        /*
         * DEATHDATE
         */
        // not mapped
        /*
         * DEATHPLACE
         */
        // not mapped
        /*
         * EMAIL
         */
        mappings.add(new EMailMapping());
        /*
         * EXPERTISE
         */
        // not mapped
        /*
         * FBURL
         */
        // not mapped
        /*
         * FN
         */
        mappings.add(new FormattedNameMapping());
        /*
         * GENDER
         */
        // not mapped
        /*
         * GEO
         */
        // not mapped
        /*
         * HOBBY
         */
        // not mapped
        /*
         * IMPP
         */
        mappings.add(new IMPPMapping());
        /*
         * INTEREST
         */
        // not mapped
        /*
         * KEY
         */
        // not mapped
        /*
         * KIND / MEMBER
         */
        mappings.add(new DistributionlistMapping());
        // not mapped
        /*
         * LANG
         */
        // not mapped
        /*
         * LOGO
         */
        // not mapped
        /*
         * MAILER
         */
        // not mapped
        /*
         * MEMBER
         */
        // see KIND
        /*
         * NICKNAME
         */
        mappings.add(new NicknameMapping());
        /*
         * NOTE
         */
        mappings.add(new NoteMapping());
        /*
         * ORG
         */
        mappings.add(new OrganizationMapping());
        /*
         * ORG-DIRECTORY
         */
        // not mapped
        /*
         * LABEL
         */
        // not mapped
        /*
         * PHOTO
         */
        mappings.add(new PhotoMapping());
        /*
         * PRODID
         */
        mappings.add(new ProductIdMapping());
        /*
         * PROFILE
         */
        // not mapped
        /*
         * RELATED
         */
        // not mapped
        /*
         * REV
         */
        mappings.add(new RevisionMapping());
        /*
         * ROLE
         */
        mappings.add(new RoleMapping());
        /*
         * SORT-STRING
         */
        // not mapped
        /*
         * SOUND
         */
        // not mapped
        /*
         * NAME
         */
        // not mapped
        /*
         * SOURCE
         */
        // not mapped
        /*
         * N
         */
        mappings.add(new StructuredNameMapping());
        /*
         * TEL
         */
        mappings.add(new TelephoneMapping());
        /*
         * TZ
         */
        // not mapped
        /*
         * TITLE
         */
        mappings.add(new TitleMapping());
        /*
         * UID
         */
        mappings.add(new UIDMapping());
        /*
         * URL
         */
        mappings.add(new URLMapping());
        /*
         * VERSION
         */
        // not mapped
        /*
         * XML
         */
        // not mapped
        /*
         * X-... (well known / used elsewhere)
         */
        mappings.add(new ExtendedStringPropertyMapping(Contact.ASSISTANT_NAME, "X-ASSISTANT", "X-MS-ASSISTANT", "X-KADDRESSBOOK-X-AssistantsName", "X-EVOLUTION-ASSISTANT"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.MANAGER_NAME, "X-MANAGER", "X-MS-MANAGER", "X-KADDRESSBOOK-X-ManagersName", "X-EVOLUTION-MANAGER"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.SPOUSE_NAME, "X-SPOUSE", "X-MS-SPOUSE", "X-KADDRESSBOOK-X-SpouseName", "X-EVOLUTION-SPOUSE"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.NUMBER_OF_CHILDREN, "X-MS-CHILD"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.YOMI_FIRST_NAME, "X-PHONETIC-FIRST-NAME"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.YOMI_LAST_NAME, "X-PHONETIC-LAST-NAME"));
        /*
         * X-OX-...
         */
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD01, "X-OX-USERFIELD-01"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD02, "X-OX-USERFIELD-02"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD03, "X-OX-USERFIELD-03"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD04, "X-OX-USERFIELD-04"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD05, "X-OX-USERFIELD-05"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD06, "X-OX-USERFIELD-06"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD07, "X-OX-USERFIELD-07"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD08, "X-OX-USERFIELD-08"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD09, "X-OX-USERFIELD-09"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD10, "X-OX-USERFIELD-10"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD11, "X-OX-USERFIELD-11"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD12, "X-OX-USERFIELD-12"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD13, "X-OX-USERFIELD-13"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD14, "X-OX-USERFIELD-14"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD15, "X-OX-USERFIELD-15"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD16, "X-OX-USERFIELD-16"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD17, "X-OX-USERFIELD-17"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD18, "X-OX-USERFIELD-18"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD19, "X-OX-USERFIELD-19"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.USERFIELD20, "X-OX-USERFIELD-20"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.ROOM_NUMBER, "X-OX-ROOM-NUMBER"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.INFO, "X-OX-INFO"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.NUMBER_OF_EMPLOYEE, "X-OX-NUMBER-OF-EMPLOYEE"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.BUSINESS_CATEGORY, "X-OX-BUSINESS-CATEGORY"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.COMMERCIAL_REGISTER, "X-OX-COMMERCIAL-REGISTER"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.TAX_ID, "X-OX-TAX-ID"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.SALES_VOLUME, "X-OX-SALES-VOLUME"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.EMPLOYEE_TYPE, "X-OX-EMPLOYEE-TYPE"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.MARITAL_STATUS, "X-OX-MARITAL-STATUS"));
        mappings.add(new ExtendedStringPropertyMapping(Contact.YOMI_COMPANY, "X-OX-YOMI-COMPANY"));

    }

    /**
     * Exports a contact to a vCard, optionally merging with an existing vCard.
     *
     * @param contact The contact to export
     * @param vCard The vCard to merge the contact into, or <code>null</code> to export to a new vCard
     * @param parameters Further options to use, or <code>null</code> to use to the default options
     * @return The exported contact as vCard
     */
    public VCard exportContact(Contact contact, VCard vCard, VCardParameters parameters) {
        if (null == vCard) {
            vCard = new VCard();
        }
        for (VCardMapping mapping : mappings) {
            mapping.exportContact(contact, vCard, parameters);
        }
        return vCard;
    }

    /**
     * Imports a vCard, optionally merging with an existing contact.
     *
     * @param vCard The vCard to import
     * @param contact The contact to merge the vCard into, or <code>null</code> to import as a new contact
     * @param parameters Further options to use, or <code>null</code> to use to the default options
     * @return The imported vCard as contact
     */
    public Contact importVCard(VCard vCard, Contact contact, VCardParameters parameters) {
        if (null == contact) {
            contact = new Contact();
        }
        for (VCardMapping mapping : mappings) {
            mapping.importVCard(vCard, contact, parameters);
        }
        return contact;
    }

}
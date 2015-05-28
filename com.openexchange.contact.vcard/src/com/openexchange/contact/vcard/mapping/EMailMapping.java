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

package com.openexchange.contact.vcard.mapping;

import java.util.List;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import com.openexchange.contact.vcard.VCardParameters;
import com.openexchange.groupware.container.Contact;
import com.openexchange.java.Strings;
import ezvcard.VCard;
import ezvcard.parameter.EmailType;
import ezvcard.property.Email;

/**
 * {@link EMailMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class EMailMapping extends AbstractMapping {

    static final String TYPE_OTHER = "X-OTHER";

    @Override
    public void exportContact(Contact contact, VCard vCard, VCardParameters parameters) {
        List<Email> emails = vCard.getEmails();
        /*
         * email1 - type "WORK"
         */
        Email businessEmail = getPropertyWithTypes(emails, EmailType.WORK);
        if (has(contact, Contact.EMAIL1)) {
            if (null == businessEmail) {
                vCard.addEmail(contact.getEmail1(), EmailType.WORK, EmailType.PREF);
            } else {
                businessEmail.setValue(contact.getEmail1());
                addTypeIfMissing(businessEmail, EmailType.PREF.getValue());
            }
        } else if (null != businessEmail) {
            vCard.removeProperty(businessEmail);
        }
        /*
         * email2 - type "HOME"
         */
        Email homeEmail = getPropertyWithTypes(emails, EmailType.HOME);
        if (has(contact, Contact.EMAIL2)) {
            if (null == homeEmail) {
                vCard.addEmail(contact.getEmail2(), EmailType.HOME);
            } else {
                homeEmail.setValue(contact.getEmail2());
            }
        } else if (null != homeEmail) {
            vCard.removeProperty(homeEmail);
        }
        /*
         * email3 - type "X-OTHER", or no specific type
         */
        Email otherEmail = getPropertyWithTypes(emails, TYPE_OTHER);
        if (null == otherEmail) {
            otherEmail = getPropertyWithoutTypes(emails, 0, EmailType.WORK.getValue(), EmailType.HOME.getValue(), TYPE_OTHER, EmailType.TLX.getValue());
            if (null != otherEmail) {
                otherEmail.addParameter(ezvcard.parameter.VCardParameters.TYPE, TYPE_OTHER);
            }
        }
        if (has(contact, Contact.EMAIL3)) {
            if (null == otherEmail) {
                otherEmail = new Email(contact.getEmail3());
                otherEmail.addParameter(ezvcard.parameter.VCardParameters.TYPE, TYPE_OTHER);
                vCard.addEmail(otherEmail);
            } else {
                otherEmail.setValue(contact.getEmail3());
            }
        } else if (null != otherEmail) {
            vCard.removeProperty(otherEmail);
        }
        /*
         * telex - type "TLX"
         */
        Email telexEmail = getPropertyWithTypes(emails, EmailType.TLX);
        if (contact.containsTelephoneTelex()) {
            if (null == telexEmail) {
                vCard.addEmail(contact.getTelephoneTelex(), EmailType.TLX);
            } else {
                telexEmail.setValue(contact.getTelephoneTelex());
            }
        } else if (null != telexEmail) {
            vCard.removeProperty(telexEmail);
        }
    }

    @Override
    public void importVCard(VCard vCard, Contact contact, VCardParameters parameters) {
        /*
         * skip import for legacy distribution list vCards
         */
        if (isLegacyDistributionList(vCard)) {
            return;
        }
        List<Email> emails = vCard.getEmails();
        /*
         * email1 - type "WORK"
         */
        Email businessEmail = getPropertyWithTypes(emails, EmailType.WORK);
        contact.setEmail1(parseEMail(businessEmail, parameters));
        /*
         * email2 - type "HOME"
         */
        Email homeEmail = getPropertyWithTypes(emails, EmailType.HOME);
        contact.setEmail2(parseEMail(homeEmail, parameters));
        /*
         * email3 - type "X-OTHER", or no specific type
         */
        Email otherEmail = getPropertyWithTypes(emails, TYPE_OTHER);
        if (null == otherEmail) {
            otherEmail = getPropertyWithoutTypes(emails, 0, EmailType.WORK.getValue(), EmailType.HOME.getValue(), TYPE_OTHER, EmailType.TLX.getValue());
        }
        contact.setEmail3(parseEMail(otherEmail, parameters));
        /*
         * telex - type "TLX"
         */
        Email telexEmail = getPropertyWithTypes(emails, EmailType.TLX);
        contact.setTelephoneTelex(parseEMail(telexEmail, parameters));
    }

    private String parseEMail(Email property, VCardParameters parameters) {
        if (null != property) {
            String value = property.getValue();
            if (false == Strings.isEmpty(value)) {
                if (null != parameters && parameters.isValidateContactEMail()) {
                    try {
                        new InternetAddress(value);
                    } catch (AddressException e) {
                        addConversionWarning(parameters, e, "EMAIL", e.getMessage());
                    }
                }
                return value;
            }
        }
        return null;
    }

}

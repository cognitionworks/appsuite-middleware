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

package com.openexchange.contact.storage.rdb.sql;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.openexchange.contact.storage.rdb.internal.RdbServiceLookup;
import com.openexchange.contact.storage.rdb.mapping.Mappers;
import com.openexchange.context.ContextService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.notify.NotificationConfig;
import com.openexchange.groupware.notify.NotificationConfig.NotificationProperty;
import com.openexchange.java.Reference;
import com.openexchange.java.Strings;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.user.User;

/**
 * {@link ContactReader}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ContactReader {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ContactReader.class);

    private final Connection connection;
    private final int contextID;
    private final ResultSet resultSet;

    private Context context;

    /**
     * Initializes a new {@link ContactReader}.
     *
     * @param contextID The context ID
     * @param connection The database connection
     * @param resultSet The result set to read contacts from
     */
    public ContactReader(int contextID, Connection connection, ResultSet resultSet) {
        super();
        this.connection = connection;
        this.contextID = contextID;
        this.resultSet = resultSet;
    }

    /**
     * Deserializes all contacts found in the result set, using the supplied fields.
     *
     * @param fields The contact fields to read
     * @param withObjectUseCount ResultSet contains additional data for sorting contacts
     * @return The contacts, or an empty list if no there were no results
     * @throws SQLException
     * @throws OXException
     */
    public List<Contact> readContacts(ContactField[] fields, boolean withObjectUseCount) throws SQLException, OXException {
        List<Contact> contacts = new ArrayList<Contact>();
        while (resultSet.next()) {
            Contact contact = Mappers.CONTACT.fromResultSet(resultSet, fields);

            if (withObjectUseCount) {
                try {
                    contact.setUseCount(resultSet.getInt("value"));
                } catch (SQLException e) {
                    String query = Databases.getSqlStatement(resultSet.getStatement(), null);
                    LOGGER.warn("Failed to determine use-count information from {}", null == query ? "<unknown>" : query, e);
                }
            }
            contacts.add(contact);
        }
        return patchMailAddress(contacts);
    }

    /**
     * Deserializes the first contact found in the result set, using the supplied fields.
     *
     * @param fields The contact fields to read
     * @param withObjectUseCount ResultSet contains additional data for sorting contacts
     * @return The contact, or <code>null</code> if there was no result
     * @throws SQLException
     * @throws OXException
     */
    public Contact readContact(ContactField[] fields, boolean withObjectUseCount) throws SQLException, OXException {
        return resultSet.next() ? patch(Mappers.CONTACT.fromResultSet(resultSet, fields), withObjectUseCount) : null;
    }

    private static final int GUEST_CONTACT_FOLDER_ID = FolderObject.VIRTUAL_GUEST_CONTACT_FOLDER_ID;

    /**
     * Patches the default sender address into contacts of internal users.
     *
     * @param contacts The contacts
     * @return The contacts with the email address added
     * @throws OXException in case of errors
     */
    private List<Contact> patchMailAddress(List<Contact> contacts) throws OXException {
        List<Contact> internal = contacts.stream().filter((c) -> (c != null && c.getInternalUserId() > 0 && c.containsEmail1())).collect(Collectors.toList());
        if (internal.isEmpty()) {
            return contacts;
        }

        String senderSource = NotificationConfig.getProperty(NotificationProperty.FROM_SOURCE, "primaryMail");
        if (senderSource.equalsIgnoreCase("defaultSenderAddress")) {
            Reference<List<Contact>> guestContactFolderRef = new Reference<>(null);
            Reference<List<Contact>> notGuestContactFolderRef = new Reference<>(null);
            internal.forEach((c) -> {
                Reference<List<Contact>> ref = c.getParentFolderID() == GUEST_CONTACT_FOLDER_ID ? guestContactFolderRef : notGuestContactFolderRef;
                List<Contact> l = ref.getValue();
                if (l == null) {
                    l = new ArrayList<Contact>();
                    ref.setValue(l);
                }
                l.add(c);
            });

            // for contacts not in (virtual) guest contact folder (id 16)
            if (notGuestContactFolderRef.hasValue()) {
                List<Contact> notGuestContactFolder = notGuestContactFolderRef.getValue();
                Map<Integer, Contact> map = notGuestContactFolder.stream().collect(Collectors.toMap(c -> I(c.getInternalUserId()), c -> c));
                Map<Integer, String> addresses = UserSettingMailStorage.getInstance().getSenderAddresses(map.keySet(), getContext(), connection);
                addresses.entrySet().forEach((entry) -> {
                    String address = entry.getValue();
                    if (Strings.isNotEmpty(address)) {
                        map.get(entry.getKey()).setEmail1(address);
                    }
                });
            }

            // for contacts in (virtual) guest contact folder (id 16)
            if (guestContactFolderRef.hasValue()) {
                Map<Integer, Contact> map = guestContactFolderRef.getValue().stream().collect(Collectors.toMap(c -> I(c.getInternalUserId()), c -> c));
                User[] users = UserStorage.getInstance().getUser(getContext(), map.keySet().stream().mapToInt(i -> i(i)).toArray(), connection);
                for (User u : users) {
                    map.get(I(u.getId())).setEmail1(u.getMail());
                }
            }
        } else {
            Map<Integer, Contact> map = internal.stream().collect(Collectors.toMap(c -> I(c.getInternalUserId()), c -> c));
            User[] users = UserStorage.getInstance().getUser(getContext(), map.keySet().stream().mapToInt(i -> i(i)).toArray(), connection);
            for (User u : users) {
                map.get(I(u.getId())).setEmail1(u.getMail());
            }
        }

        return contacts;
    }

    private Contact patch(Contact contact, boolean withObjectUseCount) throws SQLException, OXException {
        if (null != contact) {
            if (contact.getInternalUserId() > 0 && contact.containsEmail1()) {
                String senderSource = NotificationConfig.getProperty(NotificationProperty.FROM_SOURCE, "primaryMail");

                //TODO: Guests do not have mail settings
                if (contact.getParentFolderID() != GUEST_CONTACT_FOLDER_ID && senderSource.equalsIgnoreCase("defaultSenderAddress")) {
                    Optional<String> optionalAddress = UserSettingMailStorage.getInstance().getSenderAddress(contact.getInternalUserId(), context, connection);
                    String defaultSendAddress = optionalAddress.orElse(null);
                    if (Strings.isNotEmpty(defaultSendAddress)) {
                        contact.setEmail1(defaultSendAddress);
                    }
                } else {
                    String primaryMail = UserStorage.getInstance().getUser(contact.getInternalUserId(), contextID).getMail();
                    contact.setEmail1(primaryMail);
                }
            }
            if (withObjectUseCount) {
                try {
                    contact.setUseCount(resultSet.getInt("value"));
                } catch (SQLException e) {
                    String query = Databases.getSqlStatement(resultSet.getStatement(), null);
                    LOGGER.warn("Failed to determine use-count information from {}", null == query ? "<unknown>" : query, e);
                }
            }
        }
        return contact;
    }

    private Context getContext() throws OXException {
        if (null == context) {
            context = RdbServiceLookup.getService(ContextService.class, true).getContext(contextID);
        }
        return context;
    }

}

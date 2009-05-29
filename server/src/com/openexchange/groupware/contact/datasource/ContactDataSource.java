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

package com.openexchange.groupware.contact.datasource;

import static com.openexchange.ajax.AJAXServlet.PARAMETER_FOLDERID;
import static com.openexchange.ajax.AJAXServlet.PARAMETER_ID;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.api2.OXException;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataException;
import com.openexchange.conversion.DataExceptionCodes;
import com.openexchange.conversion.DataProperties;
import com.openexchange.conversion.DataSource;
import com.openexchange.conversion.SimpleData;
import com.openexchange.groupware.contact.ContactInterface;
import com.openexchange.groupware.contact.ContactInterfaceDiscoveryService;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;
import com.openexchange.tools.versit.Versit;
import com.openexchange.tools.versit.VersitDefinition;
import com.openexchange.tools.versit.VersitObject;
import com.openexchange.tools.versit.converter.ConverterException;
import com.openexchange.tools.versit.converter.OXContainerConverter;

/**
 * {@link ContactDataSource} - A data source for contacts.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ContactDataSource implements DataSource {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(ContactDataSource.class);

    private static final Class<?>[] TYPES = { InputStream.class, byte[].class };

    private static final String[] ARGS = { "com.openexchange.groupware.contact.pairs" };

    /**
     * Initializes a new {@link ContactDataSource}
     */
    public ContactDataSource() {
        super();
    }

    public <D> Data<D> getData(final Class<? extends D> type, final DataArguments dataArguments, final Session session) throws DataException {
        if (!InputStream.class.equals(type) && !byte[].class.equals(type)) {
            throw DataExceptionCodes.TYPE_NOT_SUPPORTED.create(type.getName());
        }
        /*
         * Check arguments
         */
        final int[] folderIds;
        final int[] objectIds;
        {
            final JSONArray pairsArray;
            try {
                pairsArray = new JSONArray(dataArguments.get(ARGS[0]));
                final int len = pairsArray.length();
                if (len == 0) {
                    throw DataExceptionCodes.MISSING_ARGUMENT.create(ARGS[0]);
                }
                folderIds = new int[len];
                objectIds = new int[len];
                for (int i = 0; i < len; i++) {
                    final JSONObject pairObject = pairsArray.getJSONObject(i);
                    folderIds[i] = pairObject.getInt(PARAMETER_FOLDERID);
                    objectIds[i] = pairObject.getInt(PARAMETER_ID);
                }
            } catch (final JSONException e1) {
                throw DataExceptionCodes.INVALID_ARGUMENT.create(ARGS[0], dataArguments.get(ARGS[0]));
            }
        }
        /*
         * Get contact
         */
        final ContactObject[] contacts = new ContactObject[folderIds.length];
        try {
            final Map<Integer, ContactInterface> tmp = new HashMap<Integer, ContactInterface>(contacts.length);
            for (int i = 0; i < contacts.length; i++) {
                final int folderId = folderIds[i];
                final Integer key = Integer.valueOf(folderId);
                ContactInterface contactInterface = tmp.get(key);
                if (null == contactInterface) {
                    contactInterface = ServerServiceRegistry.getInstance().getService(ContactInterfaceDiscoveryService.class).newContactInterface(
                        folderId,
                        session);
                    tmp.put(key, contactInterface);
                }
                contacts[i] = contactInterface.getObjectById(objectIds[i], folderId);
            }
        } catch (final OXException e) {
            throw new DataException(e);
        }
        /*
         * Create necessary objects
         */
        final ByteArrayOutputStream byteArrayOutputStream = new UnsynchronizedByteArrayOutputStream(folderIds.length << 12);
        final VersitDefinition contactDef = Versit.getDefinition("text/vcard");
        for (final ContactObject contact : contacts) {
            writeVCard2Stream(contact, byteArrayOutputStream, contactDef, session);
        }
        /*
         * Return data
         */
        final DataProperties properties = new DataProperties();
        properties.put(DataProperties.PROPERTY_CHARSET, "UTF-8");
        properties.put(DataProperties.PROPERTY_VERSION, "3.0");
        properties.put(DataProperties.PROPERTY_CONTENT_TYPE, "text/vcard");
        final String displayName = contacts.length == 1 ? contacts[0].getDisplayName() : null;
        properties.put(DataProperties.PROPERTY_NAME, displayName == null ? "vcard.vcf" : new StringBuilder(
            displayName.replaceAll(" +", "_")).append(".vcf").toString());
        final byte[] vcardBytes = byteArrayOutputStream.toByteArray();
        properties.put(DataProperties.PROPERTY_SIZE, String.valueOf(vcardBytes.length));
        return new SimpleData<D>(
            (D) (InputStream.class.equals(type) ? new UnsynchronizedByteArrayInputStream(vcardBytes) : vcardBytes),
            properties);
    }

    private static void writeVCard2Stream(final ContactObject contact, final ByteArrayOutputStream byteArrayOutputStream, final VersitDefinition contactDef, final Session session) throws DataException {
        final VersitDefinition.Writer versitWriter;
        try {
            versitWriter = contactDef.getWriter(byteArrayOutputStream, "UTF-8");
        } catch (final IOException e) {
            throw DataExceptionCodes.ERROR.create(e, e.getMessage());
        }
        final OXContainerConverter oxContainerConverter;
        try {
            oxContainerConverter = new OXContainerConverter(session);
        } catch (final ConverterException e) {
            throw DataExceptionCodes.ERROR.create(e, e.getMessage());
        }
        /*
         * Convert
         */
        try {
            final VersitObject versitObject = oxContainerConverter.convertContact(contact, "3.0");
            contactDef.write(versitWriter, versitObject);
            versitWriter.flush();
        } catch (final ConverterException e) {
            throw DataExceptionCodes.ERROR.create(e, e.getMessage());
        } catch (final IOException e) {
            throw DataExceptionCodes.ERROR.create(e, e.getMessage());
        } finally {
            closeVersitResources(oxContainerConverter, versitWriter);
        }
    }

    public String[] getRequiredArguments() {
        return ARGS;
    }

    public Class<?>[] getTypes() {
        return TYPES;
    }

    private static void closeVersitResources(final OXContainerConverter oxContainerConverter, final VersitDefinition.Writer versitWriter) {
        if (oxContainerConverter != null) {
            oxContainerConverter.close();
        }
        if (versitWriter != null) {
            try {
                versitWriter.close();
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}

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

package com.openexchange.groupware.infostore.database.impl;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.*;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.tx.DBProvider;
import com.openexchange.groupware.tx.TransactionException;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.infostore.Classes;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreExceptionFactory;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.groupware.infostore.utils.MetadataSwitcher;
import com.openexchange.tools.sql.DBUtils;
import com.openexchange.tools.encoding.Charsets;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@OXExceptionSource(
		classId=Classes.COM_OPENEXCHANGE_GROUPWARE_INFOSTORE_DATABASE_IMPL_CHECKSIZESWITCH,
		component=EnumComponent.INFOSTORE
)
public class CheckSizeSwitch {
	
	private static final InfostoreExceptionFactory EXCEPTIONS = new InfostoreExceptionFactory(CheckSizeSwitch.class);
    private static final Log LOG = LogFactory.getLog(CheckSizeSwitch.class);

    private static Map<Metadata, Integer> SIZES = new HashMap<Metadata, Integer>();
    private DBProvider provider;
    private Context ctx;

    private static final Set<Metadata> FIELDS_TO_CHECK = new HashSet<Metadata>() {{
        add(Metadata.CATEGORIES_LITERAL);
        add(Metadata.FILE_MIMETYPE_LITERAL);
        add(Metadata.FILENAME_LITERAL);
        add(Metadata.URL_LITERAL);
    }};

    public CheckSizeSwitch(DBProvider provider, Context ctx) {
        this.provider = provider;
        this.ctx = ctx;
    }

    @OXThrows(
			category = Category.TRUNCATED,
			desc = "The User entered values that are to long for the database schema.",
			exceptionId = 0,
			msg = "Some fields have values, that are too long"
	)
	public static void checkSizes(final DocumentMetadata metadata, DBProvider provider, Context ctx) throws OXException {
		boolean error = false;
		
		final CheckSizeSwitch checkSize = new CheckSizeSwitch(provider, ctx);
		final GetSwitch get = new GetSwitch(metadata);
		
		final List<Metadata> tooLongData = new ArrayList<Metadata>();
        final OXException x = EXCEPTIONS.create(0);
        
        for(final Metadata m : Metadata.VALUES) {
            if(!FIELDS_TO_CHECK.contains(m)) {
                continue;
            }
            Object value = m.doSwitch(get);
            int maxSize = checkSize.getSize(m);
            int valueLength;
            if (value instanceof String) {
                valueLength = Charsets.getBytes((String) value, Charsets.UTF_8).length;
            } else {
                valueLength = 0;
            }
            if(maxSize < valueLength) {
                AbstractOXException.ProblematicAttribute attr = new SimpleTruncatedAttribute(m.getId(), maxSize, valueLength);
                x.addProblematic(attr);
                error = true;
            }
        }
		
		if(error) {
			throw x;
		}
	}
	


    public int getSize(Metadata field) {
        if(SIZES.containsKey(field)) {
            return SIZES.get(field);
        }

        Connection con = null;
        try {
            con = provider.getWriteConnection(ctx);
            String[] tuple = new InfostoreQueryCatalog().getFieldTuple(field, new InfostoreQueryCatalog.VersionWins());
            int size = DBUtils.getColumnSize(con, tuple[0], tuple[1]);
            SIZES.put(field, size);
            return size;
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
            return 0;
        } catch (TransactionException e) {
            LOG.error(e.getMessage(),  e);
            return 0;
        } finally {
            provider.releaseWriteConnection(ctx, con);
        }

    }

}

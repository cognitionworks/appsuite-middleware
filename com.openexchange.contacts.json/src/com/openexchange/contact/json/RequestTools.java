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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.contact.json;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;


/**
 * {@link RequestTools}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class RequestTools {
    
    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(RequestTools.class));
    
    public static int[] getColumnsAsIntArray(final AJAXRequestData request, final String parameter) throws OXException {
        final String valueStr = request.getParameter("columns");
        final String[] valueStrArr = valueStr.split(",");
        
        final int[] values = new int[valueStrArr.length];
        for (int i = 0; i < values.length; i++) {
            try {
                values[i] = Integer.parseInt(valueStrArr[i].trim());
            } catch (final NumberFormatException e) {
                throw AjaxExceptionCodes.InvalidParameterValue.create(e, "columns", valueStr);
            }
        }
        
        return values;
    }
    
    public static int getNullableIntParameter(final AJAXRequestData request, final String parameter) throws OXException {
        Integer intParam = null;
        try {
            intParam = request.getParameter(parameter, int.class);
            if (intParam == null) {
                return 0;
            } else {
                return intParam.intValue();
            }
        } catch (final NumberFormatException e) {
            throw AjaxExceptionCodes.InvalidParameterValue.create(e, parameter, intParam);
        } 
    }

    public static int[][] buildObjectIdAndFolderId(final JSONArray json) throws OXException {
        final int[][] objectIdAndFolderId = new int[json.length()][];
        for (int i = 0; i < json.length(); i++) {
            try {
                final JSONObject object = json.getJSONObject(i);
                final int folder = object.getInt("folder");
                final int id = object.getInt("id");
                objectIdAndFolderId[i] = new int[] { id, folder };
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
            }
        }
        
        return objectIdAndFolderId;
    }
    
    public static void setImageData(final Contact contact, final UploadFile file) throws OXException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file.getTmpFile());
            final ByteArrayOutputStream tmp = new UnsynchronizedByteArrayOutputStream((int) file.getSize());
            final byte[] buf = new byte[2048];
            int len = -1;
            while ((len = fis.read(buf)) != -1) {
                tmp.write(buf, 0, len);
            }
            contact.setImage1(tmp.toByteArray());
            contact.setImageContentType(file.getContentType());
        } catch (final FileNotFoundException e) {
            throw AjaxExceptionCodes.NoUploadImage.create(e);
        } catch (final IOException e) {
            throw AjaxExceptionCodes.UnexpectedError.create(e, "I/O error while reading uploaded contact image.");
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (final IOException e) {
                    LOG.warn("Error while closing FileInputStream for contact image upload.", e);
                }
            }
        }        
    }

}

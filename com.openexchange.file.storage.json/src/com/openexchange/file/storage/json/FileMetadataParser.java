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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.file.storage.json;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.file.storage.AbstractFileFieldHandler;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.meta.FileFieldSet;
import com.openexchange.file.storage.parse.FileMetadataParserService;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.tools.servlet.AjaxException;

/**
 * {@link FileMetadataParser}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class FileMetadataParser implements FileMetadataParserService {

    private static final FileMetadataParser instance = new FileMetadataParser();

    /**
     * Gets the instance.
     * 
     * @return The instance
     */
    public static FileMetadataParser getInstance() {
        return instance;
    }

    private final JSONParserHandler jsonHandler;
    
    private FileMetadataParser() {
        super();
        jsonHandler = new JSONParserHandler();
    }

    public File parse(JSONObject object) throws AbstractOXException {
        DefaultFile file = new DefaultFile();

        try {
            File.Field.inject(jsonHandler, file, object);
        } catch (RuntimeException x) {
            if(x.getCause() != null && JSONException.class.isInstance(x.getCause())) {
                throw new AjaxException(AjaxException.Code.JSONError, x.getCause().getMessage());
            }
            throw x;
        }

        return file;
    }

    private static final class JSONParserHandler extends AbstractFileFieldHandler {

        private FileFieldSet set = new FileFieldSet();

        public Object handle(Field field, Object... args) {
            File md = md(args);
            JSONObject object = get(1, JSONObject.class, args);
            if(!object.has(field.getName())) {
                return md;
            }
            
            try {
                Object value = object.get(field.getName());
                
                value = process(field, value);
                
                field.doSwitch(set, md, value);
            } catch (JSONException x) {
                throw new RuntimeException(x);
            }
            
            
            return md;
        }

        private Object process(Field field, Object value) throws JSONException {
            switch(field) {
            case CATEGORIES: return categories((JSONArray) value);
            default: return value;
            }
        }

        private Object categories(JSONArray value) throws JSONException {
            StringBuilder b = new StringBuilder();
            for(int i = 0, size = value.length(); i < size; i++) {
                b.append(value.getString(i)).append(", ");
            }
            b.setLength(b.length()-2);
            return b.toString();
        }
    }

    public List<Field> getFields(final JSONObject object) {
        return File.Field.inject(new AbstractFileFieldHandler() {

            public Object handle(Field field, Object... args) {
                List<File.Field> fields = (List<File.Field>) args[0];
                if(object.has(field.getName())) {
                    fields.add(field);
                }
                return fields;
            }
            
        }, new ArrayList<File.Field>());
    }

}

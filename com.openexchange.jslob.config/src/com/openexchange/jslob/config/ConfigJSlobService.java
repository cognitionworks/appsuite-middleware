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

package com.openexchange.jslob.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONValue;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.jslob.JSONPathElement;
import com.openexchange.jslob.JSONUpdate;
import com.openexchange.jslob.JSlob;
import com.openexchange.jslob.JSlobExceptionCodes;
import com.openexchange.jslob.JSlobService;
import com.openexchange.jslob.storage.JSlobId;
import com.openexchange.jslob.storage.JSlobStorage;
import com.openexchange.jslob.storage.registry.JSlobStorageRegistry;
import com.openexchange.server.ServiceLookup;

/**
 * {@link ConfigJSlobService}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ConfigJSlobService implements JSlobService {

    private static final List<String> ALIASES = Arrays.asList("config");

    private static final String PREFERENCE_PATH = "preferencePath".intern();

    private static final String METADATA_PREFIX = "meta".intern();

    private static final String ID = "io.ox.wd.jslob.config";

    /*-
     * ------------------------- Member stuff -----------------------------
     */

    private final ServiceLookup services;

    private final Map<String, AttributedProperty> preferenceItems;

    /**
     * Initializes a new {@link ConfigJSlobService}.
     * 
     * @throws OXException If initialization fails
     */
    public ConfigJSlobService(final ServiceLookup services) throws OXException {
        super();
        this.services = services;
        preferenceItems = initPreferenceItems();
    }

    private Map<String, AttributedProperty> initPreferenceItems() throws OXException {
        final ConfigView view = getConfigViewFactory().getView();
        final Map<String, ComposedConfigProperty<String>> all = view.all();
        final Map<String, AttributedProperty> preferenceItems = new HashMap<String, AttributedProperty>(all.size() >> 1);
        final Log logger = com.openexchange.log.Log.valueOf(LogFactory.getLog(ConfigJSlobService.class));
        for (final Map.Entry<String, ComposedConfigProperty<String>> entry : all.entrySet()) {
            // Check for existence of "preferencePath"
            final ComposedConfigProperty<String> property = entry.getValue();
            final String preferencePath = property.get(PREFERENCE_PATH);
            if (null != preferencePath) {
                try {
                    preferenceItems.put(preferencePath, new AttributedProperty(preferencePath, entry.getKey(), property));
                } catch (final Exception e) {
                    logger.warn("Couldn't initialize preference path: " + preferencePath, e);
                }
            }
        }
        return Collections.unmodifiableMap(preferenceItems);
    }

    @Override
    public JSlob get(final String id, final int user, final int context) throws OXException {
        /*
         * Get from storage
         */
        JSlob jsonJSlob = getStorage().opt(new JSlobId(ID, id, user, context));
        if (null == jsonJSlob) {
            jsonJSlob = new JSlob(new JSONObject());
        }
        /*
         * Fill with config cascade settings
         */
        final ConfigView view = getConfigViewFactory().getView(user, context);
        for (final AttributedProperty attributedProperty : preferenceItems.values()) {
            add2JSlob(attributedProperty, jsonJSlob, view);
        }
        return jsonJSlob;
    }

    @Override
    public String getIdentifier() {
        return ID;
    }

    @Override
    public List<String> getAliases() {
        return ALIASES;
    }

    @Override
    public void set(final String id, final JSlob jsonJSlob, final int user, final int context) throws OXException {
        /*
         * Set in storage
         */
        if (null == jsonJSlob) {
            getStorage().remove(new JSlobId(ID, id, user, context));
        } else {
            final ConfigView view = getConfigViewFactory().getView(user, context);
            final JSONObject current = jsonJSlob.getJsonObject();
            for (final AttributedProperty attributedProperty : preferenceItems.values()) {
                final Object value = getPathFrom(attributedProperty.path, current);
                if (null != value) {
                    try {
                        final String oldValue = view.get(attributedProperty.propertyName, String.class);
                        // Clients have a habit of dumping the config back at us, so we only save differing values.
                        if (!value.equals(oldValue)) {
                            view.set("user", attributedProperty.propertyName, value);
                        }
                    } catch (final OXException e) {
                        throw new OXException(e);
                    }
                }
            }
            // Finally store JSlob
            getStorage().store(new JSlobId(ID, id, user, context), jsonJSlob);
        }
    }

    @Override
    public void update(final String id, final JSONUpdate jsonUpdate, final int user, final int context) throws OXException {
        try {
            /*
             * Look-up appropriate storage
             */
            final JSlobStorage storage = getStorage();
            final JSlobId jslobId = new JSlobId(ID, id, user, context);
            /*
             * Get JSlob
             */
            final JSONObject storageObject;
            JSlob jsonJSlob = storage.opt(jslobId);
            if (null == jsonJSlob) {
                jsonJSlob = new JSlob();
                storageObject = new JSONObject();
            } else {
                storageObject = jsonJSlob.getJsonObject();
            }
            /*
             * Examine path
             */
            final List<JSONPathElement> path = jsonUpdate.getPath();
            if (path.isEmpty()) {
                /*
                 * Update whole object
                 */
                set(id, jsonJSlob.setJsonObject((JSONObject) jsonUpdate.getValue()), user, context);
                return;
            }
            /*
             * Update in config cascade
             */
            final int size = path.size();
            {
                final StringBuilder pathBuilder = new StringBuilder(16);
                pathBuilder.append(path.get(0).toString());
                for (int i = 1; i < size; i++) {
                    pathBuilder.append('/').append(path.get(i).toString());
                }
                final AttributedProperty attributedProperty = preferenceItems.get(pathBuilder.toString());
                if (null != attributedProperty) {
                    final Object value = jsonUpdate.getValue();
                    if (null != value) {
                        try {
                            final ConfigView view = getConfigViewFactory().getView(user, context);
                            final String oldValue = view.get(attributedProperty.propertyName, String.class);
                            // Clients have a habit of dumping the config back at us, so we only save differing values.
                            if (!value.equals(oldValue)) {
                                view.set("user", attributedProperty.propertyName, value);
                            }
                        } catch (final OXException e) {
                            throw new OXException(e);
                        }
                    }
                }
            }
            /*
             * Iterate path except last element
             */
            final int msize = size - 1;
            JSONObject current = storageObject;
            for (int i = 0; i < msize; i++) {
                final JSONPathElement jsonPathElem = path.get(i);
                final int index = jsonPathElem.getIndex();
                final String name = jsonPathElem.getName();
                if (index >= 0) {
                    /*
                     * Denotes an index within a JSON array
                     */
                    if (isInstance(name, JSONArray.class, current)) {
                        final JSONArray jsonArray = current.getJSONArray(name);
                        if (index >= jsonArray.length()) {
                            current = putNewJSONObject(jsonArray);
                        } else {
                            if (isInstance(index, JSONObject.class, jsonArray)) {
                                current = jsonArray.getJSONObject(index);
                            } else {
                                current = putNewJSONObject(index, jsonArray);
                            }
                        }
                    } else {
                        final JSONArray newArray = new JSONArray();
                        current.put(name, newArray);
                        current = putNewJSONObject(newArray);
                    }
                } else {
                    /*
                     * Denotes an element within a JSON object
                     */
                    if (isInstance(name, JSONObject.class, current)) {
                        current = current.getJSONObject(name);
                    } else {
                        final JSONObject newObject = new JSONObject();
                        current.put(name, newObject);
                        current = newObject;
                    }
                }
            }
            /*
             * Handle last path element
             */
            final JSONPathElement lastPathElem = path.get(msize);
            final int index = lastPathElem.getIndex();
            final String name = lastPathElem.getName();
            if (index >= 0) {
                if (isInstance(name, JSONArray.class, current)) {
                    current.getJSONArray(name).put(index, jsonUpdate.getValue());
                } else {
                    final JSONArray newArray = new JSONArray();
                    current.put(name, newArray);
                    newArray.put(jsonUpdate.getValue());
                }
            } else {
                current.put(name, jsonUpdate.getValue());
            }
            /*
             * Write to store
             */
            storage.store(jslobId, jsonJSlob.setJsonObject(storageObject));
        } catch (final JSONException e) {
            throw JSlobExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private static JSONObject putNewJSONObject(final JSONArray jsonArray) throws JSONException {
        return putNewJSONObject(-1, jsonArray);
    }

    private static JSONObject putNewJSONObject(final int index, final JSONArray jsonArray) throws JSONException {
        final JSONObject newObject = new JSONObject();
        if (index >= 0) {
            jsonArray.put(index, newObject);
        } else {
            jsonArray.put(newObject);
        }
        return newObject;
    }

    private static boolean isInstance(final String name, final Class<? extends JSONValue> clazz, final JSONObject jsonObject) {
        if (!jsonObject.hasAndNotNull(name)) {
            return false;
        }
        return clazz.isInstance(jsonObject.opt(name));
    }

    private static boolean isInstance(final int index, final Class<? extends JSONValue> clazz, final JSONArray jsonArray) {
        return clazz.isInstance(jsonArray.opt(index));
    }

    private JSlobStorage getStorage() throws OXException {
        final JSlobStorageRegistry storageRegistry = services.getService(JSlobStorageRegistry.class);
        // TODO: Make configurable
        final String storageId = "io.ox.wd.jslob.storage.db";
        final JSlobStorage storage = storageRegistry.getJSlobStorage(storageId);
        if (null == storage) {
            throw JSlobExceptionCodes.NOT_FOUND.create(storageId);
        }
        return storage;
    }

    private ConfigViewFactory getConfigViewFactory() {
        return services.getService(ConfigViewFactory.class);
    }

    private static void add2JSlob(final AttributedProperty attributedProperty, final JSlob jsonJSlob, final ConfigView view) throws OXException {
        if (null == attributedProperty) {
            return;
        }
        try {
            // Add property's value
            List<JSONPathElement> path = attributedProperty.path;
            Object value = asJSObject(view.get(attributedProperty.propertyName, String.class));
            addValueByPath(path, value, jsonJSlob);
            // Add the metadata as well
            final StringBuilder sb = new StringBuilder(attributedProperty.preferencePath);
            final int resetLength = sb.length();
            final ComposedConfigProperty<String> preferenceItem = attributedProperty.property;
            final List<String> metadataNames = preferenceItem.getMetadataNames();
            if (null != metadataNames && !metadataNames.isEmpty()) {
                for (final String metadataName : metadataNames) {
                    // Parse metadata path
                    sb.setLength(resetLength);
                    sb.append('/').append(metadataName).append('/').append(METADATA_PREFIX);
                    path = JSONPathElement.parsePath(sb.toString());
                    // Metadata value
                    final ComposedConfigProperty<String> prop = view.property(attributedProperty.propertyName, String.class);
                    value = asJSObject(prop.get(metadataName));
                    // Add to JSlob
                    addValueByPath(path, value, jsonJSlob);
                }
            }
            // Lastly, let's add configurability.
            sb.setLength(resetLength);
            sb.append('/').append("configurable").append('/').append(METADATA_PREFIX);
            path = JSONPathElement.parsePath(sb.toString());
            final String finalScope = preferenceItem.get("final");
            final String isProtected = preferenceItem.get("protected");
            final boolean writable =
                (finalScope == null || finalScope.equals("user")) && (isProtected == null || !preferenceItem.get("protected", boolean.class).booleanValue());
            value = Boolean.valueOf(writable);
            addValueByPath(path, value, jsonJSlob);
        } catch (final JSONException e) {
            throw JSlobExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private static void addValueByPath(final List<JSONPathElement> path, final Object value, final JSlob jsonJSlob) throws JSONException {
        final int msize = path.size() - 1;
        JSONObject current = jsonJSlob.getJsonObject();
        for (int i = 0; i < msize; i++) {
            final JSONPathElement jsonPathElem = path.get(i);
            final int index = jsonPathElem.getIndex();
            final String name = jsonPathElem.getName();
            if (index >= 0) {
                /*
                 * Denotes an index within a JSON array
                 */
                if (isInstance(name, JSONArray.class, current)) {
                    final JSONArray jsonArray = current.getJSONArray(name);
                    if (index >= jsonArray.length()) {
                        current = putNewJSONObject(jsonArray);
                    } else {
                        if (isInstance(index, JSONObject.class, jsonArray)) {
                            current = jsonArray.getJSONObject(index);
                        } else {
                            current = putNewJSONObject(index, jsonArray);
                        }
                    }
                } else {
                    final JSONArray newArray = new JSONArray();
                    current.put(name, newArray);
                    current = putNewJSONObject(newArray);
                }
            } else {
                /*
                 * Denotes an element within a JSON object
                 */
                if (isInstance(name, JSONObject.class, current)) {
                    current = current.getJSONObject(name);
                } else {
                    final JSONObject newObject = new JSONObject();
                    current.put(name, newObject);
                    current = newObject;
                }
            }
        }
        /*
         * Handle last path element
         */
        final JSONPathElement lastPathElem = path.get(msize);
        final int index = lastPathElem.getIndex();
        final String name = lastPathElem.getName();
        if (index >= 0) {
            if (isInstance(name, JSONArray.class, current)) {
                current.getJSONArray(name).put(index, value);
            } else {
                final JSONArray newArray = new JSONArray();
                current.put(name, newArray);
                newArray.put(value);
            }
        } else {
            current.put(name, value);
        }
    }

    private static Object getPathFrom(final List<JSONPathElement> jPath, final JSONObject jObject) {
        JSONObject jCurrent = jObject;
        final int msize = jPath.size() - 1;
        for (int i = 0; i < msize; i++) {
            final JSONPathElement jPathElement = jPath.get(i);
            final int index = jPathElement.getIndex();
            final String name = jPathElement.getName();
            if (index >= 0) {
                /*
                 * Denotes an index within a JSON array
                 */
                if (isInstance(name, JSONArray.class, jCurrent)) {
                    try {
                        final JSONArray jsonArray = jCurrent.getJSONArray(name);
                        jCurrent = jsonArray.getJSONObject(index);
                    } catch (final JSONException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                /*
                 * Denotes an element within a JSON object
                 */
                if (isInstance(name, JSONObject.class, jCurrent)) {
                    try {
                        jCurrent = jCurrent.getJSONObject(name);
                    } catch (final JSONException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }
        try {
            final JSONPathElement leaf = jPath.get(msize);
            final int index = leaf.getIndex();
            final String name = leaf.getName();
            final Object retval;
            if (index >= 0) {
                retval = jCurrent.getJSONArray(name).get(index);
            } else {
                retval = jCurrent.get(name);
            }
            if (retval instanceof JSONValue) {
                // Not a leaf
                return null;
            }
            return retval;
        } catch (final JSONException e) {
            return null;
        }
    }

    private static Object asJSObject(final String propertyValue) {
        if (null == propertyValue) {
            return null;
        }
        try {
            return new JSONTokener(propertyValue).nextValue();
        } catch (final Exception e) {
            return propertyValue;
        }
    }

    /**
     * <ul>
     * <li><b><code>preferencePath</code></b>:<br>&nbsp;&nbsp;The preference path</li>
     * <li><b><code>propertyName</code></b>:<br>&nbsp;&nbsp;The property name of the preference item property.</li>
     * <li><b><code>property</code></b>:<br>&nbsp;&nbsp;The property representing a preference item.</li>
     * <li><b><code>path</code></b>:<br>&nbsp;&nbsp;The parsed preference path.</li>
     * </ul>
     */
    private static final class AttributedProperty {

        /**
         * The property name of the preference item property.
         */
        protected final String propertyName;

        /**
         * The preference path
         */
        protected final String preferencePath;

        /**
         * The property representing a preference item.
         */
        protected final ComposedConfigProperty<String> property;

        /**
         * The parsed preference path.
         */
        protected final List<JSONPathElement> path;

        protected AttributedProperty(final String preferencePath, final String propertyName, final ComposedConfigProperty<String> property) throws OXException {
            super();
            this.propertyName = propertyName;
            this.property = property;
            this.preferencePath = preferencePath;
            path = JSONPathElement.parsePath(preferencePath);
        }
    } // End of class AttributedProperty

}

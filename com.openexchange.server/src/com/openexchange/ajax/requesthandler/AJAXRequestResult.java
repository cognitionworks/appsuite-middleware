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

package com.openexchange.ajax.requesthandler;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONValue;
import com.openexchange.exception.OXException;

/**
 * {@link AJAXRequestResult} - Simple container for a {@link JSONValue result}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class AJAXRequestResult {

    public static final long SECOND_IN_MILLIS = 1000;

    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;

    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;

    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;

    public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;

    /**
     * This constant is actually the length of 364 days, not of a year!
     */
    public static final long YEAR_IN_MILLIS = WEEK_IN_MILLIS * 52;

    /**
     * The constant representing an empty AJAX request result.
     * <p>
     * Both data and time stamp are set to <code>null</code>.
     */
    public static final AJAXRequestResult EMPTY_REQUEST_RESULT = new AJAXRequestResult();

    /**
     * The request result type.
     */
    public static enum ResultType {
        /**
         * A common request result which should be further handled.
         */
        COMMON,
        /**
         * An <i>ETag</i> request result.
         */
        ETAG;
    }

    private ResultType resultType;

    private Object resultObject;

    private Date timestamp;

    private Collection<OXException> warnings;

    private boolean deferred;

    private final Map<String, String> headers;

    private String format;

    private long expires;

    /**
     * Initializes a new {@link AJAXRequestResult} with data and time stamp set to <code>null</code>.
     *
     * @see #EMPTY_REQUEST_RESULT
     */
    public AJAXRequestResult() {
        this(null, null, null);
    }

    /**
     * Initializes a new {@link AJAXRequestResult} with time stamp set to <code>null</code>.
     *
     * @param resultObject The result object
     */
    public AJAXRequestResult(final Object resultObject) {
        this(resultObject, null, null);
    }

    /**
     * Initializes a new {@link AJAXRequestResult}.
     *
     * @param resultObject The result object
     * @param timestamp The server's last-modified time stamp (corresponding to either a GET, ALL, or LIST request)
     */
    public AJAXRequestResult(final Object resultObject, final Date timestamp) {
        this(resultObject, timestamp, null);
    }

    /**
     * Initializes a new {@link AJAXRequestResult} with time stamp set to <code>null</code>.
     *
     * @param resultObject The result object
     * @param format The format of the result object
     */
    public AJAXRequestResult(final Object resultObject, final String format) {
        this(resultObject, null, format);
    }

    /**
     * Initializes a new {@link AJAXRequestResult}.
     *
     * @param resultObject The result object
     * @param timestamp The server's last-modified time stamp (corresponding to either a GET, ALL, or LIST request)
     * @param format The format of the result object
     */
    public AJAXRequestResult(final Object resultObject, final Date timestamp, final String format) {
        super();
        headers = new LinkedHashMap<String, String>();
        this.resultObject = resultObject;
        this.timestamp = null == timestamp ? null : new Date(timestamp.getTime());
        if (format == null) {
            this.format = "json";
        } else {
            this.format = format;
        }
        resultType = ResultType.COMMON;
        expires = -1;
    }

    /**
     * Gets the result type
     *
     * @return The result type
     */
    public ResultType getType() {
        return resultType;
    }

    /**
     * Sets the result type
     *
     * @param resultType The result type to set
     */
    public void setType(final ResultType resultType) {
        this.resultType = resultType;
    }

    /**
     * Gets the expires time.
     * <p>
     * Have a notion of a time-to-live value.
     *
     * @return The expires time or <code>-1</code> for no expiry
     */
    public long getExpires() {
        return expires;
    }

    /**
     * Sets the expires time
     *
     * @param expires The expires time or <code>-1</code> for no expiry
     */
    public void setExpires(final long expires) {
        this.expires = expires;
    }

    /**
     * Gets the deferred flag
     *
     * @return The deferred flag
     */
    public boolean isDeferred() {
        return deferred;
    }

    /**
     * Sets the deferred flag
     *
     * @param deferred The deferred flag to set
     */
    public void setDeferred(final boolean deferred) {
        this.deferred = deferred;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resultObject == null) ? 0 : resultObject.hashCode());
        result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AJAXRequestResult)) {
            return false;
        }
        final AJAXRequestResult other = (AJAXRequestResult) obj;
        if (resultObject == null) {
            if (other.resultObject != null) {
                return false;
            }
        } else if (!resultObject.equals(other.resultObject)) {
            return false;
        }
        if (timestamp == null) {
            if (other.timestamp != null) {
                return false;
            }
        } else if (!timestamp.equals(other.timestamp)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the result object.
     *
     * @return The result object
     */
    public Object getResultObject() {
        return resultObject;
    }

    /**
     * Sets the resultObject
     *
     * @param resultObject The resultObject to set
     */
    public void setResultObject(final Object resultObject) {
        this.resultObject = resultObject;
    }

    /**
     * Gets the result's format.
     *
     * @return The format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets this result's format.
     *
     * @param format The format
     */
    public void setFormat(final String format) {
        this.format = format;
    }

    /**
     * Gets the time stamp.
     *
     * @return The time stamp
     */
    public Date getTimestamp() {
        return null == timestamp ? null : new Date(timestamp.getTime());
    }

    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the warnings.
     *
     * @return The warnings
     */
    public Collection<OXException> getWarnings() {
        return null == warnings ? Collections.<OXException> emptySet() : Collections.unmodifiableCollection(warnings);
    }

    /**
     * Sets the warnings.
     *
     * @param warnings The warnings to set
     * @return This request result with specified warnings added
     */
    public AJAXRequestResult addWarnings(final Collection<OXException> warnings) {
        if (null == warnings) {
            return this;
        }
        if (null == this.warnings) {
            this.warnings = new HashSet<OXException>(warnings);
        } else {
            this.warnings.addAll(warnings);
        }
        return this;
    }

    /**
     * Sets a header value
     */
    public void setHeader(final String header, final String value) {
        headers.put(header, value);
    }

    /**
     * Gets a header value
     */
    public String getHeader(final String header) {
        return headers.get(header);
    }

    /**
     * Gets the headers
     *
     * @return The headers
     */
    public Map<String, String> getHeaders() {
        return new HashMap<String, String>(headers);
    }

    @Override
    public String toString() {
        return new StringBuilder(34).append(super.toString()).append(" resultObject=").append(resultObject).append(", timestamp=").append(
            timestamp).append(" warnings=").append(null == warnings ? "<none>" : warnings.toString()).toString();
    }

    public void setResultObject(final Object object, final String format) {
        setResultObject(object);
        setFormat(format);
    }

}

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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.http.requestwatcher.osgi.services;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServletRequestRegistryEntry} keeps track of the incoming Request and its associated thread. The Date of instantiation is saved
 * to be able to calculate the age of the entry
 * 
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class RequestRegistryEntry implements Comparable<RequestRegistryEntry> {

    private final Thread thread;

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final long birthTime;

    /**
     * Initializes a new {@link RequestRegistryEntry}.
     * 
     * @param request the incoming request to register
     * @param thread the thread associated with the request
     */
    public RequestRegistryEntry(final HttpServletRequest request, final HttpServletResponse response, final Thread thread) {
        this.thread = thread;
        this.request = request;
        this.response = response;
        this.birthTime = System.currentTimeMillis();
    }

    /**
     * Get the age of this entry.
     * 
     * @return the age of the entry in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - birthTime;
    }

    /**
     * Get the request url.
     * 
     * @return the request url as String
     */
    public String getRequestUrl() {
        return request.getRequestURL().toString();
    }

    /**
     * Get the request parameters in the form of name=value&name=value.
     * 
     * @return the request parameters as String in the form of name=value&name=value
     */
    public String getRequestParameters() {
        final com.openexchange.java.StringAllocator sa = new com.openexchange.java.StringAllocator();
        @SuppressWarnings("unchecked")
        final Map<String, String[]> parameterMap = request.getParameterMap();
        final String[] parameterNames = parameterMap.keySet().toArray(new String[0]);
        for (int i = 0; i < parameterNames.length; i++) {
            final String[] parameterValues = parameterMap.get(parameterNames[i]);
            for (int j = 0; j < parameterValues.length; j++) {
                sa.append(parameterNames[i]).append("=").append(parameterValues[j]);
                if (j != parameterValues.length - 1) {
                    sa.append("&");
                }
            }
            if (i != parameterNames.length - 1) {
                sa.append("&");
            }
        }
        return sa.toString();
    }

    /**
     * Get the StackTrace of the Thread processing this Request.
     * 
     * @see java.lang.Thread#getStackTrace()
     */
    public StackTraceElement[] getStackTrace() {
        return thread.getStackTrace();
    }

    /**
     * Return thread infos in the form of "id=threadId, name=threadName"
     * 
     * @return thread infos in the form of "id=threadId, name=threadName"
     */
    public String getThreadInfo() {
        return new com.openexchange.java.StringAllocator("id=").append(thread.getId()).append(", name=").append(thread.getName()).toString();
    }

    /**
     * Gets the thread
     *
     * @return The thread
     */
    public Thread getThread() {
        return thread;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (birthTime ^ (birthTime >>> 32));
        result = prime * result + ((request == null) ? 0 : request.hashCode());
        result = prime * result + ((response == null) ? 0 : response.hashCode());
        result = prime * result + ((thread == null) ? 0 : thread.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RequestRegistryEntry)) {
            return false;
        }
        final RequestRegistryEntry other = (RequestRegistryEntry) obj;
        if (birthTime != other.birthTime) {
            return false;
        }
        if (request == null) {
            if (other.request != null) {
                return false;
            }
        } else if (!request.equals(other.request)) {
            return false;
        }
        if (response == null) {
            if (other.response != null) {
                return false;
            }
        } else if (!response.equals(other.response)) {
            return false;
        }
        if (thread == null) {
            if (other.thread != null) {
                return false;
            }
        } else if (!thread.equals(other.thread)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(final RequestRegistryEntry otherEntry) {
        if (this.birthTime < otherEntry.birthTime) {// this one is older
            return 1;
        } else if (birthTime > otherEntry.birthTime) {// this one is younger
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * Interrupt processing of this thread and send an error to the client if the response wasn't already committed.
     * 
     * @throws IOException
     */
    public void stopProcessing() throws IOException {
        /*
         * We have to make the backend completely interrupt aware before we can enable this
         * thread.interrupt(); 
         */
        if (!response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}

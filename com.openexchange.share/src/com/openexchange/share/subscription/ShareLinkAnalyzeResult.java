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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.share.subscription;

import com.openexchange.exception.OXException;

/**
 * {@link ShareLinkAnalyzeResult}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.5
 */
public final class ShareLinkAnalyzeResult {

    private final ShareLinkState state;
    private final OXException error;
    private final ShareSubscriptionInformation infos;

    /**
     * Initializes a new {@link ShareLinkAnalyzeResult}.
     * 
     * @param state The state of the result
     * @param infos Detailed information about the share
     */
    public ShareLinkAnalyzeResult(ShareLinkState state, ShareSubscriptionInformation infos) {
        this(state, null, infos);
    }

    /**
     * Initializes a new {@link ShareLinkAnalyzeResult}.
     * 
     * @param state The state of the result
     * @param error The details about the state as {@link OXException}
     * @param infos Detailed information about the share
     */
    public ShareLinkAnalyzeResult(ShareLinkState state, OXException error, ShareSubscriptionInformation infos) {
        super();
        this.state = state;
        this.error = error;
        this.infos = infos;
    }

    ShareLinkAnalyzeResult(Builder builder) {
        this.state = builder.state;
        this.error = builder.error;
        this.infos = builder.infos;
    }

    /**
     * Gets the state
     *
     * @return The state
     */
    public ShareLinkState getState() {
        return state;
    }

    /**
     * Further details of the state
     *
     * @return Details as {@link OXException}, might be <code>null</code>
     */
    public OXException getDetails() {
        return error;
    }

    /**
     * Gets the infos
     *
     * @return The infos
     */
    public ShareSubscriptionInformation getInfos() {
        return infos;
    }

    @Override
    public String toString() {
        return "ShareLinkAnalyzeResult [state=" + state + ", error=" + (null == error ? "null" : error.getMessage()) + ", infos=" + (null != infos ? infos.toString() : "null") + "]";
    }

    /**
     * {@link Builder}
     *
     * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
     * @since v7.10.5
     */
    public static class Builder {

        ShareLinkState state;
        OXException error;
        ShareSubscriptionInformation infos;

        /**
         * Initializes a new {@link Builder}.
         */
        public Builder() {}

        /**
         * Initializes a new {@link Builder}.
         * 
         * @param state The state of the result
         * @param error The detailed error about the state as {@link OXException}
         * @param infos Detailed information about the share
         */
        public Builder(ShareLinkState state, OXException error, ShareSubscriptionInformation infos) {
            this.state = state;
            this.error = error;
            this.infos = infos;
        }

        /**
         * Add the state
         *
         * @param state The state
         * @return This instance for chaining
         */
        public Builder state(ShareLinkState state) {
            this.state = state;
            return Builder.this;
        }

        /**
         * Add a detailed error message to the state
         *
         * @param error The details
         * @return This instance for chaining
         */
        public Builder error(OXException error) {
            this.error = error;
            return Builder.this;
        }

        /**
         * Add the infos
         *
         * @param infos The infos
         * @return This instance for chaining
         */
        public Builder infos(ShareSubscriptionInformation infos) {
            this.infos = infos;
            return Builder.this;
        }

        /**
         * Builds the result
         *
         * @return The analyze result
         */
        public ShareLinkAnalyzeResult build() {

            return new ShareLinkAnalyzeResult(this);
        }

    }

}

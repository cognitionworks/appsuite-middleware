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

package com.openexchange.i18n.tools.replacement;

import java.text.DateFormat;
import java.util.Locale;
import java.util.TimeZone;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.i18n.tools.TemplateReplacement;
import com.openexchange.i18n.tools.TemplateToken;

/**
 * {@link LocalizedStringReplacement}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public abstract class LocalizedStringReplacement implements TemplateReplacement {

    private StringHelper stringHelper;

    private final TemplateToken token;

    private String replacement;

    protected Locale locale;

    protected boolean changed;

    /**
     * Initializes a new {@link LocalizedStringReplacement}
     */
    public LocalizedStringReplacement(final TemplateToken token, final String replacement) {
        super();
        this.token = token;
        this.replacement = replacement;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        final LocalizedStringReplacement clone = (LocalizedStringReplacement) super.clone();
        clone.stringHelper = null;
        clone.locale = (Locale) (this.locale == null ? null : this.locale.clone());
        return clone;
    }

    @Override
    public TemplateReplacement getClone() throws CloneNotSupportedException {
        return (TemplateReplacement) clone();
    }

    protected final StringHelper getStringHelper() {
        if (stringHelper == null) {
            if (locale == null) {
                stringHelper = StringHelper.valueOf(Locale.ENGLISH);
            } else {
                stringHelper = StringHelper.valueOf(locale);
            }
        }
        return stringHelper;
    }

    @Override
    public boolean changed() {
        return changed;
    }

    @Override
    public boolean relevantChange() {
        return changed();
    }

    @Override
    public String getReplacement() {
        return getStringHelper().getString(replacement);
    }

    @Override
    public TemplateToken getToken() {
        return token;
    }

    @Override
    public TemplateReplacement setChanged(final boolean changed) {
        this.changed = changed;
        return this;
    }

    @Override
    public final TemplateReplacement setLocale(final Locale locale) {
        if (locale == null || locale.equals(this.locale)) {
            return this;
        }
        this.locale = locale;
        stringHelper = null;
        return this;
    }

    @Override
    public TemplateReplacement setTimeZone(final TimeZone timeZone) {
        // Not applicable
        return this;
    }

    @Override
    public boolean merge(final TemplateReplacement other) {
        if (!LocalizedStringReplacement.class.isInstance(other)) {
            /*
             * Class mismatch or null
             */
            return false;
        }
        if (!getToken().equals(other.getToken())) {
            /*
             * Token mismatch
             */
            return false;
        }
        if (!other.changed()) {
            /*
             * Other replacement does not reflect a changed value; leave
             * unchanged
             */
            return false;
        }
        final LocalizedStringReplacement o = (LocalizedStringReplacement) other;
        this.replacement = o.replacement;
        this.changed = true;
        return true;
    }

    @Override
    public TemplateReplacement setDateFormat(DateFormat dateFormat) {
        return this;
    }
}

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

package com.openexchange.i18n.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.i18n.parsing.I18NException;
import com.openexchange.i18n.parsing.POParser;
import com.openexchange.i18n.parsing.Translations;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public class POTranslationsDiscoverer extends FileDiscoverer {

    private static final Log LOG = LogFactory.getLog(POTranslationsDiscoverer.class);

    public POTranslationsDiscoverer(final File dir) throws FileNotFoundException {
        super(dir);
    }

    public List<Translations> getTranslations() {
        final String[] files = getFilesFromLanguageFolder(".po");
        if (files.length == 0) {
            return Collections.emptyList();
        }
        final List<Translations> list = new ArrayList<Translations>(files.length);
        for (final String file : files) {
            Locale l = null;
            InputStream input = null;

            try {
                l = getLocale(file);
                final File poFile = new File(getDirectory(), file);
                input = new BufferedInputStream(new FileInputStream(poFile));
                // POParser remembers headers of PO file. Therefore a new one is needed for every file.
                final Translations translations = new POParser().parse(input, poFile.getAbsolutePath());
                translations.setLocale(l);
                list.add(translations);
            } catch (final FileNotFoundException e) {
                LOG.error("File disappeared?", e);
            } catch (final I18NException e) {
                LOG.error("Could not parse po file: ", e);
            } finally {
                if (null != input) {
                    try {
                        input.close();
                    } catch (final IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        }
        return list;
    }
}

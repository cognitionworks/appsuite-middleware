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

package com.openexchange.ajax;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import com.openexchange.configuration.SystemConfig;
import com.openexchange.groupware.importexport.ImporterExporter;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.xml.spring.SpringParser;

/**
 * Abtract class for both importers and exporters that does 
 * the configuration via Spring. This means importers and
 * exporters are loaded from a configuration file and you
 * do not need to hardcode them somewhere. 
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias 'Tierlieb' Prinz</a> (spring configuration)
 */

public abstract class ImportExport extends SessionServlet {

	private static final long serialVersionUID = -7502282736897750395L;

	public static final String AJAX_TYPE = "type";
	
	private static final Log LOG = LogFactory.getLog(ImportExport.class);
	
	protected ImporterExporter importerExporter = null;

    private static BeanFactory beanFactory = null;

    @Override
    public void init(){
        if (importerExporter != null) {
            return;
        }
        if(beanFactory  == null) {
            final String beanPath = SystemConfig.getProperty("IMPORTEREXPORTER");
		    if (beanPath != null) {
                final SpringParser springParser = ServerServiceRegistry.getInstance().getService(SpringParser.class);
                beanFactory = springParser.parseFile(beanPath, ImportExport.class.getClassLoader());
            } else {
	     		LOG.error("missing property: IMPORTEREXPORTER");
		    }
        }
        if(beanFactory != null) {
            importerExporter = (ImporterExporter) beanFactory.getBean("importerExporter");
        }
    }
}

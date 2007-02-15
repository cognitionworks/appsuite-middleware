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

package com.openexchange.groupware.importexport;

import java.io.InputStream;
import java.util.List;

import com.openexchange.groupware.importexport.exceptions.ImportExportException;
import com.openexchange.sessiond.SessionObject;
import java.util.Map;

/**
 * This interface defines an importer, meaning a class able to
 * import one or more data formats into the OX.
 *  
 * @author Tobias Prinz, mailto:tobias.prinz@open-xchange.com
 *
 */
public interface Importer {

	/**
	 *
	 * @param sessObj: Session object enabling us to check write access.
	 * @param format: Format of the data that is meant to be imported
	 * @param foldermapping: Those folders (plus their type stored as int as defined in the class Types) the data is meant to be imported int
	 * @param optionalParams: Params that might be needed by a specific implementor of this interface. Note: The format was chosen to be congruent with HTTP-GET
	 * @return true, if this importer can import this format for this module; false otherwise
	 * @see com.openexchange.groupware.Types
	 */
	public abstract boolean canImport(SessionObject sessObj, Format format, Map<String, Integer> folderMappings, Map<String, String[]> optionalParams) throws ImportExportException;

	/**
	 * 
	 * @param sessObj: session object enabling us to check access rights (write rights needed)
	 * @param format: Format of the data to be imported
	 * @param is: InputStream containing data to be imported
	 * @param folders: Identifiers for folders (plus their type as int) - usually only one, but iCal may need two and future extensions might need even more (remember: Folders can have only one type, so type is not a necessary argument)
	 * @param optionalParams: Params that might be needed by a specific implementor of this interface. Note: The format was chosen to be congruent with HTTP-GET
	 * @return
	 * @throws ImportExportException
	 * @see com.openexchange.groupware.Types
	 */
	public abstract List<ImportResult> importData(
			SessionObject sessObj,
			Format format,
			InputStream is,
			Map<String, Integer> folderMappings, 
			Map<String, String[]> optionalParams ) throws ImportExportException;

}
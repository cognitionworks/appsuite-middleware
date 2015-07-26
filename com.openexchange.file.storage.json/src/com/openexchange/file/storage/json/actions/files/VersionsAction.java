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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.file.storage.json.actions.files;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageFileAccess.SortDirection;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.tools.iterator.FilteringSearchIterator;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link VersionsAction}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
@Action(method = RequestMethod.GET, name = "versions", description = "Get all versions", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "id", description = "Object ID of the infoitem whose versions are requested."),
    @Parameter(name = "columns", description = "A comma-separated list of columns to return. Each column is specified by a numeric column identifier. Column identifiers for infoitems are defined in Common object data and Detailed infoitem data."),
    @Parameter(name = "sort", optional=true, description = "The identifier of a column which determines the sort order of the response. If this parameter is specified, then the parameter order must be also specified."),
    @Parameter(name = "order", optional=true, description = "\"asc\" if the response entires should be sorted in the ascending order, \"desc\" if the response entries should be sorted in the descending order. If this parameter is specified, then the parameter sort must be also specified.")
}, responseDescription = "Response with timestamp: An array with infoitem data. Each array element describes one infoitem and is itself an array. The elements of each array contain the information specified by the corresponding identifiers in the columns parameter. The timestamp is the timestamp relating to the requested infostore item.")
public class VersionsAction extends AbstractListingAction {

    @Override
    public AJAXRequestResult handle(InfostoreRequest request) throws OXException {
        request.require(Param.ID);

        IDBasedFileAccess fileAccess = request.getFileAccess();

        List<Field> columns = new ArrayList<File.Field>(request.getFieldsToLoad());
        if(!columns.contains(File.Field.VERSION)) {
            columns.add(File.Field.VERSION);
        }
        final Field sortingField = request.getSortingField();
        final SortDirection sortingOrder = request.getSortingOrder();
        TimedResult<File> versions = fileAccess.getVersions(request.getId(), columns, sortingField, sortingOrder);

        if (Field.CREATED_BY.equals(sortingField)) {
            final ServerSession serverSession = request.getSession();
            final CreatedByComparator comparator = new CreatedByComparator(serverSession.getUser().getLocale(), serverSession.getContext()).setDescending(SortDirection.DESC.equals(sortingOrder));
            final SearchIterator<File> iter = CreatedByComparator.resort(versions.results(), comparator);
            final TimedResult<File> delegate = versions;
            versions = new TimedResult<File>() {

                @Override
                public long sequenceNumber() throws OXException {
                    return delegate.sequenceNumber();
                }

                @Override
                public SearchIterator<File> results() throws OXException {
                    return iter;
                }
            };
        }

        return result( skipVersion0( versions ), request);
    }

    private TimedResult<File> skipVersion0(final TimedResult<File> versions) throws OXException {

        return new TimedResult<File>() {

            @Override
            public SearchIterator<File> results() throws OXException {
                return new FilteringSearchIterator<File>(versions.results()) {

                    @Override
                    public boolean accept(final File thing) throws OXException {
                        final String version = thing.getVersion();
                        return version != null && !version.equals("0");
                    }


                };
            }

            @Override
            public long sequenceNumber() throws OXException {
                return versions.sequenceNumber();
            }

        };
    }

}

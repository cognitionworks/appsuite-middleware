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

package com.openexchange.gdpr.dataexport.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.exception.OXException;
import com.openexchange.gdpr.dataexport.DataExportDownload;

/**
 * {@link DataExportFileHolder} - A file holder backed by a data export download.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class DataExportFileHolder implements IFileHolder {

    private final DataExportDownload dataExportDownload;
    private String delivery;
    private final List<Runnable> tasks;

    /**
     * Initializes a new {@link DataExportFileHolder}.
     *
     * @param dataExportDownload The data export download
     */
    public DataExportFileHolder(DataExportDownload dataExportDownload) {
        super();
        this.dataExportDownload = dataExportDownload;
        tasks = new LinkedList<Runnable>();
    }

    /**
     * Sets the delivery
     *
     * @param delivery The delivery to set
     */
    public void setDelivery(final String delivery) {
        this.delivery = delivery;
    }

    @Override
    public boolean repetitive() {
        return true;
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }

    @Override
    public InputStream getStream() throws OXException {
        return dataExportDownload.getInputStream();
    }

    @Override
    public RandomAccess getRandomAccess() throws OXException {
        return null;
    }

    @Override
    public long getLength() {
        return -1;
    }

    @Override
    public String getContentType() {
        return dataExportDownload.getContentType();
    }

    @Override
    public String getName() {
        return dataExportDownload.getFileName();
    }

    @Override
    public String getDisposition() {
        return "attachment";
    }

    @Override
    public String getDelivery() {
        return delivery;
    }

    @Override
    public List<Runnable> getPostProcessingTasks() {
        return tasks;
    }

    @Override
    public void addPostProcessingTask(Runnable task) {
        if (null != task) {
            tasks.add(task);
        }
    }

}
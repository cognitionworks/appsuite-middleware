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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.report.appsuite.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;
import com.openexchange.hazelcast.serialization.AbstractCustomPortable;
import com.openexchange.hazelcast.serialization.CustomPortable;

/**
 * {@link PortableReport} that is distributed within a hazelcast cluster.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.6.1
 */
public class PortableReport extends AbstractCustomPortable {

    public static final String PARAMETER_UUID = "uuid";

    public static final String PARAMETER_TYPE = "type";

    public static final String PARAMETER_STARTTIME = "startTime";

    public static final String PARAMETER_STOPTIME = "stopTime";

    public static final String PARAMETER_NUMBER_OF_TASKS = "numberOfTasks";

    public static final String PARAMETER_PENDING_TASKS = "pendingTasks";

    public static final String PARAMETER_NAMESPACES = "namespaces";

    private String uuid;

    private String type;

    private Map<String, Map<String, Object>> namespaces = new HashMap<String, Map<String, Object>>();

    private long startTime;

    private long stopTime;

    private int numberOfTasks;

    private int pendingTasks;

    public PortableReport() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getClassId() {
        return CustomPortable.PORTABLEREPORT_CLASS_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        writer.writeUTF(PARAMETER_UUID, uuid);
        writer.writeUTF(PARAMETER_TYPE, type);
        writer.writeLong(PARAMETER_STARTTIME, startTime);
        writer.writeLong(PARAMETER_STOPTIME, stopTime);
        writer.writeInt(PARAMETER_NUMBER_OF_TASKS, numberOfTasks);
        writer.writeInt(PARAMETER_PENDING_TASKS, pendingTasks);
        writer.getRawDataOutput().writeObject(namespaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readPortable(PortableReader reader) throws IOException {
        uuid = reader.readUTF(PARAMETER_UUID);
        type = reader.readUTF(PARAMETER_TYPE);
        startTime = reader.readLong(PARAMETER_STARTTIME);
        stopTime = reader.readLong(PARAMETER_STOPTIME);
        numberOfTasks = reader.readInt(PARAMETER_NUMBER_OF_TASKS);
        pendingTasks = reader.readInt(PARAMETER_PENDING_TASKS);
        namespaces = reader.getRawDataInput().readObject();
    }

    /**
     * Wraps the given {@link Report} into a {@link PortableReport}.
     *
     * @param {@link Report} that should be wrapped
     * @return {@link PortableReport} that was wrapped
     * @see PortableReport.unwrap(PortableReport)
     */
    public static PortableReport wrap(Report report) {
        if (null == report) {
            return null;
        }
        PortableReport portableReport = new PortableReport();
        portableReport.uuid = report.getUUID();
        portableReport.type = report.getType();
        portableReport.startTime = report.getStartTime();
        portableReport.stopTime = report.getStopTime();
        portableReport.numberOfTasks = report.getNumberOfTasks();
        portableReport.pendingTasks = report.getNumberOfPendingTasks();
        portableReport.namespaces = report.getData();
        return portableReport;
    }

    /**
     * Unwraps the given {@link PortableReport} into a {@link Report}.
     *
     * @param {@link PortableReport} that should be unwrapped
     * @return {@link Report} that was unwrapped
     * @see PortableReport.wrap(Report)
     */
    public static Report unwrap(PortableReport portableReport) {
        if (null == portableReport) {
            return null;
        }
        Report report = new Report(portableReport.uuid, portableReport.type, portableReport.startTime);
        report.setStopTime(portableReport.stopTime);
        report.setTaskState(portableReport.numberOfTasks, portableReport.pendingTasks);
        report.getData().putAll(portableReport.namespaces);
        return report;
    }

    /**
     * Unwraps the given Collection {@link PortableReport} into a {@link Report}.
     *
     * @param {@link PortableReport} that should be unwrapped
     * @return {@link Report[]} that was unwrapped
     * @see PortableReport.wrap(Report)
     */
    public static Report[] unwrap(Collection<PortableReport> portableReports) {
        List<Report> reports = new ArrayList<Report>();

        for (PortableReport portableReport : portableReports) {
            reports.add(PortableReport.unwrap(portableReport));
        }
        return null != reports ? reports.toArray(new Report[reports.size()]) : new Report[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + uuid.hashCode();
        result = prime * result + type.hashCode();
        result = prime * result + (int) startTime;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PortableReport)) {
            return false;
        }

        PortableReport other = (PortableReport) obj;
        if (uuid != other.uuid) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        if (startTime != other.startTime) {
            return false;
        }
        if (numberOfTasks != other.numberOfTasks) {
            return false;
        }
        if (pendingTasks != other.pendingTasks) {
            return false;
        }
        if (stopTime != other.stopTime) {
            return false;
        }
        return true;
    }
}

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

package com.openexchange.report.appsuite.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;
import com.openexchange.report.appsuite.ContextReport;
import com.openexchange.report.appsuite.ContextReportCumulator;
import com.openexchange.report.appsuite.ReportFinishingTouches;
import com.openexchange.report.appsuite.ReportService;
import com.openexchange.report.appsuite.ReportSystemHandler;
import com.openexchange.report.appsuite.Services;
import com.openexchange.report.appsuite.serialization.PortableReport;
import com.openexchange.report.appsuite.serialization.Report;


/**
 * The {@link Orchestration} class uses hazelcast to coordinate the clusters efforts in producing reports. It maintains the following resources via hazelcast:
 *
 * A Map "com.openexchange.report.Reports" that contains an entry per reportType of the last successful report run for that type
 * A Map "com.openexchange.report.PendingReports.[reportType] per reportType that contains currently running reports.
 *
 *
 * A Lock com.openexchange.report.Reports.[reportType] that acts as a cluster-wide lock for the given resourceType to coordinate
 * when to set up a  new report
 * A Lock com.openexchange.report.Reports.Merge.[reportType] that protects the merge operations for the global report
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class Orchestration implements ReportService {

    private static final String REPORT_TYPE_DEFAULT = "default";

    private static final String REPORTS_MERGE_PRE_KEY = "com.openexchange.report.Reports.Merge.";

    private static final String PENDING_REPORTS_PRE_KEY = "com.openexchange.report.PendingReports.";

    private static final String REPORTS_KEY = "com.openexchange.report.Reports";

    private static final AtomicReference<Orchestration> INSTANCE = new AtomicReference<Orchestration>(new Orchestration());

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Orchestration.class);

    public static Orchestration getInstance() {
        return INSTANCE.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Report getLastReport() {
        return getLastReport(REPORT_TYPE_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Report getLastReport(String reportType) {
        IMap<String, PortableReport> map = Services.getService(HazelcastInstance.class).getMap(REPORTS_KEY);
        PortableReport portableReport = map.get(reportType);
        return PortableReport.unwrap(portableReport);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String run() throws OXException {
        return run(REPORT_TYPE_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String run(String reportType) throws OXException {
        // Start a new report run or retrieve the UUID of an already running report

        HazelcastInstance hazelcast = Services.getService(HazelcastInstance.class);

        String uuid;
        IMap<String, PortableReport> pendingReports;
        int numberOfTasks;
        List<Integer> allContextIds;
        // Firstly retrieve the global lock per this report type to make sure, we are the only one coordinating a report run of this type for now.
        ILock lock = hazelcast.getLock("com.openexchange.report.Reports." + reportType);
        lock.lock();
        try {
            // Is a report pending?
            pendingReports = hazelcast.getMap(PENDING_REPORTS_PRE_KEY + reportType);

            if (!pendingReports.isEmpty()) {
                // Yes, there is a report running, so retrieve its UUID and return it.
                return pendingReports.keySet().iterator().next();
            }

            // No, we have to set up a  new report
            uuid = UUIDs.getUnformattedString(UUID.randomUUID());

            hazelcast.getLock(REPORTS_MERGE_PRE_KEY + reportType);

            // Load all contextIds
            allContextIds = Services.getService(ContextService.class).getAllContextIds();

            // Chop them up into blocks of 200 each + a remainder
            numberOfTasks = allContextIds.size() / 200;

            int rest = allContextIds.size() % 200;
            if (rest > 0) {
                numberOfTasks++;
            }

            // Set up the report instance
            Report report = new Report(uuid, reportType, System.currentTimeMillis());
            report.setNumberOfTasks(allContextIds.size());
            // Put it into hazelcast for others to discover
            pendingReports.put(uuid, PortableReport.wrap(report));

        } finally {
            if (lock != null) {
                lock.forceUnlock();
            }
        }

        // Set up an AnalyzeContextBatch instance for every chunk of contextIds
        ExecutorService executorService = hazelcast.getExecutorService(REPORT_TYPE_DEFAULT);

        for (int i = 0; i < numberOfTasks; i++) {
            int startIndex = i * 200;
            int endIndex = (i + 1) * 200;
            if (endIndex >= allContextIds.size()) {
                endIndex = allContextIds.size();
            }

            List<Integer> chunk = new ArrayList<Integer>(allContextIds.subList(startIndex, endIndex)); // Create new ArrayList, so it is serializable
            executorService.submit(new AnalyzeContextBatch(uuid, reportType, chunk));
        }


        return uuid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Report[] getPendingReports(String reportType) {
        // Simply look up the pending reports for this type in a hazelcast map
        HazelcastInstance hazelcast = Services.getService(HazelcastInstance.class);

        IMap<String, PortableReport> pendingReports = hazelcast.getMap(PENDING_REPORTS_PRE_KEY + reportType);
        Collection<PortableReport> reportCol = pendingReports.values();

        return PortableReport.unwrap(reportCol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Report[] getPendingReports() {
        return getPendingReports(REPORT_TYPE_DEFAULT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushPending(String uuid, String reportType) {
        // Remove entries from hazelcasts pending maps, so a report can be started again
        HazelcastInstance hazelcast = Services.getService(HazelcastInstance.class);

        IMap<String, PortableReport> pendingReports = hazelcast.getMap(PENDING_REPORTS_PRE_KEY + reportType);

        ILock lock = hazelcast.getLock(REPORTS_MERGE_PRE_KEY + reportType);

        pendingReports.remove(uuid);
        lock.destroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushPending(String uuid) {
        flushPending(uuid, REPORT_TYPE_DEFAULT);
    }

    // Called by the AnalyzeContextBatch for every context so the context specific entries can be
    // added to the global report
    public void done(ContextReport contextReport) {

        String reportType = contextReport.getType();

        HazelcastInstance hazelcast = Services.getService(HazelcastInstance.class);

        // Retrieve general report and merge in the contextReport
        IMap<String, PortableReport> pendingReports = hazelcast.getMap(PENDING_REPORTS_PRE_KEY + reportType);

        // Reports are not threadsafe, plus we have to prevent other nodes from modifying the report
        // Until we've merged in the results of this context analysis
        ILock lock = hazelcast.getLock(REPORTS_MERGE_PRE_KEY + reportType);
        Report report;
        try {
            if(!lock.tryLock(60, TimeUnit.MINUTES)) {
                // Abort report
                flushPending(contextReport.getUUID(), reportType);
                LOG.error("Could not acquire merge lock! Aborting {} for type: {}", contextReport.getUUID(), reportType);
            }
        } catch (InterruptedException e) {
            return;
        }
        try {
            report = PortableReport.unwrap(pendingReports.get(contextReport.getUUID()));
            if (report == null) {
                // Somebody cancelled the report, so just discard the result
                lock.unlock();
                lock.destroy();
                lock = null;
                return;
            }
            // Run all applicable cumulators to add the context report results to the global report
            for(ContextReportCumulator cumulator: Services.getContextReportCumulators()) {
                if (cumulator.appliesTo(reportType)) {
                    cumulator.merge(contextReport, report);
                }
            }
            // Mark context as done, thereby decreasing the number of pending tasks
            report.markTaskAsDone();

            // Save it back to hazelcast
            pendingReports.put(contextReport.getUUID(), PortableReport.wrap(report));
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }

        if (report.getNumberOfPendingTasks() == 0) {
            finishUpReport(reportType, hazelcast, pendingReports, lock, report);
        }
    }

    private void finishUpReport(String reportType, HazelcastInstance hazelcast, IMap<String, PortableReport> pendingReports, ILock lock, Report report) {
        // Looks like this was the last context result we were waiting for
        // So finish up the report
        // First run the global system handlers
        for(ReportSystemHandler handler: Services.getSystemHandlers()) {
            if (handler.appliesTo(reportType)) {
                handler.runSystemReport(report);
            }
        }

        // And perform the finishing touches
        for(ReportFinishingTouches handler: Services.getFinishingTouches()) {
            if (handler.appliesTo(reportType)) {
                handler.finish(report);
            }
        }

        // We are done. Dump Report
        report.setStopTime(System.currentTimeMillis());
        hazelcast.getMap(REPORTS_KEY).put(report.getType(), PortableReport.wrap(report));

        // Clean up resources
        pendingReports.remove(report.getUUID());
        lock.destroy();
    }

    public void abort(String uuid, String reportType, int ctxId) {
        // This contextReport failed, so at least decrese the number of pending tasks
        HazelcastInstance hazelcast = Services.getService(HazelcastInstance.class);

        IMap<String, PortableReport> pendingReports = hazelcast.getMap(PENDING_REPORTS_PRE_KEY + reportType);

        ILock lock = hazelcast.getLock(REPORTS_MERGE_PRE_KEY + reportType);
        Report report;
        try {
            if(!lock.tryLock(10, TimeUnit.MINUTES)) {
                // Abort report
                lock = null; // Don't care about locking then
            }
        } catch (InterruptedException e) {
            return;
        }
        try {
            report = PortableReport.unwrap(pendingReports.get(uuid));
            if (report == null) {
                lock.unlock();
                lock.destroy();
                lock = null;
                return;
            }
            // Mark context as done
            report.markTaskAsDone();
            // Save it back to hazelcast
            pendingReports.put(report.getUUID(), PortableReport.wrap(report));
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }

        if (report.getNumberOfPendingTasks() == 0) {
            finishUpReport(reportType, hazelcast, pendingReports, lock, report);
        }
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.openexchange.soap.cxf;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import com.openexchange.java.Strings;
import com.openexchange.java.util.Pair;


/**
 * Utilities for handling <tt>Throwable</tt>s and <tt>Exception</tt>s.
 */
public class ExceptionUtils {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ExceptionUtils.class);

    /**
     * Checks whether the supplied <tt>Throwable</tt> is one that needs to be rethrown and swallows all others.
     *
     * @param t The <tt>Throwable</tt> to check
     */
    public static void handleThrowable(final Throwable t) {
        if (t instanceof ThreadDeath) {
            String marker = " ---=== /!\\ ===--- ";
            LOG.error("{}Thread death{}", marker, marker, t);
            throw (ThreadDeath) t;
        }
        if (t instanceof OutOfMemoryError) {
            OutOfMemoryError oom = (OutOfMemoryError) t;
            String message = oom.getMessage();
            if ("unable to create new native thread".equalsIgnoreCase(message)) {
                if (!Boolean.TRUE.equals(System.getProperties().get("__thread_dump_created"))) {
                    System.getProperties().put("__thread_dump_created", Boolean.TRUE);
                    boolean error = true;
                    try {
                        // Dump all the threads to the log
                        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
                        String ls = Strings.getLineSeparator();
                        LOG.info("{}Threads: {}", ls, threads.size());
                        StringBuilder sb = new StringBuilder(2048);
                        for (Map.Entry<Thread, StackTraceElement[]> mapEntry : threads.entrySet()) {
                            Thread thread = mapEntry.getKey();
                            sb.setLength(0);
                            sb.append("        Thread: ").append(thread).append(" java.lang.Thread.State: ").append(thread.getState().name()).append(ls);
                            for (StackTraceElement elem : mapEntry.getValue()) {
                                sb.append(elem).append(ls);
                            }
                            LOG.info(sb.toString());
                        }
                        sb = null; // Might help GC
                        LOG.info("{}    Thread dump finished{}", ls, ls);
                        error = false;
                    } finally {
                        if (error) {
                            System.getProperties().remove("__thread_dump_created");
                        }
                    }
                }
            } else if ("Java heap space".equalsIgnoreCase(message)) {
                try {
                    MBeanServer server = ManagementFactory.getPlatformMBeanServer();

                    Pair<Boolean, String> heapDumpArgs = checkHeapDumpArguments();

                    // Is HeapDumpOnOutOfMemoryError enabled?
                    if (!heapDumpArgs.getFirst().booleanValue() && !Boolean.TRUE.equals(System.getProperties().get("__heap_dump_created"))) {
                        System.getProperties().put("__heap_dump_created", Boolean.TRUE);
                        boolean error = true;
                        try {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss", Locale.US);
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            // Either "/tmp" or path configured through "-XX:HeapDumpPath" JVM argument
                            String path = null == heapDumpArgs.getSecond() ? "/tmp" : heapDumpArgs.getSecond();
                            String fn = path + "/" + dateFormat.format(new Date()) + "-heap.hprof";
                            String mbeanName = "com.sun.management:type=HotSpotDiagnostic";
                            server.invoke(new ObjectName(mbeanName), "dumpHeap", new Object[] { fn, Boolean.TRUE }, new String[] { String.class.getCanonicalName(), "boolean" });
                            LOG.info("{}    Heap snapshot dumped to file {}{}", Strings.getLineSeparator(), fn, Strings.getLineSeparator());
                            error = false;
                        } finally {
                            if (error) {
                                System.getProperties().remove("__heap_dump_created");
                            }
                        }
                    }
                } catch (Exception e) {
                    // Failed for any reason...
                }
            }
        }
        if (t instanceof VirtualMachineError) {
            String marker = " ---=== /!\\ ===--- ";
            LOG.error("{}The Java Virtual Machine is broken or has run out of resources necessary for it to continue operating.{}", marker, marker, t);
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }

    private static Pair<Boolean, String> checkHeapDumpArguments() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        boolean heapDumpOnOOm = false;
        String path = null;
        for (String argument : arguments) {
            if ("-XX:+HeapDumpOnOutOfMemoryError".equals(argument)) {
                heapDumpOnOOm = true;
            } else if (argument.startsWith("-XX:HeapDumpPath=")) {
                path = argument.substring(17).trim();
                File file = new File(path);
                if (!file.exists() || !file.canWrite()) {
                   path = null;
                }
            }
        }
        return new Pair<Boolean, String>(Boolean.valueOf(heapDumpOnOOm), path);
    }

}

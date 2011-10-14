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

package com.openexchange.ajp13.coyote;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utilities for handling <tt>Throwable</tt>s and <tt>Exception</tt>s.
 */
public class ExceptionUtils {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(ExceptionUtils.class));

    private static final String MARKER = " ---=== /!\\ ===--- ";

    /**
     * Checks whether the supplied <tt>Throwable</tt> is one that needs to be rethrown and swallows all others.
     * 
     * @param t The <tt>Throwable</tt> to check
     */
    public static void handleThrowable(final Throwable t) {
        if (t instanceof ThreadDeath) {
            LOG.fatal(MARKER + "Thread death" + MARKER, t);
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            LOG.fatal(
                MARKER + "The Java Virtual Machine is broken or has run out of resources necessary for it to continue operating." + MARKER,
                t);
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }
}

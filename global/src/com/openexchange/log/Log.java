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

package com.openexchange.log;

import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.log.Loggable.Level;

/**
 * {@link Log} - The <code>org.apache.commons.logging.Log</code> using {@link LogService}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Log implements org.apache.commons.logging.Log {

    private static final AtomicReference<LogService> LOGSERVICE_REFERENCE = new AtomicReference<LogService>();

    /**
     * Sets the log service.
     * 
     * @param logService The log service
     */
    public static void set(final LogService logService) {
        LOGSERVICE_REFERENCE.set(logService);
    }

    /**
     * Gets the appropriate {@link Log} for specified {@link org.apache.commons.logging.Log} instance.
     * 
     * @param log The {@link org.apache.commons.logging.Log} instance
     * @return The appropriate instance
     */
    public static org.apache.commons.logging.Log valueOf(final org.apache.commons.logging.Log log) {
        if (log instanceof Log) {
            return log;
        }
        return new Log(log);
    }

    private final org.apache.commons.logging.Log delegate;

    /**
     * Initializes a new {@link Log}.
     */
    private Log(final org.apache.commons.logging.Log delegate) {
        super();
        this.delegate = delegate;
    }

    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    public boolean isFatalEnabled() {
        return delegate.isFatalEnabled();
    }

    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    public void trace(final Object message) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.trace(message);
        } else {
            logService.log(logService.loggableFor(Level.TRACE, delegate, message.toString()));
        }
    }

    public void trace(final Object message, final Throwable t) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.trace(message, t);
        } else {
            logService.log(logService.loggableFor(Level.TRACE, delegate, message.toString(), t));
        }
    }

    public void debug(final Object message) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.debug(message);
        } else {
            logService.log(logService.loggableFor(Level.DEBUG, delegate, message.toString()));
        }
    }

    public void debug(final Object message, final Throwable t) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.debug(message, t);
        } else {
            logService.log(logService.loggableFor(Level.DEBUG, delegate, message.toString(), t));
        }
    }

    public void info(final Object message) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.info(message);
        } else {
            logService.log(logService.loggableFor(Level.INFO, delegate, message.toString()));
        }
    }

    public void info(final Object message, final Throwable t) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.info(message, t);
        } else {
            logService.log(logService.loggableFor(Level.INFO, delegate, message.toString(), t));
        }
    }

    public void warn(final Object message) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.warn(message);
        } else {
            logService.log(logService.loggableFor(Level.WARNING, delegate, message.toString()));
        }
    }

    public void warn(final Object message, final Throwable t) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.warn(message, t);
        } else {
            logService.log(logService.loggableFor(Level.WARNING, delegate, message.toString(), t));
        }
    }

    public void error(final Object message) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.error(message);
        } else {
            logService.log(logService.loggableFor(Level.ERROR, delegate, message.toString()));
        }
    }

    public void error(final Object message, final Throwable t) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.error(message, t);
        } else {
            logService.log(logService.loggableFor(Level.ERROR, delegate, message.toString(), t));
        }
    }

    public void fatal(final Object message) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.fatal(message);
        } else {
            logService.log(logService.loggableFor(Level.FATAL, delegate, message.toString()));
        }
    }

    public void fatal(final Object message, final Throwable t) {
        final LogService logService = LOGSERVICE_REFERENCE.get();
        if (null == logService) {
            delegate.fatal(message, t);
        } else {
            logService.log(logService.loggableFor(Level.FATAL, delegate, message.toString(), t));
        }
    }

}

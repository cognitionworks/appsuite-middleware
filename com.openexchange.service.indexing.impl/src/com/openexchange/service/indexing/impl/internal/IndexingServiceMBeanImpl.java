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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.service.indexing.impl.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import org.apache.commons.logging.Log;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;
import com.openexchange.service.indexing.IndexingServiceMBean;


/**
 * {@link IndexingServiceMBeanImpl}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class IndexingServiceMBeanImpl extends StandardMBean implements IndexingServiceMBean {
    
    private static final Log LOG = com.openexchange.log.Log.loggerFor(IndexingServiceMBeanImpl.class);
    
    private final IndexingServiceImpl indexingService;
    

    public IndexingServiceMBeanImpl(IndexingServiceImpl indexingService) throws NotCompliantMBeanException {
        super(IndexingServiceMBean.class);
        this.indexingService = indexingService;
    }

    @Override    
    public List<String> getAllLocalRunningJobs() throws MBeanException {
        ClassLoader tmp = Thread.currentThread().getContextClassLoader();        
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            List<String> names = new ArrayList<String>();
            Scheduler scheduler = indexingService.getScheduler();
            List<JobExecutionContext> currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();
            for (JobExecutionContext job : currentlyExecutingJobs) {
                JobKey key = job.getJobDetail().getKey();
                names.add(key.getName());
            }
            
            return names;
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
            throw new MBeanException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(tmp);
        }
    }

    @Override
    public List<String> getLocalRunningJobs(int contextId, int userId) throws MBeanException {
        ClassLoader tmp = Thread.currentThread().getContextClassLoader();        
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            List<String> names = new ArrayList<String>();
            Scheduler scheduler = indexingService.getScheduler();
            List<JobExecutionContext> currentlyExecutingJobs = scheduler.getCurrentlyExecutingJobs();
            for (JobExecutionContext job : currentlyExecutingJobs) {
                JobKey key = job.getJobDetail().getKey();
                if (key.getGroup().equals(indexingService.generateJobGroup(contextId, userId))) {
                    names.add(key.getName());
                }                
            }
            
            return names;
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
            throw new MBeanException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(tmp);
        }
    }

    @Override
    public List<String> getAllScheduledJobs() throws MBeanException {
        ClassLoader tmp = Thread.currentThread().getContextClassLoader();        
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            List<String> names = new ArrayList<String>();
            Scheduler scheduler = indexingService.getScheduler();
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupStartsWith("indexingJobs/"));
            for (JobKey key : jobKeys) {
                names.add(key.getName());
            }
            
            return names;
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
            throw new MBeanException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(tmp);
        }
    }

    @Override
    public List<String> getScheduledJobs(int contextId, int userId) throws MBeanException {
        ClassLoader tmp = Thread.currentThread().getContextClassLoader();        
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            List<String> names = new ArrayList<String>();
            Scheduler scheduler = indexingService.getScheduler();        
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(indexingService.generateJobGroup(contextId, userId)));
            for (JobKey key : jobKeys) {
                names.add(key.getName());
            }
            
            return names;
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
            throw new MBeanException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(tmp);
        }
    }

}

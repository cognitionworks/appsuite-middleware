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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.subscribe.crawler.internal;

import java.io.File;
import java.io.FileNotFoundException;
import org.ho.yaml.Yaml;
import com.openexchange.subscribe.SubscriptionErrorMessage;
import com.openexchange.subscribe.SubscriptionException;
import com.openexchange.subscribe.crawler.Workflow;

/**
 * Gets a text input and creates Workflow
 * 
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class WorkflowFactory {

    public static Workflow createWorkflow(final String filename) throws SubscriptionException {

        Workflow workflow = null;
        try {
            workflow = (Workflow) Yaml.load(new File(filename));
            //checkSanity(workflow);
        } catch (final FileNotFoundException e) {
        }

        return workflow;
    }

    public static Workflow createWorkflowByString(final String string) throws SubscriptionException {

        Workflow workflow = null;

        workflow = (Workflow) Yaml.load(string);
        //checkSanity(workflow);

        return workflow;
    }

    private static void checkSanity(final Workflow workflow) throws SubscriptionException {
        Step previousStep = null;
        for (final Step currentStep : workflow.getSteps()) {
            if (previousStep != null) {                
                if (!previousStep.runEmpty()[0].equals(currentStep.runEmpty()[1])) {
                    throw SubscriptionErrorMessage.INVALID_WORKFLOW.create();
                }
            }
            previousStep = currentStep;
        }
    }

}

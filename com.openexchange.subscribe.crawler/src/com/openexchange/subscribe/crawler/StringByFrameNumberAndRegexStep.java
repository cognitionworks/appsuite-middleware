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

package com.openexchange.subscribe.crawler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.openexchange.subscribe.SubscriptionException;
import com.openexchange.subscribe.crawler.internal.AbstractStep;

/**
 * {@link StringByFrameNumberAndRegexStep}
 * 
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class StringByFrameNumberAndRegexStep extends AbstractStep<String, HtmlPage> {

    private int frameNumber;

    private String regex;

    private static final Log LOG = LogFactory.getLog(StringByFrameNumberAndRegexStep.class);

    public StringByFrameNumberAndRegexStep() {

    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.subscribe.crawler.internal.AbstractStep#execute(com.gargoylesoftware.htmlunit.WebClient)
     */
    @Override
    public void execute(final WebClient webClient) throws SubscriptionException {
        int index = 1;
        if (debuggingEnabled){
            LOG.info("Number of Frames : " + input.getFrames().size());
        }

        for (FrameWindow frame : input.getFrames()) {
            if (debuggingEnabled){
                LOG.info("Frame name : " + frame.getName() + ", number : " + index);
                HtmlPage page = (HtmlPage) frame.getEnclosedPage();
                LOG.info("Frame content : " + page.getWebResponse().getContentAsString());
            }
            
            if (index == frameNumber) {
                HtmlPage page = (HtmlPage) frame.getEnclosedPage();
                String pageString = page.getWebResponse().getContentAsString();
                LOG.debug("Frame selected : " + frame.getName() + "\n" + pageString);
                if (debuggingEnabled){
                    LOG.info("Frame selected : " + frame.getName() + "\n" + pageString);
                    openPageInBrowser(page);
                }
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(pageString);
                if (matcher.find()) {
                    output = matcher.group(1);
                    LOG.debug("String found is  : " + output);                    
                    executedSuccessfully = true;
                } else {
                    LOG.info("This pattern was not found on the page : " + regex + "\n Page: " + pageString);
                }
            }
            index++;
        }
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

}

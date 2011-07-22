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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.openexchange.subscribe.SubscriptionException;
import com.openexchange.subscribe.crawler.internal.AbstractStep;


/**
 * {@link PageByFillingOutFormStep}
 *
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 */
public class PageByFillingOutFormStep extends AbstractStep<Page, HtmlPage> {

    private String actionOfForm = "";
    
    private Map<String, String> parameters = new HashMap<String, String>();
    
    private static Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(PageByFillingOutFormStep.class));
    
    public PageByFillingOutFormStep(){
        
    }
    
    public void execute(final WebClient webClient) throws SubscriptionException {
        HtmlForm theForm = null;
        for (final HtmlForm form : input.getForms()) {
            Pattern pattern = Pattern.compile(actionOfForm);
            Matcher matcher = pattern.matcher(form.getActionAttribute());
            LOG.debug("Forms action attribute / number is : " + form.getActionAttribute() + ", should be : "+ actionOfForm);
            if (matcher.matches()) {
                theForm = form;
            }
        }
        if (theForm != null){
            try {
                if (parameters.containsKey("HtmlSubmitInput")){
                    HtmlSubmitInput submitInput = (HtmlSubmitInput) theForm.getElementById(parameters.get("HtmlSubmitInput"));
                    output = theForm.submit(submitInput);
                } else {
                    output = theForm.submit(null);
                }
                LOG.debug("Page after submitting the form : \n" + output.getWebResponse().getContentAsString());
                executedSuccessfully = true;
            } catch (IOException e) {
                LOG.error(e);
            }            
        }
    }

    
    public String getActionOfForm() {
        return actionOfForm;
    }

    
    public void setActionOfForm(String actionOfForm) {
        this.actionOfForm = actionOfForm;
    }

    
    public Map<String, String> getParameters() {
        return parameters;
    }

    
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
    
}

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

package com.openexchange.subscribe.crawler.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.TypeVariable;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.openexchange.subscribe.SubscriptionException;
import com.openexchange.subscribe.crawler.Workflow;

public abstract class AbstractStep<O,I> implements Step<O,I>{

    protected String description;

    protected Exception exception;

    protected boolean executedSuccessfully;
    
    protected Workflow workflow;
    
    protected O output;
    
    protected I input;
    
    protected boolean debuggingEnabled;

    protected AbstractStep() {
        super();
    }

    public boolean executedSuccessfully() {
        return executedSuccessfully;
    }

    public Exception getException() {
        return this.exception;
    }
    
    public void setWorkflow (final Workflow workflow){
        this.workflow = workflow;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(final String description) {
        this.description = description;
    }

    public abstract void execute(WebClient webClient) throws SubscriptionException;

    public Class inputType() {
        return input.getClass();
    }

    public Class outputType() {
        return output.getClass();
    }

    public O getOutput() {
        return output;
    }

    public void setInput(final I input) {
        this.input = input;
        
    }
    
    // Convenience Methods for Development / Debugging
    public boolean isDebuggingEnabled() {
        return debuggingEnabled;
    }

    
    public void setDebuggingEnabled(boolean debuggingEnabled) {
        this.debuggingEnabled = debuggingEnabled;
    }
    
    protected void openPageInBrowser(Page page){
        File file = new File ("./crawlerTestPage.html");
        Writer output = null;                            
        try {
          output = new BufferedWriter(new FileWriter(file));
          output.write( page.getWebResponse().getContentAsString() );
          Runtime.getRuntime().exec("open -a Safari ./crawlerTestPage.html");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
          try {
            output.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        }
    }
    
    public TypeVariable<?>[] runEmpty(){        
        return this.getClass().getTypeParameters();
    }
    // Convenience Methods for Development / Debugging    
}

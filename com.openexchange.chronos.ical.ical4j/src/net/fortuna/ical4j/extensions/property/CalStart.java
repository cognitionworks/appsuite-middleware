/**
 * Copyright (c) 2012, Ben Fortuna
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  o Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *  o Neither the name of Ben Fortuna nor the names of any other contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.fortuna.ical4j.extensions.property;

import java.text.ParseException;

import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.property.UtcProperty;

/**
 * @author fortuna
 *
 */
public class CalStart extends UtcProperty {

    private static final long serialVersionUID = -1823078836099613956L;

    public static final String PROPERTY_NAME = "X-CALSTART";
    
    public static final PropertyFactory FACTORY = new Factory();
    
    /**
     * @param factory
     */
    public CalStart(PropertyFactory factory) {
        super(PROPERTY_NAME, factory);
    }

    /**
     * @param aList
     * @param factory
     * @throws ParseException 
     */
    public CalStart(ParameterList aList, PropertyFactory factory, String value) throws ParseException {
        super(PROPERTY_NAME, aList, factory);
        setValue(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws ValidationException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCalendarProperty() {
        return true;
    }

    private static class Factory implements PropertyFactory {
        
        private static final long serialVersionUID = 596282786680252116L;

        public Property createProperty(String name) {
            return new CalStart(this);
        }
        
        public Property createProperty(String name, ParameterList parameters, String value) throws ParseException {
            CalStart property = new CalStart(parameters, this, value);
            return property;
        }
    }
}

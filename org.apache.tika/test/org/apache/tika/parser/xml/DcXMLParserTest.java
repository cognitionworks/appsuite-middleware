/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.xml;

import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

public class DcXMLParserTest extends TestCase {

    public void testXMLParserAsciiChars() throws Exception {
        InputStream input = DcXMLParserTest.class.getResourceAsStream(
                "/test-documents/testXML.xml");
        try {
            Metadata metadata = new Metadata();
            ContentHandler handler = new BodyContentHandler();
            new DcXMLParser().parse(input, handler, metadata);

            assertEquals(
                    "application/xml",
                    metadata.get(HttpHeaders.CONTENT_TYPE));
            assertEquals("Tika test document", metadata.get(DublinCore.TITLE));
            assertEquals("Rida Benjelloun", metadata.get(DublinCore.CREATOR));
            
            // The file contains 5 dc:subject tags, which come through as
            //  a multi-valued Tika Metadata entry in file order
            assertEquals(true, metadata.isMultiValued(DublinCore.SUBJECT));
            assertEquals(5,      metadata.getValues(DublinCore.SUBJECT).length);
            assertEquals("Java", metadata.getValues(DublinCore.SUBJECT)[0]);
            assertEquals("XML",  metadata.getValues(DublinCore.SUBJECT)[1]);
            assertEquals("XSLT", metadata.getValues(DublinCore.SUBJECT)[2]);
            assertEquals("JDOM", metadata.getValues(DublinCore.SUBJECT)[3]);
            assertEquals("Indexation", metadata.getValues(DublinCore.SUBJECT)[4]);

            assertEquals(
                    "Framework d\'indexation des documents XML, HTML, PDF etc..",
                    metadata.get(DublinCore.DESCRIPTION));
            assertEquals(
                    "http://www.apache.org",
                    metadata.get(DublinCore.IDENTIFIER));
            assertEquals("test", metadata.get(DublinCore.TYPE));
            assertEquals("application/msword", metadata.get(DublinCore.FORMAT));
            assertEquals("Fr", metadata.get(DublinCore.LANGUAGE));
            assertTrue(metadata.get(DublinCore.RIGHTS).contains("testing chars"));

            String content = handler.toString();
            assertTrue(content.contains("Tika test document"));
            
            assertEquals("2000-12-01T00:00:00.000Z", metadata.get(DublinCore.DATE));
        } finally {
            input.close();
        }
    }
    
    public void testXMLParserNonAsciiChars() throws Exception {
        InputStream input = DcXMLParserTest.class.getResourceAsStream("/test-documents/testXML.xml");
        try {
            Metadata metadata = new Metadata();
            new DcXMLParser().parse(input, new DefaultHandler(), metadata);
            
            final String expected = "Archim\u00E8de et Lius \u00E0 Ch\u00E2teauneuf testing chars en \u00E9t\u00E9";
            assertEquals(expected,metadata.get(DublinCore.RIGHTS));
        } finally {
            input.close();
        }
    }

}

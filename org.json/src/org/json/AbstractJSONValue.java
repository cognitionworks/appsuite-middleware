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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package org.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import org.json.helpers.StringAllocator;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;

/**
 * {@link AbstractJSONValue} - The abstract {@link JSONValue} providing some general-purpose methods.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
abstract class AbstractJSONValue implements JSONValue {

    private static final Constructor<String> STRING_CONSTRUCTOR;
    static {
        try {
            final Constructor<String> strConstructor = String.class.getDeclaredConstructor(int.class, int.class, char[].class);
            strConstructor.setAccessible(true);
            STRING_CONSTRUCTOR = strConstructor;
        } catch (final SecurityException e) {
            throw new IllegalStateException("CharArray initialization failed.", e);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("CharArray initialization failed.", e);
        }
    }

    /**
     * 2K buffer
     */
    private static final int BUF_SIZE = 0x800;

    /**
     * Initialize with 8K
     */
    private static final int SB_SIZE = 0x2000;

    /**
     * Reads the content from given reader.
     * 
     * @param reader The reader
     * @return The reader's content
     * @throws IOException If an I/O error occurs
     */
    protected static String readFrom(final Reader reader) throws IOException {
        if (null == reader) {
            return null;
        }
        final int buflen = BUF_SIZE;
        final char[] cbuf = new char[buflen];
        final org.json.helpers.StringAllocator sa = new org.json.helpers.StringAllocator(SB_SIZE);
        for (int read = reader.read(cbuf, 0, buflen); read > 0; read = reader.read(cbuf, 0, buflen)) {
            sa.append(cbuf, 0, read);
        }
        if (0 == sa.length()) {
            return null;
        }
        return sa.toString();
    }

    /**
     * Acquires next token from given {@link JsonParser} ignoring possible <code>"Unexpected character"</code> exception.
     *
     * @param jParser The JSON parser
     * @return The next token with possible <code>"Unexpected character"</code> exception(s) ignored
     * @throws IOException If an I/O error occurs
     */
    protected static JsonToken nextTokenSafe(final JsonParser jParser) throws IOException {
        JsonToken token = null;
        while (null == token) {
            try {
                token = jParser.nextToken();
            } catch (final JsonParseException e) {
                if (!e.getMessage().startsWith("Unexpected character")) {
                    throw e;
                }
                token = null;
            }
        }
        return token;
    }

    /**
     * Creates a new JSON parser.
     *
     * @param reader The reader to read from
     * @return The new parser reading from given stream
     * @throws IOException If a JSON error occurs
     * @throws JsonParseException If a parsing error occurs
     */
    protected static JsonParser createParser(final Reader reader) throws IOException, JsonParseException {
        final JsonParser jParser = JSON_FACTORY.createParser(reader);
        jParser.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        jParser.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        jParser.enable(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);
        jParser.enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS);
        jParser.enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
        return jParser;
    }

    /**
     * Creates a new JSON generator.
     * 
     * @param writer The writer to write to
     * @return The created generator
     * @throws IOException If an I/O error occurs
     */
    protected static JsonGenerator createGenerator(final Writer writer, final boolean asciiOnly) throws IOException {
        final JsonGenerator jGenerator = JSON_FACTORY.createGenerator(writer);
        jGenerator.disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);
        jGenerator.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        if (asciiOnly) {
            jGenerator.enable(JsonGenerator.Feature.ESCAPE_NON_ASCII);
        }
        return jGenerator;
    }

    /**
     * Writes end character and flushes generator.
     * 
     * @param jGenerator The generator to write to and to flush
     * @param isJsonObject Whether generating a JSON object or a JSON array
     */
    protected static void writeEndAndFlush(final JsonGenerator jGenerator, final boolean isJsonObject) {
        if (null != jGenerator) {
            try {
                if (isJsonObject) {
                    jGenerator.writeEndObject(); // }
                } else {
                    jGenerator.writeEndArray(); // ]
                }
            } catch (final Exception e) {
                // Ignore
            }
            try {
                jGenerator.flush();
            } catch (final Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Generates String directly from given character array.
     * 
     * @param off The offset
     * @param len The length
     * @param chars The character array
     * @return The resulting String
     */
    protected static String directString(final int off, final int len, final char[] chars) {
        try {
            return STRING_CONSTRUCTOR.newInstance(Integer.valueOf(off), Integer.valueOf(len), chars);
        } catch (final Exception e) {
            return new String(chars, off, len);
        }
    }

    /**
     * The JSON factory.
     */
    protected static final JsonFactory JSON_FACTORY = new JsonFactory();

    /**
     * The minimal pretty-printer.
     */
    protected static final MinimalPrettyPrinter STANDARD_MINIMAL_PRETTY_PRINTER = new MinimalPrettyPrinter();

    /**
     * The default pretty-printer.
     */
    protected static final DefaultPrettyPrinter STANDARD_DEFAULT_PRETTY_PRINTER = new DefaultPrettyPrinter();

    /**
     * Initializes a new {@link AbstractJSONValue}.
     */
    protected AbstractJSONValue() {
        super();
    }

    /**
     * Closes given <code>java.io.Closeable</code> instance (if non-<code>null</code>).
     * 
     * @param closeable The <code>java.io.Closeable</code> instance
     */
    protected static void close(final java.io.Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (final Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Writes to given generator.
     * 
     * @param asciiOnly Whether to write only ASCII characters
     * @param jGenerator The generator
     * @throws IOException If an I/O error occurs
     * @throws JSONException IOf a JSON error occurs
     */
    protected abstract void writeTo(JsonGenerator jGenerator) throws IOException, JSONException;

    /**
     * Writes specified object to given generator.
     * 
     * @param v The object
     * @param asciiOnly Whether to allow only ASCII characters
     * @param jGenerator The generator
     * @throws IOException If an I/O error occurs
     * @throws JSONException IOf a JSON error occurs
     */
    protected static void write(final Object v, final JsonGenerator jGenerator) throws IOException, JSONException {
        if (null == v || JSONObject.NULL.equals(v)) {
            jGenerator.writeNull();
        } else if (v instanceof AbstractJSONValue) {
            ((AbstractJSONValue) v).writeTo(jGenerator);
        } else if (v instanceof JSONString) {
            try {
                final String s = ((JSONString) v).toJSONString();
                jGenerator.writeString(s);
            } catch (final Exception e) {
                throw new JSONException(e);
            }
        } else if (v instanceof Number) {
            jGenerator.writeNumber(JSONObject.numberToString((Number) v));
        } else if (v instanceof Boolean) {
            jGenerator.writeBoolean(((Boolean) v).booleanValue());
        } else {
            jGenerator.writeString(v.toString());
        }
    }

    private static String toAscii(final String str) {
        if (null == str) {
            return str;
        }
        final int length = str.length();
        if (0 == length || isAscii(str)) {
            return str;
        }
        final StringAllocator sa = new StringAllocator((length * 3)/2 + 1);
        for (int i = 0; i < length; i++) {
            final char c = str.charAt(i);
            if (c > 127) {
                appendAsJsonUnicode(c, sa);
            } else {
                sa.append(c);
            }
        }
        return sa.toString();
    }

    private static void appendAsJsonUnicode(final int ch, final StringAllocator sa) {
        sa.append("\\u");
        final String hex = Integer.toString(ch, 16);
        for (int i = hex.length(); i < 4; i++) {
            sa.append('0');
        }
        sa.append(hex);
    }

    /**
     * Checks whether the specified string's characters are ASCII 7 bit
     *
     * @param s The string to check
     * @return <code>true</code> if string's characters are ASCII 7 bit; otherwise <code>false</code>
     */
    protected static boolean isAscii(final String s) {
        if (null == s) {
            return true;
        }
        final int length = s.length();
        if (0 == length) {
            return true;
        }
        boolean isAscci = true;
        for (int i = 0; (i < length) && isAscci; i++) {
            isAscci = (s.charAt(i) < 128);
        }
        return isAscci;
    }

}

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.openexchange.plist.xml;

import java.util.ArrayList;
import java.util.EmptyStackException;

/**
 * {@link FastStack} - Copy of <code>org.apache.cxf.staxutils.FastStack</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class FastStack<T> extends ArrayList<T> {

    private static final long serialVersionUID = -6459585295618120689L;

    public void push(final T o) {
        add(o);
    }

    public T pop() {
        if (empty()) {
            throw new EmptyStackException();
        }

        return remove(size() - 1);
    }

    public boolean empty() {
        return size() == 0;
    }

    public T peek() {
        if (empty()) {
            throw new EmptyStackException();
        }

        return get(size() - 1);
    }
}

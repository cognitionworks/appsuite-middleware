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

package com.openexchange.ms.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemEvent;
import com.openexchange.java.util.UUIDs;
import com.openexchange.ms.Message;
import com.openexchange.ms.MessageListener;
import com.openexchange.ms.Queue;

/**
 * {@link HzQueue}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class HzQueue<E> implements Queue<E> {

    private static final Log LOG = com.openexchange.log.Log.loggerFor(HzQueue.class);

    private final IQueue<MessageData<E>> hzQueue;
    private final String senderId;
    private final String name;
    private final ConcurrentMap<MessageListener<E>, com.hazelcast.core.ItemListener<MessageData<E>>> registeredListeners;

    /**
     * Initializes a new {@link HzQueue}.
     */
    public HzQueue(final String name, final HazelcastInstance hz) {
        super();
        this.name = name;
        senderId = UUIDs.getUnformattedString(UUID.randomUUID());
        this.hzQueue = hz.getQueue(name);
        registeredListeners = new ConcurrentHashMap<MessageListener<E>, com.hazelcast.core.ItemListener<MessageData<E>>>(8);
    }

    @Override
    public String getSenderId() {
        return senderId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addMessageListener(final MessageListener<E> listener) {
        final HzMessageListener<E> hzListener = new HzMessageListener<E>(listener, senderId);
        hzQueue.addItemListener(hzListener, true);
        registeredListeners.put(listener, hzListener);
    }

    @Override
    public void removeMessageListener(final MessageListener<E> listener) {
        final com.hazelcast.core.ItemListener<MessageData<E>> hzListener = registeredListeners.remove(listener);
        if (null != hzListener) {
            try {
                hzQueue.removeItemListener(hzListener);
            } catch (final RuntimeException e) {
                // Removing message listener failed
                if (LOG.isDebugEnabled()) {
                    LOG.warn("Couldn't remove message listener from Hazelcast queue \"" + name + "\".", e);
                } else {
                    LOG.warn("Couldn't remove message listener from Hazelcast queue \"" + name + "\".");
                }
            }
        }
    }

    @Override
    public void destroy() {
        hzQueue.destroy();
    }

    @Override
    public int size() {
        return hzQueue.size();
    }

    @Override
    public boolean isEmpty() {
        return hzQueue.isEmpty();
    }

    @Override
    public boolean add(final E e) {
        return hzQueue.add(new MessageData<E>(e, senderId));
    }

    @Override
    public Iterator<E> iterator() {
        final Iterator<MessageData<E>> iterator = hzQueue.iterator();
        return new Iterator<E>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                final MessageData<E> next = iterator.next();
                return null == next ? null : next.getObject();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    @Override
    public E remove() {
        final MessageData<E> data = hzQueue.remove();
        return null == data ? null : data.getObject();
    }

    @Override
    public boolean offer(final E e) {
        return hzQueue.offer(new MessageData<E>(e, senderId));
    }

    @Override
    public Object[] toArray() {
        final Object[] array = hzQueue.toArray();
        final int length = array.length;
        final Object[] ret = new Object[length];
        for (int i = 0; i < length; i++) {
            ret[i] = ((MessageData<E>) array[i]).getObject();
        }
        return ret;
    }

    @Override
    public E poll() {
        MessageData<E> data = hzQueue.poll();
        return null == data ? null : data.getObject();
    }

    @Override
    public E element() {
        MessageData<E> data = hzQueue.element();
        return null == data ? null : data.getObject();
    }

    @Override
    public E peek() {
        MessageData<E> data = hzQueue.peek();
        return null == data ? null : data.getObject();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        final MessageData<E>[] array = hzQueue.toArray(new MessageData[0]);
        final List<T> list = new ArrayList<T>(array.length);
        for (int i = 0; i < array.length; i++) {
            list.add((T) array[i].getObject());
        }
        return list.toArray(a);
    }

    @Override
    public void put(final E e) throws InterruptedException {
        hzQueue.put(new MessageData<E>(e, senderId));
    }

    @Override
    public boolean offer(final E e, final long timeout, final TimeUnit unit) throws InterruptedException {
        return hzQueue.offer(new MessageData<E>(e, senderId), timeout, unit);
    }

    @Override
    public E take() throws InterruptedException {
        MessageData<E> data = hzQueue.take();
        return null == data ? null : data.getObject();
    }

    @Override
    public E poll(final long timeout, final TimeUnit unit) throws InterruptedException {
        MessageData<E> data = hzQueue.poll(timeout, unit);
        return null == data ? null : data.getObject();
    }

    @Override
    public int remainingCapacity() {
        return hzQueue.remainingCapacity();
    }

    @Override
    public boolean remove(final Object o) {
        return hzQueue.remove(o);
    }

    @Override
    public boolean contains(final Object o) {
        return hzQueue.contains(o);
    }

    @Override
    public int drainTo(final Collection<? super E> c) {
        final List<MessageData<E>> list = new ArrayList<MessageData<E>>(c.size());
        final int drained = hzQueue.drainTo(list);
        for (int i = 0; i < drained; i++) {
            c.add(list.get(i).getObject());
        }
        return drained;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        final List<MessageData<E>> list = new ArrayList<MessageData<E>>(c.size());
        for (Object object : c) {
            list.add(new MessageData<E>((E) object, senderId));
        }
        return hzQueue.containsAll(list);
    }

    @Override
    public int drainTo(final Collection<? super E> c, final int maxElements) {
        final List<MessageData<E>> list = new ArrayList<MessageData<E>>(c.size());
        final int drained = hzQueue.drainTo(list, maxElements);
        for (int i = 0; i < drained; i++) {
            c.add(list.get(i).getObject());
        }
        return drained;
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        boolean retval = false;
        for (E e : c) {
            retval = hzQueue.add(new MessageData<E>(e, senderId));
        }
        return retval;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        final List<MessageData<E>> list = new ArrayList<MessageData<E>>(c.size());
        for (Object object : c) {
            list.add(new MessageData<E>((E) object, senderId));
        }
        return hzQueue.removeAll(list);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        final List<MessageData<E>> list = new ArrayList<MessageData<E>>(c.size());
        for (Object object : c) {
            list.add(new MessageData<E>((E) object, senderId));
        }
        return hzQueue.retainAll(list);
    }

    @Override
    public void clear() {
        hzQueue.clear();
    }

    // ------------------------------------------------------------------------ //

    private static final class HzMessageListener<E> implements com.hazelcast.core.ItemListener<MessageData<E>> {

        private final MessageListener<E> listener;
        private final String senderId;

        /**
         * Initializes a new {@link HzMessageListener}.
         */
        protected HzMessageListener(final MessageListener<E> listener, final String senderId) {
            super();
            this.listener = listener;
            this.senderId = senderId;
        }

        @Override
        public void itemAdded(final ItemEvent<MessageData<E>> item) {
            final MessageData<E> messageData = item.getItem();
            final String messageSender = messageData.getSenderId();
            listener.onMessage(new Message<E>(item.getSource().toString(), messageSender, messageData.getObject(), !senderId.equals(messageSender)));
        }

        @Override
        public void itemRemoved(final ItemEvent<MessageData<E>> item) {
            // Ignore
        }

    }

}

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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.realtime.packet;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.google.common.base.Predicate;
import com.openexchange.exception.OXException;
import com.openexchange.realtime.exception.RealtimeException;
import com.openexchange.realtime.payload.PayloadElement;
import com.openexchange.realtime.payload.PayloadTree;
import com.openexchange.realtime.payload.PayloadTreeNode;
import com.openexchange.realtime.util.ElementPath;

/**
 * {@link Stanza} - Abstract information unit that can be send from one entity to another.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public abstract class Stanza implements Serializable {

    public static final ElementPath ERROR_PATH = new ElementPath("error");

    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Stanza.class);

    // recipient and sender
    protected volatile ID to;
    protected volatile ID from;
    protected volatile ID sequencePrincipal;
    protected volatile ID onBehalfOf;

    // All 3 basic stanza types either have an optional or mandatory id field
    protected String id = "";

    /**
     * The error object for Presence Stanza of type error.
     */
    protected RealtimeException error = null;

    private String selector = "default";

    protected long sequenceNumber = -1;

    protected String tracer;

    private final List<String> logEntries = new LinkedList<String>();

    // Payloads carried by this Stanza as n-ary trees
    protected volatile Map<ElementPath, List<PayloadTree>> payloads;

    /**
     * Initializes a new {@link Stanza}.
     */
    protected Stanza() {
        super();
        payloads = new ConcurrentHashMap<ElementPath, List<PayloadTree>>();
    }

    /**
     * Gets the id
     *
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id
     *
     * @param id The id to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Get the {@link ID} describing the stanza's recipient.
     *
     * @return null or the ID of the stanza's recipient
     */
    public ID getTo() {
        return to;
    }

    /**
     * Set the {@link ID} describing the Stanza's recipient.
     *
     * @param to the ID of the stanza's recipient
     */
    public void setTo(final ID to) {
        this.to = to;
    }

    /**
     * Get the {@link ID} describing the Stanza's sender.
     *
     * @return the {@link ID} describing the Stanza's sender.
     */
    public ID getFrom() {
        return from;
    }

    /**
     * Set the {@link ID} describing the Stanza's sender.
     *
     * @param from the {@link ID} describing the Stanza's sender.
     */
    public void setFrom(final ID from) {
        this.from = from;
    }

    /**
     * Get the error element describing the error-type Stanza in more detail.
     *
     * @return Null or the OXException representing the error
     */
    public RealtimeException getError() {
        return error;
    }

    /**
     * Set the error element describing the error-type Stanza in more detail.
     *
     * @param error The OXException representing the error
     */
    public void setError(RealtimeException error) {
        this.error = error;
        writeThrough(ERROR_PATH, error);
    }

    /**
     * Sets the onBehalfOf
     *
     * @param onBehalfOf The onBehalfOf to set
     */
    public void setOnBehalfOf(ID onBehalfOf) {
        this.onBehalfOf = onBehalfOf;
    }


    /**
     * Gets the onBehalfOf
     *
     * @return The onBehalfOf
     */
    public ID getOnBehalfOf() {
        return onBehalfOf;
    }


    /**
     * Sets the sequenceNumber
     *
     * @param sequenceNumber The sequenceNumber to set
     */
    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }


    /**
     * Gets the sequenceNumber
     *
     * @return The sequenceNumber, -1 if the sequenceNumber is invalid/not set
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }


    /**
     * Sets the sequencePrincipal
     *
     * @param sequencePrincipal The sequencePrincipal to set
     */
    public void setSequencePrincipal(ID sequencePrincipal) {
        this.sequencePrincipal = sequencePrincipal;
    }


    /**
     * Gets the sequencePrincipal
     *
     * @return The SequencePrincipal (ID) set via setSequencePrincipal or the SequencePrincipal of the sender ID
     */
    public ID getSequencePrincipal() {
        return (sequencePrincipal != null) ? sequencePrincipal : (onBehalfOf != null) ? onBehalfOf : from;
    }

    /**
     * Get a List of namespaces of the payloads of this Stanza.
     *
     * @return Empty Set or the namespaces of the payloads of this Stanza.
     */
    public Collection<ElementPath> getElementPaths() {
        Set<ElementPath> paths = new HashSet<ElementPath>();
        for (PayloadTree tree : getPayloads()) {
            paths.addAll(tree.getElementPaths());
        }
        return paths;
    }

    /**
     * Get all Payloads of this Stanza.
     *
     * @return A List of PayloadTrees.
     */
    public Collection<PayloadTree> getPayloads() {
        ArrayList<PayloadTree> resultList = new ArrayList<PayloadTree>();
        Collection<List<PayloadTree>> values = payloads.values();
        for (List<PayloadTree> list : values) {
            resultList.addAll(list);
        }
        return resultList;
    }

    /**
     * A very common case: Get the single payload contained in this Stanza.
     *
     * @return null if the Stanza doesn't contain a Payload, otherwise the Payload
     */
    public PayloadElement getPayload() {
        Iterator<List<PayloadTree>> iterator = payloads.values().iterator();
        if (iterator.hasNext()) {
            return iterator.next().get(0).getRoot().getPayloadElement();
        }

        return null;
    }

    /**
     * Set all Payloads of this Stanza.
     *
     * @param payloadTrees The PayloadTrees forming the Payloads.
     */
    public void setPayloads(Collection<PayloadTree> payloadTrees) {
        Map<ElementPath, List<PayloadTree>> newPayloads = new ConcurrentHashMap<ElementPath, List<PayloadTree>>();
        for (PayloadTree tree : payloadTrees) {
            ElementPath elementPath = tree.getRoot().getElementPath();
            List<PayloadTree> list = newPayloads.get(elementPath);
            if (list == null) {
                list = new ArrayList<PayloadTree>();
            }
            list.add(tree);
            newPayloads.put(tree.getElementPath(), list);
        }
        payloads = newPayloads;
    }

    /**
     * Add a payload to this Stanza.
     *
     * @param tree The PayloadTreeNoode to add to this Stanza
     * @return true if the PayloadTreeNode could be added to this Stanza
     */
    public void addPayload(final PayloadTree tree) {
        addPayloadToMap(tree, this.payloads);
    }

    /**
     * Add a PayloadTree into a Map containing lists of PayloadTrees mapped to their ElementPaths.
     *
     * @param tree The tree to add
     * @param payloadTreeMap The Map containing the trees
     */
    private void addPayloadToMap(PayloadTree tree, Map<ElementPath, List<PayloadTree>> payloadTreeMap) {
        ElementPath elementPath = tree.getElementPath();
        List<PayloadTree> list = payloadTreeMap.get(elementPath);
        if (list == null) {
            list = new ArrayList<PayloadTree>();
        }
        list.add(tree);
        payloadTreeMap.put(tree.getElementPath(), list);
    }

    /**
     * Remove a PayloadTree from this Stanza.
     *
     * @param tree The PayloadTree to remove from this Stanza
     */
    public void removePayload(final PayloadTree tree) {
        ElementPath elementPath = tree.getElementPath();
        List<PayloadTree> list = payloads.get(elementPath);
        if (list != null) {
            list.remove(tree);
            payloads.put(elementPath, list);
        }
    }

    /**
     * Get a Collection of Payloads that match an ElementPath
     *
     * @param elementPath The Elementpath identifying the Payload
     * @return A Collection of PayloadTrees
     */
    @SuppressWarnings("unchecked")
    public Collection<PayloadTree> getPayloads(final ElementPath elementPath) {
        List<PayloadTree> list = payloads.get(elementPath);
        if (list == null) {
            list = Collections.EMPTY_LIST;
        }

        return list;
    }

    /**
     * Filter the payloads based on a Predicate.
     *
     * @param predicate
     * @return Payloads matching the Predicate or an empty Collection
     */
    public Collection<PayloadTree> filterPayloads(Predicate<PayloadTree> predicate) {
        Collection<PayloadTree> result = new ArrayList<PayloadTree>();
        for (PayloadTree element : getPayloads()) {
            if (predicate.apply(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Return a Map<ElementPath, List<PayloadTree>> containing deep copies of this stanza's payloads.
     *
     * @return a Map<ElementPath, List<PayloadTree>> containing deep copies of this stanza's payloads.
     */
    protected Map<ElementPath, List<PayloadTree>> deepCopyPayloads() {
        HashMap<ElementPath, List<PayloadTree>> copiedPayloads = new HashMap<ElementPath, List<PayloadTree>>();
        for (PayloadTree tree : getPayloads()) {
            PayloadTree copiedTree = new PayloadTree(tree);
            addPayloadToMap(copiedTree, copiedPayloads);
        }
        return copiedPayloads;
    }

    /**
     * Gets the selector that is used to identify GroupDispatcher instances on the server side.
     * Example: If you join a chatroom or a collaboratively edited document yo may receive messages from this chatroom. Those messages will
     * contain the sender of the message and a selector that idenifies the chatroom that distributed the message to you. Clients have to
     * choose a selector when joining a chatroom and have to take care of the mapping selector <-> chatroom themselves.
     *
     * @return the selector
     */
    public String getSelector() {
        return selector;
    }

    /**
     * Sets the selector that is used to identify GroupDispatcher instances on the server side.
     * Example: If you join a chatroom or a collaboratively edited document yo may receive messages from this chatroom. Those messages will
     * contain the sender of the message and a selector that idenifies the chatroom that distributed the message to you. Clients have to
     * choose a selector when joining a chatroom and have to take care of the mapping selector <-> chatroom themselves.
     *
     * @param selector the selector
     */
    public void setSelector(String selector) {
        this.selector = selector;
    }

    public void setTracer(String tracer) {
        this.tracer = tracer;
    }

    public String getTracer() {
        return tracer;
    }

    public boolean traceEnabled() {
        return tracer != null;
    }

    public void trace(Object trace) {
        if (traceEnabled()) {
            StringBuilder sb = new StringBuilder(tracer)
                .append(": ")
                .append(trace)
                .append(", from: ")
                .append(getFrom())
                .append(", to: ")
                .append(getTo())
                .append(", selector: ")
                .append(getSelector());
            LOG.info(sb.toString());
//            logEntries.add(trace.toString());
        }
    }

    public void trace(Object trace, Throwable t) {
        if (traceEnabled()) {
            StringBuilder sb = new StringBuilder(tracer)
                .append(": ")
                .append(trace)
                .append(", from: ")
                .append(getFrom())
                .append(", to: ")
                .append(getTo())
                .append(", selector: ")
                .append(getSelector());
            LOG.info(sb.toString(), t);
            StringWriter w = new StringWriter();
            t.printStackTrace(new PrintWriter(w));
//            logEntries.add(trace.toString());
//            logEntries.add(w.toString());
        }
    }

    public void addLogMessages(List<String> logEntries) {
        this.logEntries.addAll(logEntries);
    }



    public List<String> getLogEntries() {
        return logEntries;
    }

    public void transformPayloads(String format) throws OXException {
        List<PayloadTree> copy = new ArrayList<PayloadTree>(getPayloads().size());
        for (PayloadTree tree : getPayloads()) {
            tree = tree.toExternal(format);
            copy.add(tree);
        }
        setPayloads(copy);
    }

    public void transformPayloadsToInternal() throws OXException {
        List<PayloadTree> copy = new ArrayList<PayloadTree>(getPayloads().size());
        for (PayloadTree tree : getPayloads()) {
            tree = tree.toInternal();
            copy.add(tree);
        }
        setPayloads(copy);
    }

    /**
     * Write a payload to the PayloadTree identified by the ElementPath. There is only one tree for the default elements which only contains
     * one node so we can set the data by directly writing to the root node.
     *
     * @param path The ElementPath identifying the PayloadTree.
     * @param data The payload data to write into the root node.
     */
    protected void writeThrough(ElementPath path, Object data) {
        List<PayloadTree> payloadTrees = payloads.get(path);
        if (payloadTrees == null) {
            payloadTrees = new ArrayList<PayloadTree>();
        }
        if (payloadTrees.size() > 1) {
            throw new IllegalStateException("Stanza shouldn't contain more than one PayloadTree per basic ElementPath");
        }
        PayloadTree tree;
        if (payloadTrees.isEmpty()) {
            PayloadElement payloadElement = new PayloadElement(
                data,
                data.getClass().getSimpleName(),
                path.getNamespace(),
                path.getElement());
            PayloadTreeNode payloadTreeNode = new PayloadTreeNode(payloadElement);
            tree = new PayloadTree(payloadTreeNode);
            addPayload(tree);
        } else {
            tree = payloadTrees.get(0);
            PayloadTreeNode node = tree.getRoot();
            if (node == null) {
                throw new IllegalStateException("PayloadTreeNode removed? This shouldn't happen!");
            }
            node.setData(data, data.getClass().getSimpleName());
        }

    }
    /**
     * Init default fields from values found in the PayloadTrees of the Stanza.
     *
     * @throws OXException when the Stanza couldn't be initialized
     */
    public abstract void initializeDefaults() throws OXException;

    public abstract Stanza newInstance();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((payloads == null) ? 0 : payloads.hashCode());
        result = prime * result + ((selector == null) ? 0 : selector.hashCode());
        result = prime * result + ((to == null) ? 0 : to.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Stanza)) {
            return false;
        }
        Stanza other = (Stanza) obj;
        final ID thisFrom = from;
        if (thisFrom == null) {
            if (other.from != null) {
                return false;
            }
        } else if (!thisFrom.equals(other.from)) {
            return false;
        }
        final String thisId = id;
        if (thisId == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!thisId.equals(other.id)) {
            return false;
        }
        final Map<ElementPath, List<PayloadTree>> thisPayloads = payloads;
        if (thisPayloads == null) {
            if (other.payloads != null) {
                return false;
            }
        } else if (!thisPayloads.equals(other.payloads)) {
            return false;
        }
        final String thisSelector = selector;
        if (thisSelector == null) {
            if (other.selector != null) {
                return false;
            }
        } else if (!thisSelector.equals(other.selector)) {
            return false;
        }
        final ID thisTo = to;
        if (thisTo == null) {
            if (other.to != null) {
                return false;
            }
        } else if (!thisTo.equals(other.to)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return "From: " + from + "\nTo: " + to + "\nPayloads:\n" + payloads;
    }
}

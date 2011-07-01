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

package com.openexchange.ajax.requesthandler;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link DefaultConverter}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class DefaultConverter implements Converter {

    private Map<String, List<Node>> understandsFormat = new ConcurrentHashMap<String, List<Node>>();

    private Map<String, List<Node>> suppliesFormat = new ConcurrentHashMap<String, List<Node>>();

    private Map<Conversion, Step> cachedSteps = new ConcurrentHashMap<Conversion, Step>();
    
    public void addConverter(ResultConverter converter) {
        Node n = new Node();
        n.converter = converter;
        {
            // First let's connect this node as a follow-up to all nodes outputting what we accept as input
            Edge edge = new Edge();
            edge.node = n;

            List<Node> nodesWhoseOutputIsUnderstood = suppliesFormat.get(converter.getInputFormat());
            if (nodesWhoseOutputIsUnderstood != null && !nodesWhoseOutputIsUnderstood.isEmpty()) {
                for (Node node : nodesWhoseOutputIsUnderstood) {
                    node.edges.add(edge);
                }
            }
        }
        {
            // Next, we'll find our current followers
            String outputFormat = converter.getOutputFormat();
            List<Node> nodesWhoUnderstandMe = understandsFormat.get(outputFormat);
            if (nodesWhoUnderstandMe != null && !nodesWhoUnderstandMe.isEmpty()) {
                for (Node node : nodesWhoUnderstandMe) {
                    Edge edge = new Edge();
                    edge.node = node;
                    n.edges.add(edge);
                }
            }
        }

        {
            // Next update the lists
            List<Node> understanders = understandsFormat.get(converter.getInputFormat());
            if (understanders == null) {
                understanders = new LinkedList<Node>();
                understandsFormat.put(converter.getInputFormat(), understanders);
            }
            understanders.add(n);

            List<Node> suppliers = suppliesFormat.get(converter.getOutputFormat());
            if (suppliers == null) {
                suppliers = new LinkedList<Node>();
                suppliesFormat.put(converter.getOutputFormat(), suppliers);
            }
            suppliers.add(n);
        }

    }
    
    public void removeConverter(ResultConverter thing) {
        // TODO Auto-generated method stub
        
    }


    public void convert(String from, String to, AJAXRequestData request, AJAXRequestResult result, ServerSession session) throws AbstractOXException {
        Step path = getShortestPath(from, to);
        while(path != null) {
            path.converter.convert(request, result, session, this);
            result.setFormat(path.converter.getOutputFormat());
            path = path.next;
        }
    }

    public Step getShortestPath(String from, String to) {
        Conversion conversion = new Conversion(from, to);
        Step step = cachedSteps.get(conversion);
        if(step != null) {
            return step;
        }
        Map<Node, Mark> markings = new HashMap<Node, Mark>();
        List<Edge> edges = getInitialEdges(from);
        Mark currentMark = new Mark();
        currentMark.weight = 0;
        Node currentNode = null;

        while (true) {
            Mark nextMark = null;
            Node nextNode = null;
            for (Edge edge : edges) {
                if (edge.node.converter.getOutputFormat().equals(to)) {
                    // I guess we're done;
                    Mark m = new Mark();
                    m.previous = currentNode;
                    Step newStep = unwind(m, edge, markings);
                    cachedSteps.put(conversion, newStep);
                    return newStep;
                }
                Mark mark = markings.get(edge.node);
                if (mark == null) {
                    mark = new Mark();
                    markings.put(edge.node, mark);
                }

                if (!mark.visited && mark.weight > currentMark.weight + edge.weight()) {
                    mark.weight = currentMark.weight + edge.weight();
                    mark.previous = currentNode;
                    if (nextMark == null || nextMark.weight > mark.weight) {
                        nextMark = mark;
                        nextNode = edge.node;
                    }
                }
            }

            currentMark.visited = true;

            if (nextMark == null) {
                // This was a dead end
                currentMark.weight = 100;
                while (nextMark == null) {
                    if (currentMark.previous == null) {
                        throw new IllegalArgumentException("Can't find path from " + from + " to " + to);
                    }
                    currentNode = currentMark.previous;
                    currentMark = markings.get(currentNode);
                    for (Edge edge : currentNode.edges) {
                        Mark mark = markings.get(edge.node);
                        if (!mark.visited && (nextMark == null || nextMark.weight > mark.weight)) {
                            nextMark = mark;
                            nextNode = edge.node;
                        }
                    }
                }
            }

            currentMark = nextMark;
            currentNode = nextNode;
            edges = nextNode.edges;
            
        }

    }

    private Step unwind(Mark currentMark, Edge edge, Map<Node, Mark> markings) {
        Step current = new Step();
        current.converter = edge.node.converter;
        
        while (currentMark.previous != null) {
            Step step = new Step();
            step.converter = currentMark.previous.converter;
            step.next = current;
            current = step;
            currentMark = markings.get(currentMark.previous);
        }
        
        return current;
    }

    // Synthetic edges for entry
    private List<Edge> getInitialEdges(String format) {
        List<Node> list = understandsFormat.get(format);
        
        if(list == null || list.isEmpty()) {
            throw new IllegalArgumentException("Can't convert from "+format);
        }
        List<Edge> edges = new ArrayList<Edge>(list.size());
        for (Node node : list) {
            Edge edge = new Edge();
            edge.node = node;
            edges.add(edge);
        }
        
        return edges;
    }
    
    public static class Step {
        public Step next;
        public ResultConverter converter;
    }

    public static class Mark {

        public Node previous;

        public int weight = Integer.MAX_VALUE;

        public boolean visited = false;
    }

    public static class Edge {

        public Node node;

        public int weight() {
            switch (node.converter.getQuality()) {
            case GOOD:
                return 1;
            case BAD:
                return 2;
            }
            return 2;
        }
    }

    public static class Node {

        public ResultConverter converter;

        public List<Edge> edges = new LinkedList<Edge>();
    }

    public static class Conversion {
        
        private final String from;
        private final String to;
        private final int hashCode;
        
        Conversion(String from, String to) {
            super();
            this.from = from;
            this.to = to;
            
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + ((to == null) ? 0 : to.hashCode());
            hashCode = result;
        }
        @Override
        public int hashCode() {
            return hashCode;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Conversion other = (Conversion) obj;
            if (from == null) {
                if (other.from != null) {
                    return false;
                }
            } else if (!from.equals(other.from)) {
                return false;
            }
            if (to == null) {
                if (other.to != null) {
                    return false;
                }
            } else if (!to.equals(other.to)) {
                return false;
            }
            return true;
        }
        
        
        
    }

}

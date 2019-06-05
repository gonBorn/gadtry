/*
 * Copyright (C) 2018 The GadTry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.harbby.gadtry.graph;

import com.github.harbby.gadtry.graph.impl.NodeImpl;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface Node<NodeData, EdgeData>
        extends Serializable
{
    public abstract String getId();

    public abstract NodeData getData();

    /**
     * Get all child nodes of the current node
     *
     * @return List child nodes
     */
    public Collection<Edge<NodeData, EdgeData>> nextNodes();

    public Optional<Edge<NodeData, EdgeData>> getNextNode(String id);

    @Override
    public abstract String toString();

    public static <E, R> Builder<E, R> builder(String id, E nodeData)
    {
        return new Builder<>(id, nodeData);
    }

    public static class Builder<E, R>
    {
        private final Map<String, Edge<E, R>> nextNodes = new HashMap<>();
        private final Node<E, R> node;

        public Builder(String id, E nodeData)
        {
            this.node = new NodeImpl<>(id, nextNodes, nodeData);
        }

        public Builder<E, R> addNextNode(Edge<E, R> edge)
        {
            nextNodes.put(edge.getOutNode().getId(), edge);
            return this;
        }

        public Node<E, R> build()
        {
            return node;
        }
    }
}

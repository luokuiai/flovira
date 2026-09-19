/*
 * Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luokuiai.flovira.core.service.impl;

import com.luokuiai.flovira.core.FlowEngine;
import com.luokuiai.flovira.core.entity.Definition;
import com.luokuiai.flovira.core.entity.Node;
import com.luokuiai.flovira.core.enums.NodeType;
import com.luokuiai.flovira.core.enums.PublishStatus;
import com.luokuiai.flovira.core.invoker.FrameInvoker;
import com.luokuiai.flovira.core.service.NodeService;
import com.luokuiai.flovira.core.support.TestEntityFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefinitionPublishTest {
    private final List<Definition> definitions = new ArrayList<>();
    private final DefServiceImpl service = new DefServiceImpl() {
        @Override
        public Definition getById(Serializable id) {
            return definitions.stream().filter(def -> def.getId().equals(id)).findFirst().get();
        }

        @Override
        public List<Definition> getByFlowCode(String flowCode) {
            return definitions;
        }

        @Override
        public void updatePublishStatus(List<Long> ids, Integer status) {
            ids.forEach(id -> getById(id).setPublishStatus(status));
        }

        @Override
        public boolean updateById(Definition definition) {
            getById(definition.getId()).setPublishStatus(definition.getPublishStatus());
            return true;
        }
    };

    @Before
    public void setUp() {
        FlowEngine.setNewDef(() -> TestEntityFactory.create(Definition.class));
        NodeService nodes = new NodeServiceImpl() {
            @Override
            public List<Node> getByDefId(Long id) {
                return Collections.singletonList(TestEntityFactory.create(Node.class)
                    .setDefinitionId(id).setNodeCode("start").setNodeType(NodeType.START.getKey()));
            }

            @Override
            public Map<String, String> getExt(Node node) {
                return Collections.emptyMap();
            }
        };
        FrameInvoker.setBeanFunction(type -> {
            if (NodeService.class.equals(type)) return nodes;
            throw new AssertionError("Publishing must not query or modify instances: " + type);
        });
    }

    @After
    public void tearDown() {
        FrameInvoker.setBeanFunction(type -> null);
        FlowEngine.setNewDef(null);
    }

    @Test
    public void shouldExpireAllOtherPublishedDefinitionsWithoutConsultingInstances() {
        Definition old = definition(1L, PublishStatus.PUBLISHED);
        Definition anotherOld = definition(2L, PublishStatus.PUBLISHED);
        Definition draft = definition(3L, PublishStatus.UNPUBLISHED);
        Definition expired = definition(4L, PublishStatus.EXPIRED);
        Definition current = definition(5L, PublishStatus.UNPUBLISHED);

        assertTrue(service.publish(current.getId()));

        assertEquals(PublishStatus.EXPIRED.getKey(), old.getPublishStatus());
        assertEquals(PublishStatus.EXPIRED.getKey(), anotherOld.getPublishStatus());
        assertEquals(PublishStatus.UNPUBLISHED.getKey(), draft.getPublishStatus());
        assertEquals(PublishStatus.EXPIRED.getKey(), expired.getPublishStatus());
        assertEquals(PublishStatus.PUBLISHED.getKey(), current.getPublishStatus());
        assertEquals(1L, definitions.stream()
            .filter(def -> PublishStatus.PUBLISHED.getKey().equals(def.getPublishStatus())).count());
    }

    @Test
    public void shouldPublishFirstVersionWithoutExpiringDrafts() {
        Definition draft = definition(1L, PublishStatus.UNPUBLISHED);
        Definition current = definition(2L, PublishStatus.UNPUBLISHED);

        assertTrue(service.publish(current.getId()));

        assertEquals(PublishStatus.UNPUBLISHED.getKey(), draft.getPublishStatus());
        assertEquals(PublishStatus.PUBLISHED.getKey(), current.getPublishStatus());
    }

    private Definition definition(Long id, PublishStatus status) {
        Definition definition = TestEntityFactory.create(Definition.class)
            .setId(id).setFlowCode("expense").setPublishStatus(status.getKey());
        definitions.add(definition);
        return definition;
    }
}

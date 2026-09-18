/*
 *    Copyright 2026, LuokuiAI (luokuiai@gmail.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.luokuiai.flovira.core.entity;

/** 一次实际节点激活，重入不得复用身份。 */
public interface NodeExecution extends RootEntity {
    Long getInstanceId();
    NodeExecution setInstanceId(Long value);
    Long getDefinitionId();
    NodeExecution setDefinitionId(Long value);
    String getNodeCode();
    NodeExecution setNodeCode(String value);
    Integer getNodeType();
    NodeExecution setNodeType(Integer value);
    String getState();
    NodeExecution setState(String value);
    java.util.Date getEnteredAt();
    NodeExecution setEnteredAt(java.util.Date value);
    java.util.Date getClosedAt();
    NodeExecution setClosedAt(java.util.Date value);
    String getCloseReason();
    NodeExecution setCloseReason(String value);
    Integer getVersion();
    NodeExecution setVersion(Integer value);
}

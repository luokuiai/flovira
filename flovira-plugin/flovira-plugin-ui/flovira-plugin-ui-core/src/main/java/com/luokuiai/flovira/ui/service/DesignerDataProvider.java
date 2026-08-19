/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
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
package com.luokuiai.flovira.ui.service;

import com.luokuiai.flovira.core.handler.BusinessRelationProvider;
import com.luokuiai.flovira.ui.dto.DesignerResourceQuery;
import com.luokuiai.flovira.ui.vo.DesignerResourcePage;

/**
 * 业务系统提供的设计器基础数据与关系查询。
 *
 * @author warm
 */
public interface DesignerDataProvider extends BusinessRelationProvider {

    DesignerResourcePage queryResources(DesignerResourceQuery query);

}

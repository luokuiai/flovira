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
package com.luokuiai.flovira.core.orm.agent;

import lombok.Getter;
import lombok.Setter;
import com.luokuiai.flovira.core.orm.service.IFloviraService;
import com.luokuiai.flovira.core.utils.CollUtil;
import com.luokuiai.flovira.core.utils.ObjectUtil;
import com.luokuiai.flovira.core.utils.page.OrderBy;
import com.luokuiai.flovira.core.utils.page.Page;

import java.util.List;

/**
 * 查询代理层处理
 *
 * @author warm
 * @since 2023-03-17
 */
public class FloviraQuery<T> implements OrderBy {

    /**
     * 排序字段
     */
    @Setter
    private String orderBy;

    /**
     * 排序的方向desc或者asc
     */
    private String isAsc = "ASC";

    @Setter
    @Getter
    private IFloviraService<T> floviraService;

    public FloviraQuery(IFloviraService<T> floviraService) {
        this.floviraService = floviraService;
    }

    /**
     * 查询列表
     *
     * @param entity 实体列表
     * @return 集合
     */
    public Page<T> page(T entity, Page<T> page) {
        if (ObjectUtil.isNull(page)) {
            page = new Page<>(1, 10, orderBy, isAsc);
        }
        return floviraService.page(entity, page.setOrderBy(orderBy).setIsAsc(isAsc));
    }

    /**
     * 查询列表
     *
     * @param entity 实体列表
     * @return 集合
     */
    public List<T> list(T entity) {
        return floviraService.list(entity, this);
    }

    /**
     * 查询列表
     *
     * @param entity 实体列表
     * @return 集合
     */
    public T getOne(T entity) {
        List<T> list = floviraService.list(entity, this);
        return CollUtil.getOne(list);
    }

    /**
     * id设置正序排列
     *
     * @return 集合
     */
    public FloviraQuery<T> orderById() {
        this.orderBy = "id";
        return this;
    }

    /**
     * 创建时间设置正序排列
     *
     * @return 集合
     */
    public FloviraQuery<T> orderByCreateTime() {
        this.orderBy = "create_time";
        return this;
    }

    /**
     * 更新时间设置正序排列
     *
     * @return 集合
     */
    public FloviraQuery<T> orderByUpdateTime() {
        this.orderBy = "update_time";
        return this;
    }

    /**
     * 设置正序排列
     *
     * @return 集合
     */
    public FloviraQuery<T> desc() {
        this.isAsc = "DESC";
        return this;
    }

    /**
     * 设置正序排列
     *
     * @param orderByField 排序字段
     * @return 集合
     */
    public FloviraQuery<T> orderByAsc(String orderByField) {
        this.orderBy = orderByField;
        this.isAsc = "ASC";
        return this;
    }

    /**
     * 设置倒序排列
     *
     * @param orderByField 排序字段
     * @return 集合
     */
    public FloviraQuery<T> orderByDesc(String orderByField) {
        this.orderBy = orderByField;
        this.isAsc = "DESC";
        return this;
    }

    /**
     * 用户自定义排序方案
     *
     * @param orderByField 排序字段
     * @return 集合
     */
    public FloviraQuery<T> orderBy(String orderByField) {
        this.orderBy = orderByField;
        return this;
    }

    @Override
    public String getOrderBy() {
        return orderBy;
    }

    @Override
    public String getIsAsc() {
        return isAsc;
    }

    public FloviraQuery<T> setIsAsc(String isAsc) {
        this.isAsc = isAsc;
        return this;
    }
}

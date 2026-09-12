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
package com.luokuiai.flovira.spring.boot.config;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import com.luokuiai.flovira.core.config.Flovira;
import com.luokuiai.flovira.orm.utils.CommonUtil;
import com.luokuiai.flovira.plugin.modes.sb.config.BeanConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 工作流bean注册配置
 *
 * @author warm
 * @since 2023/6/5 23:01
 */
@Configuration
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@ConditionalOnProperty(value = "flovira.enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("com.luokuiai.flovira.orm.mapper")
public class FlowAutoConfig extends BeanConfig {

    private final SqlSessionFactory sqlSessionFactory;

    public FlowAutoConfig(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public void after(Flovira flowConfig) {
        loadXml(sqlSessionFactory);
        CommonUtil.setDataSourceType(flowConfig, sqlSessionFactory.getConfiguration());
    }

    private void loadXml(SqlSessionFactory sqlSessionFactory) {
        List<String> mapperList = Arrays.asList("flovira/FlowDefinitionMapper.xml", "flovira/FlowHisTaskMapper.xml"
            , "flovira/FlowInstanceMapper.xml", "flovira/FlowNodeMapper.xml", "flovira/FlowFormMapper.xml"
            , "flovira/FlowSkipMapper.xml", "flovira/FlowTaskMapper.xml", "flovira/FlowUserMapper.xml"
            , "flovira/FlowSubprocessRunMapper.xml", "flovira/FlowSubprocessChildMapper.xml"
            , "flovira/FlowSubprocessEventMapper.xml");
        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        try {
            for (String mapper : mapperList) {
                XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(Resources.getResourceAsStream(mapper),
                    configuration, getClass().getResource("/") + mapper, configuration.getSqlFragments());
                xmlMapperBuilder.parse();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

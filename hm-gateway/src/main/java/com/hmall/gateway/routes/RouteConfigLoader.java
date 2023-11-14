package com.hmall.gateway.routes;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class RouteConfigLoader {
    private final NacosConfigManager nacosConfigManager;

    private final RouteDefinitionWriter definitionWriter;

    private final String DATA_ID = "gateway-routes.json";
    private final String GROUP = "DEFAULT_GROUP";

    private final Set<String> routeIds = new HashSet<>();

    @PostConstruct
    public void initRouteConfiguration() throws NacosException {
        String configInfo = nacosConfigManager.getConfigService().getConfigAndSignListener(DATA_ID, GROUP, 1000, new Listener() {
            @Override
            public Executor getExecutor() {
                return Executors.newSingleThreadExecutor();
            }

            @Override
            public void receiveConfigInfo(String s) {
                //更新路由表
                updateConfigInfo(s);

            }
        });
        updateConfigInfo(configInfo);
    }
    private void updateConfigInfo(String configInfo) {
        //1.解析路由
        List<RouteDefinition> routeDefinitionList = JSONUtil.toList(configInfo,RouteDefinition.class);
        //删除旧的路由表
        for (String routeId : routeIds) {
            definitionWriter.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();
        //是否有新的路由
        if (routeDefinitionList == null || routeDefinitionList.isEmpty()) {
            return;
        }
        for (RouteDefinition routeDefinition : routeDefinitionList) {
            //写入路由表
            definitionWriter.save(Mono.just(routeDefinition)).subscribe();
            routeIds.add(routeDefinition.getId());
        }
    }

}

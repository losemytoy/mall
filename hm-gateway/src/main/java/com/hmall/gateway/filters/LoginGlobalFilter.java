package com.hmall.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LoginGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final JwtTool jwtTool;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1、获取Request
        ServerHttpRequest request = exchange.getRequest();
        //2、判断当前请求是否需要被拦截
        if (isAllowPath(request)) {
            return chain.filter(exchange);
        }
        //3、获取token
        List<String> headers = request.getHeaders().get("authorization");
        String token = null;
        if (headers != null || headers.isEmpty()) {
            token = headers.get(0);
        }
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        String userInfo = userId.toString();
        ServerWebExchange build = exchange.mutate().request(builder -> builder.header("user-info", userInfo)).build();

        return chain.filter(build);
    }

    private boolean isAllowPath(ServerHttpRequest request) {
        boolean flag = false;
        //String method = request.getMethodValue();
        String path = request.getPath().toString();
        for (String excludePath : authProperties.getExcludePaths()) {
            boolean isMatch = antPathMatcher.match(excludePath, path);
            if (isMatch) {
                flag = true;
                break;
            }
        }
        return flag;
    }


    @Override
    public int getOrder() {
        return 0;
    }
}

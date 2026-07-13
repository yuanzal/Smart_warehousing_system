package com.qst.smart_warehousing.config;

import com.qst.smart_warehousing.common.AuthConst;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI myOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧仓储与AI分拣系统-API文档")
                        .version("v1.0")
                        .description("WMS智能化调度中台接口测试"))
                // 核心：告诉 Swagger 所有的请求都需要尝试携带 Token 锁
                .addSecurityItem(new SecurityRequirement().addList(AuthConst.TOKEN_NAME))
                .components(new Components()
                        .addSecuritySchemes(AuthConst.TOKEN_NAME, new SecurityScheme()
                                .name(AuthConst.TOKEN_NAME) // 请求头的名字，即 Authorization
                                .type(SecurityScheme.Type.APIKEY) // 键值对类型
                                .in(SecurityScheme.In.HEADER) // 存在于请求头中
                                .description("请在此处输入登录后获取的有效 Token")));
    }
}
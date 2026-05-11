package com.chanyong.gunpla.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Gunpla Inventory API")
                .description("""
                    건프라 컬렉션 관리 서비스 API

                    ## 인증 방법
                    1. 아래 소셜 로그인 링크로 접속
                    2. 로그인 완료 후 URL의 `?accessToken=` 값 복사
                    3. 우상단 **Authorize** 버튼 클릭 후 붙여넣기

                    **소셜 로그인:**
                    - [Google 로그인](https://vibe.chanyongyang.com/oauth2/authorization/google)
                    - [Kakao 로그인](https://vibe.chanyongyang.com/oauth2/authorization/kakao)
                    - [Naver 로그인](https://vibe.chanyongyang.com/oauth2/authorization/naver)
                    """)
                .version("v1"))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}

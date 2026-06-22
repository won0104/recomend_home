package com.ssafy.prj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SSAFY HOME API")
                        .description("""
                                SSAFY HOME REST API 문서입니다.

                                테스트 순서:
                                1. /api/auth/login에 JSON body를 보내 로그인합니다.
                                2. /api/auth/me를 실행해 세션이 유지되는지 확인합니다.
                                3. 같은 브라우저 세션으로 회원, 관심지역, 게시판, 공지, 주택 관리자 API를 테스트합니다.

                                보호된 API는 Spring Security 세션 쿠키를 사용합니다.
                                """)
                        .version("1.0.0")
                        .license(new License().name("SSAFY")))
                .addTagsItem(new Tag().name("인증").description("현재 로그인한 Spring Security 사용자를 확인합니다."))
                .addTagsItem(new Tag().name("회원").description("회원 가입, 조회, 수정, 삭제와 관리자 회원 관리를 제공합니다."))
                .addTagsItem(new Tag().name("관심지역").description("로그인 사용자의 관심지역을 조회, 등록, 삭제합니다."))
                .addTagsItem(new Tag().name("지역").description("시도, 시군구, 읍면동 지역 코드를 조회합니다."))
                .addTagsItem(new Tag().name("부동산 거래").description("부동산 실거래 조회와 요약 통계를 제공합니다."))
                .addTagsItem(new Tag().name("상권").description("상권 점포와 업종 분류 코드를 조회합니다."))
                .addTagsItem(new Tag().name("게시판").description("공유 게시판 글 조회, 작성, 수정, 삭제를 제공합니다."))
                .addTagsItem(new Tag().name("공지사항").description("공지사항 조회와 관리자 작성, 수정, 삭제를 제공합니다."))
                .addTagsItem(new Tag().name("주택").description("주택 정보와 최근 거래 내역을 조회하고 관리자 수정을 제공합니다."));
    }
}

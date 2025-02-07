package com.ll.TeamProject.domain.user.controller;

import com.ll.TeamProject.domain.user.entity.Authentication;
import com.ll.TeamProject.domain.user.entity.SiteUser;
import com.ll.TeamProject.domain.user.enums.AuthType;
import com.ll.TeamProject.domain.user.service.AuthenticationService;
import com.ll.TeamProject.domain.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserService userService;
    @Autowired
    AuthenticationService authenticationService;

    @Test
    @DisplayName("관리자 로그인")
    void adminLogin() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        post("/admin/login")
                                .content("""
                                            {
                                                "username": "admin",
                                                "password": "admin"
                                            }
                                            """.stripIndent()
                                )
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());

        SiteUser actor = userService.findByUsername("admin").get();
        Authentication authentication = authenticationService.findByUserId(actor.getId()).get();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getUserId()).isEqualTo(actor.getId());
        assertThat(authentication.getAuthType()).isEqualTo(AuthType.LOCAL);
        assertThat(authentication.getLastLogin()).isNotNull();

        resultActions
                .andExpect(handler().handlerType(AdminController.class))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%s님 환영합니다.".formatted(actor.getNickname())))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.item").exists())
                .andExpect(jsonPath("$.data.item.id").value(actor.getId()))
                .andExpect(jsonPath("$.data.item.createDate").value(Matchers.startsWith(actor.getCreateDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.data.item.modifyDate").value(Matchers.startsWith(actor.getModifyDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.data.item.username").value(actor.getUsername()))
                .andExpect(jsonPath("$.data.item.nickname").value(actor.getNickname()))
                .andExpect(jsonPath("$.data.apiKey").value(actor.getApiKey()))
                .andExpect(jsonPath("$.data.accessToken").exists());

        resultActions.andExpect(
                result -> {
                    Cookie accessTokenCookie = result.getResponse().getCookie("accessToken");
                    assertThat(accessTokenCookie.getValue()).isNotBlank();
                    assertThat(accessTokenCookie.getPath()).isEqualTo("/");
                    assertThat(accessTokenCookie.isHttpOnly()).isTrue();

                    Cookie apiKeyCookie = result.getResponse().getCookie("apiKey");
                    assertThat(apiKeyCookie.getValue()).isEqualTo(actor.getApiKey());
                    assertThat(apiKeyCookie.getPath()).isEqualTo("/");
                    assertThat(apiKeyCookie.isHttpOnly()).isTrue();
                }
        );
    }

    @Test
    @DisplayName("회원 목록 조회")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void userList() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/admin")
                                .param("page", "1")
                                .param("pageSize", "3")
                )
                .andDo(print());

        Page<SiteUser> userPage = userService.findUsers("", "", 1, 3);

        resultActions
                .andExpect(handler().handlerType(AdminController.class))
                .andExpect(handler().methodName("users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPageNumber").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(userPage.getTotalPages()))
                .andExpect(jsonPath("$.data.totalItems").value(userPage.getTotalElements()));

        List<SiteUser> users = userPage.getContent();

        for (int i = 0; i < users.size(); i++) {
            SiteUser user = users.get(i);
            resultActions
                    .andExpect(jsonPath("$.data.items[%d].id".formatted(i)).value(user.getId()))
                    .andExpect(jsonPath("$.data.items[%d].username".formatted(i)).value(user.getUsername()))
                    .andExpect(jsonPath("$.data.items[%d].createDate".formatted(i)).value(Matchers.startsWith(user.getCreateDate().toString().substring(0, 25))))
                    .andExpect(jsonPath("$.data.items[%d].modifyDate".formatted(i)).value(Matchers.startsWith(user.getModifyDate().toString().substring(0, 25))))
                    .andExpect(jsonPath("$.data.items[%d].email".formatted(i)).value(user.getEmail()))
                    .andExpect(jsonPath("$.data.items[%d].nickname".formatted(i)).value(user.getNickname()));
        }
    }

    @Test
    @DisplayName("회원 목록 조회 - 일반 회원 접근")
    @WithMockUser(username = "test", roles = "USER")
    void t5() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        get("/admin")
                )
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("로그아웃")
    void logout() throws Exception {
        ResultActions resultActions = mvc
                .perform(
                        delete("/admin/logout")
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("로그아웃 되었습니다."))
                .andExpect(result -> {
                    Cookie accessTokenCookie = result.getResponse().getCookie("accessToken");
                    assertThat(accessTokenCookie.getValue()).isEmpty();
                    assertThat(accessTokenCookie.getMaxAge()).isEqualTo(0);
                    assertThat(accessTokenCookie.getPath()).isEqualTo("/");
                    assertThat(accessTokenCookie.isHttpOnly()).isTrue();

                    Cookie apiKeyCookie = result.getResponse().getCookie("apiKey");
                    assertThat(apiKeyCookie.getValue()).isEmpty();
                    assertThat(apiKeyCookie.getMaxAge()).isEqualTo(0);
                    assertThat(apiKeyCookie.getPath()).isEqualTo("/");
                    assertThat(apiKeyCookie.isHttpOnly()).isTrue();
                });
    }

    @Test
    @DisplayName("사용자 탈퇴")
    void deleteUser() throws Exception {
        SiteUser actor = userService.findByUsername("user1").get();
        String actorAuthToken = userService.genAuthToken(actor);

        ResultActions resultActions = mvc
                .perform(
                        delete("/user/2")
                                .header("Authorization", "Bearer " + actorAuthToken)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("회원정보가 삭제되었습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(actor.getId()))
                .andExpect(jsonPath("$.data.createDate").value(Matchers.startsWith(actor.getCreateDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.data.modifyDate").value(Matchers.startsWith(actor.getModifyDate().toString().substring(0, 25))))
                .andExpect(jsonPath("$.data.nickname").value("탈퇴한 사용자"));
    }

}
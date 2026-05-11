package com.chanyong.gunpla.user.controller;

import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.global.response.ApiResponse;
import com.chanyong.gunpla.user.dto.NicknameUpdateRequest;
import com.chanyong.gunpla.user.dto.UserResponse;
import com.chanyong.gunpla.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 내 계정 정보 조회 및 수정 API.
 * JWT 인증이 필요하며 본인 정보만 접근 가능하다.
 */
@Tag(name = "User", description = "내 계정 정보 조회 및 수정 (JWT 인증 필요)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 계정 정보를 조회한다.
     *
     * @param principal 현재 로그인 유저
     * @return 유저 정보 (id, email, nickname, provider, createdAt)
     */
    @Operation(summary = "내 정보 조회", description = "로그인된 유저의 계정 정보를 반환한다.")
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.of(userService.getMe(principal.getId()));
    }

    /**
     * 닉네임을 수정한다.
     *
     * @param principal 현재 로그인 유저
     * @param req       변경할 닉네임 (최대 50자)
     * @return 수정된 유저 정보
     */
    @Operation(summary = "닉네임 수정", description = "최대 50자. 공백 불가.")
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateNickname(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody @Valid NicknameUpdateRequest req
    ) {
        return ApiResponse.of(userService.updateNickname(principal.getId(), req));
    }
}

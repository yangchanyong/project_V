package com.chanyong.gunpla.user.controller;

import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.global.response.ApiResponse;
import com.chanyong.gunpla.user.dto.NicknameUpdateRequest;
import com.chanyong.gunpla.user.dto.UserResponse;
import com.chanyong.gunpla.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.of(userService.getMe(principal.getId()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateNickname(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody @Valid NicknameUpdateRequest req
    ) {
        return ApiResponse.of(userService.updateNickname(principal.getId(), req));
    }
}

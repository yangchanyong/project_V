package com.chanyong.gunpla.global.auth.oauth2;

import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.user.entity.User;
import com.chanyong.gunpla.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Google OIDC 소셜 로그인 처리 서비스.
 * OIDC ID Token의 sub claim을 providerId로 사용해 계정을 식별한다.
 */
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    /**
     * Google OIDC 로그인 후 유저 정보를 로드한다.
     * 기존 계정이 없으면 신규 유저를 생성한다.
     *
     * @param userRequest OIDC 로그인 요청 정보
     * @return 인증된 유저의 {@link UserPrincipal}
     */
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        // Google OIDC: subject = stable unique ID (sub claim)
        String providerId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String nickname = oidcUser.getFullName();

        User user = userRepository.findByProviderAndProviderId("GOOGLE", providerId)
            .orElseGet(() -> {
                log.info("New Google OIDC user: providerId={}", providerId);
                return userRepository.save(User.builder()
                    .email(email)
                    .nickname(nickname != null ? nickname : "Google사용자")
                    .provider("GOOGLE")
                    .providerId(providerId)
                    .role("USER")
                    .build());
            });

        return UserPrincipal.of(user);
    }
}

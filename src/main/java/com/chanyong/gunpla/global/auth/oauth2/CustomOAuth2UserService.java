package com.chanyong.gunpla.global.auth.oauth2;

import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.user.entity.User;
import com.chanyong.gunpla.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Kakao · Naver 소셜 로그인 처리 서비스 (non-OIDC).
 * 로그인 성공 시 DB에서 기존 계정을 찾거나 신규 계정을 생성한 뒤 {@link UserPrincipal}을 반환한다.
 */
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * 소셜 로그인 후 유저 정보를 로드한다.
     * provider별 attribute 구조가 다르므로 {@link OAuthAttributes}에서 정규화한다.
     *
     * @param userRequest OAuth2 로그인 요청 정보
     * @return 인증된 유저의 {@link UserPrincipal}
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google | kakao | naver
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, oAuth2User.getAttributes());

        User user = userRepository.findByProviderAndProviderId(attributes.provider(), attributes.providerId())
            .orElseGet(() -> {
                log.info("New OAuth2 user: provider={}, providerId={}", attributes.provider(), attributes.providerId());
                return userRepository.save(User.builder()
                    .email(attributes.email())
                    .nickname(attributes.nickname())
                    .provider(attributes.provider())
                    .providerId(attributes.providerId())
                    .role("USER")
                    .build());
            });

        return UserPrincipal.of(user);
    }

    // provider별 attribute 추출 로직을 내부 record로 분리
    private record OAuthAttributes(String provider, String providerId, String email, String nickname) {

        static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
            return switch (registrationId) {
                case "google" -> fromGoogle(attributes);
                case "kakao" -> fromKakao(attributes);
                case "naver" -> fromNaver(attributes);
                default -> throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
            };
        }

        // Google: attributes에 sub, email, name 직접 포함
        private static OAuthAttributes fromGoogle(Map<String, Object> attrs) {
            return new OAuthAttributes(
                "GOOGLE",
                (String) attrs.get("sub"),
                (String) attrs.get("email"),
                (String) attrs.get("name")
            );
        }

        // Kakao: id가 최상위, profile/account_email은 kakao_account 하위
        @SuppressWarnings("unchecked")
        private static OAuthAttributes fromKakao(Map<String, Object> attrs) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attrs.get("kakao_account");
            Map<String, Object> profile = kakaoAccount != null
                ? (Map<String, Object>) kakaoAccount.get("profile")
                : Map.of();
            String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
            String nickname = profile != null ? (String) profile.get("nickname") : "카카오사용자";

            return new OAuthAttributes(
                "KAKAO",
                String.valueOf(attrs.get("id")),
                email,
                nickname
            );
        }

        // Naver: user-name-attribute=response 이므로 response 맵 하위에 id, email, name
        @SuppressWarnings("unchecked")
        private static OAuthAttributes fromNaver(Map<String, Object> attrs) {
            Map<String, Object> response = (Map<String, Object>) attrs.get("response");
            String name = (String) response.get("name");
            return new OAuthAttributes(
                "NAVER",
                (String) response.get("id"),
                (String) response.get("email"),
                name != null ? name : "네이버사용자"
            );
        }
    }
}

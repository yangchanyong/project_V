package com.chanyong.gunpla.global.auth;

import com.chanyong.gunpla.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class UserPrincipal implements UserDetails, OAuth2User {

    private final Long id;
    private final String email;
    private final String nickname;
    private final Collection<? extends GrantedAuthority> authorities;

    // OAuth2User.getAttributes()용 — CustomOAuth2UserService에서 사용하지 않지만 인터페이스 계약 이행
    private Map<String, Object> attributes = Map.of();

    private UserPrincipal(Long id, String email, String nickname, String role) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public static UserPrincipal of(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getNickname(), user.getRole());
    }

    // --- UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    // UserDetails의 username은 내부 식별자 — userId 문자열
    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    // --- OAuth2User ---

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // OAuth2User의 name은 principal 식별자 — userId 문자열로 통일
    @Override
    public String getName() {
        return String.valueOf(id);
    }
}

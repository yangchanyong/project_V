package com.chanyong.gunpla.user.entity;

import com.chanyong.gunpla.global.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}),
    indexes = @Index(name = "idx_users_email", columnList = "email")
)
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
/**
 * 유저 엔티티.
 * 계정은 (provider, provider_id) 조합으로 식별한다. 같은 email이라도 소셜 프로바이더가 다르면 별도 계정이다.
 * Soft Delete 적용 — 탈퇴 시 deleted_at에 시각이 기록되고 조회에서 자동 제외된다.
 */
public class User extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소셜 로그인 이메일 (참고용, 계정 식별에 사용하지 않음) */
    @Column
    private String email;

    /** 닉네임 (최대 50자) */
    @Column(nullable = false, length = 50)
    private String nickname;

    /** 소셜 로그인 제공자 (google / kakao / naver) */
    @Column(nullable = false, length = 20)
    private String provider;

    /** 소셜 로그인 제공자의 유저 고유 ID */
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    /** 권한 (기본값: ROLE_USER) */
    @Column(nullable = false, length = 10)
    private String role;

    @Builder
    public User(String email, String nickname, String provider, String providerId, String role) {
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    /**
     * 닉네임을 변경한다.
     *
     * @param nickname 변경할 닉네임
     */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 소프트 삭제를 수행한다.
     * 실제 처리는 @SQLDelete가 담당하므로 JPA delete() 호출 시 자동 실행된다.
     */
    public void softDelete() {
    }
}

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
public class User extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

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

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void softDelete() {
        // @SQLDelete가 처리하므로 JPA delete() 호출 시 자동 실행
        // 도메인 메서드는 서비스 레이어에서 entityManager.remove() 트리거용
    }
}

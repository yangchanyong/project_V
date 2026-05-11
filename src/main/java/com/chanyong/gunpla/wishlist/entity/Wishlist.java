package com.chanyong.gunpla.wishlist.entity;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;
import com.chanyong.gunpla.global.entity.BaseTimeEntity;
import com.chanyong.gunpla.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "wishlist",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_wishlist_user_catalog",
        columnNames = {"user_id", "catalog_id"}
    )
)
/**
 * 유저 위시리스트 엔티티.
 * (user_id, catalog_id) 조합에 UNIQUE 제약이 걸려 있어 같은 카탈로그를 중복 추가할 수 없다.
 */
public class Wishlist extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", nullable = false)
    private GunplaCatalog catalog;

    @Column(nullable = false, length = 10)
    private String priority;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Builder
    public Wishlist(User user, GunplaCatalog catalog, String priority, String memo) {
        this.user = user;
        this.catalog = catalog;
        this.priority = priority;
        this.memo = memo;
    }

    /**
     * 우선순위와 메모를 수정한다.
     *
     * @param priority 우선순위
     * @param memo     메모
     */
    public void update(String priority, String memo) {
        this.priority = priority;
        this.memo = memo;
    }
}

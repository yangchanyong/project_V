package com.chanyong.gunpla.collection.entity;

import com.chanyong.gunpla.catalog.entity.GunplaCatalog;
import com.chanyong.gunpla.global.entity.SoftDeletableEntity;
import com.chanyong.gunpla.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "user_collection",
    indexes = {
        @Index(name = "idx_user_collection_user_deleted", columnList = "user_id, deleted_at"),
        @Index(name = "idx_user_collection_build_status", columnList = "build_status")
    }
)
@SQLDelete(sql = "UPDATE user_collection SET deleted_at = NOW(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class UserCollection extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", nullable = false)
    private GunplaCatalog catalog;

    @Enumerated(EnumType.STRING)
    @Column(name = "build_status", nullable = false, length = 20)
    private BuildStatus buildStatus;

    @Column(name = "purchase_price")
    private Integer purchasePrice;

    @Column(name = "purchase_currency", length = 3)
    private String purchaseCurrency;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_place", length = 100)
    private String purchasePlace;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollectionImage> images = new ArrayList<>();

    @Builder
    public UserCollection(User user, GunplaCatalog catalog, BuildStatus buildStatus,
                          Integer purchasePrice, String purchaseCurrency,
                          LocalDate purchaseDate, String purchasePlace, String memo) {
        this.user = user;
        this.catalog = catalog;
        this.buildStatus = buildStatus != null ? buildStatus : BuildStatus.UNBUILT;
        this.purchasePrice = purchasePrice;
        this.purchaseCurrency = purchaseCurrency;
        this.purchaseDate = purchaseDate;
        this.purchasePlace = purchasePlace;
        this.memo = memo;
    }

    public void changeBuildStatus(BuildStatus next) {
        this.buildStatus.validateTransitionTo(next);
        this.buildStatus = next;
    }

    public void update(Integer purchasePrice, String purchaseCurrency,
                       LocalDate purchaseDate, String purchasePlace, String memo) {
        this.purchasePrice = purchasePrice;
        this.purchaseCurrency = purchaseCurrency;
        this.purchaseDate = purchaseDate;
        this.purchasePlace = purchasePlace;
        this.memo = memo;
    }
}

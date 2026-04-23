package com.chanyong.gunpla.collection.entity;

import com.chanyong.gunpla.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "collection_images")
public class CollectionImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private UserCollection collection;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "display_order")
    private int displayOrder = 0;

    @Builder
    public CollectionImage(UserCollection collection, String s3Key, int displayOrder) {
        this.collection = collection;
        this.s3Key = s3Key;
        this.displayOrder = displayOrder;
    }
}

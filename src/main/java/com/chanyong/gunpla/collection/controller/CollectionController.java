package com.chanyong.gunpla.collection.controller;

import com.chanyong.gunpla.collection.dto.*;
import com.chanyong.gunpla.collection.service.CollectionImageService;
import com.chanyong.gunpla.collection.service.CollectionService;
import com.chanyong.gunpla.global.auth.UserPrincipal;
import com.chanyong.gunpla.global.ratelimit.RateLimited;
import com.chanyong.gunpla.global.response.ApiResponse;
import com.chanyong.gunpla.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 내 컬렉션 및 컬렉션 이미지 관리 API.
 * 모든 엔드포인트는 JWT 인증이 필요하며, 본인 컬렉션에만 접근 가능하다.
 */
@Tag(name = "Collection", description = "내 건프라 컬렉션 및 이미지 관리 (JWT 인증 필요)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;
    private final CollectionImageService collectionImageService;

    /**
     * 내 컬렉션 목록을 페이징 + 필터 조건으로 조회한다.
     *
     * @param principal 현재 로그인 유저
     * @param req       buildStatus / grade 필터 조건
     * @param pageable  페이지 정보 (기본 size=20, 최대 100)
     * @return 필터링된 컬렉션 목록 (이미지 포함)
     */
    @Operation(summary = "내 컬렉션 목록 조회", description = "buildStatus / grade 필터와 페이징을 지원한다.")
    @GetMapping
    public PageResponse<CollectionResponse> getCollections(
        @AuthenticationPrincipal UserPrincipal principal,
        @ModelAttribute CollectionSearchRequest req,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return collectionService.getCollections(principal.getId(), req, pageable);
    }

    /**
     * 새 컬렉션을 등록한다.
     *
     * @param principal 현재 로그인 유저
     * @param req       카탈로그 ID, 빌드 상태, 구매 정보 등
     * @return 생성된 컬렉션 ID
     */
    @Operation(summary = "컬렉션 등록", description = "카탈로그 ID와 빌드 상태를 지정해 새 컬렉션을 추가한다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CollectionCreateResponse> createCollection(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody @Valid CollectionCreateRequest req
    ) {
        return ApiResponse.of(collectionService.createCollection(principal.getId(), req));
    }

    /**
     * 컬렉션 단건 상세 조회.
     *
     * @param principal 현재 로그인 유저
     * @param id        컬렉션 PK
     * @return 컬렉션 상세 정보 (이미지 포함)
     * @throws com.chanyong.gunpla.global.exception.BusinessException COLLECTION_NOT_FOUND(404), COLLECTION_ACCESS_DENIED(403)
     */
    @Operation(summary = "컬렉션 단건 조회", description = "이미지 목록을 포함해 상세 정보를 반환한다.")
    @GetMapping("/{id}")
    public ApiResponse<CollectionResponse> getCollection(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        return ApiResponse.of(collectionService.getCollection(principal.getId(), id));
    }

    /**
     * 컬렉션 구매 정보·메모를 수정한다.
     *
     * @param principal 현재 로그인 유저
     * @param id        컬렉션 PK
     * @param req       수정할 구매 정보
     * @return 수정된 컬렉션 ID
     */
    @Operation(summary = "컬렉션 수정", description = "구매 정보와 메모를 수정한다.")
    @PatchMapping("/{id}")
    public ApiResponse<CollectionCreateResponse> updateCollection(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody CollectionUpdateRequest req
    ) {
        return ApiResponse.of(collectionService.updateCollection(principal.getId(), id, req));
    }

    /**
     * 컬렉션의 빌드 상태를 변경한다.
     * 허용 전이: 순방향 1단계 또는 역방향 1단계 복구. 단계 건너뛰기는 400 반환.
     *
     * @param principal 현재 로그인 유저
     * @param id        컬렉션 PK
     * @param req       변경할 빌드 상태
     * @return 변경된 컬렉션 ID + 새 빌드 상태
     */
    @Operation(summary = "빌드 상태 변경", description = "순방향 1단계 또는 역방향 1단계만 허용. 건너뛰기 시 400 반환.")
    @PatchMapping("/{id}/build-status")
    public ApiResponse<BuildStatusUpdateResponse> changeStatus(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody @Valid BuildStatusUpdateRequest req
    ) {
        return ApiResponse.of(collectionService.changeStatus(principal.getId(), id, req.buildStatus()));
    }

    /**
     * 컬렉션을 소프트 삭제한다. 연결된 S3 이미지도 함께 삭제된다.
     *
     * @param principal 현재 로그인 유저
     * @param id        컬렉션 PK
     */
    @Operation(summary = "컬렉션 삭제", description = "소프트 삭제. 연결된 S3 이미지도 함께 삭제된다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCollection(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        collectionService.deleteCollection(principal.getId(), id);
    }

    /**
     * S3 이미지 업로드용 Presigned URL을 발급한다.
     * 유저당 분당 20건으로 Rate Limit이 걸려있다.
     *
     * @param principal 현재 로그인 유저
     * @param id        컬렉션 PK
     * @param req       파일명, contentType, 파일 크기 (최대 10MB, image/jpeg·png·webp 허용)
     * @return Presigned PUT URL, s3Key, 만료 시간(초)
     */
    @Operation(summary = "이미지 업로드 URL 발급", description = "S3 PUT용 Presigned URL 발급. 유저당 분당 20건 제한. 허용 타입: image/jpeg·png·webp, 최대 10MB.")
    @RateLimited(limit = 20)
    @PostMapping("/{id}/images/presigned-url")
    public ApiResponse<PresignedUrlResponse> generatePresignedUrl(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody @Valid PresignedUrlRequest req
    ) {
        return ApiResponse.of(collectionImageService.generatePresignedUrl(principal.getId(), id, req));
    }

    /**
     * S3 업로드 완료 후 이미지 메타데이터를 DB에 저장한다.
     * S3에 실제 파일이 없으면 FILE_UPLOAD_VALIDATION_FAILED(400)를 반환한다.
     *
     * @param principal 현재 로그인 유저
     * @param id        컬렉션 PK
     * @param req       s3Key, 표시 순서
     * @return 생성된 이미지 ID
     */
    @Operation(summary = "이미지 메타데이터 저장", description = "S3 업로드 완료 후 호출. S3에 파일이 없으면 400 반환.")
    @PostMapping("/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImageSaveResponse> saveImage(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody @Valid ImageSaveRequest req
    ) {
        return ApiResponse.of(collectionImageService.saveImage(principal.getId(), id, req));
    }

    /**
     * 이미지를 S3와 DB에서 함께 삭제한다.
     *
     * @param principal 현재 로그인 유저
     * @param id        컬렉션 PK
     * @param imageId   삭제할 이미지 PK
     */
    @Operation(summary = "이미지 삭제", description = "S3와 DB에서 이미지를 함께 삭제한다.")
    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @PathVariable Long imageId
    ) {
        collectionImageService.deleteImage(principal.getId(), id, imageId);
    }
}

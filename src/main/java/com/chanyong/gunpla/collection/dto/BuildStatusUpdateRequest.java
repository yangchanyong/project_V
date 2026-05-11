package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.BuildStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 빌드 상태 변경 요청 DTO.
 *
 * @param buildStatus 변경할 빌드 상태 (필수)
 */
public record BuildStatusUpdateRequest(@NotNull BuildStatus buildStatus) {}

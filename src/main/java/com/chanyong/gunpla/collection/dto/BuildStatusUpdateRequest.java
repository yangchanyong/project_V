package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.BuildStatus;
import jakarta.validation.constraints.NotNull;

public record BuildStatusUpdateRequest(@NotNull BuildStatus buildStatus) {}

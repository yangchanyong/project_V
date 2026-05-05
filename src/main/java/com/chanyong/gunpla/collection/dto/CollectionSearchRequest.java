package com.chanyong.gunpla.collection.dto;

import com.chanyong.gunpla.collection.entity.BuildStatus;

public record CollectionSearchRequest(BuildStatus buildStatus, String grade) {}

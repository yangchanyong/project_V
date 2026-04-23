package com.chanyong.gunpla.collection.controller;

import com.chanyong.gunpla.collection.service.CollectionImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionImageController {

    private final CollectionImageService collectionImageService;
}

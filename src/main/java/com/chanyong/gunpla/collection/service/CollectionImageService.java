package com.chanyong.gunpla.collection.service;

import com.chanyong.gunpla.collection.repository.CollectionImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollectionImageService {

    private final CollectionImageRepository collectionImageRepository;
}

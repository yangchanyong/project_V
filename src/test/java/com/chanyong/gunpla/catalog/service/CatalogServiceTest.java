package com.chanyong.gunpla.catalog.service;

import com.chanyong.gunpla.catalog.dto.CatalogResponse;
import com.chanyong.gunpla.catalog.dto.CatalogSearchRequest;
import com.chanyong.gunpla.catalog.entity.GunplaCatalog;
import com.chanyong.gunpla.catalog.repository.CatalogQueryRepository;
import com.chanyong.gunpla.catalog.repository.CatalogRepository;
import com.chanyong.gunpla.global.exception.BusinessException;
import com.chanyong.gunpla.global.exception.ErrorCode;
import com.chanyong.gunpla.global.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @InjectMocks
    private CatalogService catalogService;

    @Mock
    private CatalogRepository catalogRepository;

    @Mock
    private CatalogQueryRepository catalogQueryRepository;

    @Test
    void getCatalogs_위임_확인() {
        CatalogSearchRequest req = new CatalogSearchRequest("HG", null, null);
        Pageable pageable = PageRequest.of(0, 20);
        GunplaCatalog catalog = mock(GunplaCatalog.class);
        given(catalogQueryRepository.search(req, pageable))
            .willReturn(new PageImpl<>(List.of(catalog), pageable, 1));

        PageResponse<CatalogResponse> result = catalogService.getCatalogs(req, pageable);

        assertThat(result.data()).hasSize(1);
    }

    @Test
    void getCatalogs_size_100초과_예외() {
        CatalogSearchRequest req = new CatalogSearchRequest(null, null, null);
        Pageable pageable = PageRequest.of(0, 101);

        assertThatThrownBy(() -> catalogService.getCatalogs(req, pageable))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void getCatalog_정상_조회() {
        GunplaCatalog catalog = mock(GunplaCatalog.class);
        given(catalogRepository.findById(1L)).willReturn(Optional.of(catalog));

        CatalogResponse result = catalogService.getCatalog(1L);

        assertThat(result).isNotNull();
    }

    @Test
    void getCatalog_없는_ID_예외() {
        given(catalogRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getCatalog(999L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CATALOG_NOT_FOUND);
    }
}

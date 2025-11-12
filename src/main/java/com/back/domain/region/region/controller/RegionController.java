package com.back.domain.region.region.controller;

import com.back.domain.region.region.dto.RegionResBody;
import com.back.domain.region.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/regions")
@Tag(name = "Region API", description = "지역 조회 API, 인증없이 접근 가능")
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "지역 목록 조회 API", description = "지역들과 함께 연관된 하위 지역들 목록 조회")
    @GetMapping
    public ResponseEntity<List<RegionResBody>> readRegions() {
        List<RegionResBody> regionResBodyList = regionService.getRegions();
        return ResponseEntity.ok(regionResBodyList);
    }
}

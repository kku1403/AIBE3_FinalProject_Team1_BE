package com.back.domain.region.region.controller;

import com.back.domain.region.region.dto.RegionCreateReqBody;
import com.back.domain.region.region.dto.RegionResBody;
import com.back.domain.region.region.dto.RegionUpdateReqBody;
import com.back.domain.region.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm/regions")
@Tag(name = "Region Admin API", description = "지역 관리자 API, 관리자 인증 필요")
public class RegionAdmController {

    private final RegionService regionService;

    @Operation(summary = "지역 등록 API", description = "parentId = null : 상위 지역 등록, parentId가 있다면 해당 Id의 하위 지역으로 등록")
    @PostMapping
    public ResponseEntity<RegionResBody> createRegion(@Valid @RequestBody RegionCreateReqBody regionCreateReqBody) {
        RegionResBody regionResBody = regionService.createRegion(regionCreateReqBody);
        return ResponseEntity.ok(regionResBody);
    }

    @Operation(summary = "지역 수정 API", description = "지역 이름 수정, 수정된 지역과 함께 연관된 하위 지역들 응답")
    @PatchMapping("/{id}")
    public ResponseEntity<RegionResBody> updateRegion(
            @PathVariable("id") Long regionId,
            @Valid @RequestBody RegionUpdateReqBody regionUpdateReqBody) {
        RegionResBody regionResBody = regionService.updateRegion(regionId, regionUpdateReqBody);
        return ResponseEntity.ok(regionResBody);
    }

    @Operation(summary = "지역 삭제 API", description = "해당 지역과 함께 연관된 하위 지역들도 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegion(@PathVariable("id") Long regionId) {
        regionService.deleteRegion(regionId);
        return ResponseEntity.ok().build();
    }
}

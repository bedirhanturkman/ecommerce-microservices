package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.CreateInventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.dto.StockChangeRequest;
import com.example.inventoryservice.security.PermissionConstants;
import com.example.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize(PermissionConstants.IS_ADMIN_OR_SELLER)
    public InventoryResponse createInventory(
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        return inventoryService.createInventory(request);
    }

    @GetMapping
    @PreAuthorize(PermissionConstants.IS_ADMIN_OR_SELLER)
    public Page<InventoryResponse> getInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        inventoryService.validatePagination(
                page,
                size,
                MAX_PAGE_SIZE
        );

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "productId")
        );

        return inventoryService.getInventory(pageRequest);
    }

    @GetMapping("/{productId}")
    @PreAuthorize(PermissionConstants.IS_USER_OR_ADMIN_OR_SELLER)
    public InventoryResponse getInventoryByProductId(
            @PathVariable String productId
    ) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @PatchMapping("/{productId}/increase")
    @PreAuthorize(PermissionConstants.IS_ADMIN_OR_SELLER)
    public InventoryResponse increaseStock(
            @PathVariable String productId,
            @Valid @RequestBody StockChangeRequest request
    ) {
        return inventoryService.increaseStock(productId, request);
    }

    @PatchMapping("/{productId}/decrease")
    @PreAuthorize(PermissionConstants.IS_ADMIN)
    public InventoryResponse decreaseStock(
            @PathVariable String productId,
            @Valid @RequestBody StockChangeRequest request
    ) {
        return inventoryService.decreaseStock(productId, request);
    }
}

package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.CreateInventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.dto.StockChangeRequest;
import com.example.inventoryservice.security.PermissionConstants;
import com.example.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize(PermissionConstants.IS_ADMIN_OR_SELLER)
    public InventoryResponse createInventory(
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        return inventoryService.createInventory(request);
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
package com.example.inventoryservice.mapper;

import com.example.inventoryservice.document.Inventory;
import com.example.inventoryservice.dto.InventoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(
            target = "availableQuantity",
            expression = "java(calculateAvailableQuantity(inventory))"
    )
    InventoryResponse toInventoryResponse(Inventory inventory);

    default Integer calculateAvailableQuantity(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        int quantity = inventory.getQuantity() == null
                ? 0
                : inventory.getQuantity();

        int reservedQuantity = inventory.getReservedQuantity() == null
                ? 0
                : inventory.getReservedQuantity();

        return quantity - reservedQuantity;
    }
}
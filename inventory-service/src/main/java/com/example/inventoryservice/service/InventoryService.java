package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.CreateInventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.dto.StockChangeRequest;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.exception.InventoryAlreadyExistsException;
import com.example.inventoryservice.exception.InventoryNotFoundException;
import com.example.inventoryservice.exception.InvalidStockQuantityException;
import com.example.inventoryservice.mapper.InventoryMapper;
import com.example.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional
    public InventoryResponse createInventory(
            CreateInventoryRequest request
    ) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new InventoryAlreadyExistsException(request.productId());
        }

        Inventory inventory = Inventory.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .reservedQuantity(0)
                .build();

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        return inventoryMapper.toInventoryResponse(savedInventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(
            String productId
    ) {
        Inventory inventory = findByProductId(productId);

        return inventoryMapper.toInventoryResponse(inventory);
    }

    @Transactional
    public InventoryResponse increaseStock(
            String productId,
            StockChangeRequest request
    ) {
        validateStockChangeQuantity(request.quantity());

        Inventory inventory = findByProductId(productId);

        inventory.setQuantity(
                inventory.getQuantity() + request.quantity()
        );

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        return inventoryMapper.toInventoryResponse(savedInventory);
    }

    @Transactional
    public InventoryResponse decreaseStock(
            String productId,
            StockChangeRequest request
    ) {
        validateStockChangeQuantity(request.quantity());

        Inventory inventory = findByProductId(productId);

        decreaseInventory(
                inventory,
                productId,
                request.quantity()
        );

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        return inventoryMapper.toInventoryResponse(savedInventory);
    }

    @Transactional
    public void decreaseStockFromOrder(
            String productId,
            Integer requestedQuantity
    ) {
        validateStockChangeQuantity(requestedQuantity);

        Inventory inventory = findByProductId(productId);

        decreaseInventory(
                inventory,
                productId,
                requestedQuantity
        );

        inventoryRepository.save(inventory);
    }

    private void decreaseInventory(
            Inventory inventory,
            String productId,
            Integer requestedQuantity
    ) {
        int reservedQuantity =
                inventory.getReservedQuantity() == null
                        ? 0
                        : inventory.getReservedQuantity();

        int availableQuantity =
                inventory.getQuantity() - reservedQuantity;

        if (availableQuantity < requestedQuantity) {
            throw new InsufficientStockException(
                    productId,
                    availableQuantity,
                    requestedQuantity
            );
        }

        inventory.setQuantity(
                inventory.getQuantity() - requestedQuantity
        );
    }

    private Inventory findByProductId(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() ->
                        new InventoryNotFoundException(productId)
                );
    }

    private void validateStockChangeQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidStockQuantityException(
                    "Stock change quantity must be greater than zero"
            );
        }
    }
}
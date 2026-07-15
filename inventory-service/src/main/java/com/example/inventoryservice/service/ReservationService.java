package com.example.inventoryservice.service;

import com.example.commonevents.inventory.ReservedInventoryItem;
import com.example.commonevents.order.OrderCreatedItemEvent;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.entity.ReservationStatus;
import com.example.inventoryservice.exception.ActiveReservationNotFoundException;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.exception.InvalidStockQuantityException;
import com.example.inventoryservice.exception.InventoryNotFoundException;
import com.example.inventoryservice.exception.ReservationAlreadyExistsException;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.InventoryReservationRepository;
import com.example.inventoryservice.service.model.ReservationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    @Transactional
    public ReservationResult reserveStock(
            Long orderId,
            List<OrderCreatedItemEvent> items
    ) {
        if (orderId == null) {
            throw new IllegalArgumentException(
                    "Order id cannot be null"
            );
        }

        Map<String, Integer> requestedQuantities =
                aggregateQuantitiesByProduct(items);

        Instant now = Instant.now();

        List<Inventory> inventoriesToUpdate =
                new ArrayList<>();

        List<InventoryReservation> reservationsToCreate =
                new ArrayList<>();

        List<ReservedInventoryItem> reservedItems =
                new ArrayList<>();

        for (Map.Entry<String, Integer> entry
                : requestedQuantities.entrySet()) {

            String productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();

            validateQuantity(requestedQuantity);

            Inventory inventory = findInventory(productId);

            int currentReserved =
                    getReservedQuantity(inventory);

            int availableQuantity =
                    inventory.getQuantity() - currentReserved;

            if (availableQuantity < requestedQuantity) {
                throw new InsufficientStockException(
                        productId,
                        availableQuantity,
                        requestedQuantity
                );
            }

            InventoryReservation reservation =
                    InventoryReservation.builder()
                            .orderId(orderId)
                            .productId(productId)
                            .quantity(requestedQuantity)
                            .status(ReservationStatus.RESERVED)
                            .reservedAt(now)
                            .build();

            inventory.setReservedQuantity(
                    currentReserved + requestedQuantity
            );

            reservationsToCreate.add(reservation);
            inventoriesToUpdate.add(inventory);

            reservedItems.add(
                    new ReservedInventoryItem(
                            productId,
                            requestedQuantity
                    )
            );
        }

        try {
            reservationRepository.saveAllAndFlush(
                    reservationsToCreate
            );

            inventoryRepository.saveAllAndFlush(
                    inventoriesToUpdate
            );

        } catch (DataIntegrityViolationException exception) {
            throw new ReservationAlreadyExistsException(
                    orderId
            );
        }

        return new ReservationResult(
                orderId,
                List.copyOf(reservedItems),
                now
        );
    }

    @Transactional
    public void confirmReservation(Long orderId) {

        List<InventoryReservation> reservations =
                findActiveReservations(orderId);

        Instant now = Instant.now();

        List<Inventory> inventoriesToUpdate =
                new ArrayList<>();

        for (InventoryReservation reservation : reservations) {

            Inventory inventory =
                    findInventory(reservation.getProductId());

            int reservedQuantity =
                    getReservedQuantity(inventory);

            if (reservedQuantity < reservation.getQuantity()) {
                throw new ActiveReservationNotFoundException(
                        orderId
                );
            }

            inventory.setQuantity(
                    inventory.getQuantity()
                            - reservation.getQuantity()
            );

            inventory.setReservedQuantity(
                    reservedQuantity
                            - reservation.getQuantity()
            );

            reservation.setStatus(
                    ReservationStatus.CONFIRMED
            );

            reservation.setConfirmedAt(now);

            inventoriesToUpdate.add(inventory);
        }

        inventoryRepository.saveAll(inventoriesToUpdate);
        reservationRepository.saveAll(reservations);
    }

    @Transactional
    public void releaseReservation(Long orderId) {

        List<InventoryReservation> reservations =
                findActiveReservations(orderId);

        Instant now = Instant.now();

        List<Inventory> inventoriesToUpdate =
                new ArrayList<>();

        for (InventoryReservation reservation : reservations) {

            Inventory inventory =
                    findInventory(reservation.getProductId());

            int reservedQuantity =
                    getReservedQuantity(inventory);

            if (reservedQuantity < reservation.getQuantity()) {
                throw new ActiveReservationNotFoundException(
                        orderId
                );
            }

            inventory.setReservedQuantity(
                    reservedQuantity
                            - reservation.getQuantity()
            );

            reservation.setStatus(
                    ReservationStatus.RELEASED
            );

            reservation.setReleasedAt(now);

            inventoriesToUpdate.add(inventory);
        }

        inventoryRepository.saveAll(inventoriesToUpdate);
        reservationRepository.saveAll(reservations);
    }

    private List<InventoryReservation> findActiveReservations(
            Long orderId
    ) {
        List<InventoryReservation> reservations =
                reservationRepository
                        .findAllByOrderIdAndStatus(
                                orderId,
                                ReservationStatus.RESERVED
                        );

        if (reservations.isEmpty()) {
            throw new ActiveReservationNotFoundException(
                    orderId
            );
        }

        return reservations;
    }

    private Map<String, Integer> aggregateQuantitiesByProduct(
            List<OrderCreatedItemEvent> items
    ) {
        Map<String, Integer> quantitiesByProduct =
                new LinkedHashMap<>();

        for (OrderCreatedItemEvent item : items) {
            quantitiesByProduct.merge(
                    item.productId(),
                    item.quantity(),
                    Integer::sum
            );
        }

        return quantitiesByProduct;
    }

    private Inventory findInventory(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() ->
                        new InventoryNotFoundException(productId)
                );
    }

    private int getReservedQuantity(Inventory inventory) {
        return inventory.getReservedQuantity() == null
                ? 0
                : inventory.getReservedQuantity();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidStockQuantityException(
                    "Reservation quantity must be greater than zero"
            );
        }
    }
}
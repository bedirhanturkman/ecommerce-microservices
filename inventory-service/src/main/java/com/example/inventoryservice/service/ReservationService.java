package com.example.inventoryservice.service;

import com.example.commonevents.inventory.ReservedInventoryItem;
import com.example.commonevents.order.OrderCreatedItemEvent;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.entity.ReservationStatus;
import com.example.inventoryservice.exception.*;
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
    public boolean confirmReservation(
            Long orderId
    ) {
        List<InventoryReservation> reservations =
                reservationRepository
                        .findAllByOrderIdForUpdate(orderId);

        validateReservationsExist(
                orderId,
                reservations
        );

        ReservationStatus currentStatus =
                resolveCommonStatus(
                        orderId,
                        reservations
                );

        if (currentStatus == ReservationStatus.CONFIRMED) {
            return false;
        }

        if (currentStatus == ReservationStatus.RELEASED) {
            throw new ReservationStateConflictException(
                    orderId,
                    ReservationStatus.RELEASED,
                    ReservationStatus.CONFIRMED
            );
        }

        Instant confirmedAt = Instant.now();

        for (InventoryReservation reservation
                : reservations) {

            Inventory inventory =
                    inventoryRepository
                            .findByProductIdForUpdate(
                                    reservation.getProductId()
                            )
                            .orElseThrow(
                                    () -> new InventoryNotFoundException(
                                            reservation.getProductId()
                                    )
                            );

            int reservationQuantity =
                    reservation.getQuantity();

            int currentQuantity =
                    inventory.getQuantity();

            int currentReservedQuantity =
                    getReservedQuantity(inventory);

            if (currentQuantity < reservationQuantity) {
                throw new IllegalStateException(
                        "Inventory quantity cannot be lower "
                                + "than reservation quantity. productId="
                                + reservation.getProductId()
                );
            }

            if (currentReservedQuantity
                    < reservationQuantity) {

                throw new IllegalStateException(
                        "Reserved quantity cannot be lower "
                                + "than reservation quantity. productId="
                                + reservation.getProductId()
                );
            }

            inventory.setQuantity(
                    currentQuantity
                            - reservationQuantity
            );

            inventory.setReservedQuantity(
                    currentReservedQuantity
                            - reservationQuantity
            );

            reservation.setStatus(
                    ReservationStatus.CONFIRMED
            );

            reservation.setConfirmedAt(
                    confirmedAt
            );
        }

        /*
         * Reservation ve Inventory entity'leri managed durumda.
         * Transaction commit sırasında dirty checking ile
         * değişiklikler otomatik yazılır.
         */
        return true;
    }

    @Transactional
    public boolean releaseReservation(
            Long orderId
    ) {
        List<InventoryReservation> reservations =
                reservationRepository
                        .findAllByOrderIdForUpdate(orderId);

        validateReservationsExist(
                orderId,
                reservations
        );

        ReservationStatus currentStatus =
                resolveCommonStatus(
                        orderId,
                        reservations
                );

        if (currentStatus == ReservationStatus.RELEASED) {
            return false;
        }

        if (currentStatus == ReservationStatus.CONFIRMED) {
            throw new ReservationStateConflictException(
                    orderId,
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.RELEASED
            );
        }

        Instant releasedAt = Instant.now();

        for (InventoryReservation reservation
                : reservations) {

            Inventory inventory =
                    inventoryRepository
                            .findByProductIdForUpdate(
                                    reservation.getProductId()
                            )
                            .orElseThrow(
                                    () -> new InventoryNotFoundException(
                                            reservation.getProductId()
                                    )
                            );

            int reservationQuantity =
                    reservation.getQuantity();

            int currentReservedQuantity =
                    getReservedQuantity(inventory);

            if (currentReservedQuantity
                    < reservationQuantity) {

                throw new IllegalStateException(
                        "Reserved quantity cannot be lower "
                                + "than reservation quantity. productId="
                                + reservation.getProductId()
                );
            }

            /*
             * Release işleminde gerçek quantity azalmaz.
             * Yalnızca ayrılmış miktar serbest bırakılır.
             */
            inventory.setReservedQuantity(
                    currentReservedQuantity
                            - reservationQuantity
            );

            reservation.setStatus(
                    ReservationStatus.RELEASED
            );

            reservation.setReleasedAt(
                    releasedAt
            );
        }

        return true;
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

    private void validateReservationsExist(
            Long orderId,
            List<InventoryReservation> reservations
    ) {
        if (reservations == null
                || reservations.isEmpty()) {

            throw new ActiveReservationNotFoundException(
                    orderId
            );
        }
    }

    private ReservationStatus resolveCommonStatus(
            Long orderId,
            List<InventoryReservation> reservations
    ) {
        ReservationStatus firstStatus =
                reservations.getFirst().getStatus();

        boolean sameStatus =
                reservations.stream()
                        .allMatch(
                                reservation ->
                                        reservation.getStatus()
                                                == firstStatus
                        );

        if (!sameStatus) {
            throw new IllegalStateException(
                    "Reservation records have inconsistent statuses. "
                            + "orderId="
                            + orderId
            );
        }

        return firstStatus;
    }
}
package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.ProcessedEvent;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository
            processedEventRepository;

    @Transactional(
            propagation = Propagation.MANDATORY,
            readOnly = true
    )
    public boolean isProcessed(
            Long orderId
    ) {
        return processedEventRepository
                .existsByOrderId(orderId);
    }

    @Transactional(
            propagation = Propagation.MANDATORY
    )
    public void markAsProcessed(
            Long orderId
    ) {
        ProcessedEvent processedEvent =
                ProcessedEvent.builder()
                        .orderId(orderId)
                        .build();

        processedEventRepository.save(
                processedEvent
        );
    }
}
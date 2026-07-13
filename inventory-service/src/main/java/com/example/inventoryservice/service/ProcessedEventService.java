package com.example.inventoryservice.service;

import com.example.inventoryservice.document.ProcessedEvent;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isProcessed(Long orderId) {
        return processedEventRepository.existsByOrderId(orderId);
    }

    public void markAsProcessed(Long orderId) {
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .orderId(orderId)
                .build();

        processedEventRepository.save(processedEvent);
    }
}
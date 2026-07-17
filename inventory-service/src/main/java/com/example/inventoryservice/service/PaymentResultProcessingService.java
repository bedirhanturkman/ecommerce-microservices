package com.example.inventoryservice.service;

import com.example.commonevents.payment.PaymentFailedEvent;
import com.example.commonevents.payment.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentResultProcessingService {

    private final ReservationService reservationService;

    public boolean processSucceededPayment(
            PaymentSucceededEvent event
    ) {
        return reservationService
                .confirmReservation(
                        event.orderId()
                );
    }

    public boolean processFailedPayment(
            PaymentFailedEvent event
    ) {
        return reservationService
                .releaseReservation(
                        event.orderId()
                );
    }
}
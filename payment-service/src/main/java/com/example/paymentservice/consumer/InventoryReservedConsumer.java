package com.example.paymentservice.consumer;

import com.example.commonevents.inventory.InventoryReservedEvent;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.metrics.PaymentMetrics;
import com.example.paymentservice.service.InventoryReservedEventValidator;
import com.example.paymentservice.service.PaymentEventProcessingService;
import com.example.paymentservice.service.PaymentProcessingService;
import com.example.paymentservice.service.model.PaymentInitializationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservedConsumer {

    private final InventoryReservedEventValidator validator;

    private final PaymentEventProcessingService
            paymentEventProcessingService;

    private final PaymentProcessingService
            paymentProcessingService;

    private final PaymentMetrics paymentMetrics;

    @KafkaListener(
            topics =
                    "${payment.kafka.topics.inventory-reserved}",
            containerFactory =
                    "inventoryReservedKafkaListenerContainerFactory"
    )
    public void consume(
            InventoryReservedEvent event
    ) {
        log.info(
                "InventoryReservedEvent received. "
                        + "orderId={}, customerId={}, amount={}",
                event.orderId(),
                event.customerId(),
                event.totalAmount()
        );

        validator.validate(event);

        PaymentInitializationResult initialization =
                paymentEventProcessingService
                        .initializePayment(event);

        if (!initialization.created()) {
            paymentMetrics
                    .incrementPaymentsAlreadyInitialized();

            log.info(
                    "InventoryReservedEvent already processed. "
                            + "orderId={}",
                    event.orderId()
            );
        } else {
            paymentMetrics
                    .incrementPaymentsInitialized();

            log.info(
                    "Pending payment created. "
                            + "paymentId={}, orderId={}, amount={}",
                    initialization.paymentId(),
                    initialization.orderId(),
                    event.totalAmount()
            );
        }

        PaymentStatus resultStatus =
                paymentMetrics.recordPaymentProcessing(
                        () -> paymentProcessingService
                                .processPendingPayment(
                                        initialization.paymentId()
                                )
                );

        /*
         * Başarı ve başarısızlık sayaçları yalnızca
         * bu event için yeni bir payment oluşturulduysa artırılır.
         *
         * Böylece tamamlanmış bir payment'a ait event tekrar
         * geldiğinde sayaçlar ikinci kez artmaz.
         */
        if (initialization.created()) {
            if (resultStatus == PaymentStatus.SUCCEEDED) {
                paymentMetrics
                        .incrementPaymentsSucceeded();

            } else if (resultStatus == PaymentStatus.FAILED) {
                paymentMetrics
                        .incrementPaymentsFailed();
            }
        }

        log.info(
                "Payment processing completed. "
                        + "paymentId={}, orderId={}, status={}",
                initialization.paymentId(),
                initialization.orderId(),
                resultStatus
        );
    }
}
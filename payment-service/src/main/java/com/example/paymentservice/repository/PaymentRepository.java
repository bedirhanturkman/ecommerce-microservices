package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<Payment> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select payment
            from Payment payment
            where payment.id = :paymentId
            """) // PESSIMISTIC_WRITE lock sayesinde aynı payment kaydını ikinci bir transaction eş zamanlı olarak işleyemez. Bu özellikle ileride aynı müşteriden iki kez para çekilmesini engellemek açısından önemlidir.
    Optional<Payment> findByIdForUpdate(
            @Param("paymentId") Long paymentId
    );
}
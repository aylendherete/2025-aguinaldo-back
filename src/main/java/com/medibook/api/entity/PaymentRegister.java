package com.medibook.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_register")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "turn_id", nullable = false, unique = true)
    private UUID turnId;

    @CreationTimestamp
    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "payment_amount", nullable = true)
    private Double paymentAmount;

    @Column(name = "method", nullable = true)
    private String method;

    @Column(name = "copayment_amount", nullable = true)
    private Double copaymentAmount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turn_id", insertable = false, updatable = false, unique = true
    )
    private TurnAssigned turn;
}

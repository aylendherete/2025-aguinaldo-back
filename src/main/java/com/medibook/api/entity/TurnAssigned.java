package com.medibook.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "turns_assigned")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnAssigned {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private User patient;


    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "status", nullable = false)
    private String status; // Ej: SCHEDULED, COMPLETED, CANCELED, NO_SHOW
    
    @Column(name = "motive", columnDefinition = "text")
    private String motive;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_register_id")
    private PaymentRegister paymentRegister;
}

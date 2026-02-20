package com.medibook.api.service;

import com.medibook.api.entity.BadgeType.BadgeCategory;
import com.medibook.api.model.BadgeMetadata;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BadgeMetadataService {

    private final Map<String, BadgeMetadata> doctorBadgeMetadata = new HashMap<>();
    private final Map<String, BadgeMetadata> patientBadgeMetadata = new HashMap<>();

    public BadgeMetadataService() {
        initializeDoctorBadgeMetadata();
        initializePatientBadgeMetadata();
    }

    private void initializeDoctorBadgeMetadata() {
        doctorBadgeMetadata.put("DOCTOR_EMPATHETIC_DOCTOR", BadgeMetadata.builder()
                .badgeType("DOCTOR_EMPATHETIC_DOCTOR")
                .category(BadgeCategory.QUALITY_OF_CARE)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Médico Empático")
                .description("Destaca por su empatía y comprensión hacia los pacientes")
                .icon("Psychology")
                .color("#E91E63")
                .criteria("Recibe 25 menciones positivas de empatía en total")
                .build());

        doctorBadgeMetadata.put("DOCTOR_EXCEPTIONAL_COMMUNICATOR", BadgeMetadata.builder()
                .badgeType("DOCTOR_EXCEPTIONAL_COMMUNICATOR")
                .category(BadgeCategory.QUALITY_OF_CARE)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Comunicador Excepcional")
                .description("Reconocido por su buena comunicación con pacientes")
                .icon("Chat")
                .color("#4CAF50")
                .criteria("Recibe 25 menciones positivas de comunicación en total")
                .build());

        doctorBadgeMetadata.put("DOCTOR_DETAILED_DIAGNOSTICIAN", BadgeMetadata.builder()
                .badgeType("DOCTOR_DETAILED_DIAGNOSTICIAN")
                .category(BadgeCategory.QUALITY_OF_CARE)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Historiador Detallado")
                .description("Completa historias médicas de los pacientes")
                .icon("LibraryBooks")
                .color("#795548")
                .criteria("Completa 60+ historias médicas")
                .build());

        doctorBadgeMetadata.put("DOCTOR_PUNCTUALITY_PROFESSIONAL", BadgeMetadata.builder()
                .badgeType("DOCTOR_PUNCTUALITY_PROFESSIONAL")
                .category(BadgeCategory.QUALITY_OF_CARE)
                .rarity(BadgeMetadata.BadgeRarity.COMMON)
                .name("Puntualidad Profesional")
                .description("Siempre a tiempo, respetando el horario de los pacientes")
                .icon("Schedule")
                .color("#2196F3")
                .criteria("Recibe 20 menciones positivas de puntualidad en total")
                .build());

        doctorBadgeMetadata.put("DOCTOR_COMPLETE_DOCUMENTER", BadgeMetadata.builder()
                .badgeType("DOCTOR_COMPLETE_DOCUMENTER")
                .category(BadgeCategory.PROFESSIONALISM)
                .rarity(BadgeMetadata.BadgeRarity.COMMON)
                .name("Documentador Completo")
                .description("Completa historias médicas de los pacientes")
                .icon("Assignment")
                .color("#607D8B")
                .criteria("Completa 35+ historias médicas")
                .build());

        doctorBadgeMetadata.put("DOCTOR_CONSISTENT_PROFESSIONAL", BadgeMetadata.builder()
                .badgeType("DOCTOR_CONSISTENT_PROFESSIONAL")
                .category(BadgeCategory.CONSISTENCY)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Profesional Consistente")
                .description("Logra un alto nivel de asistencia")
                .icon("VerifiedUser")
                .color("#3F51B5")
                .criteria("Completa 80+ turnos con menos del 15% de cancelaciones")
                .build());

        doctorBadgeMetadata.put("DOCTOR_AGILE_RESPONDER", BadgeMetadata.builder()
                .badgeType("DOCTOR_AGILE_RESPONDER")
                .category(BadgeCategory.PROFESSIONALISM)
                .rarity(BadgeMetadata.BadgeRarity.COMMON)
                .name("Agenda Abierta")
                .description("Responde a solicitudes de modificación de turnos")
                .icon("CalendarToday")
                .color("#FFC107")
                .criteria("Responde a 8+ solicitudes de modificación")
                .build());

        doctorBadgeMetadata.put("DOCTOR_RELATIONSHIP_BUILDER", BadgeMetadata.builder()
                .badgeType("DOCTOR_RELATIONSHIP_BUILDER")
                .category(BadgeCategory.PROFESSIONALISM)
                .rarity(BadgeMetadata.BadgeRarity.EPIC)
                .name("Constructor de Relaciones")
                .description("Atiende a una amplia variedad de pacientes")
                .icon("People")
                .color("#00BCD4")
                .criteria("25+ pacientes distintos solicitan turnos")
                .build());

        doctorBadgeMetadata.put("DOCTOR_TOP_SPECIALIST", BadgeMetadata.builder()
                .badgeType("DOCTOR_TOP_SPECIALIST")
                .category(BadgeCategory.CONSISTENCY)
                .rarity(BadgeMetadata.BadgeRarity.EPIC)
                .name("Especialista TOP")
                .description("Entre los mejores especialistas")
                .icon("EmojiEvents")
                .color("#FF5722")
                .criteria("Sus últimas 35 calificaciones tienen puntaje 4 o más")
                .build());

        doctorBadgeMetadata.put("DOCTOR_MEDICAL_LEGEND", BadgeMetadata.builder()
                .badgeType("DOCTOR_MEDICAL_LEGEND")
                .category(BadgeCategory.CONSISTENCY)
                .rarity(BadgeMetadata.BadgeRarity.LEGENDARY)
                .name("Leyenda Médica")
                .description("Ha alcanzado el más alto nivel de reconocimiento")
                .icon("Star")
                .color("#9C27B0")
                .criteria("Activa Comunicador Excepcional, Médico Empático y Puntualidad Profesional")
                .build());

        doctorBadgeMetadata.put("DOCTOR_ALWAYS_AVAILABLE", BadgeMetadata.builder()
                .badgeType("DOCTOR_ALWAYS_AVAILABLE")
                .category(BadgeCategory.CONSISTENCY)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Siempre Disponible")
                .description("Ofrece amplia disponibilidad para sus pacientes")
                .icon("AccessTime")
                .color("#8BC34A")
                .criteria("Disponibilidad en 4+ días/semana")
                .build());
    }

    private void initializePatientBadgeMetadata() {
        patientBadgeMetadata.put("PATIENT_MEDIBOOK_WELCOME", BadgeMetadata.builder()
                .badgeType("PATIENT_MEDIBOOK_WELCOME")
                .category(BadgeCategory.WELCOME)
                .rarity(BadgeMetadata.BadgeRarity.COMMON)
                .name("Bienvenido a MediBook")
                .description("Tu primer paso en el camino de la salud digital")
                .icon("WavingHand")
                .color("#4CAF50")
                .criteria("Ten tu primer turno")
                .build());

        patientBadgeMetadata.put("PATIENT_COMMITTED_PATIENT", BadgeMetadata.builder()
                .badgeType("PATIENT_COMMITTED_PATIENT")
                .category(BadgeCategory.PREVENTIVE_CARE)
                .rarity(BadgeMetadata.BadgeRarity.COMMON)
                .name("Paciente Comprometido")
                .description("Cuidando de tu salud")
                .icon("ThumbUp")
                .color("#FF9800")
                .criteria("Completa 5+ turnos")
                .build());

        patientBadgeMetadata.put("PATIENT_CONTINUOUS_FOLLOWUP", BadgeMetadata.builder()
                .badgeType("PATIENT_CONTINUOUS_FOLLOWUP")
                .category(BadgeCategory.PREVENTIVE_CARE)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Seguimiento Continuo")
                .description("Mantienes un seguimiento con tu doctor")
                .icon("Refresh")
                .color("#9C27B0")
                .criteria("Completa 3+ turnos con el mismo doctor")
                .build());

        patientBadgeMetadata.put("PATIENT_CONSTANT_PATIENT", BadgeMetadata.builder()
                .badgeType("PATIENT_CONSTANT_PATIENT")
                .category(BadgeCategory.PREVENTIVE_CARE)
                .rarity(BadgeMetadata.BadgeRarity.EPIC)
                .name("Paciente Constante")
                .description("Gracias por confiar en MediBook")
                .icon("Repeat")
                .color("#3F51B5")
                .criteria("Completa 15+ turnos")
                .build());

        patientBadgeMetadata.put("PATIENT_EXEMPLARY_PUNCTUALITY", BadgeMetadata.builder()
                .badgeType("PATIENT_EXEMPLARY_PUNCTUALITY")
                .category(BadgeCategory.ACTIVE_COMMITMENT)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Puntualidad Ejemplar")
                .description("Llegas a tiempo a tus turnos")
                .icon("Schedule")
                .color("#8BC34A")
                .criteria("Recibe 10+ menciones de puntualidad de tus médicos")
                .build());

        patientBadgeMetadata.put("PATIENT_SMART_PLANNER", BadgeMetadata.builder()
                .badgeType("PATIENT_SMART_PLANNER")
                .category(BadgeCategory.ACTIVE_COMMITMENT)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Planificador")
                .description("Reservas tus turnos médicos con anticipación")
                .icon("CalendarToday")
                .color("#00BCD4")
                .criteria("Reserva 10+ turnos con al menos 24 horas de anticipación")
                .build());

        patientBadgeMetadata.put("PATIENT_EXCELLENT_COLLABORATOR", BadgeMetadata.builder()
                .badgeType("PATIENT_EXCELLENT_COLLABORATOR")
                .category(BadgeCategory.ACTIVE_COMMITMENT)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Colaborador Excelente")
                .description("Colaboras positivamente en tus procesos de atención")
                .icon("People")
                .color("#FFC107")
                .criteria("Recibe 10+ menciones positivas de colaboración de tus médicos")
                .build());

        patientBadgeMetadata.put("PATIENT_ALWAYS_PREPARED", BadgeMetadata.builder()
                .badgeType("PATIENT_ALWAYS_PREPARED")
                .category(BadgeCategory.ACTIVE_COMMITMENT)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Siempre Preparado")
                .description("Vienes preparado a las consultas")
                .icon("Assignment")
                .color("#607D8B")
                .criteria("Sube 10+ documentos requeridos por tus doctores")
                .build());

        patientBadgeMetadata.put("PATIENT_RESPONSIBLE_EVALUATOR", BadgeMetadata.builder()
                .badgeType("PATIENT_RESPONSIBLE_EVALUATOR")
                .category(BadgeCategory.ACTIVE_COMMITMENT)
                .rarity(BadgeMetadata.BadgeRarity.RARE)
                .name("Evaluador Responsable")
                .description("Proporcionas feedback constructivo")
                .icon("Star")
                .color("#E91E63")
                .criteria("Deja 10+ evaluaciones con calificación promedio entre 3 y 5.0")
                .build());

        patientBadgeMetadata.put("PATIENT_EXCELLENCE_MODEL", BadgeMetadata.builder()
                .badgeType("PATIENT_EXCELLENCE_MODEL")
                .category(BadgeCategory.CLINICAL_EXCELLENCE)
                .rarity(BadgeMetadata.BadgeRarity.LEGENDARY)
                .name("Modelo a Seguir")
                .description("Eres un paciente ejemplar")
                .icon("Star")
                .color("#FF5722")
                .criteria("Completa 25+ turnos con 4+ otros badges")
                .build());
    }

    public BadgeMetadata getDoctorBadgeMetadata(String badgeType) {
        return doctorBadgeMetadata.get(badgeType);
    }

    public BadgeMetadata getPatientBadgeMetadata(String badgeType) {
        return patientBadgeMetadata.get(badgeType);
    }

    public Map<String, BadgeMetadata> getAllDoctorBadgeMetadata() {
        return new HashMap<>(doctorBadgeMetadata);
    }

    public Map<String, BadgeMetadata> getAllPatientBadgeMetadata() {
        return new HashMap<>(patientBadgeMetadata);
    }
}
package com.medibook.api.service;

import com.medibook.api.dto.Auth.RegisterRequestDTO;
import com.medibook.api.dto.Auth.RegisterResponseDTO;
import com.medibook.api.dto.Auth.SignInRequestDTO;
import com.medibook.api.dto.Auth.SignInResponseDTO;
import com.medibook.api.entity.EmailVerification;
import com.medibook.api.entity.RefreshToken;
import com.medibook.api.entity.User;
import com.medibook.api.mapper.AuthMapper;
import com.medibook.api.mapper.UserMapper;
import com.medibook.api.repository.EmailVerificationRepository;
import com.medibook.api.repository.RefreshTokenRepository;
import com.medibook.api.repository.UserRepository;

import static com.medibook.api.util.DateTimeUtils.ARGENTINA_ZONE;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.security.MessageDigest;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.nio.charset.StandardCharsets;



@Service
@Slf4j
class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final EmailService emailService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JwtService jwtService;

    private static final java.util.Set<String> VALID_SPECIALTIES = java.util.Set.of(
        "ALERGIA E INMUNOLOGÍA",
        "ANATOMÍA PATOLÓGICA",
        "ANESTESIOLOGÍA",
        "ANGIOLOGÍA GENERAL y HEMODINAMIA",
        "CARDIOLOGÍA",
        "CARDIÓLOGO INFANTIL",
        "CIRUGÍA GENERAL",
        "CIRUGÍA CARDIOVASCULAR",
        "CIRUGÍA DE CABEZA Y CUELLO",
        "CIRUGÍA DE TÓRAX (CIRUGÍA TORÁCICA)",
        "CIRUGÍA INFANTIL (CIRUGÍA PEDIÁTRICA)",
        "CIRUGÍA PLÁSTICA Y REPARADORA",
        "CIRUGÍA VASCULAR PERIFÉRICA",
        "CLÍNICA MÉDICA",
        "COLOPROCTOLOGÍA",
        "DERMATOLOGÍA",
        "DIAGNOSTICO POR IMÁGENES",
        "ENDOCRINOLOGÍA",
        "ENDOCRINÓLOGO INFANTIL",
        "FARMACOLOGÍA CLÍNICA",
        "FISIATRÍA (MEDICINA FÍSICA Y REHABILITACIÓN)",
        "GASTROENTEROLOGÍA",
        "GASTROENTERÓLOGO INFANTIL",
        "GENÉTICA MEDICA",
        "GERIATRÍA",
        "GINECOLOGÍA",
        "HEMATOLOGÍA",
        "HEMATÓLOGO INFANTIL",
        "HEMOTERAPIA E INMUNOHEMATOLOGÍA",
        "INFECTOLOGÍA",
        "INFECTÓLOGO INFANTIL",
        "MEDICINA DEL DEPORTE",
        "MEDICINA GENERAL y/o MEDICINA DE FAMILIA",
        "MEDICINA LEGAL",
        "MEDICINA NUCLEAR",
        "MEDICINA DEL TRABAJO",
        "NEFROLOGÍA",
        "NEFRÓLOGO INFANTIL",
        "NEONATOLOGÍA",
        "NEUMONOLOGÍA",
        "NEUMONÓLOGO INFANTIL",
        "NEUROCIRUGÍA",
        "NEUROLOGÍA",
        "NEURÓLOGO INFANTIL",
        "NUTRICIÓN",
        "OBSTETRICIA",
        "OFTALMOLOGÍA",
        "ONCOLOGÍA",
        "ONCÓLOGO INFANTIL",
        "ORTOPEDIA Y TRAUMATOLOGÍA",
        "OTORRINOLARINGOLOGÍA",
        "PEDIATRÍA",
        "PSIQUIATRÍA",
        "PSIQUIATRÍA INFANTO JUVENIL",
        "RADIOTERAPIA O TERAPIA RADIANTE",
        "REUMATOLOGÍA",
        "REUMATÓLOGO INFANTIL",
        "TERAPIA INTENSIVA",
        "TERAPISTA INTENSIVO INFANTIL",
        "TOCOGINECOLOGÍA",
        "TOXICOLOGÍA",
        "UROLOGÍA"
    );


    private static final java.util.Set<String> VALID_HEALTH_INSURANCES = java.util.Set.of(
    "OSDE",
    "SWISS MEDICAL",
    "GALENO",
    "MEDICUS",
    "OMINT",
    "SANCOR SALUD",
    "MEDIFÉ",
    "ACCORD SALUD",
    "PREVENCIÓN SALUD",
    "OSECAC",
    "OSDEPYM",
    "OSPRERA",
    "OSPACA",
    "OSPE",
    "OSUTHGRA",
    "OSUOM",
    "OSMATA",
    "IOMA",
    "IOSFA",
    "PAMI"
    );


    private static final java.util.Map<String, java.util.Set<String>> VALID_HEALTH_PLANS =
    java.util.Map.ofEntries(

    java.util.Map.entry("OSDE", java.util.Set.of("210","310","410","450","510")),
    java.util.Map.entry( "SWISS MEDICAL", java.util.Set.of("SMG20","SMG30","SMG40","SMG50","SMG60")),
    java.util.Map.entry("GALENO", java.util.Set.of("220", "330","440")),
    java.util.Map.entry("MEDICUS", java.util.Set.of("MEDICUS")),
    java.util.Map.entry("OMINT", java.util.Set.of("GLOBAL","PREMIUM")),
    java.util.Map.entry("SANCOR SALUD", java.util.Set.of("1000","2000","3000","4000")),
    java.util.Map.entry( "MEDIFÉ", java.util.Set.of("BRONCE","PLATA","ORO")),
    java.util.Map.entry("PREVENCIÓN SALUD", java.util.Set.of("A1","A2","A3")),
    java.util.Map.entry("OSECAC", java.util.Set.of("OSECAC")),
    java.util.Map.entry("OSDEPYM", java.util.Set.of("OSDEPYM")),
     java.util.Map.entry("OSPRERA", java.util.Set.of("OSPRERA")),
    java.util.Map.entry("OSPACA", java.util.Set.of("OSPACA")),
    java.util.Map.entry("OSPE", java.util.Set.of("OSPE")),
    java.util.Map.entry("OSUTHGRA", java.util.Set.of("OSUTHGRA")),
    java.util.Map.entry("OSUOM", java.util.Set.of("OSUOM")),
    java.util.Map.entry("OSMATA", java.util.Set.of("OSMATA")),
    java.util.Map.entry("IOMA", java.util.Set.of("IOMA")),
    java.util.Map.entry("IOSFA", java.util.Set.of("IOSFA")),
    java.util.Map.entry("PAMI", java.util.Set.of("PAMI"))
    );


    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            AuthMapper authMapper,
            EmailService emailService,
            EmailVerificationRepository emailVerificationRepository,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.authMapper = authMapper;
        this.emailService = emailService;
        this.emailVerificationRepository = emailVerificationRepository;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public void verifyAccount(String rawToken) {
        String hashedToken = hashToken(rawToken);

        EmailVerification verification = emailVerificationRepository.findByCodeHash(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Código de verificación inválido o no encontrado"));

        if (verification.getConsumedAt() != null) {
            throw new IllegalArgumentException("Este enlace ya fue utilizado");
        }

        if (verification.getExpiresAt().isBefore(ZonedDateTime.now(ARGENTINA_ZONE))) {
            throw new IllegalArgumentException("El enlace de verificación ha expirado");
        }

        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verification.setConsumedAt(ZonedDateTime.now(ARGENTINA_ZONE));
        emailVerificationRepository.save(verification);

        if ("PATIENT".equals(user.getRole())) {
            try {
                final String userEmail = user.getEmail();
                final String userName = user.getName();

                emailService.sendWelcomeEmailToPatientAsync(userEmail, userName);
                log.info("Email de bienvenida enviado a: {}", userEmail);                
                
            } catch (Exception e) {
                log.warn("Error enviando email de bienvenida a {}: {}", user.getEmail(), e.getMessage());            
            }
        }
        
        log.info("Cuenta verificada exitosamente para: {}", user.getEmail());
    }

    @Override
    @Transactional
    public RegisterResponseDTO registerPatient(RegisterRequestDTO request) {
        java.util.Map.Entry<String, String> normalizedCoverage = validateCommonFields(request);
        
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        if (userRepository.existsByDni(request.dni())) {
            throw new IllegalArgumentException("DNI already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = userMapper.toUser(request, "PATIENT", hashedPassword);

        applyHealthCoverage(user, normalizedCoverage);

        user.setEmailVerified(false);
        
        user = userRepository.save(user);
        
        try {
            final String userEmail = user.getEmail();
            final String userName = user.getName();
            final String verificationToken = createVerificationForUser(user);
            
            emailService.sendVerificationEmailAsync(userEmail, userName, verificationToken);                
            
            log.info("Email de verificación enviado a: {}", userEmail);
        } catch (Exception e) {
            log.warn("Error enviando email de verificación a {}: {}", user.getEmail(), e.getMessage());            
        }

        return userMapper.toRegisterResponse(user);
    }

    @Override
    @Transactional
    public RegisterResponseDTO registerDoctor(RegisterRequestDTO request) {        
        validateCommonFields(request, false);
        validateDoctorFields(request);
        
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (userRepository.existsByDni(request.dni())) {
            throw new IllegalArgumentException("DNI already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = userMapper.toUser(request, "DOCTOR", hashedPassword);
        user.setStatus("PENDING");

        user.setEmailVerified(false);

        user = userRepository.save(user);

        try {
            final String userEmail = user.getEmail();
            final String userName = user.getName();
            final String verificationToken = createVerificationForUser(user);
            
            emailService.sendVerificationEmailAsync(userEmail, userName, verificationToken);                
            
            log.info("Email de verificación enviado a: {}", userEmail);
        } catch (Exception e) {
            log.warn("Error enviando email de verificación a {}: {}", user.getEmail(), e.getMessage());            
        }

        return userMapper.toRegisterResponse(user);
    }

    @Override
    @Transactional
    public RegisterResponseDTO registerAdmin(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        if (userRepository.existsByDni(request.dni())) {
            throw new IllegalArgumentException("DNI already registered");
        }

        if (request.healthInsurance() != null || request.healthPlan() != null) {
            throw new IllegalArgumentException("Solo los pacientes pueden cargar obra social");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = userMapper.toUser(request, "ADMIN", hashedPassword);
        user = userRepository.save(user);

        return userMapper.toRegisterResponse(user);
    }

    @Override
    @Transactional
    public SignInResponseDTO signIn(SignInRequestDTO request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Correo o contraseña incorrecto"));

        if(!user.isEmailVerified()) {
            throw new IllegalArgumentException("Correo o contraseña incorrecto");
        }

        if (!isUserAuthorizedToSignIn(user)) {
            throw new IllegalArgumentException("Correo o contraseña incorrecto");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Correo o contraseña incorrecto");
        }

        String rawToken = generateSecureToken();
        String hashedToken = hashToken(rawToken);

        RefreshToken refreshToken = createRefreshToken(user, hashedToken);
        refreshTokenRepository.save(refreshToken);

        String accessToken = generateAccessToken(user);

        return authMapper.toSignInResponse(user, accessToken, rawToken);
    }

    private boolean isUserAuthorizedToSignIn(User user) {
        String role = user.getRole();
        String status = user.getStatus();
        
        if (role == null || status == null || role.trim().isEmpty() || status.trim().isEmpty()) {
            return false;
        }
        
        return switch (role.trim().toUpperCase()) {
            case "PATIENT", "ADMIN" -> "ACTIVE".equalsIgnoreCase(status.trim());
            case "DOCTOR" -> "ACTIVE".equalsIgnoreCase(status.trim()) || "PENDING".equalsIgnoreCase(status.trim());
            default -> false;
        };
    }

    @Override
    @Transactional
    public void signOut(String rawRefreshToken) {
        if (rawRefreshToken == null) {
            return;
        }
        String hashedToken = hashToken(rawRefreshToken);
        refreshTokenRepository.revokeTokenByHash(hashedToken, ZonedDateTime.now(ARGENTINA_ZONE));
    }

    @Override
    @Transactional
    public SignInResponseDTO refreshToken(String rawRefreshToken) {
        
        String hashedInputToken = hashToken(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashedInputToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        String newAccessToken = generateAccessToken(user);

        String newRawToken = generateSecureToken();
        String newHashedToken = hashToken(newRawToken);

        RefreshToken newRefreshToken = createRefreshToken(user, newHashedToken);
        refreshTokenRepository.save(newRefreshToken);
        
        refreshTokenRepository.revokeTokenByHash(hashedInputToken, ZonedDateTime.now(ARGENTINA_ZONE));

        return authMapper.toSignInResponse(user, newAccessToken, newRawToken);
    }

    private RefreshToken createRefreshToken(User user, String hashedToken) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashedToken);
        refreshToken.setExpiresAt(ZonedDateTime.now(ARGENTINA_ZONE).plusDays(30));
        refreshToken.setCreatedAt(ZonedDateTime.now(ARGENTINA_ZONE));
        return refreshToken;
    }

    private String generateSecureToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String generateAccessToken(User user) {
        return jwtService.generateToken(user);
    }

    private java.util.Map.Entry<String, String> validateCommonFields(RegisterRequestDTO request) {
        return validateCommonFields(request, true);
    }

    private java.util.Map.Entry<String, String> validateCommonFields(RegisterRequestDTO request, boolean allowHealthCoverage) {
        if (request.birthdate() == null) {
            throw new IllegalArgumentException("Birthdate is required");
        }
        
        if (request.gender() == null || request.gender().trim().isEmpty()) {
            throw new IllegalArgumentException("Gender is required");
        }
        
        if (request.phone() == null || request.phone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone is required");
        }
                
        if (request.dni() != null) {
            String dniStr = request.dni().toString();
            if (!dniStr.matches("^[0-9]{7,8}$")) {
                throw new IllegalArgumentException("Invalid DNI format (7-8 digits)");
            }
            if (request.dni() < 1000000L || request.dni() > 999999999L) {
                throw new IllegalArgumentException("DNI out of valid range");
            }
        }
                
        if (request.birthdate().isAfter(LocalDate.now(ARGENTINA_ZONE).minusYears(18))) {
            throw new IllegalArgumentException("Must be at least 18 years old");
        }
        
        if (request.birthdate().isBefore(LocalDate.now(ARGENTINA_ZONE).minusYears(120))) {
            throw new IllegalArgumentException("Invalid birth date");
        }

        if (allowHealthCoverage) {
            return validateAndNormalizeHealthCoverage(request.healthInsurance(), request.healthPlan());
        }

        if (request.healthInsurance() != null || request.healthPlan() != null) {
            throw new IllegalArgumentException("Solo los pacientes pueden cargar obra social");
        }

        return null;
    }

    private void validateDoctorFields(RegisterRequestDTO request) {
        if (request.medicalLicense() == null || request.medicalLicense().trim().isEmpty()) {
            throw new IllegalArgumentException("Medical license is required for doctors");
        }
        
        if (request.specialty() == null || request.specialty().trim().isEmpty()) {
            throw new IllegalArgumentException("Specialty is required for doctors");
        }
        
        if (!VALID_SPECIALTIES.contains(request.specialty().trim())) {
            throw new IllegalArgumentException("Invalid specialty selected");
        }
        
        if (request.slotDurationMin() == null) {
            throw new IllegalArgumentException("Slot duration is required for doctors");
        }
                
        if (!request.medicalLicense().matches("^[0-9]{4,10}$")) {
            throw new IllegalArgumentException("Medical license must be 4-10 digits");
        }
                
        if (request.slotDurationMin() < 5 || request.slotDurationMin() > 180) {
            throw new IllegalArgumentException("Slot duration must be between 5 and 180 minutes");
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Error initializing SHA-256", e);
        }
    }

    private String createVerificationForUser(User user) {
        String rawToken = generateSecureToken();
        String hashedToken = hashToken(rawToken);

        EmailVerification verification = new EmailVerification();
        verification.setUser(user);
        verification.setCodeHash(hashedToken);
        verification.setExpiresAt(ZonedDateTime.now(ARGENTINA_ZONE).plusHours(72));

        emailVerificationRepository.save(verification);

        return rawToken;
    }

    static void applyHealthCoverage(User user, java.util.Map.Entry<String, String> coverage) {
        if (coverage == null) {
            user.setHealthInsurance(null);
            user.setHealthPlan(null);
            return;
        }

        user.setHealthInsurance(coverage.getKey());
        user.setHealthPlan(coverage.getValue());
    }

    static java.util.Map.Entry<String, String> validateAndNormalizeHealthCoverage(String healthInsurance, String healthPlan) {
        String normalizedInsurance = normalizeCoverageValue(healthInsurance);
        String normalizedPlan = normalizeCoverageValue(healthPlan);

        if (normalizedInsurance == null && normalizedPlan == null) {
            return null;
        }

        if (normalizedInsurance == null) {
            throw new IllegalArgumentException("Debe seleccionar una obra social para ese plan");
        }

        if (!VALID_HEALTH_INSURANCES.contains(normalizedInsurance)) {
            throw new IllegalArgumentException("Invalid health insurance selected");
        }

        if (normalizedPlan == null) {
            throw new IllegalArgumentException("Debe seleccionar un plan para la obra social");
        }

        java.util.Set<String> plans = VALID_HEALTH_PLANS.get(normalizedInsurance);
        if (plans == null || !plans.contains(normalizedPlan)) {
            throw new IllegalArgumentException("Invalid health plan for the selected insurance");
        }

        return java.util.Map.entry(normalizedInsurance, normalizedPlan);
    }

    static String normalizeCoverageValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.toUpperCase(java.util.Locale.ROOT);
    }
}
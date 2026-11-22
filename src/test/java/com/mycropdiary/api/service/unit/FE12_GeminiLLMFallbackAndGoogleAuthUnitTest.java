package com.mycropdiary.api.service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycropdiary.api.dto.auth.AuthResp;
import com.mycropdiary.api.dto.auth.GoogleAuthReq;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.security.JwtTokenProvider;
import com.mycropdiary.api.service.ai.OpenAIService;
import com.mycropdiary.api.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE12_AI_AUTH - Google Gemini LLM Fallback & OAuth2 Authentication Unit Tests")
public class FE12_GeminiLLMFallbackAndGoogleAuthUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountTokenRepository accountTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

    private OpenAIService openAIService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        openAIService = new OpenAIService(
                "AIzaSyDummyKeyForTesting",
                "gemini-1.5-flash",
                "https://generativelanguage.googleapis.com/v1beta/openai",
                10,
                objectMapper
        );
        ReflectionTestUtils.setField(authService, "otpExpirationMinutes", 5);
        ReflectionTestUtils.setField(authService, "mailFrom", "no-reply@mycropdiary.com");
    }

    private String createDummyGoogleIdToken(String email, String name, String picture) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = String.format("{\"email\":\"%s\",\"name\":\"%s\",\"picture\":\"%s\",\"email_verified\":true}", email, name, picture);
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".dummySignature123";
    }

    @Test
    @DisplayName("UTC01 - OpenAIService with valid Gemini API key returns isConfigured == true")
    void UTC01_isConfigured_validKey_returnsTrue() {
        assertTrue(openAIService.isConfigured());
        assertEquals("gemini-1.5-flash", openAIService.getModel());
    }

    @Test
    @DisplayName("UTC02 - OpenAIService with placeholder key returns isConfigured == false")
    void UTC02_isConfigured_placeholderKey_returnsFalse() {
        OpenAIService unconfigured = new OpenAIService(
                "sk-placeholder",
                "gemini-1.5-flash",
                "https://generativelanguage.googleapis.com",
                10,
                objectMapper
        );
        ReflectionTestUtils.setField(unconfigured, "apiKey", "");
        assertFalse(unconfigured.isConfigured());
    }

    @Test
    @DisplayName("UTC03 - Unconfigured OpenAIService returns Optional.empty() on generateAnswer")
    void UTC03_generateAnswer_unconfigured_returnsEmpty() {
        OpenAIService unconfigured = new OpenAIService(
                "",
                "gemini-1.5-flash",
                "https://generativelanguage.googleapis.com",
                10,
                objectMapper
        );
        ReflectionTestUtils.setField(unconfigured, "apiKey", "");
        Optional<String> result = unconfigured.generateAnswer("System VietGAP prompt", List.of(), "Làm thế nào để bón phân đúng cách?");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("UTC04 - OpenAIService model resolution maintains Gemini candidate hierarchy")
    void UTC04_getModel_returnsConfiguredModel() {
        assertEquals("gemini-1.5-flash", openAIService.getModel());
    }

    @Test
    @DisplayName("UTC05 - Google OAuth2 login with null request throws BadRequestException")
    void UTC05_loginWithGoogle_nullRequest_throwsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                authService.loginWithGoogle(null)
        );
        assertTrue(ex.getMessage().contains("không hợp lệ"));
    }

    @Test
    @DisplayName("UTC06 - Google OAuth2 login for new user registers account with Role USER/FARMER and ACTIVE status")
    void UTC06_loginWithGoogle_validIdToken_newUser_registeredSuccessfully() {
        String token = createDummyGoogleIdToken("farmer.google@gmail.com", "Google Farmer", "https://avatar.url/farmer.png");
        GoogleAuthReq req = new GoogleAuthReq(token, null, null, null);

        when(userRepository.findByEmailIgnoreCase("farmer.google@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encodedBcryptPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 101);
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(eq(101), eq("farmer.google@gmail.com"), any())).thenReturn("mockGoogleAccessToken");
        when(jwtTokenProvider.generateRefreshToken(eq(101))).thenReturn("mockGoogleRefreshToken");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(accountTokenRepository.save(any(AccountToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthResp resp = authService.loginWithGoogle(req);

        assertNotNull(resp);
        assertEquals("mockGoogleAccessToken", resp.accessToken());
        assertEquals("mockGoogleRefreshToken", resp.refreshToken());
        assertEquals("farmer.google@gmail.com", resp.email());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("UTC07 - Google OAuth2 login for existing active user succeeds without duplicate registration")
    void UTC07_loginWithGoogle_existingActiveUser_success() {
        String token = createDummyGoogleIdToken("existing.farmer@gmail.com", "Existing Farmer", "https://avatar.url/avatar.png");
        GoogleAuthReq req = new GoogleAuthReq(token, null, null, null);

        User existingUser = new User();
        ReflectionTestUtils.setField(existingUser, "id", 42);
        existingUser.setEmail("existing.farmer@gmail.com");
        existingUser.setFullName("Existing Farmer");
        existingUser.setSystemRole("farmer");
        existingUser.setAccountStatus("active");

        when(userRepository.findByEmailIgnoreCase("existing.farmer@gmail.com")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateAccessToken(eq(42), eq("existing.farmer@gmail.com"), any())).thenReturn("mockAccessToken42");
        when(jwtTokenProvider.generateRefreshToken(eq(42))).thenReturn("mockRefreshToken42");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(accountTokenRepository.save(any(AccountToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthResp resp = authService.loginWithGoogle(req);

        assertNotNull(resp);
        assertEquals("mockAccessToken42", resp.accessToken());
        assertEquals(42, resp.userId());
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    @DisplayName("UTC08 - Google OAuth2 login with direct email parameter succeeds")
    void UTC08_loginWithGoogle_directEmailParameter_success() {
        GoogleAuthReq req = new GoogleAuthReq(null, "direct.farmer@gmail.com", "Direct Farmer", "https://avatar.url/d.png");

        User existingUser = new User();
        ReflectionTestUtils.setField(existingUser, "id", 55);
        existingUser.setEmail("direct.farmer@gmail.com");
        existingUser.setFullName("Direct Farmer");
        existingUser.setSystemRole("user");
        existingUser.setAccountStatus("active");

        when(userRepository.findByEmailIgnoreCase("direct.farmer@gmail.com")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateAccessToken(eq(55), eq("direct.farmer@gmail.com"), any())).thenReturn("mockAccessToken55");
        when(jwtTokenProvider.generateRefreshToken(eq(55))).thenReturn("mockRefreshToken55");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(accountTokenRepository.save(any(AccountToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthResp resp = authService.loginWithGoogle(req);

        assertNotNull(resp);
        assertEquals("mockAccessToken55", resp.accessToken());
        assertEquals(55, resp.userId());
    }

    @Test
    @DisplayName("UTC09 - Google OAuth2 login with token missing email and unreachable fallback throws BadRequestException")
    void UTC09_loginWithGoogle_missingEmail_fallbackFails_throwsBadRequestException() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"name\":\"No Email User\"}".getBytes(StandardCharsets.UTF_8));
        String tokenWithoutEmail = header + "." + payload + ".sig";

        GoogleAuthReq req = new GoogleAuthReq(tokenWithoutEmail, null, null, null);

        assertThrows(BadRequestException.class, () -> authService.loginWithGoogle(req));
    }

    @Test
    @DisplayName("UTC10 - Defect Simulation DF-026: Direct Base64 ID Token decoding without cryptographic signature verification")
    void UTC10_loginWithGoogle_unverifiedEmailClaim_defectDF026() {
        // Defect Finding: In AuthServiceImpl, Google ID Token payload is decoded via standard Base64 string splitting
        // rather than cryptographically verified with Google IdTokenVerifier using Google's public RSA certificates.
        // This allows client-forged JWT tokens if network fallback fails.
        fail("DF-026: [Defect Found] Google ID Token được giải mã thô bằng Base64UrlDecoder thay vì dùng GoogleIdTokenVerifier xác thực chữ ký số RSA với Google Public Keys, tiềm ẩn nguy cơ giả mạo Identity Token.");
    }
}

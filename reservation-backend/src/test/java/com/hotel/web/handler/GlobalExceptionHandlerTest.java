package com.hotel.web.handler;

import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.infrastructure.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = GlobalExceptionHandlerTest.DummyController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.DummyController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test-errors")
    static class DummyController {

        @GetMapping("/access-denied")
        public void accessDenied() {
            throw new AccessDeniedException("No tiene permisos suficientes");
        }

        @GetMapping("/disabled")
        public void disabled() {
            throw new DisabledException("Cuenta deshabilitada");
        }

        @GetMapping("/data-integrity")
        public void dataIntegrity() {
            throw new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint \"uk_users_email\"");
        }

        @PostMapping("/unreadable")
        public void unreadable(@RequestBody Map<String, Object> body) {
        }

        @GetMapping("/not-found")
        public void notFound() {
            throw new ResourceNotFoundException("Recurso no encontrado");
        }

        @GetMapping("/bad-credentials")
        public void badCredentials() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/unexpected")
        public void unexpected() {
            throw new RuntimeException("Internal sensitive SQL detail");
        }
    }

    @Test
    void handleAccessDenied_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/test-errors/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No tiene permisos suficientes"));
    }

    @Test
    void handleDisabled_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/test-errors/disabled"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Cuenta deshabilitada"));
    }

    @Test
    void handleDataIntegrityViolation_Returns409ConflictWithoutSqlLeak() throws Exception {
        mockMvc.perform(get("/test-errors/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", not(containsString("uk_users_email"))));
    }

    @Test
    void handleHttpMessageNotReadable_Returns400BadRequest() throws Exception {
        mockMvc.perform(post("/test-errors/unreadable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed or unreadable request payload."));
    }

    @Test
    void handleResourceNotFound_Returns404NotFound() throws Exception {
        mockMvc.perform(get("/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Recurso no encontrado"));
    }

    @Test
    void handleBadCredentials_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/test-errors/bad-credentials"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handleGeneralException_Returns500WithoutLeakingMessage() throws Exception {
        mockMvc.perform(get("/test-errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail", not(containsString("Internal sensitive SQL detail"))));
    }
}

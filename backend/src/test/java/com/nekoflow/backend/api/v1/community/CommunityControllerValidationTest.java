package com.nekoflow.backend.api.v1.community;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.nekoflow.backend.security.JwtAuthenticationFilter;

@WebMvcTest(
    controllers = CommunityController.class,
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class CommunityControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommunityService communityService;

    private final String episodeUrl = "/api/v1/episodes/" + UUID.randomUUID() + "/comments";

    private void postJson(String body, int expectedStatus) throws Exception {
        mockMvc.perform(post(episodeUrl).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is(expectedStatus));
    }

    @Test
    void rejectsBlankBody() throws Exception {
        postJson("{\"body\":\"\",\"containsSpoiler\":false}", 400);
    }

    @Test
    void rejectsWhitespaceOnlyBody() throws Exception {
        postJson("{\"body\":\"   \",\"containsSpoiler\":false}", 400);
    }

    @Test
    void rejectsBodyOverMaxLength() throws Exception {
        String tooLong = "a".repeat(501);
        postJson("{\"body\":\"" + tooLong + "\",\"containsSpoiler\":false}", 400);
    }

    @Test
    void acceptsValidBodyIncludingScriptTextAndEmoji() throws Exception {
        // Conteudo com <script> e emoji e um payload VALIDO no backend (texto puro);
        // a neutralizacao acontece na renderizacao do frontend (React escapa).
        postJson("{\"body\":\"<script>alert(1)</script> 😀 @alice\",\"containsSpoiler\":true}", 201);
    }

    @Test
    void acceptsBodyAtMaxLength() throws Exception {
        String exactly = "a".repeat(500);
        postJson("{\"body\":\"" + exactly + "\",\"containsSpoiler\":false}", 201);
    }
}

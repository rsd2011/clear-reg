package com.example.backend.support;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

public abstract class MockMvcSupport extends IntegrationTest {

    protected ResultActions getJson(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions postJson(String url, Object payload) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(payload)));
    }

    protected ResultActions putJson(String url, Object payload) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(payload)));
    }

    protected <T> T readResponse(ResultActions actions, Class<T> responseType) throws Exception {
        String content = actions.andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return fromJson(content, responseType);
    }
}

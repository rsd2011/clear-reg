package com.example.server.readmodel;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReadModelWorkerController.class)
@Import(ReadModelWorkerController.class)
class ReadModelWorkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReadModelWorker worker;

    @Test
    void rebuildOrganization() throws Exception {
        mockMvc.perform(post("/internal/read-model/organization/rebuild"))
                .andExpect(status().isAccepted());
        verify(worker).rebuildOrganization();
    }

    @Test
    void rebuildMenu() throws Exception {
        mockMvc.perform(post("/internal/read-model/menu/rebuild"))
                .andExpect(status().isAccepted());
        verify(worker).rebuildMenu();
    }

    @Test
    void rebuildPermissionMenu() throws Exception {
        mockMvc.perform(post("/internal/read-model/permission-menu/user1/rebuild"))
                .andExpect(status().isAccepted());
        verify(worker).rebuildPermissionMenu("user1");
    }
}

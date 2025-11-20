package com.example.server.readmodel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReadModelWorkerControllerTest {

    private MockMvc mockMvc;
    private ReadModelWorker worker;

    @BeforeEach
    void setUp() {
        this.worker = mock(ReadModelWorker.class);
        ReadModelWorkerController controller = new ReadModelWorkerController(worker);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

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

package com.hapi.gridwatch.alert.controller;


import com.hapi.gridwatch.alert.config.WebSecurityConfig;
import com.hapi.gridwatch.alert.exception.WorkOrderNotFoundException;
import com.hapi.gridwatch.alert.service.ReadingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.*;

@WebMvcTest(WorkOrderController.class)
@Import(WebSecurityConfig.class)
public class WorkOrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReadingService readingService;

    @Test
    @WithMockUser
    void closeReturns200() throws Exception {
        mockMvc.perform(post("/work-orders/42/close"))
                .andExpect(status().isOk());
        verify(readingService).closeWorkOrder(42L);
    }

    @Test
    @WithMockUser
    void unknownIdReturns404() throws Exception {
        doThrow( new WorkOrderNotFoundException("WorkOrderNotFoundException")).when(readingService).closeWorkOrder(42L);
        mockMvc.perform(
                post("/work-orders/42/close")
        ).andExpect(status().isNotFound());
        verify(readingService).closeWorkOrder(42L);
    }
    @Test
    void noLoginReturns401() throws Exception {
        mockMvc
                .perform(
                        post("/work-orders/42/close")
                ).andExpect(status().isUnauthorized());
    }
}

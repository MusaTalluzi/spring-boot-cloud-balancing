/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.springbootcloudbalancing;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.springbootcloudbalancing.domain.CloudBalance;
import org.optaplanner.springbootcloudbalancing.domain.CloudComputer;
import org.optaplanner.springbootcloudbalancing.domain.CloudProcess;
import org.optaplanner.springbootcloudbalancing.solver.SolverStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CloudBalancingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void submitProblemAndSolveIt() throws Exception {
        CloudComputer c1 = new CloudComputer(1L, 1000, 1, 1, 1);
        CloudProcess p1 = new CloudProcess(1L, 700, 5, 5);
        CloudBalance cloudBalance = new CloudBalance(0L, Collections.singletonList(c1), Collections.singletonList(p1));

        String cloudBalanceBody = objectMapper.writeValueAsString(cloudBalance);
        mockMvc.perform(MockMvcRequestBuilders.post("/")
                .content(cloudBalanceBody).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        String solvingStatusJsonString = objectMapper.writeValueAsString(SolverStatus.SOLVING);
        mockMvc.perform(MockMvcRequestBuilders.get("/solverStatus")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(solvingStatusJsonString));

        Thread.sleep(1000L); // Give solver time to solve

        Score expectedScore = HardSoftScore.of(-8, -1);
        String expectedScoreJsonString = objectMapper.writeValueAsString(expectedScore);
        mockMvc.perform(MockMvcRequestBuilders.get("/bestScore")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedScoreJsonString));
    }
}

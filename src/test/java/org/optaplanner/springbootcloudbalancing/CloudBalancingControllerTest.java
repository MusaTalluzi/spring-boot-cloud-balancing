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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.springbootcloudbalancing.domain.CloudBalance;
import org.optaplanner.springbootcloudbalancing.domain.CloudComputer;
import org.optaplanner.springbootcloudbalancing.domain.CloudProcess;
import org.optaplanner.springbootcloudbalancing.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CloudBalancingControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(CloudBalancingControllerTest.class);
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long newCloudBalanceId = 0L;
    private Random random = new Random(47);

    @Before
    public void setup() {
        newCloudBalanceId = 0L;
        random = new Random(47);
    }

    @Test
    public void submitOneProblemAndSolveIt() throws Exception {
        CloudBalance cloudBalance = generateCloudBalancingProblem(1, 1);
        String cloudBalanceBody = objectMapper.writeValueAsString(cloudBalance);
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/solvers")
                .content(cloudBalanceBody).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBodyAsString = mvcResult.getResponse().getContentAsString();
        Long solverId = Long.parseLong(responseBodyAsString);

        String solvingStatusJsonString = objectMapper.writeValueAsString(SolverStatus.SOLVING);
        mockMvc.perform(get("/solvers/{solverId}/solverStatus", solverId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(solvingStatusJsonString));

        Thread.sleep(1000L); // Give solver time to solve

        String cloudBalanceSolutionAsJsonString = mockMvc.perform(get("/solvers/{solverId}/bestSolution", solverId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        CloudBalance solution = objectMapper.readValue(cloudBalanceSolutionAsJsonString, CloudBalance.class);

        Score expectedScore = getExpectedHardSoftScore(solution);
        String expectedScoreJsonString = objectMapper.writeValueAsString(expectedScore);
        mockMvc.perform(get("/solvers/{solverId}/bestScore", solverId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedScoreJsonString));
    }

    @Test
    public void submitMultipleProblems() {
        int numOfProblems = 3;
        for (int i = 0; i < numOfProblems; i++) {

        }
    }

    private CloudBalance generateCloudBalancingProblem(int computerListSize, int processListSize) {
        CloudBalance cloudBalance = new CloudBalance();
        cloudBalance.setId(newCloudBalanceId++);
        createComputerList(cloudBalance, computerListSize);
        createProcessList(cloudBalance, processListSize);

        return cloudBalance;
    }

    private void createComputerList(CloudBalance cloudBalance, int computerListSize) {
        List<CloudComputer> computerList = new ArrayList<>(computerListSize);
        for (int i = 0; i < computerListSize; i++) {
            CloudComputer computer = new CloudComputer(
                    (long) i,
                    random.nextInt(10),
                    random.nextInt(10),
                    random.nextInt(10),
                    random.nextInt(10) * 10);
            computerList.add(computer);
            logger.info("Created a computer with cpuPower ({}), memory ({}), networkBandwidth ({}).",
                    computer.getCpuPower(), computer.getMemory(), computer.getNetworkBandwidth());
        }
        cloudBalance.setComputerList(computerList);
    }

    private void createProcessList(CloudBalance cloudBalance, int processListSize) {
        List<CloudProcess> processList = new ArrayList<>(processListSize);
        for (int i = 0; i < processListSize; i++) {
            CloudProcess process = new CloudProcess((long) i, random.nextInt(10),
                    random.nextInt(10), random.nextInt(10));
            processList.add(process);
            logger.info("Created a process with requiredCpuPower ({}), requiredMemory ({}), requiredNetworkBandwidth ({}).",
                    process.getRequiredCpuPower(), process.getRequiredMemory(), process.getRequiredNetworkBandwidth());
        }
        cloudBalance.setProcessList(processList);
    }

    private Score getExpectedHardSoftScore(CloudBalance cloudBalance) {
        int expectedHardScore = 0;
        for (CloudComputer computer : cloudBalance.getComputerList()) {
            int totalCpuPower = 0;
            int totalMemory = 0;
            int totalBandwidth = 0;
            for (CloudProcess process : cloudBalance.getProcessList()) {
                if (process.getComputer().getId().equals(computer.getId())) {
                    totalCpuPower += process.getRequiredCpuPower();
                    totalMemory += process.getRequiredMemory();
                    totalBandwidth += process.getRequiredNetworkBandwidth();
                }
            }

            if (totalCpuPower > computer.getCpuPower()) {
                expectedHardScore += computer.getCpuPower() - totalCpuPower;
            }
            if (totalMemory > computer.getMemory()) {
                expectedHardScore += computer.getMemory() - totalMemory;
            }
            if (totalBandwidth > computer.getNetworkBandwidth()) {
                expectedHardScore += computer.getNetworkBandwidth() - totalBandwidth;
            }
        }

        int expectedSoftScore = 0;
        for (CloudProcess process : cloudBalance.getProcessList()) {
            expectedSoftScore -= process.getComputer() == null ? 0 : process.getComputer().getCost();
        }

        return HardSoftScore.of(expectedHardScore, expectedSoftScore);
    }
}

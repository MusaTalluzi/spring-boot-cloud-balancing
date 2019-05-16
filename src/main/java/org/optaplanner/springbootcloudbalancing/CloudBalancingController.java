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

import org.optaplanner.core.api.score.Score;
import org.optaplanner.springbootcloudbalancing.domain.CloudBalance;
import org.optaplanner.springbootcloudbalancing.solver.CloudBalancingSolverManager;
import org.optaplanner.springbootcloudbalancing.solver.SolverStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudBalancingController {

    @Autowired
    private CloudBalancingSolverManager<CloudBalance> cloudBalanceCloudBalancingSolverManager;

    @PostMapping
    public void startSolver(@RequestBody CloudBalance cloudBalance) {
        cloudBalanceCloudBalancingSolverManager.startSolver(cloudBalance);
    }

    @GetMapping("/bestSolution")
    public CloudBalance bestSolution() {
        return cloudBalanceCloudBalancingSolverManager.getBestSolution();
    }

    @GetMapping("/bestScore")
    public Score bestScore() {
        return cloudBalanceCloudBalancingSolverManager.getBestScore();
    }

    @GetMapping("/solverStatus")
    public SolverStatus solverStatus() {
        return cloudBalanceCloudBalancingSolverManager.getSolverStatus();
    }
}

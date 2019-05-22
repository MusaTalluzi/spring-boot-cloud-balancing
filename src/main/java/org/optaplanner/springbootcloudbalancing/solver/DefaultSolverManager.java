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

package org.optaplanner.springbootcloudbalancing.solver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.springbootcloudbalancing.domain.CloudBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultSolverManager implements SolverManager<CloudBalance> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSolverManager.class);

    private static final String SOLVER_CONFIG = "org/optaplanner/springbootcloudbalancing/solver/cloudBalancingSolverConfig.xml";

    private ExecutorService executorService;
    private SolverFactory<CloudBalance> solverFactory;
    private Map<String, SolverTask<CloudBalance>> tenantIdToSolverTaskMap;

    public DefaultSolverManager() {
        solverFactory = SolverFactory.createFromXmlResource(SOLVER_CONFIG, DefaultSolverManager.class.getClassLoader());
        tenantIdToSolverTaskMap = new HashMap<>();
    }

    @PostConstruct
    private void init() {
        int numAvailableProcessors = Runtime.getRuntime().availableProcessors();
        logger.info("Number of available processors: {}.", numAvailableProcessors);
        executorService = Executors.newFixedThreadPool(numAvailableProcessors - 2);
    }

    @PreDestroy
    private void shutdown() {
        logger.info("Shutting down {}.", DefaultSolverManager.class.getName());
        executorService.shutdownNow();
    }

    @Override
    public void solve(String tenantId, CloudBalance cloudBalance) {
        synchronized (this) {
            if (tenantIdToSolverTaskMap.containsKey(tenantId)) {
                throw new IllegalArgumentException("Tenant id (" + tenantId + ") already exists.");
            }
            SolverTask<CloudBalance> newSolverTask = new SolverTask<>(tenantId, solverFactory.buildSolver(), cloudBalance);
            executorService.submit(newSolverTask);
            tenantIdToSolverTaskMap.put(tenantId, newSolverTask);
            logger.info("A new solver task was created with tenantId {}.", tenantId);
        }
    }

    @Override
    public CloudBalance getBestSolution(String tenantId) {
        logger.info("Getting best solution of tenantId {}.", tenantId);
        return tenantIdToSolverTaskMap.get(tenantId).getBestSolution();
    }

    @Override
    public Score getBestScore(String tenantId) {
        logger.info("Getting best score of tenantId {}.", tenantId);
        return tenantIdToSolverTaskMap.get(tenantId).getBestScore();
    }

    @Override
    public SolverStatus getSolverStatus(String tenantId) {
        logger.info("Getting solver status of tenantId {}.", tenantId);
        return tenantIdToSolverTaskMap.get(tenantId).getSolverStatus();
    }
}

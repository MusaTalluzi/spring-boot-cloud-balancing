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
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.springbootcloudbalancing.domain.CloudBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CloudBalancingSolverManagerImpl implements CloudBalancingSolverManager<CloudBalance> {

    private static final Logger logger = LoggerFactory.getLogger(CloudBalancingSolverManagerImpl.class);

    private static final String SOLVER_CONFIG = "org/optaplanner/springbootcloudbalancing/solver/cloudBalancingSolverConfig.xml";

    private ExecutorService executorService;
    private SolverFactory<CloudBalance> solverFactory;
    private Map<Long, SolverTask<CloudBalance>> solverTaskIdToSolverTaskMap;
    private AtomicLong newSolverId = new AtomicLong(0);

    public CloudBalancingSolverManagerImpl() {
        solverFactory = SolverFactory.createFromXmlResource(SOLVER_CONFIG, CloudBalancingSolverManagerImpl.class.getClassLoader());
        solverTaskIdToSolverTaskMap = new HashMap<>();
    }

    @PostConstruct
    private void init() {
        int numAvailableProcessors = Runtime.getRuntime().availableProcessors();
        logger.info("Number of available processors: {}.", numAvailableProcessors);
        executorService = Executors.newFixedThreadPool(numAvailableProcessors * 10);
    }

    @PreDestroy
    private void shutdown() {
        logger.info("Shutting down {}.", CloudBalancingSolverManagerImpl.class.getName());
        executorService.shutdownNow();
    }

    @Override
    public Long solve(CloudBalance cloudBalance) {
        Long id = newSolverId.getAndIncrement();
        SolverTask<CloudBalance> newSolverTask = new SolverTask<>(id, solverFactory.buildSolver(), cloudBalance);
        executorService.submit(newSolverTask);
        solverTaskIdToSolverTaskMap.put(id, newSolverTask);
        logger.info("A new solver task was created with id {}.", id);
        return id;
    }

    @Override
    public CloudBalance getBestSolution(Long solverId) {
        logger.info("Getting best solution of solver {}.", solverId);
        return solverTaskIdToSolverTaskMap.get(solverId).getBestSolution();
    }

    @Override
    public Score getBestScore(Long solverId) {
        logger.info("Getting best score of solver {}.", solverId);
        return solverTaskIdToSolverTaskMap.get(solverId).getBestScore();
    }

    @Override
    public SolverStatus getSolverStatus(Long solverId) {
        logger.info("Getting solver status of solver {}.", solverId);
        return solverTaskIdToSolverTaskMap.get(solverId).getSolverStatus();
    }
}

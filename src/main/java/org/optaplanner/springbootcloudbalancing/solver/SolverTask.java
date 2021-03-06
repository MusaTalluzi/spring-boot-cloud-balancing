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

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.Solver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolverTask<Solution_> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SolverTask.class);

    private final Comparable<?> tenantId;
    private Solver<Solution_> solver;
    private Solution_ planningProblem;

    public SolverTask(Comparable<?> tenantId, Solver<Solution_> solver, Solution_ planningProblem) {
        this.tenantId = tenantId;
        this.solver = solver;
        this.planningProblem = planningProblem;
    }

    @Override
    public void run() {
        logger.info("Running solverTask for tenantId ({}).", tenantId);
        solver.solve(planningProblem);
    }

    public Comparable<?> getTenantId() {
        return tenantId;
    }

    public Solution_ getBestSolution() {
        return solver.getBestSolution();
    }

    public Score getBestScore() {
        return solver.getBestScore();
    }

    public SolverStatus getSolverStatus() {
        if (solver.isTerminateEarly()) {
            return SolverStatus.TERMINATING_EARLY;
        } else if (solver.isSolving()) {
            return SolverStatus.SOLVING;
        } else {
            return SolverStatus.STOPPED;
        }
    }
}


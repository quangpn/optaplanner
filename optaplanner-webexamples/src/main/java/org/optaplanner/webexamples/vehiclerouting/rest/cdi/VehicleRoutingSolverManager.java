/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.webexamples.vehiclerouting.rest.cdi;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.examples.vehiclerouting.domain.VehicleRoutingSolution;
import org.optaplanner.examples.vehiclerouting.persistence.VehicleRoutingImporter;

@ApplicationScoped
public class VehicleRoutingSolverManager implements Serializable {

    private static final String SOLVER_CONFIG = "org/optaplanner/examples/vehiclerouting/solver/vehicleRoutingSolverConfig.xml";
    private static final String IMPORT_DATASET = "/org/optaplanner/webexamples/vehiclerouting/belgium-road-time-n50-k10.vrp";

    private SolverFactory<VehicleRoutingSolution> solverFactory;
    // TODO After upgrading to JEE 7, replace ExecutorService by ManagedExecutorService:
    // @Resource(name = "DefaultManagedExecutorService")
    // private ManagedExecutorService executor;
    private ExecutorService executor;

    private Map<String, VehicleRoutingSolution> sessionSolutionMap;
    private Map<String, Solver<VehicleRoutingSolution>> sessionSolverMap;

    @PostConstruct
    public synchronized void init() {
        solverFactory = SolverFactory.createFromXmlResource(SOLVER_CONFIG);
        // Always terminate a solver after 2 minutes
        solverFactory.getSolverConfig().setTerminationConfig(new TerminationConfig().withMinutesSpentLimit(2L));
        executor = Executors.newFixedThreadPool(2); // Only 2 because the other examples have their own Executor
        // TODO these probably don't need to be thread-safe because all access is synchronized
        sessionSolutionMap = new ConcurrentHashMap<>();
        sessionSolverMap = new ConcurrentHashMap<>();
    }

    @PreDestroy
    public synchronized void destroy() {
        for (Solver<VehicleRoutingSolution> solver : sessionSolverMap.values()) {
            solver.terminateEarly();
        }
        executor.shutdown();
    }

    public synchronized VehicleRoutingSolution retrieveOrCreateSolution(String key, String vrpFile) {
        VehicleRoutingSolution solution = sessionSolutionMap.get(key);
        if (solution == null) {
            String fileName = "/data/belgium-road-time-n50-k10.vrp";
            if (vrpFile != null && !vrpFile.equals("")) {
                fileName = "/data/" + vrpFile;
            }
            File file = new File(fileName);
            URL unsolvedSolutionURL = null;
            try {
                unsolvedSolutionURL = file.toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (unsolvedSolutionURL == null) {
                throw new IllegalArgumentException("The IMPORT_DATASET (" + IMPORT_DATASET
                        + ") is not a valid classpath resource.");
            }
            solution = new VehicleRoutingImporter()
                    .readSolution(unsolvedSolutionURL);
            sessionSolutionMap.put(key, solution);
        }
        return solution;
    }

    public synchronized boolean solve(final String key, String vrpFile) {
        final Solver<VehicleRoutingSolution> solver = solverFactory.buildSolver();
        solver.addEventListener(event -> {
            VehicleRoutingSolution bestSolution = event.getNewBestSolution();
            synchronized (VehicleRoutingSolverManager.this) {
                sessionSolutionMap.put(key, bestSolution);
            }
        });
        if (sessionSolverMap.containsKey(key)) {
            return false;
        }
        sessionSolverMap.put(key, solver);
        final VehicleRoutingSolution solution = retrieveOrCreateSolution(key, vrpFile);
        executor.submit((Runnable) () -> {
            VehicleRoutingSolution bestSolution = solver.solve(solution);
            synchronized (VehicleRoutingSolverManager.this) {
                sessionSolutionMap.put(key, bestSolution);
                sessionSolverMap.remove(key);
            }
        });
        return true;
    }

    public synchronized boolean terminateEarly(String key) {
        Solver<VehicleRoutingSolution> solver = sessionSolverMap.remove(key);
        if (solver != null) {
            solver.terminateEarly();
            return true;
        } else {
            return false;
        }
    }

}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.examples.vehiclerouting.persistence;

import org.optaplanner.examples.common.persistence.AbstractTxtSolutionImporter;
import org.optaplanner.examples.common.persistence.SolutionConverter;
import org.optaplanner.examples.vehiclerouting.app.VehicleRoutingApp;
import org.optaplanner.examples.vehiclerouting.domain.Customer;
import org.optaplanner.examples.vehiclerouting.domain.Depot;
import org.optaplanner.examples.vehiclerouting.domain.Vehicle;
import org.optaplanner.examples.vehiclerouting.domain.VehicleRoutingSolution;
import org.optaplanner.examples.vehiclerouting.domain.location.AirLocation;
import org.optaplanner.examples.vehiclerouting.domain.location.DistanceType;
import org.optaplanner.examples.vehiclerouting.domain.location.Location;
import org.optaplanner.examples.vehiclerouting.domain.timewindowed.TimeWindowedCustomer;
import org.optaplanner.examples.vehiclerouting.domain.timewindowed.TimeWindowedDepot;
import org.optaplanner.examples.vehiclerouting.domain.timewindowed.TimeWindowedVehicleRoutingSolution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VehicleRoutingImporter extends AbstractTxtSolutionImporter<VehicleRoutingSolution> {

    public static void main(String[] args) {
        SolutionConverter<VehicleRoutingSolution> converter = SolutionConverter.createImportConverter(
                VehicleRoutingApp.DATA_DIR_NAME, new VehicleRoutingImporter(), VehicleRoutingSolution.class);
        converter.convert("vrpweb/timewindowed/air/Solomon_025_C101.vrp", "cvrptw-25customers.xml");
        converter.convert("vrpweb/timewindowed/air/Solomon_100_R101.vrp", "cvrptw-100customers-A.xml");
        converter.convert("vrpweb/timewindowed/air/Solomon_100_R201.vrp", "cvrptw-100customers-B.xml");
        converter.convert("vrpweb/timewindowed/air/Homberger_0400_R1_4_1.vrp", "cvrptw-400customers.xml");
        converter.convert("vrpweb/basic/road-unknown/bays-n29-k5.vrp", "road-cvrp-29customers.xml");
    }

    @Override
    public String getInputFileSuffix() {
        return "vrp";
    }

    @Override
    public TxtInputBuilder<VehicleRoutingSolution> createTxtInputBuilder() {
        return new VehicleRoutingInputBuilder();
    }

    public static class VehicleRoutingInputBuilder extends TxtInputBuilder<VehicleRoutingSolution> {

        private VehicleRoutingSolution solution;

        private int customerListSize;
        private List<Integer> vehicleListSizes = new ArrayList<>();
        private List<Integer> vehicleCapacities = new ArrayList<>();
        private List<Depot> depotList;

        @Override
        public VehicleRoutingSolution readSolution() throws IOException {
            String firstLine = readStringValue();
            solution = new TimeWindowedVehicleRoutingSolution();
            solution.setId(0L);
            solution.setName(firstLine);
            readTimeWindowedFormat();
            return solution;
        }

        private void createVehicleList() {
            List<Vehicle> vehicleList = new ArrayList<>();
            long id = 0;
            for (int i = 0; i < vehicleListSizes.size(); i++) {
                Integer vehicleListSize = vehicleListSizes.get(i);
                Integer capacity = vehicleCapacities.get(i);
                for (int j = 0; j < vehicleListSize; j++) {
                    Vehicle vehicle = new Vehicle();
                    vehicle.setId(id);
                    id++;
                    vehicle.setCapacity(capacity);
                    // Round robin the vehicles to a depot if there are multiple depots
                    vehicle.setDepot(depotList.get(j % depotList.size()));

//                    long returnDepotTime = ((TimeWindowedDepot) depotList.get(0)).getDueTime();
//                    if (j % 2 == 0) {
//                        returnDepotTime = 46800;
//                    }
//                    vehicle.setReturnToDepotTime(returnDepotTime);
                    vehicleList.add(vehicle);
                }
            }
            solution.setVehicleList(vehicleList);
        }

        // ************************************************************************
        // CVRPTW normal format. See http://neo.lcc.uma.es/vrp/
        // ************************************************************************

        public void readTimeWindowedFormat() throws IOException {
            readTimeWindowedHeaders();
            readTimeWindowedDepotAndCustomers();
            createVehicleList();
        }

        private void readTimeWindowedHeaders() throws IOException {
            solution.setDistanceType(DistanceType.AIR_DISTANCE);
            solution.setDistanceUnitOfMeasurement("distance");
            readEmptyLine();
            readConstantLine("VEHICLE");
            readConstantLine("NUMBER +CAPACITY");
            String[] lineTokens = splitBySpacesOrTabs(readStringValue(), 2);
            Integer vehicleListSize1 = Integer.parseInt(lineTokens[0]);
            vehicleListSizes.add(vehicleListSize1);
            Integer capacity1 = Integer.parseInt(lineTokens[1]);
            vehicleCapacities.add(capacity1);
            lineTokens = splitBySpacesOrTabs(readStringValue(), 2);
            Integer vehicleListSize2 = Integer.parseInt(lineTokens[0]);
            vehicleListSizes.add(vehicleListSize2);
            Integer capacity2 = Integer.parseInt(lineTokens[1]);
            vehicleCapacities.add(capacity2);
            readEmptyLine();
            readConstantLine("CUSTOMER");
            readConstantLine("CUST\\s+NO\\.\\s+XCOORD\\.\\s+YCOORD\\.\\s+DEMAND\\s+READY\\s+TIME\\s+DUE\\s+DATE\\s+SERVICE\\s+TIME");
            readEmptyLine();
        }

        private void readTimeWindowedDepotAndCustomers() throws IOException {
            String line = bufferedReader.readLine();
            int locationListSizeEstimation = 25;
            List<Location> locationList = new ArrayList<>(locationListSizeEstimation);
            depotList = new ArrayList<>(1);
            TimeWindowedDepot depot = null;
            List<Customer> customerList = new ArrayList<>(locationListSizeEstimation);
            int count = 0;
            while (line != null && !line.trim().isEmpty()) {
                String[] lineTokens = splitBySpacesOrTabs(line.trim(), 7);
                long id = Long.parseLong(lineTokens[0]);
                AirLocation location = new AirLocation();
                location.setId(id);
                location.setLatitude(Double.parseDouble(lineTokens[1]));
                location.setLongitude(Double.parseDouble(lineTokens[2]));
                locationList.add(location);
                int demand = Integer.parseInt(lineTokens[3]);
                long readyTime = Long.parseLong(lineTokens[4]) * 1000L;
                long dueTime = Long.parseLong(lineTokens[5]) * 1000L;
                long serviceDuration = Long.parseLong(lineTokens[6]) * 1000L;
                if (count < 2) {
                    depot = new TimeWindowedDepot();
                    depot.setId(id);
                    depot.setLocation(location);
                    if (demand != 0) {
                        throw new IllegalArgumentException("The depot with id (" + id
                                + ") has a demand (" + demand + ").");
                    }
                    depot.setReadyTime(readyTime);
                    depot.setDueTime(dueTime);
                    if (serviceDuration != 0) {
                        throw new IllegalArgumentException("The depot with id (" + id
                                + ") has a serviceDuration (" + serviceDuration + ").");
                    }
                    depotList.add(depot);
                    count++;
                } else {
                    TimeWindowedCustomer customer = new TimeWindowedCustomer();
                    customer.setId(id);
                    customer.setLocation(location);
                    customer.setDemand(demand);
                    customer.setReadyTime(readyTime);
                    // Score constraint arrivalAfterDueTimeAtDepot is a built-in hard constraint in VehicleRoutingImporter
//                    long maximumDueTime = depot.getDueTime()
//                            - serviceDuration - location.getDistanceTo(depot.getLocation());
//                    if (dueTime > maximumDueTime) {
//                        logger.warn("The customer ({})'s dueTime ({}) was automatically reduced" +
//                                " to maximumDueTime ({}) because of the depot's dueTime ({}).",
//                                customer, dueTime, maximumDueTime, depot.getDueTime());
//                        dueTime = maximumDueTime;
//                    }
                    customer.setDueTime(dueTime);
                    customer.setServiceDuration(serviceDuration);
                    // Notice that we leave the PlanningVariable properties on null
                    // Do not add a customer that has no demand
                    if (demand != 0) {
                        customerList.add(customer);
                    }
                }
                line = bufferedReader.readLine();
            }
            solution.setLocationList(locationList);
            solution.setDepotList(depotList);
            solution.setCustomerList(customerList);
            customerListSize = locationList.size();
        }

    }

}

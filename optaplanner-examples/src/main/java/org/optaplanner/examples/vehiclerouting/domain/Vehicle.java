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

package org.optaplanner.examples.vehiclerouting.domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.optaplanner.core.api.domain.variable.CustomShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableReference;
import org.optaplanner.examples.common.domain.AbstractPersistable;
import org.optaplanner.examples.vehiclerouting.domain.location.Location;
import org.optaplanner.examples.vehiclerouting.domain.timewindowed.solver.VehicleCapacityReuseVariableListener;

@XStreamAlias("VrpVehicle")
public class Vehicle extends AbstractPersistable implements Standstill {

    protected int capacity;
    protected Depot depot;
    protected Integer minReturnToDepotTime;
    protected Integer maxReturnToDepotTime;

    // Shadow variables
    protected Customer nextCustomer;

    @CustomShadowVariable(variableListenerClass = VehicleCapacityReuseVariableListener.class,
            sources = {@PlanningVariableReference(entityClass = Customer.class, variableName = "previousStandstill" )})
    protected Integer currentDemand;

    protected Long returnToDepotTime;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        this.depot = depot;
    }

    @Override
    public Customer getNextCustomer() {
        return nextCustomer;
    }

    @Override
    public void setNextCustomer(Customer nextCustomer) {
        this.nextCustomer = nextCustomer;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public Vehicle getVehicle() {
        return this;
    }

    @Override
    public Location getLocation() {
        return depot.getLocation();
    }

    /**
     * @param standstill never null
     * @return a positive number, the distance multiplied by 1000 to avoid floating point arithmetic rounding errors
     */
    public long getDistanceTo(Standstill standstill) {
        return depot.getDistanceTo(standstill);
    }

    @Override
    public String toString() {
        Location location = getLocation();
        if (location.getName() == null) {
            return super.toString();
        }
        return location.getName() + "/" + super.toString();
    }

    public Integer getCurrentDemand() {
        return currentDemand;
    }

    public void setCurrentDemand(Integer currentDemand) {
        this.currentDemand = currentDemand;
        System.out.println("----------------" + currentDemand);
    }

    public Long getReturnToDepotTime() {
        return returnToDepotTime;
    }

    public void setReturnToDepotTime(Long returnToDepotTime) {
        this.returnToDepotTime = returnToDepotTime;
    }

    public Integer getMinReturnToDepotTime() {
        return minReturnToDepotTime;
    }

    public void setMinReturnToDepotTime(Integer minReturnToDepotTime) {
        this.minReturnToDepotTime = minReturnToDepotTime;
    }

    public Integer getMaxReturnToDepotTime() {
        return maxReturnToDepotTime;
    }

    public void setMaxReturnToDepotTime(Integer maxReturnToDepotTime) {
        this.maxReturnToDepotTime = maxReturnToDepotTime;
    }

}

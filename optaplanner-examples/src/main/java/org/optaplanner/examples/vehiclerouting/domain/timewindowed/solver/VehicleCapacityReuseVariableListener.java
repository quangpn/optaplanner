package org.optaplanner.examples.vehiclerouting.domain.timewindowed.solver;

import org.optaplanner.core.impl.domain.variable.listener.VariableListener;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.examples.vehiclerouting.domain.Customer;
import org.optaplanner.examples.vehiclerouting.domain.Standstill;
import org.optaplanner.examples.vehiclerouting.domain.Vehicle;
import org.optaplanner.examples.vehiclerouting.domain.timewindowed.TimeWindowedCustomer;

public class VehicleCapacityReuseVariableListener implements VariableListener<Customer> {
    @Override
    public void beforeEntityAdded(ScoreDirector scoreDirector, Customer customer) {

    }

    @Override
    public void afterEntityAdded(ScoreDirector scoreDirector, Customer customer) {
        if (customer instanceof TimeWindowedCustomer) {
            updateVehicleDemandTotal(scoreDirector, (TimeWindowedCustomer) customer);
        }
    }

    @Override
    public void beforeVariableChanged(ScoreDirector scoreDirector, Customer customer) {

    }

    @Override
    public void afterVariableChanged(ScoreDirector scoreDirector, Customer customer) {
        if (customer instanceof TimeWindowedCustomer) {
            updateVehicleDemandTotal(scoreDirector, (TimeWindowedCustomer) customer);
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector scoreDirector, Customer customer) {

    }

    @Override
    public void afterEntityRemoved(ScoreDirector scoreDirector, Customer customer) {

    }

    protected void updateVehicleDemandTotal(ScoreDirector scoreDirector, Customer sourceCustomer) {
        Standstill previousStandstill = sourceCustomer.getPreviousStandstill();
        Vehicle vehicle = previousStandstill == null ? null : previousStandstill.getVehicle();
        TimeWindowedCustomer shadowCustomer = (TimeWindowedCustomer) sourceCustomer;
        Integer currentDemand = shadowCustomer.getDemand();

        Vehicle currentVehicle = shadowCustomer.getVehicle();
        Vehicle nextVehicle = currentVehicle;
        while (shadowCustomer != null) {
            scoreDirector.beforeVariableChanged(shadowCustomer, "vehicle");

            currentVehicle = shadowCustomer.getVehicle();

            scoreDirector.afterVariableChanged(shadowCustomer, "vehicle");

            shadowCustomer = shadowCustomer.getNextCustomer();
            if (shadowCustomer != null) {
                nextVehicle = shadowCustomer.getVehicle();
                if (!currentVehicle.equals(nextVehicle)){
                    scoreDirector.beforeVariableChanged(nextVehicle, "currentDemand");
                    nextVehicle.setCurrentDemand(currentDemand);
                    scoreDirector.afterVariableChanged(nextVehicle, "currentDemand");
                    currentVehicle.setCurrentDemand(currentVehicle.getCurrentDemand() - currentDemand);
                    currentDemand = nextVehicle.getCurrentDemand();
                }
                currentDemand += shadowCustomer.getDemand();
                nextVehicle.setCurrentDemand(currentDemand);
            }
        }
    }
}

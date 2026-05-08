package ar.edu.itba.sds.sistema2.sim;

import ar.edu.itba.sds.sistema2.core.Particle;
import ar.edu.itba.sds.sistema2.integrators.Integrator2D;
import ar.edu.itba.sds.sistema2.physics.ForceModel;

import java.util.List;

public final class Simulator2D {
    public final List<Particle> particles;
    public final ForceModel forceModel;
    public final Integrator2D integrator;
    public final double dt;
    public final double tf;
    public final int totalSteps;

    public Simulator2D(List<Particle> particles, ForceModel forceModel, Integrator2D integrator,
                       double dt, double tf) {
        this.particles = particles;
        this.forceModel = forceModel;
        this.integrator = integrator;
        this.dt = dt;
        this.tf = tf;
        this.totalSteps = (int) Math.round(tf / dt);
    }

    public interface StepHook {
        void onStep(int step, double t, List<Particle> particles, ForceModel fm);
    }

    public void run(StepHook hook) {
        if (hook != null) hook.onStep(0, 0.0, particles, forceModel);
        for (int step = 1; step <= totalSteps; step++) {
            integrator.step(dt);
            double t = step * dt;
            if (hook != null) hook.onStep(step, t, particles, forceModel);
        }
    }
}

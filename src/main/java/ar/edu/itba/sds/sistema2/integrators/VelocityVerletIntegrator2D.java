package ar.edu.itba.sds.sistema2.integrators;

import ar.edu.itba.sds.sistema2.ForceModel;
import ar.edu.itba.sds.sistema2.Particle;

import java.util.List;

public final class VelocityVerletIntegrator2D implements Integrator2D {
    private final List<Particle> particles;
    private final ForceModel forceModel;

    public VelocityVerletIntegrator2D(List<Particle> particles, ForceModel forceModel) {
        this.particles = particles;
        this.forceModel = forceModel;
        forceModel.compute(particles);
    }

    @Override public String name() { return "velocity_verlet"; }

    @Override public void step(double dt) {
        double halfDt = 0.5 * dt;
        for (Particle p : particles) {
            p.vel.x += halfDt * p.acc.x;
            p.vel.y += halfDt * p.acc.y;
            p.pos.x += dt * p.vel.x;
            p.pos.y += dt * p.vel.y;
        }
        forceModel.compute(particles);
        for (Particle p : particles) {
            p.vel.x += halfDt * p.acc.x;
            p.vel.y += halfDt * p.acc.y;
        }
    }
}

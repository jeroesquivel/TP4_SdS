package ar.edu.itba.sds.sistema2.integrators;

import ar.edu.itba.sds.sistema2.ForceModel;
import ar.edu.itba.sds.sistema2.Particle;
import ar.edu.itba.sds.sistema2.Vec2;

import java.util.List;

public final class BeemanIntegrator2D implements Integrator2D {
    private final List<Particle> particles;
    private final ForceModel forceModel;
    private final Vec2[] aOld;
    private final Vec2[] aPrevOld;

    public BeemanIntegrator2D(List<Particle> particles, ForceModel forceModel) {
        this.particles = particles;
        this.forceModel = forceModel;
        int n = particles.size();
        this.aOld = new Vec2[n];
        this.aPrevOld = new Vec2[n];
        for (int i = 0; i < n; i++) {
            aOld[i] = new Vec2();
            aPrevOld[i] = new Vec2();
        }
        forceModel.compute(particles);
        for (Particle p : particles) p.accPrev.set(p.acc);
    }

    @Override public String name() { return "beeman"; }

    @Override public void step(double dt) {
        double dt2 = dt * dt;
        int n = particles.size();
        for (int i = 0; i < n; i++) {
            Particle p = particles.get(i);
            aOld[i].set(p.acc);
            aPrevOld[i].set(p.accPrev);
        }

        for (int i = 0; i < n; i++) {
            Particle p = particles.get(i);
            p.pos.x += p.vel.x * dt + (2.0 / 3.0) * aOld[i].x * dt2 - (1.0 / 6.0) * aPrevOld[i].x * dt2;
            p.pos.y += p.vel.y * dt + (2.0 / 3.0) * aOld[i].y * dt2 - (1.0 / 6.0) * aPrevOld[i].y * dt2;
        }

        forceModel.compute(particles);

        for (int i = 0; i < n; i++) {
            Particle p = particles.get(i);
            p.vel.x += (1.0 / 3.0) * p.acc.x * dt + (5.0 / 6.0) * aOld[i].x * dt - (1.0 / 6.0) * aPrevOld[i].x * dt;
            p.vel.y += (1.0 / 3.0) * p.acc.y * dt + (5.0 / 6.0) * aOld[i].y * dt - (1.0 / 6.0) * aPrevOld[i].y * dt;
            p.accPrev.set(aOld[i]);
        }
    }
}

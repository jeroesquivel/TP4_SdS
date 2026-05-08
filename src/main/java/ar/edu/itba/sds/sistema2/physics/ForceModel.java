package ar.edu.itba.sds.sistema2.physics;

import ar.edu.itba.sds.sistema2.core.Geometry;
import ar.edu.itba.sds.sistema2.core.Particle;

import java.util.List;

public final class ForceModel {
    public final double k;
    private final CellIndexMethod cim;
    private double potentialEnergy;

    public ForceModel(double k, CellIndexMethod cim) {
        this.k = k;
        this.cim = cim;
    }

    public double potentialEnergy() { return potentialEnergy; }

    public void compute(List<Particle> particles) {
        for (Particle p : particles) p.acc.zero();
        potentialEnergy = 0.0;

        cim.rebuild(particles);
        cim.forEachPair(particles, (i, j) -> {
            Particle pi = particles.get(i);
            Particle pj = particles.get(j);
            double dx = pi.pos.x - pj.pos.x;
            double dy = pi.pos.y - pj.pos.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            double xi = pi.radius + pj.radius - dist;
            if (xi <= 0.0 || dist == 0.0) return;
            double f = k * xi;
            double fx = f * dx / dist;
            double fy = f * dy / dist;
            pi.acc.x += fx / pi.mass;
            pi.acc.y += fy / pi.mass;
            pj.acc.x -= fx / pj.mass;
            pj.acc.y -= fy / pj.mass;
            potentialEnergy += 0.5 * k * xi * xi;
        });

        for (Particle p : particles) {
            double rNorm = p.pos.norm();
            if (rNorm > 0.0) {
                double xiObs = (p.radius + Geometry.R_OBSTACLE) - rNorm;
                if (xiObs > 0.0) {
                    double f = k * xiObs;
                    p.acc.x += f * (p.pos.x / rNorm) / p.mass;
                    p.acc.y += f * (p.pos.y / rNorm) / p.mass;
                    potentialEnergy += 0.5 * k * xiObs * xiObs;
                }
                double xiBor = rNorm - (Geometry.R - p.radius);
                if (xiBor > 0.0) {
                    double f = k * xiBor;
                    p.acc.x -= f * (p.pos.x / rNorm) / p.mass;
                    p.acc.y -= f * (p.pos.y / rNorm) / p.mass;
                    potentialEnergy += 0.5 * k * xiBor * xiBor;
                }
            }
        }
    }
}

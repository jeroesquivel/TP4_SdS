package ar.edu.itba.sds.sistema2.observables;

import ar.edu.itba.sds.sistema2.Geometry;
import ar.edu.itba.sds.sistema2.Particle;

import java.util.List;

public final class CfcTracker {
    private int cfc;
    private final int[] cfcPerStep;
    private int currentStep;

    public CfcTracker(int totalSteps) {
        this.cfcPerStep = new int[totalSteps + 1];
    }

    public void step(List<Particle> particles) {
        for (Particle p : particles) {
            double r = p.pos.norm();
            double xiObs = (p.radius + Geometry.R_OBSTACLE) - r;
            double xiBor = r - (Geometry.R - p.radius);

            if (xiObs > 0.0) {
                if (!p.inContactObs) {
                    p.inContactObs = true;
                    if (p.state == Particle.FRESH) {
                        p.state = Particle.USED;
                        cfc++;
                    }
                }
            } else {
                p.inContactObs = false;
            }

            if (xiBor > 0.0) {
                if (!p.inContactBorder) {
                    p.inContactBorder = true;
                    if (p.state == Particle.USED) {
                        p.state = Particle.FRESH;
                        if (!p.usedReachedBorder) p.usedReachedBorder = true;
                    }
                }
            } else {
                p.inContactBorder = false;
            }
        }
        cfcPerStep[currentStep] = cfc;
        currentStep++;
    }

    public int[] cfcPerStep() { return cfcPerStep; }
    public int currentStep() { return currentStep; }
}

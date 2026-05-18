package ar.edu.itba.sds.sistema1.sim;

import ar.edu.itba.sds.sistema1.core.Oscillator;
import ar.edu.itba.sds.sistema1.core.Trajectory;
import ar.edu.itba.sds.sistema1.integrators.Integrator;

public final class Simulator {
    private Simulator() {}

    public static Trajectory run(Integrator integrator, double dt, double tf, int sampleEvery) {
        int totalSteps = (int) Math.round(tf / dt);
        int nOut = totalSteps / sampleEvery + 1;
        double[] t = new double[nOut];
        double[] r = new double[nOut];
        double[] v = new double[nOut];
        int outIdx = 0;
        t[outIdx] = 0.0;
        r[outIdx] = integrator.r();
        v[outIdx] = integrator.v();
        outIdx++;
        for (int step = 1; step <= totalSteps; step++) {
            integrator.step();
            if (step % sampleEvery == 0 && outIdx < nOut) {
                t[outIdx] = step * dt;
                r[outIdx] = integrator.r();
                v[outIdx] = integrator.v();
                outIdx++;
            }
        }
        if (outIdx < nOut) {
            double[] tt = new double[outIdx];
            double[] rr = new double[outIdx];
            double[] vv = new double[outIdx];
            System.arraycopy(t, 0, tt, 0, outIdx);
            System.arraycopy(r, 0, rr, 0, outIdx);
            System.arraycopy(v, 0, vv, 0, outIdx);
            return new Trajectory(tt, rr, vv);
        }
        return new Trajectory(t, r, v);
    }

    public static double computeECM(Integrator integrator, double dt, double tf) {
        int totalSteps = (int) Math.round(tf / dt);
        double sumSq = (integrator.r() - Oscillator.analyticalR(0.0));
        sumSq = sumSq * sumSq;
        for (int step = 1; step <= totalSteps; step++) {
            integrator.step();
            double t = step * dt;
            double err = integrator.r() - Oscillator.analyticalR(t);
            sumSq += err * err;
        }
        return sumSq / (totalSteps + 1);
    }
}

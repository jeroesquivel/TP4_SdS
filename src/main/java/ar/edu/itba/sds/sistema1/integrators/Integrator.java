package ar.edu.itba.sds.sistema1.integrators;

public interface Integrator {
    String name();
    void step();
    double r();
    double v();
}

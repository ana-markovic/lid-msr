package org.crossflow.tests.techrankHistoric;

public class LinRegPoint {
    private final double x;
    private final double y;
    private final String repository;

    public LinRegPoint(double x, double y, String repository) {
        this.x = x;
        this.y = y;
        this.repository = repository;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getRepository() {
        return repository;
    }
}

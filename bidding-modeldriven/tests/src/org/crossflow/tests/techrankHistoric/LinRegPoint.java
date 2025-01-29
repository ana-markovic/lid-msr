package org.crossflow.tests.techrankHistoric;

public class LinRegPoint {
    private double x;
    private double y;
    private String repository;

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

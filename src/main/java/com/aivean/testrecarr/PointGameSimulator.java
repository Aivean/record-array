package com.aivean.testrecarr;

import com.aivean.recarr.Record;

import java.util.Random;

/**
 * Naively simulates "brownian motion" for a points in a 2D space.
 */
public abstract class PointGameSimulator {

    public static final float POINT_RADIUS = 0.02f;

    @Record
    public interface Point2D {
        float getX();

        float getY();

        float getVx();

        float getVy();

        void setX(float x);

        void setY(float y);

        void setVx(float vx);

        void setVy(float vy);

        Point2D copy();
    }

    abstract int getNumPoints();

    abstract Point2D getPoint(int index);

    long wallCollisions = 0;
    long pointCollisions = 0;

    /**
     * Randomly initializes the points.
     */
    public void init() {
        // set position using stable pseudo-random number generator
        Random r = new Random(123);
        for (int i = 0; i < getNumPoints(); i++) {
            Point2D p = getPoint(i);
            p.setX(r.nextFloat() * 2 - 1);
            p.setY(r.nextFloat() * 2 - 1);
            p.setVx(r.nextFloat() * 0.01f - 0.02f);
            p.setVy(r.nextFloat() * 0.01f - 0.02f);
        }
    }

    /**
     * Simulates a single step of the simulation.
     */
    public void simulate() {
        simulateMotion();
        simulateWallCollisions();
        simulatePointToPointCollisions();
    }

    void simulatePointToPointCollisions() {
        // collision detection
        for (int i = 0; i < getNumPoints(); i++) {
            Point2D p = getPoint(i);
            float px = p.getX();
            float py = p.getY();
            for (int j = i + 1; j < getNumPoints(); j++) {
                Point2D q = getPoint(j);
                float dx = px - q.getX();
                float dy = py - q.getY();
                float d2 = dx * dx + dy * dy;

                // collision
                if (d2 < POINT_RADIUS * POINT_RADIUS * 4) {
                    // check if d2 is 0 to avoid division by zero
                    float d; // distance
                    if (Float.compare(d2, 0f) == 0) {
                        d = 0.00001f;
                    } else {
                        d = (float) Math.sqrt(d2);
                    }

                    float tx = -dy / d; // tangent vector of the impact (normalized)
                    float ty = dx / d;
                    float vix = p.getVx() - q.getVx(); // impact velocity (relative velocity)
                    float viy = p.getVy() - q.getVy();
                    float vt = vix * tx + viy * ty; // impact velocity along the tangent vector
                    float vpx = vt * tx; // vector component parallel to the tangent
                    float vpy = vt * ty;
                    float vnx = vix - vpx; // vector component perpendicular to the tangent
                    float vny = viy - vpy;
                    p.setVx(p.getVx() - vnx);
                    p.setVy(p.getVy() - vny);
                    q.setVx(q.getVx() + vnx);
                    q.setVy(q.getVy() + vny);

                    pointCollisions++;
                }
            }
        }
    }

    private void simulateWallCollisions() {
        // collision with walls
        for (int i = 0; i < getNumPoints(); i++) {
            Point2D p = getPoint(i);
            if (p.getX() < -1) {
                p.setX(-1);
                p.setVx(-p.getVx());
                wallCollisions++;
            }
            if (p.getX() > 1) {
                p.setX(1);
                p.setVx(-p.getVx());
                wallCollisions++;
            }
            if (p.getY() < -1) {
                p.setY(-1);
                p.setVy(-p.getVy());
                wallCollisions++;
            }
            if (p.getY() > 1) {
                p.setY(1);
                p.setVy(-p.getVy());
                wallCollisions++;
            }
        }
    }

    private void simulateMotion() {
        for (int i = 0; i < getNumPoints(); i++) {
            Point2D p = getPoint(i);
            p.setX(p.getX() + p.getVx());
            p.setY(p.getY() + p.getVy());
        }
    }

}

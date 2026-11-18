package com.nothatcher.vehicleforge.client.model;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A vehicle model: lists of quads grouped by how they animate. Built once in code with the Builder.
 * Local space: x = LEFT, y = up, z = forward (front of the vehicle). Units = blocks.
 */
public final class Model {
    public enum Kind {
        BODY, WHEEL, STEER_WHEEL, NEEDLE, ROTOR_Y, ROTOR_X, PROP_Z, TURRET, BARREL, BIKE_FORK,
        LADDER, FLAME, SIREN_A, SIREN_B, FLAG
    }

    public static final class Quad {
        public final float[] p = new float[12];
        public final float[] uv = new float[8];
        public float nx, ny, nz;
        public Mat mat;
    }

    public static final class Group {
        public final Kind kind;
        public final Vector3f pivot;
        /** wheels: radius; steering wheel / gauge: tilt degrees */
        public final float param;
        /** wheels: steers with the front axle */
        public final boolean steers;
        public final List<Quad> quads = new ArrayList<>();

        Group(Kind kind, Vector3f pivot, float param, boolean steers) {
            this.kind = kind;
            this.pivot = pivot;
            this.param = param;
            this.steers = steers;
        }
    }

    public final List<Group> groups = new ArrayList<>();

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Model model = new Model();
        private Group current;

        Builder() {
            group(Kind.BODY, 0, 0, 0, 0, false);
        }

        public Builder group(Kind kind, double px, double py, double pz, double param, boolean steers) {
            current = new Group(kind, new Vector3f((float) px, (float) py, (float) pz), (float) param, steers);
            model.groups.add(current);
            return this;
        }

        public Builder body() {
            return group(Kind.BODY, 0, 0, 0, 0, false);
        }

        public Model build() {
            model.groups.removeIf(g -> g.quads.isEmpty());
            return model;
        }

        // ------------------------------------------------------------ quads

        public Builder quad(double[] a, double[] b, double[] c, double[] d, Mat m) {
            Quad q = new Quad();
            double[][] pts = {a, b, c, d};
            for (int i = 0; i < 4; i++) {
                q.p[i * 3] = (float) pts[i][0];
                q.p[i * 3 + 1] = (float) pts[i][1];
                q.p[i * 3 + 2] = (float) pts[i][2];
            }
            float[] uv = {m.u0(), m.v1(), m.u1(), m.v1(), m.u1(), m.v0(), m.u0(), m.v0()};
            System.arraycopy(uv, 0, q.uv, 0, 8);
            // normal from the diagonals
            double ux = c[0] - a[0], uy = c[1] - a[1], uz = c[2] - a[2];
            double vx = d[0] - b[0], vy = d[1] - b[1], vz = d[2] - b[2];
            double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1.0E-9) {
                nx = 0;
                ny = 1;
                nz = 0;
                len = 1;
            }
            q.nx = (float) (nx / len);
            q.ny = (float) (ny / len);
            q.nz = (float) (nz / len);
            q.mat = m;
            current.quads.add(q);
            return this;
        }

        private static double[] v(double x, double y, double z) {
            return new double[]{x, y, z};
        }

        /** Hexahedron from bottom rectangle (y0) and top rectangle (y1); great for tapered cabins and hoods. */
        public Builder hull(double bx0, double bx1, double bz0, double bz1, double y0,
                            double tx0, double tx1, double tz0, double tz1, double y1, Mat side, Mat top) {
            double[] b00 = v(bx0, y0, bz0), b10 = v(bx1, y0, bz0), b11 = v(bx1, y0, bz1), b01 = v(bx0, y0, bz1);
            double[] t00 = v(tx0, y1, tz0), t10 = v(tx1, y1, tz0), t11 = v(tx1, y1, tz1), t01 = v(tx0, y1, tz1);
            quad(t00, t01, t11, t10, top);           // top
            quad(b00, b10, b11, b01, side);          // bottom
            quad(b01, b11, t11, t01, side);          // front (+z)
            quad(b10, b00, t00, t10, side);          // back (-z)
            quad(b11, b10, t10, t11, side);          // +x
            quad(b00, b01, t01, t00, side);          // -x
            return this;
        }

        public Builder box(double x0, double y0, double z0, double x1, double y1, double z1, Mat m) {
            return hull(x0, x1, z0, z1, y0, x0, x1, z0, z1, y1, m, m);
        }

        public Builder box(double x0, double y0, double z0, double x1, double y1, double z1, Mat side, Mat top) {
            return hull(x0, x1, z0, z1, y0, x0, x1, z0, z1, y1, side, top);
        }

        /** Mirror across x = 0 (the vehicle is symmetric). */
        public Builder pair(double x0, double y0, double z0, double x1, double y1, double z1, Mat m) {
            box(x0, y0, z0, x1, y1, z1, m);
            return box(-x1, y0, z0, -x0, y1, z1, m);
        }

        /** A sloped glass pane between two horizontal edges (windshields, side windows). */
        public Builder pane(double xa0, double xa1, double ya, double za, double xb0, double xb1, double yb, double zb, Mat m) {
            return quad(v(xa0, ya, za), v(xa1, ya, za), v(xb1, yb, zb), v(xb0, yb, zb), m);
        }

        /** Vertical side pane at fixed x spanning z0..z1 with slanted ends. */
        public Builder sidePane(double x, double y0, double y1, double z0b, double z1b, double z0t, double z1t, Mat m) {
            return quad(v(x, y0, z0b), v(x, y0, z1b), v(x, y1, z1t), v(x, y1, z0t), m);
        }

        /** Cylinder / cone along an axis ('x', 'y' or 'z'), from centre c to c + len along axis. */
        public Builder cyl(double cx, double cy, double cz, char axis, double r0, double r1, double len, int segs, Mat side, Mat cap) {
            for (int i = 0; i < segs; i++) {
                double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
                double[] p00 = ring(cx, cy, cz, axis, r0, a0, 0), p01 = ring(cx, cy, cz, axis, r0, a1, 0);
                double[] p10 = ring(cx, cy, cz, axis, r1, a0, len), p11 = ring(cx, cy, cz, axis, r1, a1, len);
                quad(p00, p01, p11, p10, side);
                if (cap != null) {
                    double[] c0 = ring(cx, cy, cz, axis, 0, 0, 0), c1 = ring(cx, cy, cz, axis, 0, 0, len);
                    if (r0 > 0) quad(c0, c0, p01, p00, cap);
                    if (r1 > 0) quad(c1, c1, p10, p11, cap);
                }
            }
            return this;
        }

        private static double[] ring(double cx, double cy, double cz, char axis, double r, double a, double along) {
            double s = Math.sin(a) * r, c = Math.cos(a) * r;
            return switch (axis) {
                case 'x' -> v(cx + along, cy + s, cz + c);
                case 'y' -> v(cx + c, cy + along, cz + s);
                default -> v(cx + s, cy + c, cz + along);
            };
        }

        /** A wheel with tyre, rim and hub, animated (spins; front wheels steer). */
        public Builder wheel(double x, double y, double z, double radius, double width, boolean steers) {
            group(Kind.WHEEL, x, y, z, radius, steers);
            double half = width / 2;
            cyl(x - half, y, z, 'x', radius, radius, width, 14, Mat.TIRE, Mat.RUBBER);
            double side = x > 0 ? half + 0.01 : -half - 0.01;
            cyl(x + side - (x > 0 ? 0.0 : 0.0), y, z, 'x', radius * 0.62, radius * 0.62, x > 0 ? 0.02 : -0.02, 10, Mat.RIM, Mat.RIM);
            box(x + side - 0.03, y - radius * 0.15, z - radius * 0.15, x + side + 0.03, y + radius * 0.15, z + radius * 0.15, Mat.CHROME);
            return body();
        }

        public Builder wheels(double xOut, double y, double zFront, double zBack, double radius, double width) {
            wheel(xOut, y, zFront, radius, width, true);
            wheel(-xOut, y, zFront, radius, width, true);
            wheel(xOut, y, zBack, radius, width, false);
            wheel(-xOut, y, zBack, radius, width, false);
            return this;
        }

        /** Seat: cushion top at cushionY, facing forward. */
        public Builder seat(double x, double cushionY, double z, double w, Mat m) {
            box(x - w / 2, cushionY - 0.15, z - 0.3, x + w / 2, cushionY, z + 0.25, m);
            return box(x - w / 2, cushionY, z - 0.38, x + w / 2, cushionY + 0.7, z - 0.25, m);
        }

        /** Steering wheel facing the driver (in -z direction), tilted back by tilt degrees. */
        public Builder steeringWheel(double x, double y, double z, double r, double tiltDeg) {
            group(Kind.STEER_WHEEL, x, y, z, tiltDeg, false);
            int n = 10;
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                double[] o0 = v(x + Math.cos(a0) * r, y + Math.sin(a0) * r, z), o1 = v(x + Math.cos(a1) * r, y + Math.sin(a1) * r, z);
                double[] i0 = v(x + Math.cos(a0) * r * 0.78, y + Math.sin(a0) * r * 0.78, z), i1 = v(x + Math.cos(a1) * r * 0.78, y + Math.sin(a1) * r * 0.78, z);
                quad(i0, i1, o1, o0, Mat.BLACK);
            }
            box(x - r * 0.8, y - 0.02, z - 0.01, x + r * 0.8, y + 0.02, z + 0.01, Mat.BLACK);
            box(x - 0.05, y - r * 0.8, z - 0.01, x + 0.05, y, z + 0.01, Mat.BLACK);
            box(x - 0.06, y - 0.06, z - 0.02, x + 0.06, y + 0.06, z + 0.02, Mat.CHROME);
            return body();
        }

        /** Speedometer face with a needle that follows the vehicle's speed. Faces the driver (-z). */
        public Builder gauge(double x, double y, double z, double size) {
            double h = size / 2;
            quad(v(x - h, y - h, z), v(x + h, y - h, z), v(x + h, y + h, z), v(x - h, y + h, z), Mat.GAUGE);
            group(Kind.NEEDLE, x, y, z - 0.01, 0, false);
            box(x - 0.012, y, z - 0.015, x + 0.012, y + h * 0.85, z - 0.008, Mat.RED);
            return body();
        }

        /** Dashboard slab with gauges and a centre screen. */
        public Builder dashboard(double xl, double xr, double y, double z, double driverX) {
            box(xr, y - 0.25, z - 0.1, xl, y, z + 0.3, Mat.DASH);
            gauge(driverX, y + 0.05, z - 0.11, 0.2);
            box(-0.12, y - 0.2, z - 0.115, 0.12, y - 0.04, z - 0.1, Mat.SCREEN);
            return this;
        }
    }
}

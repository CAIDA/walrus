// 
// The Walrus Graph Visualization Tool.
// Copyright (C) 2000,2001,2002 The Regents of the University of California.
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// 
// ######END_HEADER######
// 


import javax.vecmath.*;

public class H3Math
{
    ////////////////////////////////////////////////////////////////////////
    // PUBLIC CONSTANTS
    ////////////////////////////////////////////////////////////////////////

    public static final double EPSILON = 1e-10;
    public static final double TWO_PI = 2.0 * Math.PI;
    public static final double HALF_PI = Math.PI / 2.0;

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public static boolean isFinite(double x)
    {
	return !Double.isNaN(x) && !Double.isInfinite(x);
    }

    public static boolean isFinite(Point4d p)
    {
	return isFinite(p.x) && isFinite(p.y)
	    && isFinite(p.z) && isFinite(p.w);
    }

    public static boolean isFinite(Matrix4d m)
    {
	return isFinite(m.m00) && isFinite(m.m01) && isFinite(m.m02) && isFinite(m.m03)
	    && isFinite(m.m10) && isFinite(m.m11) && isFinite(m.m12) && isFinite(m.m13)
	    && isFinite(m.m20) && isFinite(m.m21) && isFinite(m.m22) && isFinite(m.m23)
	    && isFinite(m.m30) && isFinite(m.m31) && isFinite(m.m32) && isFinite(m.m33);
    }

    public static void validateFinite(Point4d p)
    {
	if (!isFinite(p))
	{
	    String msg = "ERROR: invalid coordinates: [" + p.x + ", "
		+ p.y + ", " + p.z + ", " + p.w + "]";
	    throw new RuntimeException(msg);
	}
    }

    public static void validateFinite(Matrix4d m)
    {
	if (!isFinite(m))
	{
	    String msg = "ERROR: invalid component(s) in matrix";
	    throw new RuntimeException(msg);
	}
    }

    public static boolean epsilonZero(double x)
    {
	return Math.abs(x) < EPSILON;
    }

    // -inf < x < inf
    public static double sinh(double x)
    {
	return (Math.exp(x) - Math.exp(-x)) / 2.0;
    }

    // -inf < x < inf
    public static double cosh(double x)
    {
	return (Math.exp(x) + Math.exp(-x)) / 2.0;
    }

    // -inf < x < inf
    public static double tanh(double x)
    {
	double expX = Math.exp(x);
	double exp_X = Math.exp(-x);

	return (expX - exp_X) / (expX + exp_X);
    }

    // -inf < x < inf
    public static double asinh(double x)
    {
	return Math.log(x + Math.sqrt(x * x + 1));
    }

    // x >= 1
    public static double acosh(double x)
    {
	return Math.log(x + Math.sqrt(x * x - 1));
    }

    // | x | < 1
    public static double atanh(double x)
    {
	return Math.log((1.0 + x) / (1.0 - x)) / 2.0;
    }

    /**
     * Converts a distance given in the hyperbolic metric to the corresponding
     * distance in the usual Euclidean metric of the Klein model.
     *
     * This function is an inversion of a restriction of the Klein metric
     * given in "Visualizing Hyperbolic Space: Unusual Uses of 4x4 Matrices"
     * by Phillips and Gunn.
     *
     * NOTE: This assumes that x specifies a point lying on the positive
     *       x-axis and computes the Euclidean distance from the origin to x.
     *       Because of the non-linear mapping between the metrics, this
     *       assumption is significant: a constant distance in the hyperbolic
     *       metric can map to an infinite number of different distances in
     *       the Klein metric, depending on where the distance was originally
     *       measured in hyperbolic space.  That is to say that in mapping
     *       from the hyperbolic metric to the Klein metric, we need not only
     *       the distance but also the positions of the endpoints used to
     *       measure the distance.
     */
    public static double euclideanDistance(double x)
    {
	double y = cosh(x / 2.0);
	return Math.sqrt(1.0 - 1.0 / (y * y));
    }    

    // This is the Klein metric given in "Visualizing Hyperbolic Space:
    // Unusual Uses of 4x4 Matrices" by Phillips and Gunn.

    public static double hyperbolicDistance(Point3d x, Point3d y)
    {
	double t1 = dotProduct(x, y) - 1.0;
	double t2 = dotProduct(x, x) - 1.0;
	double t3 = dotProduct(y, y) - 1.0;	

	return 2.0 * acosh(Math.sqrt((t1 * t1) / (t2 * t3)));
    }

    // This is the Klein metric given in "Visualizing Hyperbolic Space:
    // Unusual Uses of 4x4 Matrices" by Phillips and Gunn.

    public static double hyperbolicDistance(Point4d x, Point4d y)
    {
	double t1 = minkowski(x, y);
	double t2 = minkowski(x, x);
	double t3 = minkowski(y, y);

	return 2.0 * acosh(Math.sqrt((t1 * t1) / (t2 * t3)));
    }

    // 0 <= r < 1
    public static double kleinToPoincare(double r)
    {
	return r / (1.0 + Math.sqrt(1.0 - r * r));
    }

    // 0 <= r < 1
    public static double poincareToKlein(double r)
    {
	return 2.0 * r / (1.0 + r * r);
    }

    public static double dotProduct(Point3d x, Point3d y)
    {
	return x.x*y.x + x.y*y.y + x.z*y.z;
    }

    public static double dotProduct(Point4d x, Point4d y)
    {
	return (x.x*y.x + x.y*y.y + x.z*y.z) / (x.w * y.w);
    }

    public static double minkowski(Point4d x, Point4d y)
    {
	return x.x*y.x + x.y*y.y + x.z*y.z - x.w*y.w;
    }

    public static double vectorLength(Tuple4d p)
    {
	double w2 = p.w * p.w;
	return Math.sqrt((p.x*p.x + p.y*p.y + p.z*p.z) / w2);
    }

    public static void makeUnitVector(Tuple4d p)
    {
	double s = p.w * vectorLength(p);
	p.x /= s;
	p.y /= s;
	p.z /= s;
	p.w = 1.0;
    }

    // The following two methods (computeRadiusHyperbolic and
    // computeRadiusEuclidean) are very special-purpose methods.  They're
    // used to calculate the visible size of nodes, when the rendering of
    // nodes at multiple sizes is enabled.  These are called in H3Transformer
    // and in H3NonadaptiveRenderLoop.

    // Returns 1.0 / d_hyp(0, p), where d_hyp is the hyperbolic metric
    // (simplified) and 0 is the origin.
    public static double computeRadiusHyperbolic(Point4d p)
    {
	double d = (p.x * p.x + p.y * p.y + p.z * p.z) / (p.w * p.w);
	double a = Math.sqrt(1.0 / (1.0 - d));
	double b = Math.sqrt(d / (1.0 - d));
	return 1.0 / (1.0 + 2.0 * Math.log(a + b));
    }

    // This seems to be about ten times faster than computeRadiusHyperbolic().
    public static double computeRadiusEuclidean(Point4d p)
    {
	double d = (p.x * p.x + p.y * p.y + p.z * p.z) / (p.w * p.w);
	return 1.0 - d;
    }
}

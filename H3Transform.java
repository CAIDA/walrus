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


//--------------------------------------------------------------------------
// Nov 4, 2002
//
// The routines buildTranslation(), buildReflection(), and findPivotPoint()
// have been rewritten from scratch without consulting Jim Donohoe's original
// code.  The new code is unavoidably very similar to the old since these
// routines are basically transcriptions of formulas from Phillips and Gunn's
// paper "Visualizing Hyperbolic Space: Unusual Uses of 4x4 Matrices."  Some
// comments describing the routines have been preserved.
//
// Also, buildEuclideanRotation() has been simply deleted, since it is never
// used in Walrus.
//
//--------------------------------------------------------------------------

import javax.vecmath.*;
import mpfun.*;

public class H3Transform
{
    //-------------------------------------------------------------------------
    // PUBLIC CONSTANTS
    //-------------------------------------------------------------------------

    public static final Point3d ORIGIN3 = new Point3d( 0.0, 0.0, 0.0 );
    public static final Point4d ORIGIN4 = new Point4d( 0.0, 0.0, 0.0, 1.0 );
    public static final Matrix4d I4 = new Matrix4d( 1.0, 0.0, 0.0, 0.0,
						     0.0, 1.0, 0.0, 0.0,
						     0.0, 0.0, 1.0, 0.0,
						     0.0, 0.0, 0.0, 1.0 );

    public static final H3Point4d ORIGIN4_MP =
	new H3Point4d(0.0, 0.0, 0.0, 1.0);

    public static final H3Matrix4d I4_MP =
	new H3Matrix4d( 1.0, 0.0, 0.0, 0.0,
			0.0, 1.0, 0.0, 0.0,
			0.0, 0.0, 1.0, 0.0,
			0.0, 0.0, 0.0, 1.0 );

    //-------------------------------------------------------------------------
    // PUBLIC STATIC METHODS
    //-------------------------------------------------------------------------
   
    /* NOTE: The points a and b must not both be the origin. */
    public static Matrix4d buildCanonicalOrientation(Point4d a, Point4d b)
    {
	/* local scratch variables; will be transformed */
	Point4d pa = new Point4d(a);
	Point4d pb = new Point4d(b);

	Point4d pivot = findPivotPoint(pa, pb);

	Matrix4d retval = buildTranslation(ORIGIN4, pivot);
       
	Matrix4d t1 = buildTranslation(pivot, ORIGIN4);
	t1.transform(pa);
	t1.transform(pb);

	retval.mul(buildTranslation(ORIGIN4, pa));

	Matrix4d t2 = buildTranslation(pa, ORIGIN4);
	t2.transform(pa);
	t2.transform(pb);

	/* calculate spherical coordinates (rho, phi, theta) of pb */

	// Projection to affine coordinates is necessary so that we can
	// directly reference the x, y, and z components in the following
	// calculations.
	pb.project(pb);

	double rho = H3Math.vectorLength(pb);
	double phi = Math.acos(pb.x / rho);
	double theta = Math.atan2(pb.z, pb.y);

	if (!H3Math.epsilonZero(phi))
	{
	    /* rotate line to achieve alignment on positive x-axis */
	    retval.mul(buildXRotation(theta));
	    retval.mul(buildZRotation(phi));
	}

	return retval;
    }

    /* NOTE: The points a and b must not both be the origin. */
    public static H3Matrix4d buildCanonicalOrientation
	(H3Point4d a, H3Point4d b)
    {
	/* local scratch variables; will be transformed */
	H3Point4d pa = new H3Point4d(a);
	H3Point4d pb = new H3Point4d(b);

	H3Point4d pivot = findPivotPoint(pa, pb);

	H3Matrix4d retval = buildTranslation(ORIGIN4_MP, pivot);
       
	H3Matrix4d t1 = buildTranslation(pivot, ORIGIN4_MP);
	t1.transform(pa);
	t1.transform(pb);

	retval.mul(buildTranslation(ORIGIN4_MP, pa));

	H3Matrix4d t2 = buildTranslation(pa, ORIGIN4_MP);
	t2.transform(pa);
	t2.transform(pb);

	/* calculate spherical coordinates (rho, phi, theta) of pb */

	// Projection to affine coordinates is necessary so that we can
	// directly reference the x, y, and z components in the following
	// calculations.
	pb.project(pb);

	double rho = pb.vectorLength().doubleValue();
	double phi = Math.acos(pb.x.doubleValue() / rho);
	double theta = Math.atan2(pb.z.doubleValue(), pb.y.doubleValue());

	if (!H3Math.epsilonZero(phi))
	{
	    /* rotate line to achieve alignment on positive x-axis */
	    retval.mul(buildXRotationMP(theta));
	    retval.mul(buildZRotationMP(phi));
	}

	return retval;
    }
   
    public static Matrix4d buildXRotation(double angle)
    {
	Matrix4d m = new Matrix4d();
	m.rotX(angle);
	return m;
    }

    public static Matrix4d buildYRotation(double angle)
    {
	Matrix4d m = new Matrix4d();
	m.rotY(angle);
	return m;
    }

    public static Matrix4d buildZRotation(double angle)
    {
	Matrix4d m = new Matrix4d();
	m.rotZ(angle);
	return m;
    }

    public static H3Matrix4d buildXRotationMP(double angle)
    {
	H3Matrix4d m = new H3Matrix4d();
	m.rotX(angle);
	return m;
    }

    public static H3Matrix4d buildYRotationMP(double angle)
    {
	H3Matrix4d m = new H3Matrix4d();
	m.rotY(angle);
	return m;
    }

    public static H3Matrix4d buildZRotationMP(double angle)
    {
	H3Matrix4d m = new H3Matrix4d();
	m.rotZ(angle);
	return m;
    }

    // Build a 4x4 matrix for hyperbolic translation from source point to
    // dest point.  From p. 212 of Phillips and Gunn paper:
    //   translate(source,dest)  =  reflect(midpoint) . reflect(source)
    public static Matrix4d buildTranslation(Point4d source, Point4d dest)
    {
	double aa_h = H3Math.minkowski(source, source);
	double bb_h = H3Math.minkowski(dest, dest);
	double ab_h = H3Math.minkowski(source, dest);
	double sourceScale = Math.sqrt(bb_h * ab_h);
	double destScale = Math.sqrt(aa_h * ab_h);
	Point4d midpoint = new Point4d();
	midpoint.x = sourceScale * source.x + destScale * dest.x;
	midpoint.y = sourceScale * source.y + destScale * dest.y;
	midpoint.z = sourceScale * source.z + destScale * dest.z;
	midpoint.w = sourceScale * source.w + destScale * dest.w;

	Matrix4d r_a = buildReflection(source);
	Matrix4d r_m = buildReflection(midpoint);
	r_m.mul(r_a);
	return r_m;
    }

    public static H3Matrix4d buildTranslation(H3Point4d source, H3Point4d dest)
    {
	MPReal aa_h = source.minkowski(source);
	MPReal bb_h = dest.minkowski(dest);
	MPReal ab_h = source.minkowski(dest);
	MPReal sourceScale = bb_h.multiply(ab_h).sqrt();
	MPReal destScale = aa_h.multiply(ab_h).sqrt();
	H3Point4d midpoint = new H3Point4d();
	midpoint.x =
	    sourceScale.multiply(source.x).add(destScale.multiply(dest.x));
	midpoint.y =
	    sourceScale.multiply(source.y).add(destScale.multiply(dest.y));
	midpoint.z =
	    sourceScale.multiply(source.z).add(destScale.multiply(dest.z));
	midpoint.w =
	    sourceScale.multiply(source.w).add(destScale.multiply(dest.w));

	H3Matrix4d r_a = buildReflection(source);
	H3Matrix4d r_m = buildReflection(midpoint);
	r_m.mul(r_a);
	return r_m;
    }

    // Build a 4x4 matrix for hyperbolic reflection across point p.  From
    // p. 211 of Phillips and Gunn paper:
    //       reflect_p = I(4) - 2 * p . pT * I(3,1) / <p,p>_h
    // where p . pT is the outer product of p with itself, <p,p>_h is the
    // Minkowski inner product.
    public static Matrix4d buildReflection(Point4d p)
    {
	double xx = p.x * p.x;
	double xy = p.x * p.y;
	double xz = p.x * p.z;
	double xw = p.x * p.w;

	double yy = p.y * p.y;
	double yz = p.y * p.z;
	double yw = p.y * p.w;

	double zz = p.z * p.z;
	double zw = p.z * p.w;

	double ww = p.w * p.w;

	Matrix4d ppTI31 = new Matrix4d(xx, xy, xz, -xw,
				       xy, yy, yz, -yw,
				       xz, yz, zz, -zw,
				       xw, yw, zw, -ww);

	double pp_h = xx + yy + zz - ww;
	ppTI31.mul(-2.0 / pp_h);

	ppTI31.m00 += 1.0;
	ppTI31.m11 += 1.0;
	ppTI31.m22 += 1.0;
	ppTI31.m33 += 1.0;

	return ppTI31;
    }

    public static H3Matrix4d buildReflection(H3Point4d p)
    {
	MPReal xx = p.x.multiply(p.x);
	MPReal xy = p.x.multiply(p.y);
	MPReal xz = p.x.multiply(p.z);
	MPReal xw = p.x.multiply(p.w);

	MPReal yy = p.y.multiply(p.y);
	MPReal yz = p.y.multiply(p.z);
	MPReal yw = p.y.multiply(p.w);

	MPReal zz = p.z.multiply(p.z);
	MPReal zw = p.z.multiply(p.w);

	MPReal ww = p.w.multiply(p.w);

	H3Matrix4d ppTI31 = new H3Matrix4d(xx, xy, xz, xw.negate(),
					   xy, yy, yz, yw.negate(),
					   xz, yz, zz, zw.negate(),
					   xw, yw, zw, ww.negate());

	MPReal pp_h = xx.add(yy).add(zz).subtract(ww);
	ppTI31.mul(new MPReal(-2.0).divide(pp_h));

	MPReal one = new MPReal(1.0);
        ppTI31.m00 = ppTI31.m00.add(one);
        ppTI31.m11 = ppTI31.m11.add(one);
        ppTI31.m22 = ppTI31.m22.add(one);
        ppTI31.m33 = ppTI31.m33.add(one);

	return ppTI31;
    }

    //-------------------------------------------------------------------------
    // PRIVATE STATIC METHODS
    //-------------------------------------------------------------------------

    // Find the point closest to the origin on the line in H3 passing
    // through the points a and b. From p. 212 of Phillips and Gunn paper:
    // 
    //               a . (a-b)         b . (b-a)
    //         p = ------------- b + ------------- a
    //             (a-b) . (a-b)     (b-a) . (b-a)
    //
    // --------------------------------------------------------------------
    // This is computed using affine coordinates and the usual dot product.
    // --------------------------------------------------------------------
    //
    // NOTE: By factoring out a -1, all the (b-a) become (a-b), reducing
    //       the number of operations.  This also allows us to factor
    //       out the denominator [(a-b).(a-b)] and make it the new
    //       homogeneous coordinate w, thus saving three divisions.
    //
    //               1
    //         p = ----- [ (a . d)b - (b . d)a ]
    //             d . d
    //
    //       where d = a - b.  Hence,
    //
    //         p_x = (a . d)b_x - (b . d)a_x
    //         p_y = (a . d)b_y - (b . d)a_y
    //         p_z = (a . d)b_z - (b . d)a_z
    //         p_w = d . d
    //
    private static Point4d findPivotPoint(Point4d a4, Point4d b4)
    {
	Point3d a = new Point3d();
	Point3d b = new Point3d();

	a.project(a4);
	b.project(b4);

	Point3d a_minus_b = new Point3d();
	a_minus_b.sub(a, b);

	double p = H3Math.dotProduct(a, a_minus_b);
	double q = H3Math.dotProduct(b, a_minus_b);
	double r = H3Math.dotProduct(a_minus_b, a_minus_b);

	return new Point4d(p * b.x - q * a.x,
			   p * b.y - q * a.y,
			   p * b.z - q * a.z,
			   r);
    }

    private static H3Point4d findPivotPoint(H3Point4d a4, H3Point4d b4)
    {
	H3Point4d a = new H3Point4d();
	H3Point4d b = new H3Point4d();

	a.project(a4);
	b.project(b4);

	H3Point4d a_minus_b = new H3Point4d(a);
	a_minus_b.sub(b);

	MPReal p = a.vectorDot3(a_minus_b);
	MPReal q = b.vectorDot3(a_minus_b);
	MPReal r = a_minus_b.vectorDot3(a_minus_b);

	MPReal pbx = p.multiply(b.x);  MPReal qax = q.multiply(a.x);
	MPReal pby = p.multiply(b.y);  MPReal qay = q.multiply(a.y);
	MPReal pbz = p.multiply(b.z);  MPReal qaz = q.multiply(a.z);

	return new H3Point4d(pbx.subtract(qax),
			     pby.subtract(qay),
			     pbz.subtract(qaz),
			     r);
    }
}

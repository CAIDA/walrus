// 
// Copyright 2000,2001,2002 The Regents of the University of California
// All Rights Reserved
// 
// Permission to use, copy, modify and distribute any part of this
// Walrus software package for educational, research and non-profit
// purposes, without fee, and without a written agreement is hereby
// granted, provided that the above copyright notice, this paragraph
// and the following paragraphs appear in all copies.
//   
// Those desiring to incorporate this into commercial products or use
// for commercial purposes should contact the Technology Transfer
// Office, University of California, San Diego, 9500 Gilman Drive, La
// Jolla, CA 92093-0910, Ph: (858) 534-5815, FAX: (858) 534-7345.
// 
// IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY
// PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL
// DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
//  
// THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE
// UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
// SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS. THE UNIVERSITY
// OF CALIFORNIA MAKES NO REPRESENTATIONS AND EXTENDS NO WARRANTIES
// OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
// PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE
// ANY PATENT, TRADEMARK OR OTHER RIGHTS.
//  
// The Walrus software is developed by the Walrus Team at the
// University of California, San Diego under the Cooperative Association
// for Internet Data Analysis (CAIDA) Program.  Support for this effort
// is provided by NSF grant ANI-9814421, DARPA NGI Contract N66001-98-2-8922,
// Sun Microsystems, and CAIDA members.
// 

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
   
    public static Matrix4d buildEuclideanRotation(double angle, Vector4d axis)
    {
	// Build the matrix for a rotation by the input angle around the axis
	// of rotation. The notation follows p. 213 of Phillips and Gunn paper.
	double c = Math.cos( angle );
	double s = Math.sin( angle );
	double c1 = 1.0 - c;
	double u1c1 = axis.x * c1;
	double u1u2c1 = u1c1 * axis.y;
	double u1u3c1 = u1c1 * axis.z;
	double u2u3c1 = axis.y * axis.z * c1;
	double u1s = axis.x * s;
	double u2s = axis.y * s;
	double u3s = axis.z * s;
	double u1_2 = axis.x * axis.x;
	double u2_2 = axis.y * axis.y;
	double u3_2 = axis.z * axis.z;

	return
	    new Matrix4d( u1_2 + c*(1.0-u1_2), u1u2c1 - u3s, u1u3c1 + u2s, 0.0,
			  u1u2c1 + u3s, u2_2 + c*(1.0-u2_2), u2u3c1 - u1s, 0.0,
			  u1u3c1 - u2s, u2u3c1 + u1s, u3_2 + c*(1.0-u3_2), 0.0,
			  0.0,          0.0,                 0.0,          1.0 );
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

    public static Matrix4d buildTranslation(Point4d source, Point4d dest)
    {
	// Build a 4x4 matrix for hyperbolic translation from source point to
	// dest point.  From p. 212 of Phillips and Gunn paper:
	//   translate(source,dest)  =  reflect(midpoint) . reflect(source)
	//
	// Midpoint m between hyperbolic points s and d, in homogeneous coords:
	//    m = SQRT(<d,d>_h * <s,d>_h) * s + SQRT(<s,s>_h * <s,d>_h) * d
	// where <s,d>_h is the Minkowski inner product:
	//    <s,d>_h = (s.x*d.x) + (s.y*d.y) + (s.z*d.z) - (s.w*d.w)

	// Store some of these in temporaries so they can be used when building
	// the reflection matrix.
	double sx2 = source.x * source.x;
	double sy2 = source.y * source.y;
	double sz2 = source.z * source.z;
	double sw2 = source.w * source.w;

	// Calculate Minkowski inner products <s,s>_h, <d,d>_h, and <s,d>_h.
	double s_s_MIP = sx2 + sy2 + sz2 - sw2;
	double d_d_MIP = (dest.x * dest.x) + (dest.y * dest.y) +
	    (dest.z * dest.z) - (dest.w * dest.w);
	double s_d_MIP = (source.x * dest.x) +
	    (source.y * dest.y) + (source.z * dest.z) - (source.w * dest.w);

	Point4d midpoint = new Point4d( source );
      
	// Scale the source point by SQRT(<d,d>_h * <s,d>_h)
	midpoint.scale( Math.sqrt( d_d_MIP * s_d_MIP ) );

	// Scale the dest point by SQRT(<s,s>_h * <s,d>_h) the add it to the
	// scaled source point.
	midpoint.scaleAdd( Math.sqrt( s_s_MIP * s_d_MIP ), dest, midpoint );

	// Build the reflection matrix for the midpoint.
	Matrix4d matrix = buildReflection( midpoint );

	// Build the reflection matrix for the source point.
	Matrix4d reflectSource = buildReflection( source, sx2, sy2,
							sz2, sw2, s_s_MIP );

	// Multiply the matrix for reflection across the midpoint by the
	// matrix for reflection across the source.
	matrix.mul( reflectSource );

	return matrix;
    }

    public static H3Matrix4d buildTranslation(H3Point4d source, H3Point4d dest)
    {
	// Build a 4x4 matrix for hyperbolic translation from source point to
	// dest point.  From p. 212 of Phillips and Gunn paper:
	//   translate(source,dest)  =  reflect(midpoint) . reflect(source)
	//
	// Midpoint m between hyperbolic points s and d, in homogeneous coords:
	//    m = SQRT(<d,d>_h * <s,d>_h) * s + SQRT(<s,s>_h * <s,d>_h) * d
	// where <s,d>_h is the Minkowski inner product:
	//    <s,d>_h = (s.x*d.x) + (s.y*d.y) + (s.z*d.z) - (s.w*d.w)

	// Store some of these in temporaries so they can be used when building
	// the reflection matrix.
	MPReal sx2 = source.x.multiply(source.x);
	MPReal sy2 = source.y.multiply(source.y);
	MPReal sz2 = source.z.multiply(source.z);
	MPReal sw2 = source.w.multiply(source.w);

	MPReal dx2 = dest.x.multiply(dest.x);
	MPReal dy2 = dest.y.multiply(dest.y);
	MPReal dz2 = dest.z.multiply(dest.z);
	MPReal dw2 = dest.w.multiply(dest.w);

	MPReal sdx = source.x.multiply(dest.x);
	MPReal sdy = source.y.multiply(dest.y);
	MPReal sdz = source.z.multiply(dest.z);
	MPReal sdw = source.w.multiply(dest.w);

	// Calculate Minkowski inner products <s,s>_h, <d,d>_h, and <s,d>_h.
	MPReal s_s_MIP = sx2.add(sy2).add(sz2).subtract(sw2);
	MPReal d_d_MIP = dx2.add(dy2).add(dz2).subtract(dw2);
	MPReal s_d_MIP = sdx.add(sdy).add(sdz).subtract(sdw);

	H3Point4d midpoint = new H3Point4d( source );
      
	// Scale the source point by SQRT(<d,d>_h * <s,d>_h)
	MPReal s_scale = d_d_MIP.multiply(s_d_MIP).sqrt();
	midpoint.scale(s_scale);

	// Scale the dest point by SQRT(<s,s>_h * <s,d>_h) the add it to the
	// scaled source point.
	MPReal d_scale = s_s_MIP.multiply(s_d_MIP).sqrt();
	midpoint.scaleAdd(d_scale, dest, midpoint );

	// Build the reflection matrix for the midpoint.
	H3Matrix4d matrix = buildReflection( midpoint );

	// Build the reflection matrix for the source point.
	H3Matrix4d reflectSource = buildReflection( source, sx2, sy2,
						    sz2, sw2, s_s_MIP );

	// Multiply the matrix for reflection across the midpoint by the
	// matrix for reflection across the source.
	matrix.mul( reflectSource );

	return matrix;
    }

    public static Matrix4d buildReflection(Point4d p)
    {
	// Prepare to build the reflection matrix for the input point.
	double px2 = p.x * p.x;
	double py2 = p.y * p.y;
	double pz2 = p.z * p.z;
	double pw2 = p.w * p.w;

	// Calculate the Minkowski inner product <p,p>_h
	double p_p_MIP = px2 + py2 + pz2 - pw2;

	return buildReflection( p, px2, py2, pz2, pw2, p_p_MIP );
    }

    public static H3Matrix4d buildReflection(H3Point4d p)
    {
	// Prepare to build the reflection matrix for the input point.
	MPReal px2 = p.x.multiply(p.x);
	MPReal py2 = p.y.multiply(p.y);
	MPReal pz2 = p.z.multiply(p.z);
	MPReal pw2 = p.w.multiply(p.w);

	// Calculate the Minkowski inner product <p,p>_h
	MPReal p_p_MIP = px2.add(py2).add(pz2).subtract(pw2);

	return buildReflection( p, px2, py2, pz2, pw2, p_p_MIP );
    }

    //-------------------------------------------------------------------------
    // PRIVATE STATIC METHODS
    //-------------------------------------------------------------------------

    private static Matrix4d buildReflection(Point4d p,
					    double x2, double y2,
					    double z2, double w2,
					    double innerProduct )
    {
	// Build a 4x4 matrix for hyperbolic reflection across point p.  From
	// p. 211 of Phillips and Gunn paper:
	//       reflect_p = I(4) - 2 * p . pT * I(3,1) / <p,p>_h
	// where p . pT is the outer product of p with itself, <p,p>_h is the
	// Minkowski inner product.

	// The Java Vector class doesn't have a builtin outer product, but the
	// I(3,1) vector has a simple structure so we can construct the outer
	// product at the same time as we do the multiplication by I(3,1).
	//
	//       p . pT           I(3,1)     =   (p . pT) I(3,1)
	//  -              -   -          -     -               - 
	// | x2  xy  xz  xw | | 1  0  0  0 |   | x2  xy  xz  -xw |
	// | yx  y2  yz  yw | | 0  1  0  0 | = | yx  y2  yz  -yw |
	// | zx  zy  z2  zw | | 0  0  1  0 |   | zx  zy  z2  -zw |
	// | wx  wy  wz  w2 | | 0  0  0 -1 |   | wx  wy  wz  -w2 |
	//  -              -   -          -     -               -
	// The coordinates of point p are real numbers (doubles, actually) so
	// p_i * p_j = p_j * p_i, so we can save some multiplies.
	double xy = p.x * p.y;
	double xz = p.x * p.z;
	double xw = p.x * p.w;
	double yz = p.y * p.z;
	double yw = p.y * p.w;
	double zw = p.z * p.w;
	Matrix4d matrix = new Matrix4d( x2, xy, xz, -xw,
					xy, y2, yz, -yw,
					xz, yz, z2, -zw,
					xw, yw, zw, -w2 );

	matrix.mul( -2.0 / innerProduct );
	matrix.add( I4 );

	return matrix;
    }

    private static H3Matrix4d buildReflection(H3Point4d p,
					      MPReal x2, MPReal y2,
					      MPReal z2, MPReal w2,
					      MPReal innerProduct )
    {
	// Build a 4x4 matrix for hyperbolic reflection across point p.  From
	// p. 211 of Phillips and Gunn paper:
	//       reflect_p = I(4) - 2 * p . pT * I(3,1) / <p,p>_h
	// where p . pT is the outer product of p with itself, <p,p>_h is the
	// Minkowski inner product.

	// The Java Vector class doesn't have a builtin outer product, but the
	// I(3,1) vector has a simple structure so we can construct the outer
	// product at the same time as we do the multiplication by I(3,1).
	//
	//       p . pT           I(3,1)     =   (p . pT) I(3,1)
	//  -              -   -          -     -               - 
	// | x2  xy  xz  xw | | 1  0  0  0 |   | x2  xy  xz  -xw |
	// | yx  y2  yz  yw | | 0  1  0  0 | = | yx  y2  yz  -yw |
	// | zx  zy  z2  zw | | 0  0  1  0 |   | zx  zy  z2  -zw |
	// | wx  wy  wz  w2 | | 0  0  0 -1 |   | wx  wy  wz  -w2 |
	//  -              -   -          -     -               -
	// The coordinates of point p are real numbers (MPReals, actually) so
	// p_i * p_j = p_j * p_i, so we can save some multiplies.
	MPReal xy = p.x.multiply(p.y);
	MPReal xz = p.x.multiply(p.z);
	MPReal xw = p.x.multiply(p.w);
	MPReal yz = p.y.multiply(p.z);
	MPReal yw = p.y.multiply(p.w);
	MPReal zw = p.z.multiply(p.w);
	MPReal nxw = xw.negate();
	MPReal nyw = yw.negate();
	MPReal nzw = zw.negate();
	MPReal nw2 = w2.negate();

	MPReal s = new MPReal(-2).divide(innerProduct);
	MPReal sx2 = x2.multiply(s);
	MPReal sy2 = y2.multiply(s);
	MPReal sz2 = z2.multiply(s);
	MPReal sxy = xy.multiply(s);
	MPReal sxz = xz.multiply(s);
	MPReal sxw = xw.multiply(s);
	MPReal syz = yz.multiply(s);
	MPReal syw = yw.multiply(s);
	MPReal szw = zw.multiply(s);
	MPReal snxw = nxw.multiply(s);
	MPReal snyw = nyw.multiply(s);
	MPReal snzw = nzw.multiply(s);
	MPReal snw2 = nw2.multiply(s);

	MPReal one = new MPReal(1);
	MPReal isx2 = sx2.add(one);
	MPReal isy2 = sy2.add(one);
	MPReal isz2 = sz2.add(one);
	MPReal isnw2 = snw2.add(one);

	H3Matrix4d matrix = new H3Matrix4d(isx2,  sxy,  sxz,  snxw,
					    sxy, isy2,  syz,  snyw,
					    sxz,  syz, isz2,  snzw,
					    sxw,  syw,  szw, isnw2);

	return matrix;
    }


    private static Point4d findPivotPoint(Point4d a4, Point4d b4)
    {
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

		// Project from homogenous coordinates to affine coordinates, then create
		// create vectors from the points so they can be used in the builtin dot
		// product.
	Point3d a3 = new Point3d();
	a3.project( a4 );
	Vector3d vector_a = new Vector3d( a3 );
      
	Point3d b3 = new Point3d();
	b3.project( b4 );
	Vector3d vector_b = new Vector3d( b3 );
      
	Vector3d a_minus_b = new Vector3d();
	Vector3d b_minus_a = new Vector3d();
	a_minus_b.sub( a3, b3 );
	b_minus_a.sub( b3, a3 );

		// The point is constructed in two operations, then at the end result
		// is scaled by ||a-b||^2, which is (a-b).(a-b), same as (b-a).(b-a).
		// Scaling is done at the end so we only do one division instead of two.
	Point3d pivot = new Point3d();

	// pivot <- (a . (a-b)) * b
	pivot.scale( vector_a.dot( a_minus_b ), b3 );

	// pivot <- (b . (b-a)) * a + (a . (a-b)) * b
	pivot.scaleAdd( vector_b.dot( b_minus_a ), a3, pivot );

	// Divide result components by ||a-b||^2
	pivot.scale( 1.0 / a_minus_b.lengthSquared() );

	return new Point4d( pivot.x, pivot.y, pivot.z, 1.0 );
    }

    private static H3Point4d findPivotPoint(H3Point4d a4, H3Point4d b4)
    {
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

	MPReal zero = new MPReal(0);

	H3Point4d a = new H3Point4d(a4);
	a.project(a);
	a.w = zero;
      
	H3Point4d b = new H3Point4d(b4);
	b.project(b);
	b.w = zero;
      
	H3Point4d a_minus_b = new H3Point4d(a);
	a_minus_b.sub(b);

	H3Point4d b_minus_a = new H3Point4d(b);
	b_minus_a.sub(a);

	MPReal a_dot_a_minus_b = a.vectorDot(a_minus_b);
	MPReal b_dot_b_minus_a = b.vectorDot(b_minus_a);

	MPReal x1 = a_dot_a_minus_b.multiply(b.x);
	MPReal y1 = a_dot_a_minus_b.multiply(b.y);
	MPReal z1 = a_dot_a_minus_b.multiply(b.z);

	MPReal x2 = b_dot_b_minus_a.multiply(a.x);
	MPReal y2 = b_dot_b_minus_a.multiply(a.y);
	MPReal z2 = b_dot_b_minus_a.multiply(a.z);

	// ||a-b||^2 == (a-b).(a-b) == (b-a).(b-a)
	MPReal lengthSquared = a_minus_b.vectorDot(a_minus_b);

	MPReal x = x1.add(x2).divide(lengthSquared);
	MPReal y = y1.add(y2).divide(lengthSquared);
	MPReal z = z1.add(z2).divide(lengthSquared);

	return new H3Point4d(x, y, z, new MPReal(1));
    }
}

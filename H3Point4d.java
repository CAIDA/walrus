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


// A limited substitute for javax.vecmath.Point4d that uses the MPJava
// multi-precision floating-point classes.
//
// This was independently written without consulting Sun's source, though
// the interface of this class mimics Sun's.

import javax.vecmath.Point4d;
import mpfun.*;

public final class H3Point4d
{
    public MPReal x;
    public MPReal y;
    public MPReal z;
    public MPReal w;

    public H3Point4d()
    {
	MPReal zero = new MPReal(0);
	x = zero;  y = zero;  z = zero;  w = zero;
    }

    public H3Point4d(H3Point4d rhs)
    {
	x = rhs.x;  y = rhs.y;  z = rhs.z;  w = rhs.w;
    }

    public H3Point4d(Point4d rhs)
    {
	x = new MPReal(rhs.x);
	y = new MPReal(rhs.y);
	z = new MPReal(rhs.z);
	w = new MPReal(rhs.w);
    }

    public H3Point4d(MPReal x, MPReal y, MPReal z, MPReal w)
    {
	this.x = x;
	this.y = y;
	this.z = z;
	this.w = w;
    }

    public H3Point4d(double x, double y, double z, double w)
    {
	this.x = new MPReal(x);
	this.y = new MPReal(y);
	this.z = new MPReal(z);
	this.w = new MPReal(w);
    }

    public void scale(MPReal s)
    {
	x = x.multiply(s);
	y = y.multiply(s);
	z = z.multiply(s);
	w = w.multiply(s);
    }

    // this = s*t1 + t2
    public void scaleAdd(MPReal s, H3Point4d t1, H3Point4d t2)
    {
	MPReal tx = t1.x.multiply(s).add(t2.x);
	MPReal ty = t1.y.multiply(s).add(t2.y);
	MPReal tz = t1.z.multiply(s).add(t2.z);
	MPReal tw = t1.w.multiply(s).add(t2.w);

	x = tx;  y = ty;  z = tz;  w = tw;
    }

    public void project(H3Point4d p1)
    {
	MPReal tx = p1.x.divide(p1.w);
	MPReal ty = p1.y.divide(p1.w);
	MPReal tz = p1.z.divide(p1.w);

	x = tx;  y = ty;  z = tz;  w = new MPReal(1);
    }

    public void sub(H3Point4d t1)
    {
	x = x.subtract(t1.x);
	y = y.subtract(t1.y);
	z = z.subtract(t1.z);
	w = w.subtract(t1.w);
    }

    public void set(H3Point4d t1)
    {
	x = t1.x;  y = t1.y;  z = t1.z;  w = t1.w;
    }

    public void set(Point4d t1)
    {
	x = new MPReal(t1.x);
	y = new MPReal(t1.y);
	z = new MPReal(t1.z);
	w = new MPReal(t1.w);
    }

    public void set(double x, double y, double z, double w)
    {
	this.x = new MPReal(x);
	this.y = new MPReal(y);
	this.z = new MPReal(z);
	this.w = new MPReal(w);
    }

    // Euclidean norm of homogeneous coordinates [and equivalent to
    // Point4d.distance(new Point4d(0, 0, 0, 0))].
    public MPReal vectorLength()
    {
	MPReal x2 = x.multiply(x);
	MPReal y2 = y.multiply(y);
	MPReal z2 = z.multiply(z);
	MPReal w2 = w.multiply(w);
	return x2.add(y2).add(z2).divide(w2).sqrt();
    }

    // The usual vector dot product.
    public MPReal vectorDot(H3Point4d v1)
    {
	MPReal tx = x.multiply(v1.x);
	MPReal ty = y.multiply(v1.y);
	MPReal tz = z.multiply(v1.z);
	MPReal tw = w.multiply(v1.w);
	return tx.add(ty).add(tz).add(tw);
    }

    // The usual vector dot product computed from x, y, and z only.
    public MPReal vectorDot3(H3Point4d v1)
    {
	MPReal tx = x.multiply(v1.x);
	MPReal ty = y.multiply(v1.y);
	MPReal tz = z.multiply(v1.z);
	return tx.add(ty).add(tz);
    }

    // Returns the Minkowski inner product of this with y.
    public MPReal minkowski(H3Point4d v)
    {
	MPReal tx = x.multiply(v.x);
	MPReal ty = y.multiply(v.y);
	MPReal tz = z.multiply(v.z);
	MPReal tw = w.multiply(v.w);
	return tx.add(ty).add(tz).subtract(tw);
    }

    public void print()
    {
	System.out.println("[" + x + ", " + y + ", " + z + ", " + w + "]");
    }
}

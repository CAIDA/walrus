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


// A limited substitute for javax.vecmath.Matrix4d that uses the MPJava
// multi-precision floating-point classes.
//
// This was independently written without consulting Sun's source, though
// the interface of this class mimics Sun's.

import mpfun.*;

public final class H3Matrix4d
{
    public MPReal m00;
    public MPReal m01;
    public MPReal m02;
    public MPReal m03;
    public MPReal m10;
    public MPReal m11;
    public MPReal m12;
    public MPReal m13;
    public MPReal m20;
    public MPReal m21;
    public MPReal m22;
    public MPReal m23;
    public MPReal m30;
    public MPReal m31;
    public MPReal m32;
    public MPReal m33;

    public H3Matrix4d()
    {
	MPReal zero = new MPReal(0);

	this.m00 = zero;  this.m01 = zero;  this.m02 = zero;  this.m03 = zero;
	this.m10 = zero;  this.m11 = zero;  this.m12 = zero;  this.m13 = zero;
	this.m20 = zero;  this.m21 = zero;  this.m22 = zero;  this.m23 = zero;
	this.m30 = zero;  this.m31 = zero;  this.m32 = zero;  this.m33 = zero;
    }

    public H3Matrix4d(MPReal m00, MPReal m01, MPReal m02, MPReal m03,
		      MPReal m10, MPReal m11, MPReal m12, MPReal m13,
		      MPReal m20, MPReal m21, MPReal m22, MPReal m23,
		      MPReal m30, MPReal m31, MPReal m32, MPReal m33)
    {
	this.m00 = m00;  this.m01 = m01;  this.m02 = m02;  this.m03 = m03;
	this.m10 = m10;  this.m11 = m11;  this.m12 = m12;  this.m13 = m13;
	this.m20 = m20;  this.m21 = m21;  this.m22 = m22;  this.m23 = m23;
	this.m30 = m30;  this.m31 = m31;  this.m32 = m32;  this.m33 = m33;
    }

    public H3Matrix4d(double m00, double m01, double m02, double m03,
		      double m10, double m11, double m12, double m13,
		      double m20, double m21, double m22, double m23,
		      double m30, double m31, double m32, double m33)
    {
	this.m00 = new MPReal(m00);  this.m01 = new MPReal(m01);  this.m02 = new MPReal(m02);  this.m03 = new MPReal(m03);
	this.m10 = new MPReal(m10);  this.m11 = new MPReal(m11);  this.m12 = new MPReal(m12);  this.m13 = new MPReal(m13);
	this.m20 = new MPReal(m20);  this.m21 = new MPReal(m21);  this.m22 = new MPReal(m22);  this.m23 = new MPReal(m23);
	this.m30 = new MPReal(m30);  this.m31 = new MPReal(m31);  this.m32 = new MPReal(m32);  this.m33 = new MPReal(m33);
    }

    public void transform(H3Point4d v)
    {
	MPReal x = m00.multiply(v.x).add(m01.multiply(v.y)).add(m02.multiply(v.z)).add(m03.multiply(v.w));
	MPReal y = m10.multiply(v.x).add(m11.multiply(v.y)).add(m12.multiply(v.z)).add(m13.multiply(v.w));
	MPReal z = m20.multiply(v.x).add(m21.multiply(v.y)).add(m22.multiply(v.z)).add(m23.multiply(v.w));
	MPReal w = m30.multiply(v.x).add(m31.multiply(v.y)).add(m32.multiply(v.z)).add(m33.multiply(v.w));

	v.x = x;  v.y = y;  v.z = z;  v.w = w;
    }

    public void mul(H3Matrix4d m1)
    {
	MPReal t00 = m00.multiply(m1.m00).add(m01.multiply(m1.m10)).add(m02.multiply(m1.m20)).add(m03.multiply(m1.m30));
	MPReal t10 = m10.multiply(m1.m00).add(m11.multiply(m1.m10)).add(m12.multiply(m1.m20)).add(m13.multiply(m1.m30));
	MPReal t20 = m20.multiply(m1.m00).add(m21.multiply(m1.m10)).add(m22.multiply(m1.m20)).add(m23.multiply(m1.m30));
	MPReal t30 = m30.multiply(m1.m00).add(m31.multiply(m1.m10)).add(m32.multiply(m1.m20)).add(m33.multiply(m1.m30));

	MPReal t01 = m00.multiply(m1.m01).add(m01.multiply(m1.m11)).add(m02.multiply(m1.m21)).add(m03.multiply(m1.m31));
	MPReal t11 = m10.multiply(m1.m01).add(m11.multiply(m1.m11)).add(m12.multiply(m1.m21)).add(m13.multiply(m1.m31));
	MPReal t21 = m20.multiply(m1.m01).add(m21.multiply(m1.m11)).add(m22.multiply(m1.m21)).add(m23.multiply(m1.m31));
	MPReal t31 = m30.multiply(m1.m01).add(m31.multiply(m1.m11)).add(m32.multiply(m1.m21)).add(m33.multiply(m1.m31));

	MPReal t02 = m00.multiply(m1.m02).add(m01.multiply(m1.m12)).add(m02.multiply(m1.m22)).add(m03.multiply(m1.m32));
	MPReal t12 = m10.multiply(m1.m02).add(m11.multiply(m1.m12)).add(m12.multiply(m1.m22)).add(m13.multiply(m1.m32));
	MPReal t22 = m20.multiply(m1.m02).add(m21.multiply(m1.m12)).add(m22.multiply(m1.m22)).add(m23.multiply(m1.m32));
	MPReal t32 = m30.multiply(m1.m02).add(m31.multiply(m1.m12)).add(m32.multiply(m1.m22)).add(m33.multiply(m1.m32));

	MPReal t03 = m00.multiply(m1.m03).add(m01.multiply(m1.m13)).add(m02.multiply(m1.m23)).add(m03.multiply(m1.m33));
	MPReal t13 = m10.multiply(m1.m03).add(m11.multiply(m1.m13)).add(m12.multiply(m1.m23)).add(m13.multiply(m1.m33));
	MPReal t23 = m20.multiply(m1.m03).add(m21.multiply(m1.m13)).add(m22.multiply(m1.m23)).add(m23.multiply(m1.m33));
	MPReal t33 = m30.multiply(m1.m03).add(m31.multiply(m1.m13)).add(m32.multiply(m1.m23)).add(m33.multiply(m1.m33));

	m00 = t00;  m01 = t01;  m02 = t02;  m03 = t03;
	m10 = t10;  m11 = t11;  m12 = t12;  m13 = t13;
	m20 = t20;  m21 = t21;  m22 = t22;  m23 = t23;
	m30 = t30;  m31 = t31;  m32 = t32;  m33 = t33;
    }

    public void mul(MPReal s)
    {
	m00 = m00.multiply(s);  m01 = m01.multiply(s);  m02 = m02.multiply(s);  m03 = m03.multiply(s);
	m10 = m10.multiply(s);  m11 = m11.multiply(s);  m12 = m12.multiply(s);  m13 = m13.multiply(s);
	m20 = m20.multiply(s);  m21 = m21.multiply(s);  m22 = m22.multiply(s);  m23 = m23.multiply(s);
	m30 = m30.multiply(s);  m31 = m31.multiply(s);  m32 = m32.multiply(s);  m33 = m33.multiply(s);
    }

    public void rotX(double angle)
    {
	MPReal zero = new MPReal(0);
	MPReal one = new MPReal(1);
	MPReal theta = new MPReal(angle);
	MPReal cos_theta = theta.cos();
	MPReal sin_theta = theta.sin();
	MPReal neg_sin_theta = sin_theta.negate();

	m00 = one;   m01 = zero;       m02 = zero;           m03 = zero;
	m10 = zero;  m11 = cos_theta;  m12 = neg_sin_theta;  m13 = zero;
	m20 = zero;  m21 = sin_theta;  m22 = cos_theta;      m23 = zero;
	m30 = zero;  m31 = zero;       m32 = zero;           m33 = one;
    }

    public void rotY(double angle)
    {
	MPReal zero = new MPReal(0);
	MPReal one = new MPReal(1);
	MPReal theta = new MPReal(angle);
	MPReal cos_theta = theta.cos();
	MPReal sin_theta = theta.sin();
	MPReal neg_sin_theta = sin_theta.negate();

	m00 = cos_theta;      m01 = zero;  m02 = sin_theta;  m03 = zero;
	m10 = zero;           m11 = one;   m12 = zero;       m13 = zero;
	m20 = neg_sin_theta;  m21 = zero;  m22 = cos_theta;  m23 = zero;
	m30 = zero;           m31 = zero;  m32 = zero;       m33 = one;
    }

    public void rotZ(double angle)
    {
	MPReal zero = new MPReal(0);
	MPReal one = new MPReal(1);
	MPReal theta = new MPReal(angle);
	MPReal cos_theta = theta.cos();
	MPReal sin_theta = theta.sin();
	MPReal neg_sin_theta = sin_theta.negate();

	m00 = cos_theta;  m01 = neg_sin_theta;  m02 = zero;  m03 = zero;
	m10 = sin_theta;  m11 = cos_theta;      m12 = zero;  m13 = zero;
	m20 = zero;       m21 = zero;           m22 = one;   m23 = zero;
	m30 = zero;       m31 = zero;           m32 = zero;  m33 = one;
    }

    public void setIdentity()
    {
	MPReal zero = new MPReal(0);
	MPReal one = new MPReal(1);

	this.m00 = one;   this.m01 = zero;  this.m02 = zero;  this.m03 = zero;
	this.m10 = zero;  this.m11 = one;   this.m12 = zero;  this.m13 = zero;
	this.m20 = zero;  this.m21 = zero;  this.m22 = one;   this.m23 = zero;
	this.m30 = zero;  this.m31 = zero;  this.m32 = zero;  this.m33 = one;
    }

    public void print()
    {
	System.out.println("--------------------------------------------");
	System.out.println(m00 + "\t" + m01 + "\t" + m02 + "\t" + m03);
	System.out.println(m10 + "\t" + m11 + "\t" + m12 + "\t" + m13);
	System.out.println(m20 + "\t" + m21 + "\t" + m22 + "\t" + m23);
	System.out.println(m30 + "\t" + m31 + "\t" + m32 + "\t" + m33);
	System.out.println("--------------------------------------------");
    }
}

// 
// Copyright 2000 The Regents of the University of California
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

import javax.media.j3d.*;
import javax.vecmath.*;

public abstract class H3PickerCommon
    implements H3Picker
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3PickerCommon(H3Graph graph, H3Canvas3D canvas,
			  H3ViewParameters parameters)
    {
	m_graph = graph;
	m_canvas = canvas;
	m_parameters = parameters;

	m_pickRadius = parameters.getPickRadius();
	m_nodeRadius = parameters.getNodeRadius();

	int numNodes = graph.getNumNodes();
	m_pointsInEyeX = new double[numNodes];
	m_pointsInEyeY = new double[numNodes];
	m_pointsInEyeZ = new double[numNodes];
    }

    ////////////////////////////////////////////////////////////////////////
    // INTERFACE METHODS (H3Picker)
    ////////////////////////////////////////////////////////////////////////

    public int pickNode(int x, int y, Point2d center)
    {
	Point3d pickedCoordinates = getPixelLocationInImagePlate(x, y);
	return pick(pickedCoordinates.x, pickedCoordinates.y, center);
    }

    public void highlightNode(int x, int y, boolean enableFrontRendering)
    {
	GraphicsContext3D gc = m_canvas.getGraphicsContext3D();

	if (enableFrontRendering)
	{
	    gc.setBufferOverride(true);
	    gc.setFrontBufferRendering(true);
	}

	Point3d pickedCoordinates = getPixelLocationInImagePlate(x, y);
	m_parameters.drawPickViewer(gc, pickedCoordinates.x,
				    pickedCoordinates.y);

	Point2d center = new Point2d();
	int node = pick(pickedCoordinates.x, pickedCoordinates.y, center);
	if (node >= 0)
	{
	    if (DEBUG_PRINT)
	    {
		System.out.println("Picked node " + node + ".");
	    }

	    Point3d nodeCoordinates = new Point3d();
	    m_graph.getNodeCoordinates(node, nodeCoordinates);

	    PointArray array = new PointArray(1, PointArray.COORDINATES);
	    array.setCoordinate(0, nodeCoordinates);

	    m_parameters.putModelTransform(gc);
	    gc.setAppearance(m_parameters.getPickAppearance());
	    gc.draw(array);
	}
	else
	{
	    if (DEBUG_PRINT)
	    {
		System.out.println("No node picked.");
	    }
	}

	gc.flush(true);

	if (enableFrontRendering)
	{
	    gc.setFrontBufferRendering(false);
	}
    }

    public void highlightNode(int node, boolean enableFrontRendering)
    {
	GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	if (enableFrontRendering)
	{
	    gc.setBufferOverride(true);
	    gc.setFrontBufferRendering(true);
	}

	Point3d p = new Point3d();
	m_graph.getNodeCoordinates(node, p);

	PointArray array = new PointArray(1, PointArray.COORDINATES);
	array.setCoordinate(0, p);

	m_parameters.putModelTransform(gc);
	gc.setAppearance(m_parameters.getPickAppearance());
	gc.draw(array);
	gc.flush(true);

	if (enableFrontRendering)
	{
	    gc.setFrontBufferRendering(false);
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // ABSTRACT METHODS
    ////////////////////////////////////////////////////////////////////////

    protected abstract void computePointsInEye();
    protected abstract int getNumComputedPointsInEye();
    protected abstract int getNodeInEye(int index);

    ////////////////////////////////////////////////////////////////////////
    // PROTECTED METHODS
    ////////////////////////////////////////////////////////////////////////

    protected Point3d getPixelLocationInImagePlate(int x, int y)
    {
	Point3d retval = new Point3d();
	m_canvas.getPixelLocationInImagePlate(x, y, retval);
	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    // (x, y) should be in image plate, rather than AWT, coordinates.
    private int pick(double x, double y, Point2d center)
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("pick.begin[" + startTime +"]");
	}

	Point3d eye = m_parameters.getEye();

	int closestIndex = -1;
	double closestPickDistance = Double.MAX_VALUE;
	double closestEyeDistanceSq = Double.MAX_VALUE;

	// The basic approach of the picking algorithm is as follows.
	// Shoot a pick ray from the eye through the screen at the location
	// where the user clicked [the (x, y) parameters to this method],
	// and compute the intersection of the ray to the z-plane in which
	// a node lies.  Then compute the distance between the node
	// and the pick ray on that z-plane.  Of all nodes which lie
	// within some radius of the pick point, choose the node that
	// is closest to the eye along the line of sight.
	//
	// The reason for shooting the ray out to the node rather than
	// projecting the node to the screen is that each node has a
	// radius which is proportional to the distance of the node from
	// the center of the hyperbolic space.  We would have to project
	// this radius, as well as the node, onto the screen.  Because
	// the screen and the node are specified in different coordinate
	// systems, it requires extra work to project the radius (presuming
	// the nodes themselves are projected with a perspective-projection
	// matrix).  Hence, it seemed just as easy, if not easier, to shoot
	// the ray out than take this approach.

	computePointsInEye();
	int numComputedPointsInEye = getNumComputedPointsInEye();
	for (int i = 0; i < numComputedPointsInEye; i++)
	{
	    double pX = m_pointsInEyeX[i];
	    double pY = m_pointsInEyeY[i];
	    double pZ = m_pointsInEyeZ[i];

	    double inversePerspectiveScale = 1.0 - pZ / eye.z;
	    double pickX = (x - eye.x) * inversePerspectiveScale;
	    double pickY = (y - eye.y) * inversePerspectiveScale;

	    double dx = pickX - pX;
	    double dy = pickY - pY;

	    double centerDistanceSq = dx * dx + dy * dy;

	    double radiusScale = m_graph.getNodeRadius(getNodeInEye(i));
	    double radius = m_pickRadius * inversePerspectiveScale
		          + m_nodeRadius * radiusScale;
	    double radiusSq = radius * radius;

	    if (centerDistanceSq < radiusSq)
	    {
		double z0 = eye.z - pZ;
		double eyeDistanceSq = pX * pX + pY * pY + z0 * z0;
		if (eyeDistanceSq < closestEyeDistanceSq)
		{
		    closestIndex = i;
		    closestEyeDistanceSq = eyeDistanceSq;
		}
	    }
	}

	if (closestIndex >= 0)
	{
	    Point3d p = new Point3d();
	    p.x = eye.x + m_pointsInEyeX[closestIndex];
	    p.y = eye.y + m_pointsInEyeY[closestIndex];
	    p.z = m_pointsInEyeZ[closestIndex];
	    m_canvas.getPixelLocationFromImagePlate(p, center);
	}

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    System.out.println("pick.end[" + stopTime + "]");
	    System.out.println("pick.time[" + (stopTime - startTime) + "]");
	}

	return (closestIndex >= 0 ? getNodeInEye(closestIndex) : -1);
    }

    ////////////////////////////////////////////////////////////////////////
    // PROTECTED FIELDS
    ////////////////////////////////////////////////////////////////////////

    protected static final boolean DEBUG_PRINT = false;

    protected H3Graph m_graph;
    protected H3Canvas3D m_canvas;
    protected H3ViewParameters m_parameters;

    protected double m_pickRadius;
    protected double m_nodeRadius;

    protected double[] m_pointsInEyeX;
    protected double[] m_pointsInEyeY;
    protected double[] m_pointsInEyeZ;
}

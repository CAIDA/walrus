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
    // Image plate coordinates are specified in meters.  The image plate
    // occupies the first quadrant of the xy-plane [that is, (0, 0) is
    // at the lower-left corner of the image plate].
    //
    // The eye looks at the center of the image plate from +z, and the
    // line of sight is perpendicular to the image plate.
    private int pick(double x, double y, Point2d center)
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("pick.begin[" + startTime +"]");
	}

	double pickRadius = m_parameters.getPickRadius();
	double pickEquivalenceRadius = m_parameters.getPickEquivalenceRadius();
	double nodeRadius = m_parameters.getNodeRadius();

	Point3d eye = m_parameters.getEye();

	// All calculations below are done in the coordinate system of the
	// eye (which is a simple translation of the image plate coordinate
	// system).
	double pickX = x - eye.x;
	double pickY = y - eye.y;

	int closestIndex = -1;
	double closestPickDistanceSq = Double.MAX_VALUE;
	double closestEyeDistanceSq = Double.MAX_VALUE;

	computePointsInEye();
	int numComputedPointsInEye = getNumComputedPointsInEye();
	for (int i = 0; i < numComputedPointsInEye; i++)
	{
	    double pX = m_pointsInEyeX[i];
	    double pY = m_pointsInEyeY[i];
	    double pZ = m_pointsInEyeZ[i];

	    double perspectiveScale = 1.0 / (1.0 - pZ / eye.z);
	    double ppX = pX * perspectiveScale;
	    double ppY = pY * perspectiveScale;

	    double dx = pickX - ppX;
	    double dy = pickY - ppY;

	    double centerDistanceSq = dx * dx + dy * dy;

	    double pickDistance = pickRadius;
	    double pickEquivalenceDistance = pickEquivalenceRadius;
	    if (USE_NODE_RADIUS)
	    {
		double radiusScale = m_graph.getNodeRadius(getNodeInEye(i));
		pickDistance += nodeRadius * radiusScale;
		pickEquivalenceDistance += nodeRadius * radiusScale;
	    }

	    // Since demanding great accuracy from the user is too onerous,
	    // we pick based on two levels of precision.  Any node falling
	    // within {pickRadius} is a candidate for picking, which
	    // allows the user to pick quickly without worrying about
	    // preciely positioning the pointer.  Nodes falling within
	    // {pickRadius} are graded based on their Euclidean distance
	    // to the pick point (on the image plate); the closer the better.
	    // However, if multiple nodes lie very close to the pick point--
	    // within {pickEquivalenceRadius}--then they are essentially all
	    // overlapping the pick point, and to provide an intuitive
	    // picking experience, we pick the node that is closest to the
	    // eye.  This prevents surprises caused by distant, but hidden,
	    // overlapping nodes being picked over nearby nodes.

	    if (centerDistanceSq < pickDistance * pickDistance)
	    {
		double pickEquivalenceDistanceSq =
		    pickEquivalenceDistance * pickEquivalenceDistance;

		if (centerDistanceSq < pickEquivalenceDistanceSq)
		{
		    double z0 = eye.z - pZ;
		    double eyeDistanceSq = pX * pX + pY * pY + z0 * z0;
		    if (eyeDistanceSq < closestEyeDistanceSq)
		    {
			closestIndex = i;
			closestEyeDistanceSq = eyeDistanceSq;

			// We always want to choose a node that falls within
			// the equivalence radius, if any do, over those
			// that do not.  By setting closestPickDistanceSq
			// to zero, we ensure that only the equivalent nodes
			// are considered.  We use zero instead of (the
			// current value of) pickEquivalenceDistanceSq
			// because the latter is not an absolute threshold
			// value when node radii are taken into account
			// (that is, if USE_NODE_RADIUS is true).
			closestPickDistanceSq = 0.0;
		    }
		}
		else if (centerDistanceSq < closestPickDistanceSq)
		{
		    closestIndex = i;
		    closestPickDistanceSq = centerDistanceSq;
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

    // This tunes the picking algorithm in pick().
    // If this is false, nodes are treated like points when calculating
    // distance to the pick point.  Otherwise, nodes are treated like
    // spheres with the radius returned by H3Graph.getNodeRadius().
    //
    // Unless nodes are being displayed at a size equal to their radius,
    // it is recommended that this value be set to false, as doing otherwise
    // would lead to apparently unintuitive picking behavior, from the user's
    // point of view.
    protected static final boolean USE_NODE_RADIUS = false;

    protected H3Graph m_graph;
    protected H3Canvas3D m_canvas;
    protected H3ViewParameters m_parameters;

    protected double[] m_pointsInEyeX;
    protected double[] m_pointsInEyeY;
    protected double[] m_pointsInEyeZ;
}

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


import java.util.*;
import javax.vecmath.*;
import mpfun.*;

public class H3GraphLayout
{
    ////////////////////////////////////////////////////////////////////////
    // PUBLIC CLASSES
    ////////////////////////////////////////////////////////////////////////

    public interface LayoutState {}

    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3GraphLayout(boolean attemptExtended)
    {
	ATTEMPT_EXTENDED = attemptExtended;
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public LayoutState layoutHyperbolic
	(H3Graph graph, boolean useExtendedPrecision)
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("layoutHyperbolic.begin[" + startTime +"]");
	}

	HyperbolicLayout retval = null;

	int numNodes = graph.getNumNodes();
	if (numNodes > 0)
	{
	    retval = new HyperbolicLayout(numNodes);
	    computeRadii(graph, retval);
	    computeAngles(graph, retval);

	    if (useExtendedPrecision)
	    {
		computeCoordinatesMP(graph, retval);
	    }
	    else
	    {
		computeCoordinates(graph, retval);
	    }
	}

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("layoutHyperbolic.end[" + stopTime + "]");
	    System.out.println("layoutHyperbolic.time[" + duration + "]");
	}

	return retval;
    }

    // Try to calculate the coordinates of the nodes using extended precision.
    // Assumes the radii and angles have been computed already.
    public void retryHyperbolicLayout(H3Graph graph, LayoutState state)
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("retryHyperbolicLayout.begin[" + startTime+"]");
	}

	if (graph.getNumNodes() > 0)
	{
	    HyperbolicLayout layout = (HyperbolicLayout)state;
	    computeCoordinatesMP(graph, layout);
	}

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("retryHyperbolicLayout.end[" + stopTime + "]");
	    System.out.println("retryHyperbolicLayout.time[" + duration + "]");
	}
    }

    public void layoutRandom(H3Graph graph)
    {
	Random random = new Random();
	Point3d p = new Point3d();
	for (int i = graph.getNumNodes() - 1; i >= 0; i--)
	{
	    computeRandomPoint(random, p);
	    graph.setNodeLayoutCoordinates(i, p.x, p.y, p.z, 1.0);
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS (hyperbolic layout)
    ////////////////////////////////////////////////////////////////////////

    private void computeRadii(H3Graph graph, HyperbolicLayout layout)
    {
	computeRadiiSubtree(graph, layout, graph.getRootNode());
    }

    private void computeRadiiSubtree(H3Graph graph,
				     HyperbolicLayout layout,
				     int node)
    {
	int childIndex = graph.getNodeChildIndex(node);
	int nontreeIndex = graph.getNodeNontreeIndex(node);

	if (childIndex < nontreeIndex)
	{
	    double HA_p = 0.0;

	    while (childIndex < nontreeIndex)
	    {
		int child = graph.getLinkDestination(childIndex);
		computeRadiiSubtree(graph, layout, child);
		HA_p += computeCircleArea(layout.radius[child]);

		++childIndex;
	    }

	    HA_p *= HEMISPHERE_AREA_SCALE;
	    layout.radius[node] = computeRadius(HA_p);
	}
	else
	{
	    layout.radius[node] = LEAF_RADIUS;
	}
    }

    private static double computeCircleArea(double r)
    {
	return H3Math.TWO_PI * (H3Math.cosh(r / K) - 1.0);
    }

    private static double computeRadius(double area)
    {
	return K * H3Math.asinh(Math.sqrt(area / (H3Math.TWO_PI * K * K)));
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void computeAngles(H3Graph graph, HyperbolicLayout layout)
    {
	Children children = new Children();
	computeAnglesSubtree(graph, layout, children, graph.getRootNode(), 0);
    }

    private void computeAnglesSubtree(H3Graph graph,
				      HyperbolicLayout layout,
				      Children children,
				      int node, int level)
    {
	int childIndex = graph.getNodeChildIndex(node);
	int nontreeIndex = graph.getNodeNontreeIndex(node);

	if (childIndex < nontreeIndex)
	{
	    // We must handle all the children first (or last), so that
	    // there won't be any contention over the use of {children}.
	    {
		for (int i = childIndex; i < nontreeIndex; i++)
		{
		    int child = graph.getLinkDestination(i);
		    computeAnglesSubtree
			(graph, layout, children, child, level + 1);
		}
	    }

	    children.clear();
	    {
		for (int i = childIndex; i < nontreeIndex; i++)
		{
		    int child = graph.getLinkDestination(i);
		    children.add(child, layout.radius[child]);
		}
	    }
	    children.sort();

	    //computeAnglesNode(graph, layout, children, node);
	    computeAnglesNode2(graph, layout, children, node, level);
	}
    }

    private void computeAnglesNode(H3Graph graph,
				   HyperbolicLayout layout,
				   Children children,
				   int node)
    {
	double rp = layout.radius[node];

	// Position the first child at the pole.

	Children.Child first = children.getChild(0);
	layout.theta[first.node] = 0.0;
	layout.phi[first.node] = 0.0;

	// Position the remaining children.

	int numChildren = children.getNumChildren();
	if (numChildren > 1)
	{
	    Children.Child second = children.getChild(1);

	    // deltaPhi: half the phi angle subtended by the current band
	    // phi: the phi of the center of the current band
	    // theta: the theta of the beginning of the current child

	    double deltaPhi = computeDeltaPhi(second.radius, rp);
	    double phi = computeDeltaPhi(first.radius, rp) + deltaPhi;
	    double theta = computeDeltaTheta(second.radius, rp, phi);

	    layout.theta[second.node] = theta;
	    layout.phi[second.node] = phi;

	    theta += theta;  // advance just past 2nd child

	    for (int i = 2; i < numChildren; i++)
	    {
		Children.Child child = children.getChild(i);
		double deltaTheta = computeDeltaTheta(child.radius, rp, phi);
		double centerTheta = theta + deltaTheta;

		if (centerTheta + deltaTheta > H3Math.TWO_PI)
		{
		    phi += deltaPhi;  // + half the angle of the current band
		    deltaPhi = computeDeltaPhi(child.radius, rp);
		    phi += deltaPhi;  // + half the angle of the next band

		    deltaTheta = computeDeltaTheta(child.radius, rp, phi);
		    centerTheta = deltaTheta;
		}

		layout.theta[child.node] = centerTheta;
		layout.phi[child.node] = phi;

		theta = centerTheta + deltaTheta;
	    }
	}
    }

    private void computeAnglesNode2(H3Graph graph,
				    HyperbolicLayout layout,
				    Children children,
				    int node, int level)
    {
	final boolean SUBTREE_3_AVG = false;
	final boolean SUBTREE_3_CENTROID = true;

	final boolean SUBTREE_MORE_4 = true;
	final boolean SUBTREE_MORE_4_AVG = false;
	final boolean SUBTREE_MORE_4_CENTROID = true;

	double rp = layout.radius[node];

	// Position the first child at the pole.

	Children.Child first = children.getChild(0);
	layout.theta[first.node] = 0.0;
	layout.phi[first.node] = 0.0;

	// Position the remaining children.

	int numChildren = children.getNumChildren();
	if (numChildren == 2)
	{
	    Children.Child second = children.getChild(1);

	    double firstPhi = computeDeltaPhi(first.radius, rp);
	    double secondPhi = computeDeltaPhi(second.radius, rp);

	    double totalPhi = firstPhi + secondPhi;
	    layout.phi[first.node] = totalPhi - firstPhi;
	    layout.phi[second.node] = totalPhi - secondPhi;

	    double twist = ((level % 2) == 0 ? 0.0 : Math.PI / 2.0);
	    layout.theta[first.node] = twist;
	    layout.theta[second.node] = Math.PI + twist;
	}
	else if (SUBTREE_3_AVG && numChildren == 3)
	{
	    Children.Child second = children.getChild(1);
	    Children.Child third = children.getChild(2);

	    double dp1 = computeDeltaPhi(first.radius, rp);
	    double dp2 = computeDeltaPhi(second.radius, rp);
	    double dp3 = computeDeltaPhi(third.radius, rp);

	    // FUDGE * ((dp1 + dp2) / 2 + (dp1 + dp3) / 2) / 2; etc. for rest
	    final double FUDGE = 1.0;
	    double firstPhi =  FUDGE * 0.25 * (2.0 * dp1 + dp2 + dp3);
	    double secondPhi = FUDGE * 0.25 * (2.0 * dp2 + dp3 + dp1);
	    double thirdPhi =  FUDGE * 0.25 * (2.0 * dp3 + dp1 + dp2);

	    double dt1 = computeDeltaTheta(first.radius, rp, dp1);
	    double dt2 = computeDeltaTheta(second.radius, rp, dp2);
	    double dt3 = computeDeltaTheta(third.radius, rp, dp3);

	    double excessTheta = 2.0 * (Math.PI - dt1 - dt2 - dt3) / 3.0;

	    double firstTheta = dt1;
	    double secondTheta = firstTheta + dt1 + excessTheta + dt2;
	    double thirdTheta = secondTheta + dt2 + excessTheta + dt3;

	    layout.phi[first.node] = firstPhi;
	    layout.theta[first.node] = firstTheta;

	    layout.phi[second.node] = secondPhi;
	    layout.theta[second.node] = secondTheta;

	    layout.phi[third.node] = thirdPhi;
	    layout.theta[third.node] = thirdTheta;
	}
	else if (SUBTREE_3_CENTROID && numChildren == 3)
	{
	    Children.Child second = children.getChild(1);
	    Children.Child third = children.getChild(2);

	    m_ternaryLayout.computeLayout(rp, first.radius, second.radius,
					  third.radius);

	    layout.theta[first.node] = m_ternaryLayout.getThetaA();
	    layout.phi[first.node] = m_ternaryLayout.getPhiA();

	    layout.theta[second.node] = m_ternaryLayout.getThetaB();
	    layout.phi[second.node] = m_ternaryLayout.getPhiB();

	    layout.theta[third.node] = m_ternaryLayout.getThetaC();
	    layout.phi[third.node] = m_ternaryLayout.getPhiC();
	}
	else if (numChildren == 4)
	{
	    Children.Child second = children.getChild(1);
	    Children.Child third = children.getChild(2);
	    Children.Child fourth = children.getChild(3);

	    double dp1 = computeDeltaPhi(first.radius, rp);
	    double dp2 = computeDeltaPhi(second.radius, rp);
	    double dp3 = computeDeltaPhi(third.radius, rp);
	    double dp4 = computeDeltaPhi(fourth.radius, rp);

	    // FUDGE * ((dp1 + dp2)/2 + (dp1 + dp3)/2 + (dp1 + dp4)/2) / 3; etc
	    final double FUDGE = 1.0;
	    double firstPhi =  FUDGE * 0.1667 * (3.0 * dp1 + dp2 + dp3 + dp4);
	    double secondPhi = FUDGE * 0.1667 * (3.0 * dp2 + dp3 + dp4 + dp1);
	    double thirdPhi =  FUDGE * 0.1667 * (3.0 * dp3 + dp4 + dp1 + dp2);
	    double fourthPhi = FUDGE * 0.1667 * (3.0 * dp4 + dp1 + dp2 + dp3);

	    double dt1 = computeDeltaTheta(first.radius, rp, dp1);
	    double dt2 = computeDeltaTheta(second.radius, rp, dp2);
	    double dt3 = computeDeltaTheta(third.radius, rp, dp3);
	    double dt4 = computeDeltaTheta(fourth.radius, rp, dp4);

	    double excessTheta = 2.0 * (Math.PI - dt1 - dt2 - dt3 - dt4) / 4.0;

	    double firstTheta = dt1;
	    double secondTheta = firstTheta + dt1 + excessTheta + dt2;
	    double thirdTheta = secondTheta + dt2 + excessTheta + dt3;
	    double fourthTheta = thirdTheta + dt3 + excessTheta + dt4;

	    layout.phi[first.node] = firstPhi;
	    layout.theta[first.node] = firstTheta;

	    layout.phi[second.node] = secondPhi;
	    layout.theta[second.node] = secondTheta;

	    layout.phi[third.node] = thirdPhi;
	    layout.theta[third.node] = thirdTheta;

	    layout.phi[fourth.node] = fourthPhi;
	    layout.theta[fourth.node] = fourthTheta;
	}
	else if (SUBTREE_MORE_4 && numChildren > 4)
	{
	    double capBottomPhi = 0.0;

	    if (SUBTREE_MORE_4_AVG)
	    {
		Children.Child second = children.getChild(1);
		Children.Child third = children.getChild(2);

		double dp1 = computeDeltaPhi(first.radius, rp);
		double dp2 = computeDeltaPhi(second.radius, rp);
		double dp3 = computeDeltaPhi(third.radius, rp);

		// FUDGE * ((dp1 + dp2) / 2 + (dp1 + dp3) / 2) / 2; etc.
		final double FUDGE = 1.0;
		double firstPhi =  FUDGE * 0.25 * (2.0 * dp1 + dp2 + dp3);
		double secondPhi = FUDGE * 0.25 * (2.0 * dp2 + dp3 + dp1);
		double thirdPhi =  FUDGE * 0.25 * (2.0 * dp3 + dp1 + dp2);

		double dt1 = computeDeltaTheta(first.radius, rp, dp1);
		double dt2 = computeDeltaTheta(second.radius, rp, dp2);
		double dt3 = computeDeltaTheta(third.radius, rp, dp3);

		double excessTheta = 2.0 * (Math.PI - dt1 - dt2 - dt3) / 3.0;

		double firstTheta = dt1;
		double secondTheta = firstTheta + dt1 + excessTheta + dt2;
		double thirdTheta = secondTheta + dt2 + excessTheta + dt3;

		layout.phi[first.node] = firstPhi;
		layout.theta[first.node] = firstTheta;

		layout.phi[second.node] = secondPhi;
		layout.theta[second.node] = secondTheta;

		layout.phi[third.node] = thirdPhi;
		layout.theta[third.node] = thirdTheta;

		capBottomPhi = Math.max(firstPhi + dp1,
					Math.max(secondPhi + dp2,
						 thirdPhi + dp3));
	    }
	    else if (SUBTREE_MORE_4_CENTROID)
	    {
		Children.Child second = children.getChild(1);
		Children.Child third = children.getChild(2);

		m_ternaryLayout.computeLayout(rp, first.radius, second.radius,
					      third.radius);

		layout.theta[first.node] = m_ternaryLayout.getThetaA();
		layout.phi[first.node] = m_ternaryLayout.getPhiA();

		layout.theta[second.node] = m_ternaryLayout.getThetaB();
		layout.phi[second.node] = m_ternaryLayout.getPhiB();

		layout.theta[third.node] = m_ternaryLayout.getThetaC();
		layout.phi[third.node] = m_ternaryLayout.getPhiC();

		double dp1 = computeDeltaPhi(first.radius, rp);
		double dp2 = computeDeltaPhi(second.radius, rp);
		double dp3 = computeDeltaPhi(third.radius, rp);

		capBottomPhi = Math.max(layout.phi[first.node] + dp1,
					Math.max(layout.phi[second.node] + dp2,
						layout.phi[third.node] + dp3));
	    }

	    Children.Child fourth = children.getChild(3);

	    // deltaPhi: half the phi angle subtended by the current band
	    // phi: the phi of the center of the current band
	    // theta: the theta of the beginning of the current child

	    double deltaPhi = computeDeltaPhi(fourth.radius, rp);
	    double phi = capBottomPhi + deltaPhi;
	    double theta = computeDeltaTheta(fourth.radius, rp, phi);

	    layout.theta[fourth.node] = theta;
	    layout.phi[fourth.node] = phi;

	    theta += theta;  // advance just past 4th child

	    boolean positiveTheta = false;

	    int firstChildInBand = 3;
	    for (int i = 4; i < numChildren; i++)
	    {
		Children.Child child = children.getChild(i);
		double deltaTheta = computeDeltaTheta(child.radius, rp, phi);
		double centerTheta = theta + deltaTheta;

		if (centerTheta + deltaTheta > H3Math.TWO_PI)
		{
		    // Evenly space out the children in the current band.

		    double excess = H3Math.TWO_PI - theta;
		    spreadChildrenEvenly2(layout, children,
					  firstChildInBand, i, excess,
					  positiveTheta);

		    // Move to the next band. - - - - - - - - - - - - - - - -

		    phi += deltaPhi;  // + half the angle of the current band
		    deltaPhi = computeDeltaPhi(child.radius, rp);
		    phi += deltaPhi;  // + half the angle of the next band

		    deltaTheta = computeDeltaTheta(child.radius, rp, phi);
		    centerTheta = deltaTheta;
		    firstChildInBand = i;

		    positiveTheta = !positiveTheta;
		}

		layout.theta[child.node] = centerTheta;
		layout.phi[child.node] = phi;

		theta = centerTheta + deltaTheta;
	    }

	    // Evenly space out the children in the last band.	    

	    double excess = H3Math.TWO_PI - theta;
	    spreadChildrenEvenly2(layout, children,
				  firstChildInBand, numChildren - 1, excess,
				  positiveTheta);
	}
	else if (!SUBTREE_MORE_4 && numChildren > 4)
	{
	    Children.Child second = children.getChild(1);

	    // deltaPhi: half the phi angle subtended by the current band
	    // phi: the phi of the center of the current band
	    // theta: the theta of the beginning of the current child

	    double deltaPhi = computeDeltaPhi(second.radius, rp);
	    double phi = computeDeltaPhi(first.radius, rp) + deltaPhi;
	    double theta = computeDeltaTheta(second.radius, rp, phi);

	    layout.theta[second.node] = theta;
	    layout.phi[second.node] = phi;

	    theta += theta;  // advance just past 2nd child

	    int firstChildInBand = 1;
	    for (int i = 2; i < numChildren; i++)
	    {
		Children.Child child = children.getChild(i);
		double deltaTheta = computeDeltaTheta(child.radius, rp, phi);
		double centerTheta = theta + deltaTheta;

		if (centerTheta + deltaTheta > H3Math.TWO_PI)
		{
		    // Evenly space out the children in the current band.

		    double excess = H3Math.TWO_PI - theta;
		    spreadChildrenEvenly(layout, children,
					firstChildInBand, i, excess);

		    // Move to the next band. - - - - - - - - - - - - - - - -

		    phi += deltaPhi;  // + half the angle of the current band
		    deltaPhi = computeDeltaPhi(child.radius, rp);
		    phi += deltaPhi;  // + half the angle of the next band

		    deltaTheta = computeDeltaTheta(child.radius, rp, phi);
		    centerTheta = deltaTheta;
		    firstChildInBand = i;
		}

		layout.theta[child.node] = centerTheta;
		layout.phi[child.node] = phi;

		theta = centerTheta + deltaTheta;
	    }

	    // Evenly space out the children in the last band.	    

	    double excess = H3Math.TWO_PI - theta;
	    spreadChildrenEvenly(layout, children,
				firstChildInBand, numChildren - 1, excess);
	}
    }

    private void spreadChildrenEvenly2(HyperbolicLayout layout,
				       Children children,
				       int first, int last,
				       double excess,
				       boolean positiveTheta)
    {
	int total = last - first + 1;
	for (int i = 1; i < total; i++)
	{
	    double delta = i * excess / total;
	    
	    Children.Child child = children.getChild(first + i);
	    layout.theta[child.node] += delta;

	    if (false && !positiveTheta)
	    {
		layout.theta[child.node] =
		    H3Math.TWO_PI - layout.theta[child.node];
	    }
	}
    }

    private void spreadChildrenEvenly(HyperbolicLayout layout,
				      Children children,
				      int first, int last,
				      double excess)
    {
	int total = last - first + 1;
	for (int i = 1; i < total; i++)
	{
	    double delta = i * excess / total;
	    
	    Children.Child child = children.getChild(first + i);
	    layout.theta[child.node] += delta;
	}
    }

    private double computeDeltaTheta(double rn, double rp, double phi)
    {
	return Math.atan(H3Math.tanh(rn / K)
			 / (H3Math.sinh(rp / K) * Math.sin(phi)));
    } 

    private double computeDeltaPhi(double rj, double rp)
    {
	return Math.atan(H3Math.tanh(rj / K) / H3Math.sinh(rp / K));
    } 

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void computeCoordinates(H3Graph graph,
				    HyperbolicLayout layout)
    {
	// The root node is always positioned at the origin.
	int rootNode = graph.getRootNode();
	graph.setNodeLayoutCoordinates(rootNode, H3Transform.ORIGIN4);
	computeCoordinatesSubtree(graph, layout, H3Transform.I4, rootNode);
    }

    private void computeCoordinatesSubtree(H3Graph graph,
					   HyperbolicLayout layout,
					   Matrix4d parentTransform,
					   int parent)
    {
	int childIndex = graph.getNodeChildIndex(parent);
	int nontreeIndex = graph.getNodeNontreeIndex(parent);

	if (childIndex < nontreeIndex)
	{
	    double parentRadiusE =
		H3Math.euclideanDistance(layout.radius[parent]);

	    double lastPhi = 0.0; 
	    Matrix4d rotPhi = H3Transform.I4;

	    Point4d childCenterAbsolute = new Point4d();
	    Point4d childPoleAbsolute = new Point4d();

	    for (int i = childIndex; i < nontreeIndex; i++)
	    {
		int child = graph.getLinkDestination(i);

		double childRadiusE =
		    H3Math.euclideanDistance(layout.radius[child]);

		double childPhi = layout.phi[child];
		if (childPhi != lastPhi)
		{
		    lastPhi = childPhi;
		    rotPhi = H3Transform.buildZRotation(childPhi);
		}

		Matrix4d rot = H3Transform.buildXRotation(layout.theta[child]);

		rot.mul(rotPhi);

		// compute child's center relative to parent's coord system
		childCenterAbsolute.set(parentRadiusE, 0.0, 0.0, 1.0);
		rot.transform(childCenterAbsolute);

		// compute child's pole relative to parent's coordinate system
		double childPoleE =
		    H3Math.euclideanDistance(layout.radius[parent]
					     + layout.radius[child]);

		childPoleAbsolute.set(childPoleE, 0.0, 0.0, 1.0);
		rot.transform(childPoleAbsolute);

		parentTransform.transform(childCenterAbsolute);
		parentTransform.transform(childPoleAbsolute);

		graph.setNodeLayoutCoordinates(child, childCenterAbsolute);

		Matrix4d childTransform = H3Transform
		    .buildCanonicalOrientation(childCenterAbsolute,
					       childPoleAbsolute);

		if (!ATTEMPT_EXTENDED || H3Math.isFinite(childTransform))
		{
		    computeCoordinatesSubtree(graph, layout,
					      childTransform, child);
		}
		else
		{
		    System.out.println("Switching to extended precision"
				       + " for subtree at node " + child);

		    H3Point4d childCenterAbsoluteMP =
			new H3Point4d(childCenterAbsolute);
		    H3Point4d childPoleAbsoluteMP =
			new H3Point4d(childPoleAbsolute);

		    H3Matrix4d childTransformMP = H3Transform
			.buildCanonicalOrientation(childCenterAbsoluteMP,
						   childPoleAbsoluteMP);

		    computeCoordinatesSubtreeMP(graph, layout,
						childTransformMP, child);
		}
	    }
	}
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void computeCoordinatesMP(H3Graph graph,
				      HyperbolicLayout layout)
    {
	MPGlobal.setMaximumPrecision(new MPPrecision(30));

	// The root node is always positioned at the origin.
	int rootNode = graph.getRootNode();
	graph.setNodeLayoutCoordinates(rootNode, H3Transform.ORIGIN4);
	computeCoordinatesSubtreeMP
	    (graph, layout, H3Transform.I4_MP, rootNode);
    }

    private void computeCoordinatesSubtreeMP(H3Graph graph,
					     HyperbolicLayout layout,
					     H3Matrix4d parentTransform,
					     int parent)
    {
	int childIndex = graph.getNodeChildIndex(parent);
	int nontreeIndex = graph.getNodeNontreeIndex(parent);

	if (childIndex < nontreeIndex)
	{
	    double parentRadiusE =
		H3Math.euclideanDistance(layout.radius[parent]);

	    double lastPhi = 0.0;
	    Matrix4d rotPhi = H3Transform.I4;

	    Point4d childCenterAbsolute = new Point4d();
	    Point4d childPoleAbsolute = new Point4d();
	    Point4d p = new Point4d();

	    H3Point4d childCenterAbsoluteMP = new H3Point4d();
	    H3Point4d childPoleAbsoluteMP = new H3Point4d();

	    for (int i = childIndex; i < nontreeIndex; i++)
	    {
		int child = graph.getLinkDestination(i);

		double childRadiusE =
		    H3Math.euclideanDistance(layout.radius[child]);

		double childPhi = layout.phi[child];
		if (childPhi != lastPhi)
		{
		    lastPhi = childPhi;
		    rotPhi = H3Transform.buildZRotation(childPhi);
		}

		Matrix4d rot = H3Transform.buildXRotation(layout.theta[child]);
		rot.mul(rotPhi);

		// compute child's center relative to parent's coord system
		childCenterAbsolute.set(parentRadiusE, 0.0, 0.0, 1.0);
		rot.transform(childCenterAbsolute);

		// compute child's pole relative to parent's coordinate system
		double childPoleE =
		    H3Math.euclideanDistance(layout.radius[parent]
					     + layout.radius[child]);

		childPoleAbsolute.set(childPoleE, 0.0, 0.0, 1.0);
		rot.transform(childPoleAbsolute);

		childCenterAbsoluteMP.set(childCenterAbsolute);
		childPoleAbsoluteMP.set(childPoleAbsolute);

		parentTransform.transform(childCenterAbsoluteMP);
		parentTransform.transform(childPoleAbsoluteMP);

		convertToDoubleCoordinates(p, childCenterAbsoluteMP);
		graph.setNodeLayoutCoordinates(child, p);

		H3Matrix4d childTransform = H3Transform
		    .buildCanonicalOrientation(childCenterAbsoluteMP,
					       childPoleAbsoluteMP);

		computeCoordinatesSubtreeMP(graph, layout,
					    childTransform, child);
	    }
	}
    }

    private void convertToDoubleCoordinates(Point4d lhs, H3Point4d rhs)
    {
	lhs.x = rhs.x.doubleValue();
	lhs.y = rhs.y.doubleValue();
	lhs.z = rhs.z.doubleValue();
	lhs.w = rhs.w.doubleValue();
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS (random layout)
    ////////////////////////////////////////////////////////////////////////

    // NOTE: The points generated by this algorithm aren't very uniformly
    //       distributed within the unit sphere (there is a concentration
    //       of points near the poles).  However, it is good enough for
    //       our purposes.
    private void computeRandomPoint(Random random, Point3d p)
    {
	double x = 2.0 * random.nextDouble() - 1.0;  // -1.0 <= x <= 1.0
	double y = 2.0 * random.nextDouble() - 1.0;  // -1.0 <= y <= 1.0
	y *= Math.sqrt(1.0 - x*x);  // | y | <= sqrt(1-x^2)

	double z = 2.0 * random.nextDouble() - 1.0;  // -1.0 <= z <= 1.0
	z *= Math.sqrt(1.0 - x*x - y*y); // | z | <= sqrt(1-x^2-y^2)

	p.x = x;
	p.y = y;
	p.z = z;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS (all layouts)
    ////////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT = true;

    // Whether extended precision calculations should be tried automatically
    // when needed.  This value is set in the constructor.
    private final boolean ATTEMPT_EXTENDED;

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS (hyperbolic layout)
    ////////////////////////////////////////////////////////////////////////

    // Some good combinations of HEMISPHERE_AREA_SCALE and LEAF_AREA:
    //  6.0 & 0.1, 7.0 & 0.1, 7.0 & 0.005, 7.0 & 0.0025
    //
    // 7.0 & 0.0025 seems good for large trees (~100K nodes)
    // 6.0 & 0.0025 also seems good for large trees, though bushier
    // 5.0 & 0.0025 seems tolerable for large trees and very bushy

    private static final double K = 2.0;
    private static final double HEMISPHERE_AREA_SCALE = 7.2;
    private static final double LEAF_AREA = 0.005;
    private static final double LEAF_RADIUS = computeRadius(LEAF_AREA);

    private TernaryTreeLayout m_ternaryLayout = new TernaryTreeLayout();

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES (hyperbolic layout)
    ////////////////////////////////////////////////////////////////////////

    private class TernaryTreeLayout
    {
	public void computeLayout(double rp, double rA, double rB, double rC)
	{
	    m_radiusParent = rp;
	    m_radiusA = rA;
	    m_radiusB = rB;
	    m_radiusC = rC;

	    computeEuclideanCenters();
	    computeCircleCentroid();
	    computeHyperbolicAngles();
	}

	public double getThetaA()
	{
	    return m_thetaA;
	}

	public double getThetaB()
	{
	    return m_thetaB;
	}

	public double getThetaC()
	{
	    return m_thetaC;
	}

	public double getPhiA()
	{
	    return m_phiA;
	}

	public double getPhiB()
	{
	    return m_phiB;
	}

	public double getPhiC()
	{
	    return m_phiC;
	}

	private void computeHyperbolicAngles()
	{
	    final double phiRadiusScale = 1.0;

	    // -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -

	    double dC = m_center.distance(m_centerC);
	    double dEC = H3Math.euclideanDistance(dC);
	    m_phiC = 2.0 * computeDeltaPhi(phiRadiusScale * dEC,
					   m_radiusParent);
	    m_thetaC = 0.0;

	    // -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -

	    double dB = m_center.distance(m_centerB);
	    double dEB = H3Math.euclideanDistance(dB);
	    m_phiB = 2.0 * computeDeltaPhi(phiRadiusScale * dEB,
					   m_radiusParent);
	    m_thetaB = computeAngle(dB, dC, m_radiusB + m_radiusC);

	    // -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -

	    double dA = m_center.distance(m_centerA);
	    double dEA = H3Math.euclideanDistance(dA);
	    m_phiA = 2.0 * computeDeltaPhi(phiRadiusScale * dEA,
					   m_radiusParent);
	    m_thetaA = -computeAngle(dA, dC, m_radiusA + m_radiusC);
	}

	private void computeCircleCentroid()
	{
	    double mA = m_radiusA * m_radiusA;
	    double mB = m_radiusB * m_radiusB;
	    double mC = m_radiusC * m_radiusC;
	    double total = mA + mB + mC;

	    double x = mA * m_centerA.x + mB * m_centerB.x + mC * m_centerC.x;
	    double y = mA * m_centerA.y + mB * m_centerB.y + mC * m_centerC.y;

	    m_center.x = x / total;
	    m_center.y = y / total;
	    m_center.z = 0.0;
	}

	private void computeEuclideanCenters()
	{
	    double a = m_radiusB + m_radiusC;
	    double b = m_radiusA + m_radiusC;
	    double c = m_radiusA + m_radiusB;

	    double x = (b * b + c * c - a * a) / (2.0 * b);
	    double y = Math.sqrt(c * c - x * x);

	    m_centerA.x = 0.0;
	    m_centerA.y = 0.0;
	    m_centerA.z = 0.0;

	    m_centerB.x = x;
	    m_centerB.y = y;
	    m_centerB.z = 0.0;

	    m_centerC.x = b;
	    m_centerC.y = 0.0;
	    m_centerC.z = 0.0;
	}

	private void makeAcute()
	{
	    double a = m_radiusB + m_radiusC;
	    double b = m_radiusA + m_radiusC;
	    double c = m_radiusA + m_radiusB;

	    double a2 = a * a;
	    double b2 = b * b;
	    double c2 = c * c;

	    if (a2 + b2 < c2)
	    {
		m_radiusC = makeAngleRight(m_radiusA, m_radiusB);
	    }
	    else if (b2 + c2 < a2)
	    {
		m_radiusA = makeAngleRight(m_radiusB, m_radiusC);
	    }
	    else if (c2 + a2 < b2)
	    {
		m_radiusB = makeAngleRight(m_radiusA, m_radiusC);
	    }
	}

	private double computeAngle(double a, double b, double c)
	{
	    // Compute the angle C using the law of cosines.

	    return Math.acos((a * a + b * b - c * c) / (2.0 * a * b));
	}

	private double makeAngleRight(double a, double b)
	{
	    double c = a + b;

	    return (-c + Math.sqrt(c * c + 4 * a * b)) / 2.0;
	}

	//------------------------------------------------------------------

	private double m_radiusParent;
	private double m_radiusA;
	private double m_radiusB;
	private double m_radiusC;
	private Point3d m_center = new Point3d();
	private Point3d m_centerA = new Point3d();
	private Point3d m_centerB = new Point3d();
	private Point3d m_centerC = new Point3d();
	private double m_phiA;
	private double m_phiB;
	private double m_phiC;
	private double m_thetaA;
	private double m_thetaB;
	private double m_thetaC;
    }

    //======================================================================

    private class HyperbolicLayout
	implements LayoutState
    {
	public HyperbolicLayout(int numNodes)
	{
	    radius = new double[numNodes];
	    theta = new double[numNodes];
	    phi = new double[numNodes];
	}

	public double[] radius;
	public double[] theta;
	public double[] phi;
    }

    //======================================================================

    private class Children
    {
	public void add(int node, double radius)
	{
	    if (m_numChildren == m_children.length)
	    {
		expandArray();
	    }

	    if (m_children[m_numChildren] == null)
	    {
		m_children[m_numChildren] = new Child(node, radius);
	    }
	    else
	    {
		// Reuse existing Child.
		m_children[m_numChildren].set(node, radius);
	    }

	    ++m_numChildren;
	}

	public void clear()
	{
	    m_numChildren = 0;
	}

	public void sort()
	{
	    if (m_numChildren > 0)
	    {
		Arrays.sort(m_children, 0, m_numChildren);
	    }
	}

	public Child getChild(int index)
	{
	    return m_children[index];
	}

	public int getNumChildren()
	{
	    return m_numChildren;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void expandArray()
	{
	    int newSize = 2 * m_children.length;
	    Child[] newArray = new Child[newSize];
	    System.arraycopy(m_children, 0, newArray, 0, m_children.length);
	    m_children = newArray;
	}

	//------------------------------------------------------------------

	private static final int INITIAL_CAPACITY = 50;

	private int m_numChildren = 0;
	private Child[] m_children = new Child[INITIAL_CAPACITY];

	//////////////////////////////////////////////////////////////////// 
	// NESTED CLASSES
	//////////////////////////////////////////////////////////////////// 

	public class Child implements Comparable
	{
	    public Child(int node, double radius)
	    {
		set(node, radius);
	    }

	    public void set(int node, double radius)
	    {
		this.node = node;
		this.radius = radius;
		this.deltaTheta = 0.0;
		this.deltaPhi = 0.0;
	    }

	    // descending order
	    public int compareTo(Object o)
	    {
		Child e = (Child)o;
		if (radius < e.radius)
		{
		    return 1;
		}
		else if (radius > e.radius)
		{
		    return -1;
		}
		else
		{
		    return 0;
		}
	    }

	    //--------------------------------------------------------------

	    public int node;
	    public double radius;
	    public double deltaTheta;
	    public double deltaPhi;
	}
    }
}

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
import javax.media.j3d.*;
import javax.vecmath.*;

public class H3NonadaptiveRenderLoop
    implements H3RenderLoop, Runnable
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3NonadaptiveRenderLoop(H3Graph graph, H3Canvas3D canvas,
				   H3ViewParameters parameters,
				   H3RenderList renderList,
				   boolean useNodeSizes)
    {
	USE_NODE_SIZES = useNodeSizes;

	m_graph = graph;
	m_canvas = canvas;
	m_parameters = parameters;
	m_renderList = renderList;
	m_numNodes = graph.getNumNodes();

	m_picker = new H3NonadaptivePicker(graph, canvas, parameters);
	m_translation.setIdentity();
    }

    ////////////////////////////////////////////////////////////////////////
    // INTERFACE METHODS (H3RenderLoop)
    ////////////////////////////////////////////////////////////////////////

    public synchronized void synchronizeWithRendering()
    {
	startRequest();
	endRequest();
    }

    public synchronized void refreshDisplay()
    {
	startRequest();
	{
	    if (m_state == STATE_IDLE)
	    {
		if (DEBUG_PRINT)
		{
		    System.out.println("refreshing display ...");
		}

		m_parameters.refresh();
		m_parameters.installDepthCueing();
		m_state = STATE_REFRESH;
	    }
	}
	endRequest();
    }

    public synchronized void resizeDisplay()
    {
	startRequest();
	{
	    if (DEBUG_PRINT)
	    {
		System.out.println("resizing display ("
				   + STATE_NAMES[m_state] + ")");
	    }
	    m_picker.reset();
	}
	endRequest();
    }

    public synchronized void rotateDisplay(H3RotationRequest request)
    {
	startRequest();
	{
	    if (DEBUG_PRINT)
	    {
		System.out.println("rotating display ("
				   + STATE_NAMES[m_state] + ")");
	    }

	    m_picker.reset();
	    m_rotationRequest = request;
	    m_state = STATE_ROTATE;
	}
	endRequest();
    }

    public synchronized int pickNode(int x, int y, Point2d center)
    {
	int retval = -1;

	startRequest();
	{
	    if (m_state == STATE_IDLE)
	    {
		m_parameters.refresh(); // See comments for this elsewhere.
		retval = m_picker.pickNode(x, y, center);
	    }
	}
	endRequest();

	return retval;
    }

    public synchronized void highlightNode(int x, int y)
    {
	startRequest();
	{
	    if (m_state == STATE_IDLE)
	    {
		m_parameters.refresh(); // See comments for this elsewhere.
		m_picker.highlightNode(x, y);
	    }
	}
	endRequest();
    }

    public synchronized void highlightNode(int node)
    {
	startRequest();
	{
	    if (m_state == STATE_IDLE)
	    {
		m_parameters.refresh(); // See comments for this elsewhere.
		m_picker.highlightNode(node);
	    }
	}
	endRequest();
    }

    public synchronized void translate(int node)
    {
	startRequest();
	{
	    if (DEBUG_PRINT)
	    {
		System.out.println("translating to node " + node + " ...");
	    }

	    m_picker.reset();
	    m_translationNode = node;
	    m_state = STATE_TRANSLATE;
	}
	endRequest();
    }

    public synchronized void saveDisplayPosition()
    {
	startRequest();
	{
	    m_parameters.saveObjectTransform();
	    m_savedTranslation.set(m_translation);
	}
	endRequest();
    }

    public synchronized void discardDisplayPosition()
    {
	startRequest();
	{
	    m_parameters.discardObjectTransform();
	}
	endRequest();
    }

    public synchronized void restoreDisplayPosition()
    {
	startRequest();
	{
	    m_picker.reset();

	    m_parameters.restoreObjectTransform();
	    m_translation.set(m_savedTranslation);
	    m_graph.transformNodes(m_translation);

	    m_state = STATE_REFRESH;
	}
	endRequest();
    }

    public synchronized H3DisplayPosition getDisplayPosition()
    {
	H3DisplayPosition retval = null;
	startRequest();
	{
	    retval = new H3DisplayPosition(m_translationNode,
					   m_parameters.getObjectTransform(),
					   m_translation);
	}
	endRequest();
	return retval;
    }

    public synchronized void setDisplayPosition(H3DisplayPosition position)
    {
	startRequest();
	{
	    m_picker.reset();

	    m_parameters.setObjectTransform(position.getRotation());
	    m_translation.set(position.getTranslation());
	    m_graph.transformNodes(m_translation);

	    m_state = STATE_REFRESH;
	}
	endRequest();
    }

    public synchronized void shutdown()
    {
	startRequest();
	{
	    m_state = STATE_SHUTDOWN;
	}
	endRequest();
    }

    // This method should not be synchronized, since it must allow the thread
    // running H3AdaptiveRenderLoop to enter the monitor as needed in order
    // to run to completion.
    public void waitForShutdown()
    {
	while (!m_isShutdown)
	{
	    waitForShutdownEvent();
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // INTERFACE METHODS (Runnable)
    ////////////////////////////////////////////////////////////////////////

    public void run()
    {
	while (true)
	{
	    // This is necessary since Java3D isn't prompt in updating the
	    // various view transformations after a window changes size.
	    m_parameters.refresh();

	    switch (m_state)
	    {
	    case STATE_SHUTDOWN:
		if (DEBUG_PRINT)
		{
		    System.out.println("STATE_SHUTDOWN");
		}
		beShutdownState();
		System.out.println("H3NonadaptiveRenderLoop exiting...");
		return;

	    case STATE_IDLE:
		if (DEBUG_PRINT)
		{
		    System.out.println("STATE_IDLE");
		}
		beIdleState();
		break;

	    case STATE_ROTATE:
		if (DEBUG_PRINT)
		{
		    System.out.println("STATE_ROTATE");
		}
		beRotateState();
		break;

	    case STATE_TRANSLATE:
		if (DEBUG_PRINT)
		{
		    System.out.println("STATE_TRANSLATE");
		}
		beTranslateState();
		break;

	    case STATE_REFRESH:
		if (DEBUG_PRINT)
		{
		    System.out.println("STATE_REFRESH");
		}
		beRefreshState();
		break;

	    default:
		throw new RuntimeException("Invalid state: " + m_state);
	    }
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS (states)
    ////////////////////////////////////////////////////////////////////////

    private void beShutdownState()
    {
	synchShutdownState();
    }

    private synchronized void synchShutdownState()
    {
	m_isShutdown = true;	

	m_isWaiting = false;
	m_isRequestTurn = true;

	if (m_numPendingRequests > 0)
	{
	    notifyAll();
	}
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void beIdleState()
    {
	synchIdleState();
    }

    private synchronized void synchIdleState()
    {
	while (m_state == STATE_IDLE)
	{
	    if (DEBUG_PRINT)
	    {
		System.out.println("synchIdleState() waiting ...");
	    }

	    m_isWaiting = true;
	    m_isRequestTurn = true;

	    if (m_numPendingRequests > 0)
	    {
		notifyAll();
	    }

	    waitIgnore();
	}
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void beRotateState()
    {
	Matrix4d rot = new Matrix4d();
	while (m_rotationRequest.getRotation(rot))
	{
	    rotate(rot);
	}

	m_state = STATE_IDLE;
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void beTranslateState()
    {
	Point4d source = new Point4d();
	Point4d destination = new Point4d();

	boolean more = true;
	while (more)
	{
	    m_graph.getNodeCoordinates(m_translationNode, source);
	    double sourceLength = H3Math.vectorLength(source);

	    if (sourceLength > TRANSLATION_THRESHOLD)
	    {
		double scale = sourceLength / (2.0 * sourceLength - 1.0);

		destination.set(source);
		destination.w *= scale;
	    }
	    else if (sourceLength > TRANSLATION_STEP_DISTANCE)
	    {
		double scale = sourceLength /
		    (sourceLength - TRANSLATION_STEP_DISTANCE);

		destination.set(source);
		destination.w *= scale;
	    }
	    else
	    {
		destination.set(H3Transform.ORIGIN4);
		more = false;
	    }

	    if (DEBUG_PRINT)
	    {
		source.project(source);
		destination.project(destination);

		System.out.println("source = " + source);
		System.out.println("destination = " + destination);
		System.out.println("sourceLength = " + sourceLength);
	    }

	    translate(source, destination);
	}

	if (DEBUG_PRINT)
	{
	    m_graph.getNodeCoordinates(m_translationNode, source);
	    source.project(source);
	    System.out.println("FINAL source = " + source);
	}

	m_state = STATE_IDLE;
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void beRefreshState()
    {
	refresh();
	m_state = STATE_IDLE;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS (rendering)
    ////////////////////////////////////////////////////////////////////////

    private void rotate(Matrix4d rot)
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("rotate.begin[" + startTime +"]");
	}

	m_parameters.extendObjectTransform(rot);

	GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	gc.clear();
	{
	    m_parameters.drawAxes(gc);
	    m_parameters.putModelTransform(gc);
	    render(gc);
	}
	m_canvas.swap();

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("rotate.end[" + stopTime + "]");
	    System.out.println("rotate.time[" + duration + "]");
	}
    }

    //======================================================================

    private void translate(Point4d source, Point4d destination)
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("translate.begin[" + startTime +"]");
	}

	Matrix4d translation =
	    H3Transform.buildTranslation(source, destination);

	translation.mul(m_translation);
	m_translation.set(translation);

	m_graph.transformNodes(m_translation);

	GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	gc.clear();
	{
	    m_parameters.drawAxes(gc);
	    m_parameters.putModelTransform(gc);
	    render(gc);
	}
	m_canvas.swap();

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("translate.end[" + stopTime + "]");
	    System.out.println("translate.time[" + duration + "]");
	}
    }

    //======================================================================

    private void refresh()
    {
	long startTime = 0;
	if (DEBUG_PRINT)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("refresh.begin[" + startTime +"]");
	}

	GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	gc.clear();
	{
	    m_parameters.drawAxes(gc);
	    m_parameters.putModelTransform(gc);
	    render(gc);
	}
	m_canvas.swap();

	if (DEBUG_PRINT)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("refresh.end[" + stopTime + "]");
	    System.out.println("refresh.time[" + duration + "]");
	}
    }

    //======================================================================

    private void render(GraphicsContext3D gc)
    {
	m_renderList.beginFrame();
	{
	    for (int i = 0; i < m_numNodes; i++)
	    {
		if (USE_NODE_SIZES)
		{
		    computeNodeRadius(i);
		}

		m_renderList.addNode(i);

		int childIndex = m_graph.getNodeChildIndex(i);
		int nontreeIndex = m_graph.getNodeNontreeIndex(i);
		int endIndex = m_graph.getNodeLinksEndIndex(i);

		for (int j = childIndex; j < nontreeIndex; j++)
		{
		    m_renderList.addTreeLink(j);
		}

		for (int j = nontreeIndex; j < endIndex; j++)
		{
		    m_renderList.addNontreeLink(j);
		}
	    }
	}
	m_renderList.endFrame();
	m_renderList.render(gc);
    }

    // The same radius calculation is done in
    // H3Transformer.transformNode(int node).
    // The two methods should be kept in sync to ensure a consistent display
    // when the user turns adaptive rendering on/off.
    private void computeNodeRadius(int node)
    {
	m_graph.getNodeCoordinates(node, m_nodeCoordinates);

	double radius = H3Math.computeRadiusEuclidean(m_nodeCoordinates);
	m_graph.setNodeRadius(node, radius);
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS (request synchronization)
    ////////////////////////////////////////////////////////////////////////

    private synchronized void waitForShutdownEvent()
    {
	if (!m_isShutdown)
	{
	    // Block till the next rendezvous point.
	    startRequest();
	    endRequest();
	}
    }

    private synchronized void startRequest()
    {
	++m_numPendingRequests;
	while (!m_isRequestTurn)
	{
	    waitIgnore();
	}
	--m_numPendingRequests;
    }

    private synchronized void endRequest()
    {
	m_isRequestTurn = false;

	if (m_numPendingRequests > 0 || m_isWaiting)
	{
	    notifyAll();
	}
    }

    private synchronized void waitIgnore()
    {
	try { wait(); } catch (InterruptedException e) { }
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT = false;
    private static final boolean ANTIALIASING = false;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private static final int STATE_SHUTDOWN = 0;
    private static final int STATE_IDLE = 1;
    private static final int STATE_ROTATE = 2;
    private static final int STATE_TRANSLATE = 3;
    private static final int STATE_REFRESH = 4;
    private static final String[] STATE_NAMES = {
	"STATE_SHUTDOWN", "STATE_IDLE", "STATE_ROTATE",
	"STATE_TRANSLATE", "STATE_REFRESH"
    };

    // USE_NODE_SIZES is set in the constructor.
    //
    // Specifies whether we should render nodes at three sizes.
    // For the nonadaptive render loop, this only determines whether we
    // should calculate the radii of nodes before rendering the display,
    // as H3PointRenderList expects the radii in H3Graph to be up-to-date.
    private final boolean USE_NODE_SIZES;

    private int m_state = STATE_IDLE;
    private int m_numPendingRequests = 0;
    private boolean m_isRequestTurn = false;
    private boolean m_isWaiting = false;

    private boolean m_isShutdown = false;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private H3Graph m_graph;
    private H3Canvas3D m_canvas;
    private H3ViewParameters m_parameters;
    private H3RenderList m_renderList;
    private H3NonadaptivePicker m_picker;
    private int m_numNodes;
    private H3RotationRequest m_rotationRequest;

    private Point4d m_nodeCoordinates = new Point4d(); // scratch variable

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private static final double TRANSLATION_STEP_DISTANCE = 0.05;
    private static final double TRANSLATION_THRESHOLD =
	1.0 - TRANSLATION_STEP_DISTANCE;

    private int m_translationNode;
    private Matrix4d m_translation = new Matrix4d();

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private Matrix4d m_savedTranslation = new Matrix4d();
}

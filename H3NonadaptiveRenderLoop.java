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
				   H3ScreenCapturer capturer,
				   boolean transformNontreeLinks)
    {
	m_graph = graph;
	m_canvas = canvas;
	m_parameters = parameters;
	m_capturer = capturer;
	m_transformNontreeLinks = transformNontreeLinks;

	m_picker = new H3NonadaptivePicker(graph, canvas, parameters);

	m_numNodes = graph.getNumNodes();
	m_nodeArray = new PointArray(m_numNodes, PointArray.COORDINATES);

	int numLines = graph.getTotalNumLinks();
	m_lineArray = new LineArray(numLines * 2, LineArray.COORDINATES);

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
		m_picker.highlightNode(x, y, true);
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
		m_picker.highlightNode(node, true);
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

	    refresh();
	}
	endRequest();
    }

    public H3DisplayPosition getDisplayPosition()
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

    public void setDisplayPosition(H3DisplayPosition position)
    {
	startRequest();
	{
	    m_picker.reset();

	    m_parameters.setObjectTransform(position.getRotation());
	    m_translation.set(position.getTranslation());
	    m_graph.transformNodes(m_translation);

	    refresh();
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
	    render(gc);
	}
	m_canvas.swap();

	if (m_capturer != null)
	{
	    m_capturer.capture(gc);
	}

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
	    render(gc);
	}
	m_canvas.swap();

	if (m_capturer != null)
	{
	    m_capturer.capture(gc);
	}

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
	Point3d source = new Point3d();
	Point3d target = new Point3d();

	int numVertices = 0;
	for (int i = 0; i < m_numNodes; i++)
	{
	    m_graph.getNodeCoordinates(i, source);
	    m_nodeArray.setCoordinate(i, source);

	    int childIndex = m_graph.getNodeChildIndex(i);
	    int endIndex = (m_transformNontreeLinks 
			    ? m_graph.getNodeLinksEndIndex(i)
			    : m_graph.getNodeNontreeIndex(i));

	    while (childIndex < endIndex)
	    {
		int child = m_graph.getLinkDestination(childIndex);
		m_graph.getNodeCoordinates(child, target);

		m_lineArray.setCoordinate(numVertices++, source);
		m_lineArray.setCoordinate(numVertices++, target);

		++childIndex;
	    }
	}

	m_lineArray.setValidVertexCount(numVertices);

	m_parameters.putModelTransform(gc);

	gc.setAppearance(m_parameters.getNodeAppearance());
	gc.draw(m_nodeArray);

	gc.setAppearance(m_parameters.getLineAppearance());
	gc.draw(m_lineArray);
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS (request synchronization)
    ////////////////////////////////////////////////////////////////////////

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

    private int m_state = STATE_IDLE;
    private int m_numPendingRequests = 0;
    private boolean m_isRequestTurn = false;
    private boolean m_isWaiting = false;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private H3Graph m_graph;
    private H3Canvas3D m_canvas;
    private H3ViewParameters m_parameters;
    private H3ScreenCapturer m_capturer;
    private H3NonadaptivePicker m_picker;

    private boolean m_transformNontreeLinks;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private int m_numNodes;
    private PointArray m_nodeArray;
    private LineArray m_lineArray;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private H3RotationRequest m_rotationRequest;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private static final double TRANSLATION_STEP_DISTANCE = 0.05;
    private static final double TRANSLATION_THRESHOLD =
	1.0 - TRANSLATION_STEP_DISTANCE;

    private int m_translationNode;
    private Matrix4d m_translation = new Matrix4d();

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private Matrix4d m_savedTranslation = new Matrix4d();
}

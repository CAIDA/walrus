// 
// Copyright (C) 2000,2001,2002 The Regents of the University of California.
// All Rights Reserved.
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
// GOVERNMENT PURPOSE RIGHTS
// Contract No.NGI N66001-98-2-8922
// Contractor Name: SPAWAR
// Expiration Date: 3/1/2008
// The Government's rights to use, modify, reproduce, release, perform, 
// display, or disclose these technical data are restricted by paragraph 
// (b)(2) of the Rights in Technical Data - Noncommercial Items clause 
// contained in the above identified contract.  No restrictions apply after 
// the expiration date shown above.  Any reproduction of technical data or 
// portions thereof marked with this legend must also reproduce the markings.
//
//######END_HEADER######

import java.util.*;
import javax.media.j3d.*;
import javax.vecmath.*;

public class H3AdaptiveRenderLoop
    implements H3RenderLoop, Runnable
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3AdaptiveRenderLoop(H3Graph graph, H3Canvas3D canvas,
				H3ViewParameters parameters,
				H3Transformer transformer,
				H3RenderQueue queue,
				H3AdaptiveRenderer renderer)
    {
	m_graph = graph;
	m_canvas = canvas;
	m_parameters = parameters;
	m_transformer = transformer;
	m_renderer = renderer;

	m_picker = new H3AdaptivePicker(graph, canvas, parameters, queue);
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
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
	    if (m_state == STATE_IDLE || m_state == STATE_COMPLETE)
	    {
		if (DEBUG_PRINT)
		{
		    System.out.print("[" + Thread.currentThread().getName()
				     + "]: ");
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
	    if (m_state == STATE_IDLE || m_state == STATE_COMPLETE)
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
	    if (m_state == STATE_IDLE || m_state == STATE_COMPLETE)
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
	    if (m_state == STATE_IDLE || m_state == STATE_COMPLETE)
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
	    m_transformer.pushPosition();
	}
	endRequest();
    }

    public synchronized void discardDisplayPosition()
    {
	startRequest();
	{
	    if (!m_restoreDisplayRequested)
            {
                m_parameters.discardObjectTransform();
                m_transformer.discardPosition();
            }
	}
	endRequest();
    }

    public synchronized void restoreDisplayPosition()
    {
	startRequest();
	{
	    m_restoreDisplayRequested = true;
	}
	endRequest();
    }

    public synchronized H3DisplayPosition getDisplayPosition()
    {
	H3DisplayPosition retval = null;
	startRequest();
	{
	    H3Transformer.Position position = m_transformer.getPosition();
	    retval = new H3DisplayPosition(position.startingNode,
					   m_parameters.getObjectTransform(),
					   position.transform);
	}
	endRequest();
	return retval;
    }

    public synchronized void setDisplayPosition(H3DisplayPosition position)
    {
	startRequest();
	{
	    m_displayPosition = position;
            m_restoreDisplayRequested = true;
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

    public synchronized void setMaxRotationDuration(long max)
    {
	startRequest();
	{
	    m_maxRotationDuration = max;
	}
	endRequest();
    }

    public synchronized void setMaxTranslationDuration(long max)
    {
	startRequest();
	{
	    m_maxTranslationDuration = max;
	}
	endRequest();
    }

    public synchronized void setMaxCompletionDuration(long max)
    {
	startRequest();
	{
	    m_maxCompletionDuration = max;
	}
	endRequest();
    }

    public synchronized long getMaxRotationDuration()
    {
	return m_maxRotationDuration;
    }

    public synchronized long getMaxTranslationDuration()
    {
	return m_maxTranslationDuration;
    }

    public synchronized long getMaxCompletionDuration()
    {
	return m_maxCompletionDuration;
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

	    if (DEBUG_PRINT)
	    {
		System.out.print("[" + Thread.currentThread().getName()
				 + "]: ");
	    }

	    switch (m_state)
	    {
	    case STATE_SHUTDOWN:
		if (DEBUG_PRINT)
		{
		    System.out.println("STATE_SHUTDOWN");
		}
		beShutdownState();
		System.out.println("H3AdaptiveRenderLoop exiting...");
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

	    case STATE_COMPLETE_INIT:
		if (DEBUG_PRINT)
		{
		    System.out.println("STATE_COMPLETE_INIT");
		}
		beCompleteInitState();
		break;

	    case STATE_COMPLETE:
		if (DEBUG_PRINT)
		{
		    System.out.println("STATE_COMPLETE");
		}
		beCompleteState();
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
	m_transformer.shutdown();
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
		System.out.print("[" + Thread.currentThread().getName()
				 + "]: ");
		System.out.println("synchIdleState() waiting ...");
	    }

	    if (m_restoreDisplayRequested)
	    {
		if (DEBUG_PRINT)
		{
		    System.out.print("[" + Thread.currentThread().getName()
				     + "]: ");
		    System.out.println("restoring display ...");
		}

		m_restoreDisplayRequested = false;
		m_picker.reset();
		m_renderer.reset();

		if (m_displayPosition == null)
		{
		    m_parameters.restoreObjectTransform();
		    m_transformer.popPosition();
		}
		else
		{
		    m_parameters.setObjectTransform
			(m_displayPosition.getRotation());

		    H3Transformer.Position position =
			new H3Transformer.Position();
		    position.startingNode = m_displayPosition.getCenterNode();
		    position.transform.set(m_displayPosition.getTranslation());
		    m_transformer.setPosition(position);

		    m_displayPosition = null;
		}

		GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
		gc.clear();
		m_parameters.drawAxes(gc);
		m_parameters.putModelTransform(gc);
		m_canvas.swap();

		m_state = STATE_COMPLETE_INIT;
	    }
	    else
	    {
		m_isWaiting = true;
		m_isRequestTurn = true;

		if (m_numPendingRequests > 0)
		{
		    notifyAll();
		}

		waitIgnore();
	    }
	}
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void beRotateState()
    {
	m_renderer.reset();
	m_renderer.setMaxDuration(m_maxRotationDuration);

	Matrix4d rot = new Matrix4d();
	while (m_rotationRequest.getRotation(rot))
	{
	    rotate(rot);
	}

	m_state = STATE_COMPLETE_INIT;
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void beTranslateState()
    {
	m_renderer.reset();
	m_renderer.setMaxDuration(m_maxTranslationDuration);

	Point4d source = new Point4d();
	Point4d destination = new Point4d();

	m_graph.getNodeCoordinates(m_translationNode, source);

	Point4d initialSource = new Point4d(source);
	m_transformer.pushPosition();

	boolean more = true;
	while (more)
	{
	    double sourceLength = H3Math.vectorLength(source);
	    if (sourceLength > TRANSLATION_THRESHOLD)
	    {
                double delta = Math.max(TRANSLATION_MIN_DELTA,
					1.0 - sourceLength);
		double scale = sourceLength / (sourceLength - delta);

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

	m_transformer.popPosition();
	translate(initialSource, H3Transform.ORIGIN4);

	m_state = STATE_COMPLETE_INIT;
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void beRefreshState()
    {
	GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	m_parameters.putModelTransform(gc);
	gc.setBufferOverride(true);
	gc.setFrontBufferRendering(true);
	gc.clear();

	m_parameters.drawAxes(gc);
	m_parameters.putModelTransform(gc);
	m_renderer.reset();

	m_state = STATE_COMPLETE;
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void beCompleteInitState()
    {
	GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	m_parameters.putModelTransform(gc);
	gc.setBufferOverride(true);
	gc.setFrontBufferRendering(true);
	m_state = STATE_COMPLETE;
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void beCompleteState()
    {
	m_renderer.setMaxDuration(m_maxCompletionDuration);

	GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	while (m_state == STATE_COMPLETE)
	{
	    if (synchCompleteState())
	    {
		m_renderer.refine(gc);
	    }
	}
    }

    private synchronized boolean synchCompleteState()
    {
	if (m_numPendingRequests > 0)
	{
	    if (DEBUG_PRINT)
	    {
		System.out.print("[" + Thread.currentThread().getName()
				 + "]: ");
		System.out.println("synchCompleteState() waiting ...");
		System.out.println("(" + m_numPendingRequests +
				   " request(s) pending)");
	    }

	    m_isRequestTurn = true;
	    notifyAll();

	    m_isWaiting = true;
	    waitIgnore();
	    m_isWaiting = false;
	}

	if (DEBUG_PRINT)
	{
	    System.out.print("[" + Thread.currentThread().getName() + "]: ");
	    System.out.println("synchCompleteState() running ...");
	}

	if (m_state == STATE_COMPLETE && m_renderer.isFinished())
	{
	    m_state = STATE_IDLE;
	}

	if (m_state != STATE_COMPLETE)
	{
	    GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	    gc.setBufferOverride(true);
	    gc.setFrontBufferRendering(false);
	}

	return m_state == STATE_COMPLETE;
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
	    m_renderer.render(gc);
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

	m_transformer.transform(translation);
	m_transformer.transformNode(m_translationNode, source);

	if (DEBUG_PRINT)
	{
	    source.project(source);
	    System.out.println("source transformed = " + source);
	}

	GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	gc.clear();
	{
	    m_parameters.drawAxes(gc);
	    m_parameters.putModelTransform(gc);
	    m_renderer.render(gc);
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
    private static final boolean DEBUG_PRINT_TRANSFORMED = false;

    private static final boolean ANTIALIASING = false;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private static final int STATE_SHUTDOWN = 0;
    private static final int STATE_IDLE = 1;
    private static final int STATE_ROTATE = 2;
    private static final int STATE_TRANSLATE = 3;
    private static final int STATE_REFRESH = 4;
    private static final int STATE_COMPLETE_INIT = 5;
    private static final int STATE_COMPLETE = 6;
    private static final String[] STATE_NAMES = {
	"STATE_SHUTDOWN", "STATE_IDLE", "STATE_ROTATE", "STATE_TRANSLATE",
	"STATE_REFRESH", "STATE_COMPLETE_INIT", "STATE_COMPLETE"
    };

    private int m_state = STATE_IDLE;
    private int m_numPendingRequests = 0;
    private boolean m_isRequestTurn = false;
    private boolean m_isWaiting = false;

    private boolean m_isShutdown = false;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private H3Graph m_graph;
    private H3Canvas3D m_canvas;
    private H3Transformer m_transformer;
    private H3AdaptiveRenderer m_renderer;
    private H3ViewParameters m_parameters;
    private H3AdaptivePicker m_picker;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private long m_maxRotationDuration = Long.MAX_VALUE;
    private long m_maxTranslationDuration = Long.MAX_VALUE;
    private long m_maxCompletionDuration = Long.MAX_VALUE;

    private H3RotationRequest m_rotationRequest;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private static final double TRANSLATION_STEP_DISTANCE = 0.05;
    private static final double TRANSLATION_MIN_DELTA = 0.01;
    private static final double TRANSLATION_THRESHOLD =
	1.0 - TRANSLATION_STEP_DISTANCE;

    private int m_translationNode;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private boolean m_restoreDisplayRequested;
    private H3DisplayPosition m_displayPosition;
}

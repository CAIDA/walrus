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

public class H3CapturingRenderLoop
    implements H3RenderLoop
{
    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////
    
    public H3CapturingRenderLoop(H3RenderLoop renderLoop)
    {
	m_renderLoop = renderLoop;
    }

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public void startCapturing()
    {
	m_isCapturing = true;
	m_movements.clear();
	m_renderLoop.saveDisplayPosition();
    }

    public void stopCapturing()
    {
	m_isCapturing = false;
    }

    public void abortCapturing()
    {
        stopCapturing();
        m_movements.clear();
        m_renderLoop.discardDisplayPosition();
    }

    public void toggleCapturing()
    {
	if (m_isCapturing)
	{
	    stopCapturing();
	}
	else
	{
	    startCapturing();
	}
    }

    public void replayMovements()
    {
	m_renderLoop.restoreDisplayPosition();

	Iterator iter = m_movements.iterator();
	while (iter.hasNext())
	{
	    Movement movement = (Movement)iter.next();
	    movement.execute(m_renderLoop);
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // INTERFACE METHODS (H3RenderLoop)
    ////////////////////////////////////////////////////////////////////////

    public void synchronizeWithRendering()
    {
	m_renderLoop.synchronizeWithRendering();
    }

    public void refreshDisplay()
    {
	m_renderLoop.refreshDisplay();
    }

    public void rotateDisplay(H3RotationRequest request)
    {
	if (m_isCapturing)
	{
	    H3CapturingRotationRequest capturing =
		new H3CapturingRotationRequest(request);

	    Rotation rotation = new Rotation(capturing);
	    m_movements.add(rotation);

	    m_renderLoop.rotateDisplay(capturing);
	}
	else
	{
	    m_renderLoop.rotateDisplay(request);
	}
    }

    public int pickNode(int x, int y, Point2d center)
    {
	return m_renderLoop.pickNode(x, y, center);
    }

    public void highlightNode(int x, int y)
    {
	m_renderLoop.highlightNode(x, y);
    }

    public void highlightNode(int node)
    {
	m_renderLoop.highlightNode(node);
    }

    public void translate(int node)
    {
	if (m_isCapturing)
	{
	    m_movements.add(new Translation(node));
	}

	m_renderLoop.translate(node);
    }

    public void saveDisplayPosition()
    {
	m_renderLoop.saveDisplayPosition();
    }

    public void discardDisplayPosition()
    {
	m_renderLoop.discardDisplayPosition();
    }

    public void restoreDisplayPosition()
    {
	m_renderLoop.restoreDisplayPosition();
    }

    public H3DisplayPosition getDisplayPosition()
    {
	return m_renderLoop.getDisplayPosition();
    }

    public void setDisplayPosition(H3DisplayPosition position)
    {
	m_renderLoop.setDisplayPosition(position);
    }

    public void shutdown()
    {
	m_renderLoop.shutdown();
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT = false;

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private boolean m_isCapturing;
    private H3RenderLoop m_renderLoop;
    private List m_movements = new ArrayList();  // List<Movement>

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE NESTED INTERFACES AND CLASSES
    ////////////////////////////////////////////////////////////////////////

    private interface Movement
    {
	void execute(H3RenderLoop renderLoop);
    }

    private static class Translation implements Movement
    {
	public Translation(int node)
	{
	    m_node = node;
	}

	public void execute(H3RenderLoop renderLoop)
	{
	    renderLoop.translate(m_node);
	}

	private int m_node;
    }

    private static class Rotation implements Movement
    {
	public Rotation(H3CapturingRotationRequest request)
	{
	    m_request = request;
	}

	public void execute(H3RenderLoop renderLoop)
	{
	    H3RotationRequest replaying = m_request.createReplayingRequest();
	    renderLoop.rotateDisplay(replaying);
	}

	private H3CapturingRotationRequest m_request;
    }
}

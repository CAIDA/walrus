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
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.universe.*;
import org.caida.libsea.ASCIIInputStreamReader;

public class H3Main
{
    public static void main(String[] args)
    {
	Frame frame = new Frame();
	frame.setSize(900, 1000);
	frame.setLayout(new BorderLayout());
	frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e)
	    {
		System.exit(0);
	    }
	});

	GraphicsConfiguration config =
	    SimpleUniverse.getPreferredConfiguration();

	H3Canvas3D canvas = new H3Canvas3D(config);
	canvas.stopRenderer();

	EventHandler handler = new EventHandler();
	handler.setCanvas(canvas);

	canvas.addMouseListener(handler);
	canvas.addMouseMotionListener(handler);

	frame.add("Center", canvas);

	SimpleUniverse univ = new SimpleUniverse(canvas);
	univ.getViewingPlatform().setNominalViewingTransform();

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	final boolean ENABLE_ADAPTIVE_DRAWING = true;
	final boolean PROCESS_NONTREE_LINKS = false;
	final boolean ENABLE_SCREEN_CAPTURE = false;

	H3Graph graph = null;

	if (args.length > 0)
	{
	    try
	    {
		System.out.println("Loading graph from \""
				   + args[0] + "\" ...");

		// Class java.io.FileReader could have been used here, but
		// it has performance problems.  See ASCIIInputStreamReader.
		ASCIIInputStreamReader reader =
		    new ASCIIInputStreamReader(new FileInputStream(args[0]));

		H3GraphLoader loader = new H3GraphLoader(reader);
		graph = loader.load();

		reader.close();

		if (graph == null)
		{
		    System.err.println("ERROR: Graph couldn't be loaded.\n");
		    System.exit(1);
		}

		//graph.checkTreeReachability();
		//graph.dumpForTesting2();
	    }
	    catch (FileNotFoundException e)
	    {
		System.err.println("File \"" + args[0] + "\" not found.\n");
		System.exit(1);
	    }
	    catch (IOException e)
	    {
		System.err.println("While reading/closing \""
				   + args[0] + "\": " + e);
		System.exit(1);
	    }

	    if (true)
	    {
		int nodeColor = Color.yellow.getRGB();
		//int nodeColor = (0xff << 16) | (0xff << 8) | 0xff;

		//int linkColor = Color.green.getRGB();
		//int linkColor = (64 << 16) | (192 << 8) | 64;
		int linkColor = (30 << 16) | (150 << 8) | 0;

		graph.setNodeDefaultColor(nodeColor);
		graph.setLinkDefaultColor(linkColor);

		if (false)
		{
		    float[] color = Color.yellow.getRGBColorComponents(null);
		    System.out.println("Node color: " + color[0] + ", "
				       + color[1] + ", "
				       + color[2]);
		 
		    color = new Color(linkColor).getRGBColorComponents(null);
		    System.out.println("Link color: " + color[0] + ", "
				       + color[1] + ", "
				       + color[2]);
		}

		if (PROCESS_NONTREE_LINKS)
		{
		    colorNontreeLinks(graph, Color.darkGray.getRGB());
		}
	    }
	}
	else
	{
	    H3GraphBuffer buffer = new H3GraphBuffer();

	    //H3GraphGenerator.createTernaryTreeSet(buffer);

	    //H3GraphGenerator.createLinearIncreasingTree(buffer, 1, 25, 30);
	    //H3GraphGenerator.createQuadraticIncreasingTree(buffer, 1, 1, 30);
	    //H3GraphGenerator.createCubicIncreasingTree(buffer, 1, 1, 10);

	    //H3GraphGenerator.createGraph(buffer, 2, 3);       // 15 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 4);       // 31 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 5);       // 63 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 6);      // 127 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 7);      // 255 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 8);      // 511 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 9);    // 1,023 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 10);   // 2,047 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 11);   // 4,095 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 12);   // 8,191 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 13);  // 16,383 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 14);  // 32,767 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 15);  // 65,535 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 16); // 131,071 nodes
	    //H3GraphGenerator.createGraph(buffer, 2, 17); // 262,143 nodes

	    //H3GraphGenerator.createGraph(buffer, 3, 3);       // 40 nodes
	    //H3GraphGenerator.createGraph(buffer, 3, 4);      // 121 nodes
	    //H3GraphGenerator.createGraph(buffer, 3, 5);      // 364 nodes
	    //H3GraphGenerator.createGraph(buffer, 3, 6);    // 1,093 nodes
	    //H3GraphGenerator.createGraph(buffer, 3, 7);    // 3,280 nodes
	    //H3GraphGenerator.createGraph(buffer, 3, 8);    // 9,841 nodes
	    //H3GraphGenerator.createGraph(buffer, 3, 9);   // 29,524 nodes
	    //H3GraphGenerator.createGraph(buffer, 3, 10);  // 88,573 nodes

	    //H3GraphGenerator.createGraph(buffer, 4, 3);       // 85 nodes
	    //H3GraphGenerator.createGraph(buffer, 4, 4);      // 341 nodes
	    //H3GraphGenerator.createGraph(buffer, 4, 5);    // 1,365 nodes
	    //H3GraphGenerator.createGraph(buffer, 4, 6);    // 5,461 nodes
	    //H3GraphGenerator.createGraph(buffer, 4, 7);   // 21,845 nodes
	    //H3GraphGenerator.createGraph(buffer, 4, 8);   // 87,381 nodes

	    //H3GraphGenerator.createGraph(buffer, 5, 3);      // 156 nodes
	    //H3GraphGenerator.createGraph(buffer, 5, 4);      // 781 nodes
	    //H3GraphGenerator.createGraph(buffer, 5, 5);    // 3,906 nodes
	    //H3GraphGenerator.createGraph(buffer, 5, 6);   // 19,531 nodes
	    //H3GraphGenerator.createGraph(buffer, 5, 7);   // 97,655 nodes

	    //H3GraphGenerator.createGraph(buffer, 6, 3);      // 259 nodes
	    //H3GraphGenerator.createGraph(buffer, 6, 4);    // 1,555 nodes
	    //H3GraphGenerator.createGraph(buffer, 6, 5);    // 9,331 nodes
	    //H3GraphGenerator.createGraph(buffer, 6, 6);   // 55,986 nodes
	    //H3GraphGenerator.createGraph(buffer, 6, 7);  // 335,923 nodes

	    //H3GraphGenerator.createGraph(buffer, 7, 3);      // 400 nodes
	    //H3GraphGenerator.createGraph(buffer, 7, 4);    // 2,801 nodes
	    H3GraphGenerator.createGraph(buffer, 7, 5);   // 19,608 nodes
	    //H3GraphGenerator.createGraph(buffer, 7, 6);  // 137,257 nodes
	    //H3GraphGenerator.createGraph(buffer, 7, 7);  // 960,800 nodes

	    //H3GraphGenerator.createGraph(buffer, 8, 3);      // 585 nodes
	    //H3GraphGenerator.createGraph(buffer, 8, 4);    // 4,681 nodes
	    //H3GraphGenerator.createGraph(buffer, 8, 5);   // 37,449 nodes
	    //H3GraphGenerator.createGraph(buffer, 8, 6);  // 299,593 nodes

	    //H3GraphGenerator.createGraph(buffer, 9, 3);      // 820 nodes
	    //H3GraphGenerator.createGraph(buffer, 9, 4);    // 7,381 nodes
	    //H3GraphGenerator.createGraph(buffer, 9, 5);   // 66,430 nodes

	    //H3GraphGenerator.createGraph(buffer, 10, 2);     // 111 nodes
	    //H3GraphGenerator.createGraph(buffer, 10, 3);   // 1,111 nodes
	    //H3GraphGenerator.createGraph(buffer, 10, 4);  // 11,111 nodes
	    //H3GraphGenerator.createGraph(buffer, 10, 5); // 111,111 nodes

	    //H3GraphGenerator.createGraph(buffer, 17, 2);     // 307 nodes
	    //H3GraphGenerator.createGraph(buffer, 17, 3);   // 5,220 nodes
	    //H3GraphGenerator.createGraph(buffer, 17, 4);  // 88,741 nodes

	    //H3GraphGenerator.createGraph(buffer, 34, 2);   // 1,191 nodes
	    //H3GraphGenerator.createGraph(buffer, 34, 3);  // 40,495 nodes

	    //H3GraphGenerator.createGraph(buffer, 56, 2);   // 3,193 nodes
	    //H3GraphGenerator.createGraph(buffer, 56, 3); // 178,809 nodes

	    //H3GraphGenerator.createGraph(buffer, 82, 2);   // 6,807 nodes

	    //H3GraphGenerator.createGraph(buffer, 152, 2); // 23,257 nodes

	    //H3GraphGenerator.createGraph(buffer, 297, 2); // 88,507 nodes

	    graph = buffer.toGraph();
	    //graph.checkTreeReachability();

	    if (true)
	    {
		//int nodeColor = (0xff << 16) | (0xff << 8) | 0xff;
		//int linkColor = (64 << 16) | (192 << 8) | 64;
		int linkColor = (30 << 16) | (150 << 8) | 0;

		int nodeColor = Color.yellow.getRGB();
		//int linkColor = Color.green.getRGB();

		graph.setNodeDefaultColor(nodeColor);
		graph.setLinkDefaultColor(linkColor);
	    }
	    else if (false)
	    {
		int linkColor = (64 << 16) | (192 << 8) | 64;
		graph.setLinkDefaultColor(linkColor);
		colorLeafNodes(graph);
	    }
	    else
	    {
		graph.setNodeDefaultColor(Color.lightGray.getRGB());
		colorRootSubtrees(graph);
	    }
	}

	System.out.println("numNodes = " + graph.getNumNodes());
	System.out.println("numTreeLinks = " + graph.getNumTreeLinks());
	System.out.println("numNontreeLinks = " + graph.getNumNontreeLinks());

	handler.setRootNode(graph.getRootNode());

	//dumpOutdegreeHistogram(graph);
        //System.exit(0);

	H3GraphLayout layout = new H3GraphLayout();
	//layout.layoutRandom(graph);
	layout.layoutHyperbolic(graph);

	System.out.println("Finished graph layout.");

	H3ViewParameters parameters = new H3ViewParameters(canvas);

	H3ScreenCapturer capturer = null;
	if (ENABLE_SCREEN_CAPTURE)
	{
	    capturer = new H3ScreenCapturer();
	}

	CapturingManager manager = new NullCapturingManager();

	H3RenderLoop loop = null;
	if (ENABLE_ADAPTIVE_DRAWING)
	{
	    int queueSize = graph.getNumNodes() + graph.getTotalNumLinks();
	    H3RenderQueue queue = new H3RenderQueue(queueSize);

	    H3Transformer transformer =
		new H3Transformer(graph, queue, PROCESS_NONTREE_LINKS);

	    new Thread(transformer).start();

	    System.out.println("Started H3Transformer.");

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	    H3PointRenderList list = new H3PointRenderList(graph, true,
				             /* nodes */   true, false,
				        /* tree links */   true, false,
				    /* non-tree links */  false, false);
	    list.setNearNodeAppearance(parameters.getNearNodeAppearance());
	    list.setMiddleNodeAppearance(parameters.getMiddleNodeAppearance());
	    list.setFarNodeAppearance(parameters.getFarNodeAppearance());
	    list.setTreeLinkAppearance(parameters.getTreeLinkAppearance());

	    if (true)
	    {
		list.setNontreeLinkAppearance(parameters
					      .getNontreeLinkAppearance());
	    }
	    else
	    {
		list.setNontreeLinkAppearance(parameters.getLineAppearance());
	    }

	    H3AdaptiveRenderer renderer = null;

	    if (true)
	    {
		renderer = new H3LineRenderer(graph, queue, list);
	    }
	    else
	    {
		renderer = new H3CircleRenderer(graph, parameters,
						queue, list);
	    }

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	    H3AdaptiveRenderLoop adaptive =
		new H3AdaptiveRenderLoop(graph, canvas, parameters,
					 transformer, queue,
					 renderer, capturer);

	    new Thread(adaptive).start();
	    loop = adaptive;

            final int DURATION = 50;
	    adaptive.setMaxRotationDuration(DURATION);
	    adaptive.setMaxTranslationDuration(DURATION);
	    adaptive.setMaxCompletionDuration(DURATION);

	    if (ENABLE_SCREEN_CAPTURE)
	    {
		manager = new AdaptiveCapturingManager(capturer, adaptive);
	    }

	    System.out.println("Started H3AdaptiveRenderLoop.");
	}
	else
	{
	    graph.transformNodes(H3Transform.I4);

	    H3NonadaptiveRenderLoop nonadaptive =
		new H3NonadaptiveRenderLoop(graph, canvas, parameters,
					    capturer, PROCESS_NONTREE_LINKS);
	    new Thread(nonadaptive).start();
	    loop = nonadaptive;

	    if (ENABLE_SCREEN_CAPTURE)
	    {
		manager = new NonadaptiveCapturingManager(capturer,
							   nonadaptive);
	    }

	    System.out.println("Started H3NonadaptiveRenderLoop.");
	}

	handler.setRenderLoop(loop);
	handler.setCapturingManager(manager);

	System.out.println("Finished initialization in Main.");

	frame.show();
    }

    /////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    /////////////////////////////////////////////////////////////////////

    private static void dumpOutdegreeHistogram(H3Graph graph)
    {
	int[] histogram = computeOutdegreeHistogram(graph);
	
	for (int i = 0; i < histogram.length; i++)
	{
	    if (histogram[i] != 0)
	    {
		System.out.println(i + " " + histogram[i]);
	    }
	}
    }

    private static int[] computeOutdegreeHistogram(H3Graph graph)
    {
	int maxOutdegree = computeMaxOutdegree(graph, graph.getRootNode());
	int[] retval = new int[maxOutdegree + 1];

	computeOutdegree(graph, retval, graph.getRootNode());
	return retval;
    }

    private static int computeMaxOutdegree(H3Graph graph, int node)
    {
	int childIndex = graph.getNodeChildIndex(node);
	int nontreeIndex = graph.getNodeNontreeIndex(node);
	int endIndex = graph.getNodeLinksEndIndex(node);

	int retval = endIndex - childIndex;

	for (int i = childIndex; i < nontreeIndex; i++)
	{
	    int child = graph.getLinkDestination(i);
	    retval = Math.max(retval, computeMaxOutdegree(graph, child));
	}

	return retval;
    }

    private static void computeOutdegree(H3Graph graph, int[] histogram,
					 int node)
    {
	int childIndex = graph.getNodeChildIndex(node);
	int nontreeIndex = graph.getNodeNontreeIndex(node);
	int endIndex = graph.getNodeLinksEndIndex(node);

	int outdegree = endIndex - childIndex;
	++histogram[outdegree];

	for (int i = childIndex; i < nontreeIndex; i++)
	{
	    int child = graph.getLinkDestination(i);
	    computeOutdegree(graph, histogram, child);
	}
    }

    /////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    /////////////////////////////////////////////////////////////////////

    private static void colorLeafNodes(H3Graph graph)
    {
	colorLeafNodesAux(graph, graph.getRootNode());
    }

    private static void colorLeafNodesAux(H3Graph graph, int parent)
    {
	int childIndex = graph.getNodeChildIndex(parent);
	int nontreeIndex = graph.getNodeNontreeIndex(parent);

	if (childIndex < nontreeIndex)
	{
	    graph.setNodeColor(parent, Color.darkGray.getRGB());
	    for (int i = childIndex; i < nontreeIndex; i++)
	    {
		int child = graph.getLinkDestination(i);
		colorLeafNodesAux(graph, child);
	    }
	}
	else
	{
	    graph.setNodeColor(parent, Color.pink.getRGB());
	}
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private static void colorRootSubtrees(H3Graph graph)
    {
	int colors[] = {
	    Color.blue.getRGB(), Color.green.getRGB(),
	    Color.magenta.getRGB(), Color.orange.getRGB(),
	    Color.pink.getRGB(), Color.red.getRGB(),
	    Color.yellow.getRGB()
	};

	int rootNode = graph.getRootNode();
	int childIndex = graph.getNodeChildIndex(rootNode);
	int nontreeIndex = graph.getNodeNontreeIndex(rootNode);

	for (int i = childIndex; i < nontreeIndex; i++)
	{
	    int child = graph.getLinkDestination(i);

	    int color = colors[i % colors.length]; 
	    graph.setLinkColor(i, color);
	    graph.setNodeColor(child, color);

	    colorRootSubtreesAux(graph, child, color);
	}
    }

    private static void colorRootSubtreesAux(H3Graph graph, int parent,
					     int color)
    {
	int childIndex = graph.getNodeChildIndex(parent);
	int nontreeIndex = graph.getNodeNontreeIndex(parent);

	for (int i = childIndex; i < nontreeIndex; i++)
	{
	    int child = graph.getLinkDestination(i);

	    graph.setLinkColor(i, color);
	    graph.setNodeColor(child, color);

	    colorRootSubtreesAux(graph, child, color);
	}
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private static void colorNontreeLinks(H3Graph graph, int color)
    {
	colorNontreeLinksAux(graph, graph.getRootNode(), color);
    }

    private static void colorNontreeLinksAux(H3Graph graph, int parent,
					     int color)
    {
	int childIndex = graph.getNodeChildIndex(parent);
	int nontreeIndex = graph.getNodeNontreeIndex(parent);
	int linksEndIndex = graph.getNodeLinksEndIndex(parent);

	for (int i = childIndex; i < nontreeIndex; i++)
	{
	    int child = graph.getLinkDestination(i);
	    colorNontreeLinksAux(graph, child, color);
	}

	for (int i = nontreeIndex; i < linksEndIndex; i++)
	{
	    graph.setLinkColor(i, color);
	}
    }

    /////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES
    /////////////////////////////////////////////////////////////////////

    private static class EventHandler extends H3MouseInputAdapter
    {
	public void setRootNode(int rootNode)
	{
	    m_rootNode = m_currentNode = m_previousNode = rootNode;
	}

	public void setRenderLoop(H3RenderLoop renderLoop)
	{
	    m_renderLoop = new H3CapturingRenderLoop(renderLoop);
	}

	public void setCapturingManager(CapturingManager manager)
	{
	    m_capturingManager = manager;
	}

	public void setCanvas(H3Canvas3D canvas)
	{
	    m_canvas = canvas;

	    m_canvas.addPaintObserver(new Observer() {
		public void update(Observable o, Object arg)
		    {
			if (!m_isRotating)
			{
			    m_renderLoop.refreshDisplay();
			}
		    }
	    });
	}

	public void mousePressed(MouseEvent e)
	{
	    //System.out.println("mousePressed(): isRotating = " + m_isRotating);

	    m_lastX = e.getX();
	    m_lastY = e.getY();

	    if (m_isRotating)
	    {
		if (m_rotationKind == REPEATING_ROTATION)
		{
		    //System.out.println("stopping repeating rotations");
		    m_isRotating = false;
		    m_repeatingRequest.end();
		}
		else if (m_rotationKind == WOBBLING_ROTATION)
		{
		    //System.out.println("stopping wobbling rotations");
		    m_isRotating = false;
		    m_wobblingRequest.end();
		}
	    }
	    else
	    {
		int modifiers = e.getModifiers();
		if (checkModifiers(modifiers, InputEvent.BUTTON3_MASK))
		{
		    if (checkModifiers(modifiers, InputEvent.SHIFT_MASK))
		    {
			if (m_isCapturing)
			{
			    System.out.println("Replaying movements ...");

			    m_isCapturing = false;
			    m_renderLoop.stopCapturing();

			    m_capturingManager.enable();
			    m_renderLoop.replayMovements();
			    m_capturingManager.disable();

			    System.out.println("Finished replaying ...");
			}
			else
			{
			    System.out.println("Recording movements ...");

			    m_isCapturing = true;
			    m_renderLoop.startCapturing();
			}

			/*
			if (m_savedDisplay)
			{
			    System.out.println("Restoring display ...");

			    m_savedDisplay = false;
			    m_renderLoop.restoreDisplayPosition();
			}
			else
			{
			    System.out.println("Saving display ...");

			    m_savedDisplay = true;
			    m_renderLoop.saveDisplayPosition();
			}
			*/
		    }
		    else
		    {
			System.out.println("Picking ...");
			int node = m_renderLoop.pickNode(e.getX(), e.getY(),
							 m_center);
			if (node >= 0)
			{
			    System.out.println("Picked node " + node + ".");
			    m_renderLoop.translate(node);
			    shiftCenterNodes(node);
			}
			else
			{
			    System.out.println("No node picked.");
			}
		    }
		}
		else if (checkModifiers(modifiers, InputEvent.BUTTON2_MASK))
		{
		    if (checkModifiers(modifiers, InputEvent.SHIFT_MASK))
		    {
		        m_renderLoop.translate(swapCenterNodes());
		    }
		    else if (checkModifiers(modifiers, InputEvent.CTRL_MASK))
		    {
			m_renderLoop.translate(m_rootNode);
			shiftCenterNodes(m_rootNode);
		    }
		    else 
		    {
                        if (m_isCapturing)
                        {
			    System.out.println("Abandoning recording ...");

			    m_isCapturing = false;
			    m_renderLoop.abortCapturing();
                        }
                        else
                        {
			    //System.out.println("Highlighting ...");
			    m_renderLoop.highlightNode(e.getX(), e.getY());
                        }
		    }
		}
	    }
	}

	public void mouseReleased(MouseEvent e)
	{
	    //System.out.println("mouseReleased(): isRotating = " +m_isRotating);

	    // NOTE: The following will be true even if button 1 has not
	    //       been actually released so long as some other button
	    //       has been released.

	    if (m_isRotating && m_rotationKind == INTERACTIVE_ROTATION
		&& checkModifiers(e.getModifiers(), InputEvent.BUTTON1_MASK))
	    {
		//System.out.println("stopping rotations ...");
		m_isRotating = false;
		m_interactiveRequest.end();
	    }
	}

	public void mouseDragged(MouseEvent e)
	{
	    int modifiers = e.getModifiers();
	    if (checkModifiers(modifiers, InputEvent.BUTTON1_MASK))
	    {
		int dx = (e.getX() - m_lastX) / MOUSE_SENSITIVITY;
		int dy = (e.getY() - m_lastY) / MOUSE_SENSITIVITY;

		m_lastX = e.getX();
		m_lastY = e.getY();

		double dxRad = Math.toRadians(dx);
		double dyRad = Math.toRadians(dy);

		//System.out.println("m_isRotating = " + m_isRotating);
		//System.out.println("m_rotationKind = " + m_rotationKind);

		if (!m_isRotating)
		{
		    m_isRotating = true;

		    if (checkModifiers(modifiers, InputEvent.SHIFT_MASK))
		    {
			m_rotationKind = REPEATING_ROTATION;
		    }
		    else if (checkModifiers(modifiers, InputEvent.CTRL_MASK))
		    {
			m_rotationKind = WOBBLING_ROTATION;
		    }
		    else
		    {
			m_rotationKind = INTERACTIVE_ROTATION;
		    }

		    switch (m_rotationKind)
		    {
		    case INTERACTIVE_ROTATION:
			m_interactiveRequest.start();
			m_interactiveRequest.rotate(dxRad, dyRad);
			m_renderLoop.rotateDisplay(m_interactiveRequest);
			//System.out.println("started interactive rotations");
			break;

		    case REPEATING_ROTATION:
			m_repeatingRequest.start();
			m_repeatingRequest.rotate(dxRad, dyRad);
			m_renderLoop.rotateDisplay(m_repeatingRequest);
			//System.out.println("started repeating rotations");
			break;

		    case WOBBLING_ROTATION:
			m_wobblingRequest.start();
			m_renderLoop.rotateDisplay(m_wobblingRequest);
			//System.out.println("started wobbling rotations");
			break;
		    }
		}
		else
		{
		    switch (m_rotationKind)
		    {
		    case INTERACTIVE_ROTATION:
			m_interactiveRequest.rotate(dxRad, dyRad);
			//System.out.println("cont'd interactive rotations");
			break;

		    case REPEATING_ROTATION:
			m_repeatingRequest.rotate(dxRad, dyRad);
			//System.out.println("cont'd repeating rotations");
			break;

		    case WOBBLING_ROTATION:
			//System.out.println("cont'd wobbling rotations");
			break;
		    }
		}
	    }
	    else if (checkModifiers(modifiers, InputEvent.BUTTON2_MASK))
	    {
		//System.out.println("Highlighting ...");
		m_renderLoop.highlightNode(e.getX(), e.getY());
	    }
	}

	//---------------------------------------------------------------

	private void shiftCenterNodes(int node)
	{
	    m_previousNode = m_currentNode;
	    m_currentNode = node;
	}

	private int swapCenterNodes()
	{
	    int t = m_previousNode;
	    m_previousNode = m_currentNode;
	    m_currentNode = t;
	    return m_currentNode;
	}

	private boolean checkModifiers(int modifiers, int mask)
	{
	    return (modifiers & mask) == mask; 
	}

	//---------------------------------------------------------------

	private static final int MOUSE_SENSITIVITY = 2;

	private CapturingManager m_capturingManager;
	private H3CapturingRenderLoop m_renderLoop;
	private H3Canvas3D m_canvas;

	private Point2d m_center = new Point2d();

	private int m_lastX = 0;
	private int m_lastY = 0;

	private int m_rootNode;
	private int m_currentNode;
	private int m_previousNode;

	private boolean m_isRotating = false;
	private boolean m_isCapturing = false;
	private boolean m_savedDisplay = false;

	private static final int INTERACTIVE_ROTATION = 0;
	private static final int REPEATING_ROTATION = 1;
	private static final int WOBBLING_ROTATION = 2;
	private int m_rotationKind;

	private H3InteractiveRotationRequest m_interactiveRequest
	    = new H3InteractiveRotationRequest();

	private H3RepeatingRotationRequest m_repeatingRequest
	    = new H3RepeatingRotationRequest();

	private H3WobblingRotationRequest m_wobblingRequest
	    = new H3WobblingRotationRequest();
    }

    private interface CapturingManager
    {
	void enable();
	void disable();
    }

    private static class NullCapturingManager
	implements CapturingManager
    {
	public void enable() {}
	public void disable() {}
    }

    private static class AdaptiveCapturingManager
	implements CapturingManager
    {
	public AdaptiveCapturingManager(H3ScreenCapturer capturer,
					 H3AdaptiveRenderLoop renderLoop)
	{
	    m_capturer = capturer;
	    m_renderLoop = renderLoop;
	}

	public void enable()
	{
	    m_maxRotationDuration = m_renderLoop.getMaxRotationDuration();
	    m_maxTranslationDuration =m_renderLoop.getMaxTranslationDuration();
	    m_maxCompletionDuration = m_renderLoop.getMaxCompletionDuration();

	    m_renderLoop.setMaxRotationDuration(CAPTURING_DURATION);
	    m_renderLoop.setMaxTranslationDuration(CAPTURING_DURATION);
	    m_renderLoop.setMaxCompletionDuration(CAPTURING_DURATION);

	    m_capturer.enableCapturing();
	}

	public void disable()
	{
	    m_renderLoop.synchronizeWithRendering();

	    m_renderLoop.setMaxRotationDuration(m_maxRotationDuration);
	    m_renderLoop.setMaxTranslationDuration(m_maxTranslationDuration);
	    m_renderLoop.setMaxCompletionDuration(m_maxCompletionDuration);

	    m_capturer.disableCapturing();
	}

	private static final long CAPTURING_DURATION = 1000;

	private long m_maxRotationDuration;
	private long m_maxTranslationDuration;
	private long m_maxCompletionDuration;

	private H3ScreenCapturer m_capturer;
	private H3AdaptiveRenderLoop m_renderLoop;
    }

    private static class NonadaptiveCapturingManager
	implements CapturingManager
    {
	public NonadaptiveCapturingManager(H3ScreenCapturer capturer,
					   H3NonadaptiveRenderLoop renderLoop)
	{
	    m_capturer = capturer;
	    m_renderLoop = renderLoop;
	}

	public void enable()
	{
	    m_capturer.enableCapturing();
	}

	public void disable()
	{
	    m_renderLoop.synchronizeWithRendering();
	    m_capturer.disableCapturing();
	}

	private H3ScreenCapturer m_capturer;
	private H3NonadaptiveRenderLoop m_renderLoop;
    }
}

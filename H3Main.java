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
import java.io.*;
import java.util.Observer;
import java.util.Observable;
import javax.swing.*;
import com.sun.j3d.utils.universe.*;
import org.caida.libsea.*;

public class H3Main
{
    ///////////////////////////////////////////////////////////////////////
    // MAIN
    ///////////////////////////////////////////////////////////////////////

    public static void main(String[] args)
    {
	new H3Main();
    }

    ///////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////

    public H3Main()
    {
	initializeCanvas3D();

	m_frame = new JFrame(WALRUS_TITLE);
	m_frame.setBackground(Color.black);
	m_frame.getContentPane().setBackground(Color.black);

	// XXX: Preserve frame dimensions in properties across sessions.
	m_frame.setSize(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT);
	m_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	ImageIcon splashIcon = new ImageIcon(SPLASH_ICON_PATH);
	m_splashLabel = new JLabel(splashIcon);
	m_frame.getContentPane().add(m_splashLabel, BorderLayout.CENTER);

	m_statusBar = new JTextField();
	m_statusBar.setEditable(false);
	m_statusBar.setText(MSG_NO_GRAPH_LOADED);
	m_frame.getContentPane().add(m_statusBar, BorderLayout.SOUTH);

	m_frame.setJMenuBar(createInitialMenuBar());

	m_frame.show();
    }

    /////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    /////////////////////////////////////////////////////////////////////

    private void handleOpenFileRequest()
    {
	File file = askUserForFile();
	if (file != null)
	{
	    handleCloseFileRequest();
	    m_statusBar.setText(MSG_LOADING_GRAPH);

	    try
	    {
		ASCIIInputStreamReader reader =
		    new ASCIIInputStreamReader(new FileInputStream(file));

		Graph backingGraph = loadGraph(file, reader);
		m_graph = new H3GraphLoader().load(backingGraph);

		if (DEBUG_PRINT_LOAD_MEMORY) { m_memoryUsage.gatherAtPeak(); }
		if (DEBUG_PRINT_LOAD_MEMORY)
		{
		    m_memoryUsage.gatherAtFinal();
		    m_memoryUsage.printUsage();
		}

		m_file = file;
		m_backingGraph = backingGraph;

		m_frame.getContentPane().remove(m_splashLabel);
		m_frame.getContentPane().add(m_canvas, BorderLayout.CENTER);
		m_frame.setTitle(WALRUS_TITLE + " -- " + file.getPath());
		m_statusBar.setText(MSG_GRAPH_LOADED);
		m_closeMenuItem.setEnabled(true);
		m_frame.validate();

		startRendering();
	    }
	    catch (FileNotFoundException e)
	    {
		String msg =  "File not found: " + file.getPath();
		JOptionPane dialog = new JOptionPane();
		dialog.showMessageDialog(null, msg, "File Not Found",
					 JOptionPane.ERROR_MESSAGE);
	    }
	    catch (H3GraphLoader.InvalidGraphDataException e)
	    {
		String msg = "Graph file lacks needed data: "
		    + e.getMessage();
		JOptionPane dialog = new JOptionPane();
		dialog.showMessageDialog(null, msg, "Open Failed",
					 JOptionPane.ERROR_MESSAGE);
	    }

	    if (m_backingGraph == null)
	    {
		m_statusBar.setText(MSG_NO_GRAPH_LOADED);
	    }
	}

	System.out.println("Finished handleOpenFileRequest()");
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleCloseFileRequest()
    {
	m_backingGraph = null;
	m_graph = null;
	if (m_renderLoop != null)
	{
	    stopRendering();
	}

	m_frame.setTitle(WALRUS_TITLE);
	m_frame.getContentPane().remove(m_canvas);
	m_frame.getContentPane().add(m_splashLabel, BorderLayout.CENTER);
	m_statusBar.setText(MSG_NO_GRAPH_LOADED);

	// XXX: Clear menu items here.

	m_frame.getContentPane().validate();
	System.out.println("Finished handleCloseFileRequest()");
    }

    ///////////////////////////////////////////////////////////////////////

    private void startRendering()
    {
	final boolean ENABLE_ADAPTIVE_DRAWING = true;
	final boolean PROCESS_NONTREE_LINKS = false;
	final boolean ENABLE_SCREEN_CAPTURE = false;

	System.out.println("numNodes = " + m_graph.getNumNodes());
	System.out.println("numTreeLinks = " + m_graph.getNumTreeLinks());
	System.out.println("numNontreeLinks = " + m_graph.getNumNontreeLinks());

	//dumpOutdegreeHistogram(m_graph);

	H3GraphLayout layout = new H3GraphLayout();
	layout.layoutHyperbolic(m_graph);

	System.out.println("Finished graph layout.");

	H3ViewParameters parameters = new H3ViewParameters(m_canvas);

	H3ScreenCapturer capturer = null;
	if (ENABLE_SCREEN_CAPTURE)
	{
	    capturer = new H3ScreenCapturer();
	}

	CapturingManager manager = new NullCapturingManager();

	if (ENABLE_ADAPTIVE_DRAWING)
	{
	    int queueSize = m_graph.getNumNodes() + m_graph.getTotalNumLinks();
	    H3RenderQueue queue = new H3RenderQueue(queueSize);

	    H3Transformer transformer =
		new H3Transformer(m_graph, queue, PROCESS_NONTREE_LINKS);

	    new Thread(transformer).start();

	    System.out.println("Started H3Transformer.");

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	    H3PointRenderList list = new H3PointRenderList(m_graph, true,
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
		renderer = new H3LineRenderer(m_graph, queue, list);
	    }
	    else
	    {
		renderer = new H3CircleRenderer(m_graph, parameters,
						queue, list);
	    }

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	    H3AdaptiveRenderLoop adaptive =
		new H3AdaptiveRenderLoop(m_graph, m_canvas, parameters,
					 transformer, queue,
					 renderer, capturer);

	    new Thread(adaptive).start();
	    m_renderLoop = adaptive;

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
	    m_graph.transformNodes(H3Transform.I4);

	    H3NonadaptiveRenderLoop nonadaptive =
		new H3NonadaptiveRenderLoop(m_graph, m_canvas, parameters,
					    capturer, PROCESS_NONTREE_LINKS);
	    new Thread(nonadaptive).start();
	    m_renderLoop = nonadaptive;

	    if (ENABLE_SCREEN_CAPTURE)
	    {
		manager = new NonadaptiveCapturingManager(capturer,
							  nonadaptive);
	    }

	    System.out.println("Started H3NonadaptiveRenderLoop.");
	}

	int rootNode = m_graph.getRootNode();
	m_eventHandler = new EventHandler
	    (m_canvas, m_renderLoop, manager, rootNode);

	System.out.println("Rendering started.");
    }

    ///////////////////////////////////////////////////////////////////////

    private void stopRendering()
    {
	m_renderLoop.shutdown();
	m_renderLoop = null;

	m_eventHandler.dispose();
	m_eventHandler = null;
    }

    ///////////////////////////////////////////////////////////////////////

    private Graph loadGraph(File file, Reader reader)
    {
	Graph retval = null;

	if (DEBUG_PRINT_LOAD_MEMORY) { m_memoryUsage.startGathering(); }

	long startTime = 0;
	if (DEBUG_PRINT_LOAD_TIME)
	{
	    startTime = System.currentTimeMillis();
	    System.out.println("load.begin[" + startTime +"]");
	}

	try
	{
	    GraphBuilder builder = GraphFactory.makeImmutableGraph();

	    GraphFileLexer lexer = new GraphFileLexer(reader);
	    GraphFileParser parser = new GraphFileParser(lexer);
	    parser.file(builder);

	    retval = builder.endConstruction();

	    if (DEBUG_PRINT_LOAD_MEMORY)
	    { m_memoryUsage.gatherAfterBufferLoaded(); }
	}
	catch (antlr.ANTLRException e)
	{
	    String msg = "Error parsing file `" + file.getPath() + "': "
		+ e.getMessage();
	    JOptionPane dialog = new JOptionPane();
	    dialog.showMessageDialog(null, msg, "Open Failed",
				     JOptionPane.ERROR_MESSAGE);
	}

	if (DEBUG_PRINT_LOAD_TIME)
	{
	    long stopTime = System.currentTimeMillis();
	    long duration = stopTime - startTime;
	    System.out.println("load.end[" + stopTime + "]");
	    System.out.println("load.time[" + duration + "]");
	}

	return retval;
    }

    ///////////////////////////////////////////////////////////////////////

    private File askUserForFile()
    {
	File retval = null;
	int result = m_fileChooser.showOpenDialog(m_frame);
	if (result == JFileChooser.APPROVE_OPTION)
	{
	    retval = m_fileChooser.getSelectedFile();
	    if (!retval.isFile())
	    {
		if (retval.exists())
		{
		    String msg =  "Path is not that of an ordinary file: "
			+ retval.getPath();
		    JOptionPane dialog = new JOptionPane();
		    dialog.showMessageDialog(null, msg, "Invalid Path",
					     JOptionPane.ERROR_MESSAGE);
		}
		else
		{
		    String msg =  "File not found: " + retval.getPath();
		    JOptionPane dialog = new JOptionPane();
		    dialog.showMessageDialog(null, msg, "File Not Found",
					     JOptionPane.ERROR_MESSAGE);
		}
	    }
	}
	return retval;
    }

    ///////////////////////////////////////////////////////////////////////

    private void initializeCanvas3D()
    {
	GraphicsConfiguration config =
	    SimpleUniverse.getPreferredConfiguration();

	m_canvas = new H3Canvas3D(config);
	m_canvas.stopRenderer();

	SimpleUniverse univ = new SimpleUniverse(m_canvas);
	univ.getViewingPlatform().setNominalViewingTransform();
    }

    ///////////////////////////////////////////////////////////////////////

    private JMenuBar createInitialMenuBar()
    {
	// Cause menus to be rendered as heavyweight objects.
	JPopupMenu.setDefaultLightWeightPopupEnabled(false);

	// Create "File" menu. --------------------------------------------

	JMenuItem openMenuItem = new JMenuItem("Open");
	openMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    handleOpenFileRequest();
		}
	    });

	m_saveWithLayoutMenuItem = new JMenuItem("Save With Layout");
	m_saveWithLayoutMenuItem.setEnabled(false);

	m_saveWithLayoutAsMenuItem = new JMenuItem("Save With Layout As");
	m_saveWithLayoutAsMenuItem.setEnabled(false);

	m_closeMenuItem = new JMenuItem("Close");
	m_closeMenuItem.setEnabled(false);
	m_closeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_closeMenuItem.setEnabled(false);
		    handleCloseFileRequest();
		}
	    });

	JMenuItem preferencesMenuItem = new JMenuItem("Preferences");
	preferencesMenuItem.setEnabled(false);

	JMenuItem exitMenuItem = new JMenuItem("Exit");
	exitMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    System.exit(0);
		}
	    });

	m_fileMenu = new JMenu("File");
	m_fileMenu.add(openMenuItem);
	m_fileMenu.add(m_saveWithLayoutMenuItem);
	m_fileMenu.add(m_saveWithLayoutAsMenuItem);
	m_fileMenu.add(m_closeMenuItem);
	m_fileMenu.addSeparator();
	m_fileMenu.add(preferencesMenuItem);
	m_fileMenu.addSeparator();
	m_fileMenu.add(exitMenuItem);

	// Create "Spanning Tree" menu. ------------------------------------

	m_spanningTreeMenu = new JMenu("Spanning Tree");

	// Create "Color Scheme" menu. -------------------------------------

	m_colorSchemeMenu = new JMenu("Color Scheme");

	// Create "Color Scheme" menu. -------------------------------------

	m_nodeLabelMenu = new JMenu("Node Label");

	// Create menu bar. ------------------------------------------------

	JMenuBar retval = new JMenuBar();
	retval.add(m_fileMenu);
	retval.add(m_spanningTreeMenu);
	retval.add(m_colorSchemeMenu);
	retval.add(m_nodeLabelMenu);
	return retval;
    }

    ///////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ///////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT_LOAD_TIME = true;
    private static final boolean DEBUG_PRINT_LOAD_MEMORY = true;

    private static final int DEFAULT_FRAME_WIDTH = 900;
    private static final int DEFAULT_FRAME_HEIGHT = 1000;

    private static final String WALRUS_TITLE = "Walrus";
    private static final String SPLASH_ICON_PATH = "walrus-splash.jpg";
    private static final String MSG_NO_GRAPH_LOADED = "No graph loaded.";
    private static final String MSG_GRAPH_LOADED = "Graph loaded.";
    private static final String MSG_LOADING_GRAPH = "Loading graph...";

    ///////////////////////////////////////////////////////////////////////

    private File m_file;  // Will be non-null when a graph is open.
    private Graph m_backingGraph;  // Will be non-null when a graph is open.
    private H3Graph m_graph;  // Will be non-null when a graph is open.
    private H3Canvas3D m_canvas;
    private H3RenderLoop m_renderLoop;
    private EventHandler m_eventHandler;
    private MemoryUsage m_memoryUsage = new MemoryUsage();

    private JFrame m_frame;
    private JTextField m_statusBar;
    private JLabel m_splashLabel;
    private JFileChooser m_fileChooser = new JFileChooser();

    private JMenu m_fileMenu;
    private JMenuItem m_saveWithLayoutMenuItem;
    private JMenuItem m_saveWithLayoutAsMenuItem;
    private JMenuItem m_closeMenuItem;
    private JMenu m_spanningTreeMenu;
    private JMenu m_colorSchemeMenu;
    private JMenu m_nodeLabelMenu;

    ///////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES
    ///////////////////////////////////////////////////////////////////////

    private static class MemoryUsage
    {
	public MemoryUsage()
	{
	}

	public void startGathering()
	{
	    System.gc();
	    m_baseTotalMemory = Runtime.getRuntime().totalMemory();
	    m_baseFreeMemory = Runtime.getRuntime().freeMemory();

	    m_bufferTotalMemory = 0;
	    m_bufferFreeMemory = 0;
	    m_peakTotalMemory = 0;
	    m_peakFreeMemory = 0;
	    m_finalTotalMemory = 0;
	    m_finalFreeMemory = 0;
	}

	public void gatherAfterBufferLoaded()
	{
	    System.gc();
	    m_bufferTotalMemory = Runtime.getRuntime().totalMemory();
	    m_bufferFreeMemory = Runtime.getRuntime().freeMemory();
	}

	public void gatherAtPeak()
	{
	    System.gc();
	    m_peakTotalMemory = Runtime.getRuntime().totalMemory();
	    m_peakFreeMemory = Runtime.getRuntime().freeMemory();
	}

	public void gatherAtFinal()
	{
	    System.gc();
	    m_finalTotalMemory = Runtime.getRuntime().totalMemory();
	    m_finalFreeMemory = Runtime.getRuntime().freeMemory();
	}

	public void printUsage()
	{
	    long baseUsed = m_baseTotalMemory - m_baseFreeMemory;
	    long bufferUsed = m_bufferTotalMemory - m_bufferFreeMemory;
	    long peakUsed = m_peakTotalMemory - m_peakFreeMemory;
	    long finalUsed = m_finalTotalMemory - m_finalFreeMemory;

	    System.out.println("===========================================");
	    System.out.println("baseTotalMemory = " + M(m_baseTotalMemory));
	    System.out.println("baseFreeMemory = " + M(m_baseFreeMemory));
	    System.out.println("baseUsed = " + M(baseUsed));
	    System.out.println("bufferTotalMemory = " +M(m_bufferTotalMemory));
	    System.out.println("bufferFreeMemory = " + M(m_bufferFreeMemory));
	    System.out.println("bufferUsed = " + M(bufferUsed));
	    System.out.println("peakTotalMemory = " + M(m_peakTotalMemory));
	    System.out.println("peakFreeMemory = " + M(m_peakFreeMemory));
	    System.out.println("peakUsed = " + M(peakUsed));
	    System.out.println("finalTotalMemory = " + M(m_finalTotalMemory));
	    System.out.println("finalFreeMemory = " + M(m_finalFreeMemory));
	    System.out.println("finalUsed = " + M(finalUsed));
	    System.out.println();
	    System.out.println("bufferUsed - baseUsed = "
			       + M(bufferUsed - baseUsed));
	    System.out.println("peakUsed - baseUsed = "
			       + M(peakUsed - baseUsed));
	    System.out.println("peakUsed - bufferUsed = "
			       + M(peakUsed - bufferUsed));
	    System.out.println("finalUsed - baseUsed = "
			       + M(finalUsed - baseUsed));
	    System.out.println("finalUsed - peakUsed = "
			       + M(finalUsed - peakUsed));
	    System.out.println("===========================================");
	}

	private String M(long n)
	{
	    long x = n / 100000;
	    long mega = x / 10;
	    long kilo = Math.abs(x % 10);
	    return "" + n + " (" + mega + "." + kilo + "e6)";
	}

	// At start.
	private long m_baseTotalMemory;
	private long m_baseFreeMemory;

	// After loading into graph buffer.
	private long m_bufferTotalMemory;
	private long m_bufferFreeMemory;

	// After populating graph, but before freeing the graph buffer.
	private long m_peakTotalMemory;  
	private long m_peakFreeMemory;

	// After freeing graph buffer, and leaving only graph.
	private long m_finalTotalMemory;
	private long m_finalFreeMemory;
    }

    ///////////////////////////////////////////////////////////////////////

    private static class EventHandler
	extends H3MouseInputAdapter
    {
	public EventHandler(H3Canvas3D canvas, H3RenderLoop renderLoop,
			    CapturingManager manager, int rootNode)
	{
	    m_canvas = canvas;
	    m_canvas.addMouseListener(this);
	    m_canvas.addMouseMotionListener(this);
	    m_canvas.addPaintObserver(m_paintObserver);

	    m_renderLoop = new H3CapturingRenderLoop(renderLoop);
	    m_capturingManager = manager;
	    m_rootNode = m_currentNode = m_previousNode = rootNode;
	}

	public void dispose()
	{
	    m_canvas.removeMouseListener(this);
	    m_canvas.removeMouseMotionListener(this);
	    m_canvas.removePaintObserver(m_paintObserver);
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

	private H3Canvas3D m_canvas;
	private H3CapturingRenderLoop m_renderLoop;
	private CapturingManager m_capturingManager;

	private int m_rootNode;
	private int m_currentNode;
	private int m_previousNode;

	private Point2d m_center = new Point2d();

	private int m_lastX = 0;
	private int m_lastY = 0;

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

	private PaintObserver m_paintObserver = new PaintObserver();

	private class PaintObserver
	    implements Observer
	{
	    public void update(Observable o, Object arg)
	    {
		if (!m_isRotating)
		{
		    m_renderLoop.refreshDisplay();
		}
	    }
	}
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

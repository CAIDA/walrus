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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;
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
	// NOTE: We must be running under JDK 1.3.0 or later, if on Solaris,
	//       owing to various JDK bugs.  Even 1.3.0 isn't enough in
	//       some cases.
	// 
	// Some bugs to keep in mind from Sun's bug database
	//  (http://developer.java.sun.com/developer/bugParade/bugs/...):
	// 
	//  * (4209844.html) "Modifiers not set for KEY_TYPED events on win32"
	//    This affects 1.1.8 and 1.2.2 on Solaris.  The modifiers
	//    (shift, meta, etc.) aren't set for KeyListener.keyTyped()
	//    events.  This is fixed in Java 1.3.0.
	//
	//  * (4344900.html) "Keyboard accelerators not working with canvas"
	//    This affects 1.2.2 and 1.3.0.  When a heavyweight component
	//    has focus, keyboard accelerators for Swing menus don't work.
	//
	//  * (4362074.html) "KeyEvent not get unless requestFocus() when
	//    focus lost in heavy weight Canvas"  This affects 1.2.2 and
	//    1.3.0.  It seems once a heavyweight component loses focus to
	//    a lightweight component, the heavyweight component fails to
	//    get key events (even after being selected) until requestFocus()
	//    has been called.

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

        java.net.URL url = H3Main.class.getResource(SPLASH_ICON_PATH);
	if (url != null)
	{
	    m_splashLabel = new JLabel(new ImageIcon(url));
	}
	else
	{
	    m_splashLabel = new JLabel(SPLASH_HTML_LABEL, JLabel.CENTER);
	}
	m_frame.getContentPane().add(m_splashLabel, BorderLayout.CENTER);

	m_statusBar = new JTextField();
	m_statusBar.setEditable(false);
	m_statusBar.setText(MSG_NO_GRAPH_LOADED);
	m_frame.getContentPane().add(m_statusBar, BorderLayout.SOUTH);

	m_frame.setJMenuBar(createInitialMenuBar());
	m_colorSchemeMenu.enableDefaultColorScheme();

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

		if (DEBUG_PRINT_LOAD_MEMORY) { m_memoryUsage.gatherAtPeak(); }
		if (DEBUG_PRINT_LOAD_MEMORY)
		{
		    m_memoryUsage.gatherAtFinal();
		    m_memoryUsage.printUsage();
		}

		if (backingGraph != null)
		{
		    populateMenus(backingGraph);

		    m_backingGraph = backingGraph;
		    m_viewParameters.resetObjectTransform();

		    m_frame.setTitle(WALRUS_TITLE + " -- " + file.getPath());
		    m_statusBar.setText(MSG_GRAPH_LOADED);

		    m_closeMenuItem.setEnabled(true);
		    m_startMenuItem.setEnabled(true);
		}
	    }
	    catch (FileNotFoundException e)
	    {
		String msg =  "File not found: " + file.getPath();
		JOptionPane dialog = new JOptionPane();
		dialog.showMessageDialog(null, msg, "File Not Found",
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

    private RenderingConfiguration createRenderingConfigurationSnapshot()
    {
	RenderingConfiguration retval = new RenderingConfiguration();

	int numSpanningTrees = m_spanningTreeMenu.getItemCount();
	for (int i = 0; i < numSpanningTrees; i++)
	{
	    JMenuItem menuItem = m_spanningTreeMenu.getItem(i);
	    if (menuItem.isSelected())
	    {
		retval.spanningTree = menuItem.getText();
		break;
	    }
	}

	retval.adaptiveRendering = m_adaptiveMenuItem.isSelected();
	retval.multipleNodeSizes = m_multipleNodeSizesMenuItem.isSelected();
	retval.depthCueing = m_depthCueingMenuItem.isSelected();
	retval.axes = m_axesMenuItem.isSelected();
	retval.supportScreenCapture = m_screenCaptureMenuItem.isSelected();
	retval.automaticRefresh = m_automaticRefreshMenuItem.isSelected();
	retval.nodeColor =
	    m_colorSchemeMenu.createNodeColorConfigurationSnapshot();
	retval.treeLinkColor =
	    m_colorSchemeMenu.createTreeLinkColorConfigurationSnapshot();
	retval.nontreeLinkColor =
	    m_colorSchemeMenu.createNontreeLinkColorConfigurationSnapshot();

	int numSelected = countNumSelectedItems(m_nodeLabelMenu);
	retval.nodeLabelAttributes = new int[numSelected];

	int numAdded = 0;
	int numAttributes = m_nodeLabelMenu.getItemCount();
	for (int i = 0; i < numAttributes; i++)
	{
	    JMenuItem menuItem = m_nodeLabelMenu.getItem(i);
	    if (menuItem != null && menuItem.isSelected())
	    {
		AttributeDefinitionIterator iterator =
		    m_backingGraph.getAttributeDefinition(menuItem.getText());
		if (iterator.atEnd())
		{
		    String msg = "no attribute named `" + menuItem.getText()
			+ "' found";
		    throw new RuntimeException(msg);
		}
		retval.nodeLabelAttributes[numAdded++] = iterator.getID();
	    }
	}

	return retval;
    }

    ///////////////////////////////////////////////////////////////////////

    private void populateMenus(Graph graph)
    {
	m_spanningTreeQualifiers =
	    m_graphLoader.loadSpanningTreeQualifiers(graph);
	{
	    m_spanningTreeButtonGroup = new ButtonGroup();
	    ListIterator iterator = m_spanningTreeQualifiers.listIterator();
	    while (iterator.hasNext())
	    {
		String name = (String)iterator.next();
		JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(name);
		m_spanningTreeMenu.add(menuItem);
		m_spanningTreeButtonGroup.add(menuItem);
	    }

	    if (m_spanningTreeMenu.getItemCount() > 0)
	    {
		m_spanningTreeMenu.getItem(0).setSelected(true);
	    }
	}

	m_nodeLabelAttributes = m_graphLoader.loadAttributes
	    (graph, m_allAttributeTypeMatcher);
	{
	    ListIterator iterator = m_nodeLabelAttributes.listIterator();
	    while (iterator.hasNext())
	    {
		String name = (String)iterator.next();
		JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(name);
		m_nodeLabelMenu.add(menuItem);
	    }
	}

	m_colorSchemeMenu.populateAttributeMenus(m_graphLoader, graph);
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleCloseFileRequest()
    {
	m_renderingConfiguration = null;
	m_backingGraph = null;
	m_graph = null;
	m_displayPosition = null;
	if (m_renderLoop != null)
	{
	    stopRendering();
	}

	// UI.
	m_frame.setTitle(WALRUS_TITLE);
	m_statusBar.setText(MSG_NO_GRAPH_LOADED);
	reinstateSplashScreenContentPane();

	// File menu.
	m_saveWithLayoutMenuItem.setEnabled(false);
	m_saveWithLayoutAsMenuItem.setEnabled(false);
	m_closeMenuItem.setEnabled(false);

	// Rendering menu.
	m_startMenuItem.setEnabled(false);
	m_stopMenuItem.setEnabled(false);
	m_updateMenuItem.setEnabled(false);
	m_refreshMenuItem.setEnabled(false);

	// Spanning Tree menu.
	m_spanningTreeMenu.removeAll();
	m_spanningTreeButtonGroup = null;
	m_spanningTreeQualifiers = null;

	// Color Scheme menu.
	m_colorSchemeMenu.removeAttributeMenus();
	m_colorSchemeMenu.enableReasonableColorScheme();

	// Node Label menu.
	m_nodeLabelMenu.removeAll();
	m_nodeLabelAttributes = null;

	System.out.println("Finished handleCloseFileRequest()");
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleStartRenderingRequest()
    {
	RenderingConfiguration renderingConfiguration =
	    createRenderingConfigurationSnapshot();
	renderingConfiguration.print();

	if (setupRendering(renderingConfiguration))
	{
	    reinstateCanvasContentPane();
	    setupActiveRenderingMenu();
	    startRendering(renderingConfiguration);
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private void colorNodes(ColorConfiguration configuration)
    {
	System.out.print("(colorNodes) ");
	configuration.print();

	setNodeTransparencyEnabled
	    (configuration.scheme == ColorConfiguration.TRANSPARENT);

	switch (configuration.scheme)
	{
	case ColorConfiguration.INVISIBLE:
	    // No explicit coloring needed.
	    // startRendering() will take care of setting things up.
	    break;

	case ColorConfiguration.TRANSPARENT:
	    // No further work needed.
	    break;

	case ColorConfiguration.FIXED_COLOR:
	    {
		ColoringAttributes attributes =
		    makeColoringAttributes(configuration.fixedColor);

		m_viewParameters.getNodeAppearance()
		    .setColoringAttributes(attributes);
		m_viewParameters.getNearNodeAppearance()
		    .setColoringAttributes(attributes);
		m_viewParameters.getMiddleNodeAppearance()
		    .setColoringAttributes(attributes);
		m_viewParameters.getFarNodeAppearance()
		    .setColoringAttributes(attributes);
	    }	    
	    break;

	case ColorConfiguration.HOT_TO_COLD:
	    break;

	case ColorConfiguration.LOG_HOT_TO_COLD:
	    break;

	case ColorConfiguration.HUE:
	    break;

	case ColorConfiguration.RGB:
	    colorNodesRGB(configuration.colorAttribute);
	    break;

	default: throw new RuntimeException();
	}
    }

    private void setNodeTransparencyEnabled(boolean state)
    {
	if (state)
	{
	    TransparencyAttributes attributes =
		m_viewParameters.getTransparencyAttributes();

	    m_viewParameters.getNodeAppearance()
		.setTransparencyAttributes(attributes);
	    m_viewParameters.getNearNodeAppearance()
		.setTransparencyAttributes(attributes);
	    m_viewParameters.getMiddleNodeAppearance()
		.setTransparencyAttributes(attributes);
	    m_viewParameters.getFarNodeAppearance()
		.setTransparencyAttributes(attributes);
	}
	else
	{
	    m_viewParameters.getNodeAppearance()
		.setTransparencyAttributes(null);
	    m_viewParameters.getNearNodeAppearance()
		.setTransparencyAttributes(null);
	    m_viewParameters.getMiddleNodeAppearance()
		.setTransparencyAttributes(null);
	    m_viewParameters.getFarNodeAppearance()
		.setTransparencyAttributes(null);
	}
    }

    private void colorTreeLinks(ColorConfiguration configuration)
    {
	System.out.print("(colorTreeLinks) ");
	configuration.print();

	setTreeLinkTransparencyEnabled
	    (configuration.scheme == ColorConfiguration.TRANSPARENT);

	switch (configuration.scheme)
	{
	case ColorConfiguration.INVISIBLE:
	    // No explicit coloring needed.
	    // startRendering() will take care of setting things up.
	    break;

	case ColorConfiguration.TRANSPARENT:
	    // Nothing further to do.
	    break;

	case ColorConfiguration.FIXED_COLOR:
	    {
		ColoringAttributes attributes =
		    makeColoringAttributes(configuration.fixedColor);
		m_viewParameters.getTreeLinkAppearance()
		    .setColoringAttributes(attributes);
	    }	    
	    break;

	case ColorConfiguration.HOT_TO_COLD:
	    break;

	case ColorConfiguration.LOG_HOT_TO_COLD:
	    break;

	case ColorConfiguration.HUE:
	    break;

	case ColorConfiguration.RGB:
	    colorLinksRGB(configuration.colorAttribute, true);
	    break;

	default: throw new RuntimeException();
	}
    }

    private void setTreeLinkTransparencyEnabled(boolean state)
    {
	if (state)
	{
	    TransparencyAttributes attributes =
		m_viewParameters.getTransparencyAttributes();

	    m_viewParameters.getTreeLinkAppearance()
		.setTransparencyAttributes(attributes);
	}
	else
	{
	    m_viewParameters.getTreeLinkAppearance()
		.setTransparencyAttributes(null);
	}
    }

    private void colorNontreeLinks(ColorConfiguration configuration)
    {
	System.out.print("(colorNontreeLinks) ");
	configuration.print();

	setNontreeLinkTransparencyEnabled
	    (configuration.scheme == ColorConfiguration.TRANSPARENT);

	switch (configuration.scheme)
	{
	case ColorConfiguration.INVISIBLE:
	    // No explicit coloring needed.
	    // startRendering() will take care of setting things up.
	    break;

	case ColorConfiguration.TRANSPARENT:
	    // Nothing further to do.
	    break;

	case ColorConfiguration.FIXED_COLOR:
	    {
		ColoringAttributes attributes =
		    makeColoringAttributes(configuration.fixedColor);
		m_viewParameters.getNontreeLinkAppearance()
		    .setColoringAttributes(attributes);
	    }	    
	    break;

	case ColorConfiguration.HOT_TO_COLD:
	    break;

	case ColorConfiguration.LOG_HOT_TO_COLD:
	    break;

	case ColorConfiguration.HUE:
	    break;

	case ColorConfiguration.RGB:
	    colorLinksRGB(configuration.colorAttribute, false);
	    break;

	default: throw new RuntimeException();
	}
    }

    private void setNontreeLinkTransparencyEnabled(boolean state)
    {
	if (state)
	{
	    TransparencyAttributes attributes =
		m_viewParameters.getTransparencyAttributes();

	    m_viewParameters.getNontreeLinkAppearance()
		.setTransparencyAttributes(attributes);
	}
	else
	{
	    m_viewParameters.getNontreeLinkAppearance()
		.setTransparencyAttributes(null);
	}
    }

    // Performance Note:
    //
    //   There is a possibility for poor performance in the following methods
    //   that color nodes and links.  It turns out that exceptions are not
    //   lightweight in Java, and so an extensive use of exceptions can
    //   dramatically slow down a program.  The weakness of the following
    //   methods is exactly in their potentially extensive use of exceptions.
    //   Exceptions will be thrown whenever a node or link does not have a
    //   value for a coloring attribute.  Ordinarily, the creator of a graph
    //   will supply attribute values for all objects, but they are not
    //   required to do so.  A realistic scenario where they may not supply
    //   all values is when these values result from measurements, and there
    //   are gaps in the measurement data.  In such situations, it would be
    //   best if the graph creator were to add a default value to the coloring
    //   attribute, so that all objects effectively have some value.  Some
    //   out-of-range value indicating "unknown" would be the best approach.
    //
    //   Owing to the fact that IDs of nodes and links in H3Graph need not
    //   necessarily match the IDs used in the backing libsea Graph, we
    //   cannot simply eliminate this undesirable trait by accessing the
    //   attributes in a different way.  Specifically, we could theoretically
    //   avoid exceptions altogether by using AttributesByAttributeIterator,
    //   which iterates over only the objects that have values.  What makes
    //   this approach impracticable is its reliance on some way of mapping
    //   IDs of objects in the backing libsea Graph to IDs used in H3Graph.
    //   It is not impossible to set up such mappings, either in H3Graph or
    //   in libsea.  For example, we could do so in the latter by adding an
    //   internal attribute to all the objects which gives the ID.  Then
    //   we could simply use the mechanisms already in place for accessing
    //   attributes to carry out the mapping.  To set up a mapping in H3Graph,
    //   on the other hand, would require something like a hash table or
    //   binary search on a sorted array.  There are drawbacks, however, to
    //   either approach.  I've therefore avoided relying on such mappings,
    //   and used instead an approach which will work well in typical cases.
    //   If the graph creator is careful, this approach will in fact work
    //   well in all cases.

    // NOTE: Attribute must be of type int, float3, or double3.
    private void colorNodesRGB(String colorAttribute)
    {
	int attribute =
	    m_backingGraph.getAttributeDefinition(colorAttribute).getID();

	int defaultColor = Color.white.getRGB();
	int numNodes = m_graph.getNumNodes();
	for (int i = 0; i < numNodes; i++)
	{
	    int color = defaultColor;
	    try
	    {
		int nodeID = m_graph.getNodeID(i);
		ValueIterator iterator =
		    m_backingGraph.getNodeAttribute(nodeID, attribute);
		color = extractRGBColor(iterator);
	    }
	    catch (AttributeUnavailableException e)
	    {
		// Nothing to do--simply use the default color.
	    }
	    m_graph.setNodeColor(i, color);
	}
    }

    // NOTE: Attribute must be of type int, float3, or double3.
    private void colorLinksRGB(String colorAttribute, boolean treeLink)
    {
	int attribute =
	    m_backingGraph.getAttributeDefinition(colorAttribute).getID();

	int defaultColor = Color.white.getRGB();
	int numLinks = m_graph.getTotalNumLinks();
	for (int i = 0; i < numLinks; i++)
	{
	    if (m_graph.checkTreeLink(i) == treeLink)
	    {
		int color = defaultColor;
		try
		{
		    int linkID = m_graph.getLinkID(i);
		    ValueIterator iterator =
			m_backingGraph.getLinkAttribute(linkID, attribute);
		    color = extractRGBColor(iterator);
		}
		catch (AttributeUnavailableException e)
		{
		    // Nothing to do--simply use the default color.
		}
		m_graph.setLinkColor(i, color);
	    }
	}
    }

    private int extractRGBColor(ValueIterator iterator)
    {
	int retval = 0;
	switch (iterator.getType().getType())
	{
	case ValueType._INTEGER:
	    retval = iterator.getIntegerValue();
	    break;

	case ValueType._FLOAT3:
	    {
		iterator.getFloat3Value(m_float3Temporary);
		normalizeColorComponents(m_float3Temporary);
		retval = makeColor(m_float3Temporary);
	    }
	    break;

	case ValueType._DOUBLE3:
	    {
		iterator.getDouble3Value(m_double3Temporary);
		normalizeColorComponents(m_double3Temporary);
		retval = makeColor(m_double3Temporary);
	    }
	    break;

	case ValueType._BOOLEAN:
	    //FALLTHROUGH
	case ValueType._FLOAT:
	    //FALLTHROUGH
	case ValueType._DOUBLE:
	    //FALLTHROUGH
	case ValueType._STRING:
	    //FALLTHROUGH
	case ValueType._ENUMERATION:
	    //FALLTHROUGH
	default: throw new RuntimeException();
	}
	return retval;
    }

    private void normalizeColorComponents(float[] color)
    {
	for (int i = 0; i < color.length; i++)
	{
	    float value = color[i];
	    if (value < 0.0f)
	    {
		value = 0.0f;
	    }
	    else if (value > 1.0f)
	    {
		value = 1.0f;
	    }
	    color[i] = value;
	}
    }

    private void normalizeColorComponents(double[] color)
    {
	for (int i = 0; i < color.length; i++)
	{
	    double value = color[i];
	    if (value < 0.0)
	    {
		value = 0.0;
	    }
	    else if (value > 1.0)
	    {
		value = 1.0;
	    }
	    color[i] = value;
	}
    }

    private int makeColor(float[] color)
    {
	int r = (int)(255.0f * color[0]);
	int g = (int)(255.0f * color[1]);
	int b = (int)(255.0f * color[2]);
	return (r << 16) | (g << 8) | b;
    }

    private int makeColor(double[] color)
    {
	int r = (int)(255.0 * color[0]);
	int g = (int)(255.0 * color[1]);
	int b = (int)(255.0 * color[2]);
	return (r << 16) | (g << 8) | b;
    }

    private ColoringAttributes makeColoringAttributes(int color)
    {
	int r = (color >> 16) & 0xFF;
	int g = (color >> 8) & 0xFF;
	int b = color & 0xFF;
	return new ColoringAttributes(r / 255.0f, g / 255.0f, b / 255.0f,
				      ColoringAttributes.FASTEST);
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleStopRenderingRequest()
    {
	m_displayPosition = m_renderLoop.getDisplayPosition();
	stopRendering();

	reinstateSplashScreenContentPane();
	setupIdleRenderingMenu();
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleUpdateRenderingRequest()
    {
	m_displayPosition = m_renderLoop.getDisplayPosition();
	stopRendering();

	RenderingConfiguration renderingConfiguration =
	    createRenderingConfigurationSnapshot();
	renderingConfiguration.print();

	if (setupRendering(renderingConfiguration))
	{
	    startRendering(renderingConfiguration);
	}
	else
	{
	    reinstateSplashScreenContentPane();
	    setupIdleRenderingMenu();
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private boolean setupRendering
	(RenderingConfiguration renderingConfiguration)
    {
	boolean retval = false;
	try
	{
	    if (m_renderingConfiguration == null
		|| !renderingConfiguration.spanningTree.equals
		(m_renderingConfiguration.spanningTree))
	    {
		m_displayPosition = null;
		m_graph = m_graphLoader.load
		    (m_backingGraph, renderingConfiguration.spanningTree);

		if (DEBUG_CHECK_ID_MAPPINGS)
		{
		    checkGraphIDMappings(m_graph, m_backingGraph);
		}

		H3GraphLayout layout = new H3GraphLayout();
		layout.layoutHyperbolic(m_graph);

		System.out.println("Finished graph layout.");

		colorNodes(renderingConfiguration.nodeColor);
		colorTreeLinks(renderingConfiguration.treeLinkColor);
		colorNontreeLinks(renderingConfiguration.nontreeLinkColor);
	    }
	    else
	    {
		if (!renderingConfiguration.nodeColor
		    .equalColoring(m_renderingConfiguration.nodeColor))
		{
		    colorNodes(renderingConfiguration.nodeColor);
		}

		if (!renderingConfiguration.treeLinkColor
		    .equalColoring(m_renderingConfiguration.treeLinkColor))
		{
		    colorTreeLinks(renderingConfiguration.treeLinkColor);
		}

		if (!renderingConfiguration.nontreeLinkColor
		    .equalColoring(m_renderingConfiguration.nontreeLinkColor))
		{
		    colorNontreeLinks(renderingConfiguration.nontreeLinkColor);
		}
	    }

	    m_renderingConfiguration = renderingConfiguration;
	    retval = true;
	}
	catch (H3GraphLoader.InvalidGraphDataException e)
	{
	    String msg = "Graph file lacks needed data: " + e.getMessage();
	    JOptionPane dialog = new JOptionPane();
	    dialog.showMessageDialog(null, msg, "Rendering Failed",
				     JOptionPane.ERROR_MESSAGE);
	}
	return retval;
    }

    // Debugging routine.
    // XXX: Only works for ImmutableGraph at the moment.
    private void checkGraphIDMappings(H3Graph graph, Graph backingGraph)
    {
	// For immutable graphs, the node ID mapping will always be identity.
	int numNodes = graph.getNumNodes();
	for (int i = 0; i < numNodes; i++)
	{
	    if (graph.getNodeID(i) != i)
	    {
		String msg = "node id[" + graph.getNodeID(i) + "] != " + i;
		throw new RuntimeException(msg);
	    }
	}

	// The following check should work for any kind of graph, immutable
	// or otherwise.
	int numLinks = graph.getTotalNumLinks();
	for (int i = 0; i < numLinks; i++)
	{
	    int source = graph.getLinkSource(i);
	    int destination = graph.getLinkDestination(i);
	    int sourceID = graph.getNodeID(source);
	    int destinationID = graph.getNodeID(destination);
	    int linkID = graph.getLinkID(i);

	    LinkIterator iterator = backingGraph.getLink(linkID);
	    if (iterator.getSource() != sourceID)
	    {
		String msg = "source[" + iterator.getSource()
		    + "] of link[" + linkID + ", " + i + "] != ["
		    + sourceID + "]";
		throw new RuntimeException(msg);
	    }

	    if (iterator.getDestination() != destinationID)
	    {
		String msg = "destination[" + iterator.getDestination()
		    + "] of link[" + linkID + ", " + i + "] != ["
		    + destinationID + "]";
		throw new RuntimeException(msg);
	    }
	}

	System.out.println("ID mappings look consistent.");
    }

    ///////////////////////////////////////////////////////////////////////

    private void startRendering(RenderingConfiguration renderingConfiguration)
    {
	System.out.println("numNodes = " + m_graph.getNumNodes());
	System.out.println("numTreeLinks = " + m_graph.getNumTreeLinks());
	System.out.println("numNontreeLinks = " +m_graph.getNumNontreeLinks());

	m_viewParameters.setDepthCueingEnabled
	    (renderingConfiguration.depthCueing);
	m_viewParameters.setAxesEnabled
	    (renderingConfiguration.axes);

	H3ScreenCapturer capturer = null;
	if (renderingConfiguration.supportScreenCapture)
	{
	    capturer = new H3ScreenCapturer();
	}

	CapturingManager manager = new NullCapturingManager();

	if (renderingConfiguration.adaptiveRendering)
	{
	    int queueSize = m_graph.getNumNodes() + m_graph.getTotalNumLinks();
	    H3RenderQueue queue = new H3RenderQueue(queueSize);

	    boolean processNontreeLinks =
		(renderingConfiguration.nontreeLinkColor.scheme
		 != ColorConfiguration.INVISIBLE);

	    H3Transformer transformer =
		new H3Transformer(m_graph, queue, processNontreeLinks);

	    new Thread(transformer).start();

	    System.out.println("Started H3Transformer.");

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	    boolean useNodeSizes =
		renderingConfiguration.multipleNodeSizes;
	    boolean includeNodes = determineWhetherToIncludeObject
		(renderingConfiguration.nodeColor);
	    boolean includeTreeLinks = determineWhetherToIncludeObject
		(renderingConfiguration.treeLinkColor);
	    boolean includeNontreeLinks = determineWhetherToIncludeObject
		(renderingConfiguration.nontreeLinkColor);
	    boolean includeNodeColor = determineWhetherToIncludeColor
		(renderingConfiguration.nodeColor);
	    boolean includeTreeLinkColor = determineWhetherToIncludeColor
		(renderingConfiguration.treeLinkColor);
	    boolean includeNontreeLinkColor = determineWhetherToIncludeColor
		(renderingConfiguration.nontreeLinkColor);

	    H3PointRenderList list = new H3PointRenderList
		(m_graph, useNodeSizes,
		 includeNodes, includeNodeColor,
		 includeTreeLinks, includeTreeLinkColor,
		 includeNontreeLinks, includeNontreeLinkColor);

	    list.setNearNodeAppearance
		(m_viewParameters.getNearNodeAppearance());
	    list.setMiddleNodeAppearance
		(m_viewParameters.getMiddleNodeAppearance());
	    list.setFarNodeAppearance
		(m_viewParameters.getFarNodeAppearance());
	    list.setTreeLinkAppearance
		(m_viewParameters.getTreeLinkAppearance());

	    if (true)
	    {
		list.setNontreeLinkAppearance
		    (m_viewParameters.getNontreeLinkAppearance());
	    }
	    else
	    {
		list.setNontreeLinkAppearance
		    (m_viewParameters.getLineAppearance());
	    }

	    H3AdaptiveRenderer renderer = null;

	    if (true)
	    {
		renderer = new H3LineRenderer(m_graph, queue, list);
	    }
	    else
	    {
		renderer = new H3CircleRenderer
		    (m_graph, m_viewParameters, queue, list);
	    }

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	    H3AdaptiveRenderLoop adaptive = new H3AdaptiveRenderLoop
		(m_graph, m_canvas, m_viewParameters,
		 transformer, queue, renderer, capturer);

	    new Thread(adaptive).start();
	    m_renderLoop = adaptive;

            final int DURATION = 50;
	    adaptive.setMaxRotationDuration(DURATION);
	    adaptive.setMaxTranslationDuration(DURATION);
	    adaptive.setMaxCompletionDuration(DURATION);

	    if (renderingConfiguration.supportScreenCapture)
	    {
		manager = new AdaptiveCapturingManager(capturer, adaptive);
	    }

	    System.out.println("Started H3AdaptiveRenderLoop.");
	}
	else
	{
	    m_graph.transformNodes(H3Transform.I4);

	    boolean processNontreeLinks =
		(renderingConfiguration.nontreeLinkColor.scheme
		 != ColorConfiguration.INVISIBLE);

	    H3NonadaptiveRenderLoop nonadaptive = new H3NonadaptiveRenderLoop
		(m_graph, m_canvas, m_viewParameters,
		 capturer, processNontreeLinks);
	    new Thread(nonadaptive).start();
	    m_renderLoop = nonadaptive;

	    if (renderingConfiguration.supportScreenCapture)
	    {
		manager = new NonadaptiveCapturingManager
		    (capturer, nonadaptive);
	    }

	    System.out.println("Started H3NonadaptiveRenderLoop.");
	}

	int rootNode = m_graph.getRootNode();
	m_eventHandler = new EventHandler
	    (m_canvas, m_renderLoop, manager, rootNode,
	     m_graph, m_backingGraph,
	     renderingConfiguration.nodeLabelAttributes, m_statusBar,
	     renderingConfiguration.automaticRefresh);

	if (m_displayPosition != null)
	{
	    m_renderLoop.setDisplayPosition(m_displayPosition);
	}

	System.out.println("Rendering started.");
    }

    ///////////////////////////////////////////////////////////////////////

    private boolean determineWhetherToIncludeColor
	(ColorConfiguration configuration)
    {
	return (configuration.scheme != ColorConfiguration.INVISIBLE
		&& configuration.scheme != ColorConfiguration.TRANSPARENT
		&& configuration.scheme != ColorConfiguration.FIXED_COLOR);
    }

    private boolean determineWhetherToIncludeObject
	(ColorConfiguration configuration)
    {
	return (configuration.scheme != ColorConfiguration.INVISIBLE);
    }

    ///////////////////////////////////////////////////////////////////////

    private void stopRendering()
    {
	m_eventHandler.dispose();
	m_eventHandler = null;

	// We must call waitForShutdown() to avoid problems that occur
	// when multiple H3RenderLoop instances overlap in lifetime.
	// Specifically, when a user selects Rendering->Update, the old
	// and the new render loops may overlap briefly in lifetime.  Because
	// each render loop accesses the same Canvas3D (via GraphicsContext3D),
	// the old render loop may change settings in Canvas3D that affect
	// the new render loop.  An example of this problem involves
	// front-buffer rendering.  H3AdaptiveRenderLoop.synchCompleteState()
	// turns off front-buffer rendering as it changes to STATE_SHUTDOWN.
	// If the old render loop turns off front-buffer rendering sometime
	// after the new render loop has completed its beCompleteInitState(),
	// in which front-buffer rendering is turned on, then the new render
	// loop may execute its beCompleteState() with front-buffer rendering
	// turned off.  The result is a partially drawn display, since
	// rendering is taking place in the back buffer.
	//
	// The simplest solution to this problem is to simply wait for the
	// old render loop to completely exit before starting the new
	// render loop.  This is exactly what waitForShutdown() lets us do.

	m_renderLoop.shutdown();
	m_renderLoop.waitForShutdown();
	m_renderLoop = null;
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
	    // NOTE: ANTLRException.toMessage() doesn't include position.
	    String msg = "Error parsing file `" + file.getPath() + "': "
		+ e.toString();
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

    private void reinstateCanvasContentPane()
    {
	m_frame.getContentPane().remove(m_splashLabel);
	m_frame.getContentPane().add(m_canvas, BorderLayout.CENTER);
	m_frame.validate();
    }

    ///////////////////////////////////////////////////////////////////////

    private void reinstateSplashScreenContentPane()
    {
	m_frame.getContentPane().remove(m_canvas);
	m_frame.getContentPane().add(m_splashLabel, BorderLayout.CENTER);
	m_frame.validate();
    }

    ///////////////////////////////////////////////////////////////////////

    // Changes the enabled state of the items in the Rendering menu to
    // reflect an idle rendering state (when the splash screen is being shown).
    private void setupIdleRenderingMenu()
    {
	m_startMenuItem.setEnabled(true);
	m_stopMenuItem.setEnabled(false);
	m_updateMenuItem.setEnabled(false);
	m_refreshMenuItem.setEnabled(false);
    }

    ///////////////////////////////////////////////////////////////////////

    // Changes the enabled state of the items in the Rendering menu to
    // reflect an active rendering state (when a Canvas3D is being shown).
    private void setupActiveRenderingMenu()
    {
	m_startMenuItem.setEnabled(false);
	m_stopMenuItem.setEnabled(true);
	m_updateMenuItem.setEnabled(true);
	m_refreshMenuItem.setEnabled(true);
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

	m_viewParameters = new H3ViewParameters(m_canvas);
    }

    ///////////////////////////////////////////////////////////////////////

    private JMenuBar createInitialMenuBar()
    {
	// Cause menus to be rendered as heavyweight objects.
	JPopupMenu.setDefaultLightWeightPopupEnabled(false);

	// Create "File" menu. --------------------------------------------

	JMenuItem openMenuItem = new JMenuItem("Open");
	openMenuItem.setMnemonic(KeyEvent.VK_O);
	openMenuItem.setAccelerator
	    (KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
	openMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    handleOpenFileRequest();
		}
	    });

	m_saveWithLayoutMenuItem = new JMenuItem("Save With Layout");
	m_saveWithLayoutMenuItem.setMnemonic(KeyEvent.VK_S);
	m_saveWithLayoutMenuItem.setAccelerator
	    (KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
	m_saveWithLayoutMenuItem.setEnabled(false);

	m_saveWithLayoutAsMenuItem = new JMenuItem("Save With Layout As");
	m_saveWithLayoutAsMenuItem.setMnemonic(KeyEvent.VK_A);
	m_saveWithLayoutAsMenuItem.setEnabled(false);

	m_closeMenuItem = new JMenuItem("Close");
	m_closeMenuItem.setEnabled(false);
	m_closeMenuItem.setMnemonic(KeyEvent.VK_C);
	m_closeMenuItem.setAccelerator
	    (KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
	m_closeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_closeMenuItem.setEnabled(false);
		    handleCloseFileRequest();
		}
	    });

	JMenuItem preferencesMenuItem = new JMenuItem("Preferences");
	preferencesMenuItem.setMnemonic(KeyEvent.VK_R);
	preferencesMenuItem.setEnabled(false);

	JMenuItem exitMenuItem = new JMenuItem("Exit");
	exitMenuItem.setMnemonic(KeyEvent.VK_X);
	exitMenuItem.setAccelerator
	    (KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
	exitMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    System.exit(0);
		}
	    });

	m_fileMenu = new JMenu("File");
	m_fileMenu.setMnemonic(KeyEvent.VK_F);
	m_fileMenu.add(openMenuItem);
	m_fileMenu.add(m_saveWithLayoutMenuItem);
	m_fileMenu.add(m_saveWithLayoutAsMenuItem);
	m_fileMenu.add(m_closeMenuItem);
	m_fileMenu.addSeparator();
	m_fileMenu.add(preferencesMenuItem);
	m_fileMenu.addSeparator();
	m_fileMenu.add(exitMenuItem);

	// Create "Rendering" menu. ----------------------------------------

	m_startMenuItem = new JMenuItem("Start");
	m_startMenuItem.setEnabled(false);
	m_startMenuItem.setMnemonic(KeyEvent.VK_S);
	m_startMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    handleStartRenderingRequest();
		}
	    });

	m_stopMenuItem = new JMenuItem("Stop");
	m_stopMenuItem.setMnemonic(KeyEvent.VK_P);
	m_stopMenuItem.setEnabled(false);
	m_stopMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    handleStopRenderingRequest();
		}
	    });

	m_updateMenuItem = new JMenuItem("Update");
	m_updateMenuItem.setMnemonic(KeyEvent.VK_U);
	m_updateMenuItem.setEnabled(false);
	m_updateMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    handleUpdateRenderingRequest();
		}
	    });

	m_refreshMenuItem = new JMenuItem("Refresh Display");
	m_refreshMenuItem.setMnemonic(KeyEvent.VK_R);
	m_refreshMenuItem.setEnabled(false);
	m_refreshMenuItem.setAccelerator
	    (KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
	m_refreshMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    if (m_renderLoop != null)
		    {
			m_renderLoop.refreshDisplay();
		    }
		}
	    });

	m_adaptiveMenuItem = new JCheckBoxMenuItem("Adaptive Rendering");
	m_adaptiveMenuItem.setMnemonic(KeyEvent.VK_A);
	m_adaptiveMenuItem.setSelected(true);
	m_adaptiveMenuItem.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e)
		{
		    boolean enable =(e.getStateChange() == ItemEvent.SELECTED);
		    m_multipleNodeSizesMenuItem.setEnabled(enable);
		}
	    });

	m_multipleNodeSizesMenuItem =
	    new JCheckBoxMenuItem("Multiple Node Sizes");
	m_multipleNodeSizesMenuItem.setMnemonic(KeyEvent.VK_M);
	m_multipleNodeSizesMenuItem.setSelected(true);

	m_depthCueingMenuItem =
	    new JCheckBoxMenuItem("Depth Cueing");
	m_depthCueingMenuItem.setMnemonic(KeyEvent.VK_D);
	m_depthCueingMenuItem.setSelected(true);

	m_axesMenuItem =
	    new JCheckBoxMenuItem("Coordinate Axes");
	m_axesMenuItem.setMnemonic(KeyEvent.VK_X);
	m_axesMenuItem.setSelected(true);

	m_screenCaptureMenuItem
	    = new JCheckBoxMenuItem("Screen Capture Support");
	m_screenCaptureMenuItem.setMnemonic(KeyEvent.VK_C);
	m_screenCaptureMenuItem.setSelected(false);

	m_automaticRefreshMenuItem
	    = new JCheckBoxMenuItem("Automatic Refresh");
	m_automaticRefreshMenuItem.setMnemonic(KeyEvent.VK_F);
	m_automaticRefreshMenuItem.setSelected(true);

	m_renderingMenu = new JMenu("Rendering");
	m_renderingMenu.setMnemonic(KeyEvent.VK_R);
	m_renderingMenu.add(m_startMenuItem);
	m_renderingMenu.add(m_stopMenuItem);
	m_renderingMenu.add(m_updateMenuItem);
	m_renderingMenu.addSeparator();
	m_renderingMenu.add(m_refreshMenuItem);
	m_renderingMenu.addSeparator();
	m_renderingMenu.add(m_adaptiveMenuItem);
	m_renderingMenu.add(m_multipleNodeSizesMenuItem);
	m_renderingMenu.add(m_depthCueingMenuItem);
	m_renderingMenu.add(m_axesMenuItem);
	m_renderingMenu.add(m_screenCaptureMenuItem);
	m_renderingMenu.add(m_automaticRefreshMenuItem);

	// Create "Spanning Tree" menu. ------------------------------------

	m_spanningTreeMenu = new JMenu("Spanning Tree");
	m_spanningTreeMenu.setMnemonic(KeyEvent.VK_S);

	// Create "Color Scheme" menu. -------------------------------------

	m_colorSchemeMenu = new ColorSchemeMenu();

	// Create "Node Label" menu. -------------------------------------

	m_nodeLabelMenu = new JMenu("Node Label");
	m_nodeLabelMenu.setMnemonic(KeyEvent.VK_N);

	// Create menu bar. ------------------------------------------------

	JMenuBar retval = new JMenuBar();
	retval.add(m_fileMenu);
	retval.add(m_renderingMenu);
	retval.add(m_spanningTreeMenu);
	retval.add(m_colorSchemeMenu.getColorSchemeMenu());
	retval.add(m_nodeLabelMenu);
	return retval;
    }

    ///////////////////////////////////////////////////////////////////////

    private int countNumSelectedItems(JMenu menu)
    {
	int retval = 0;
	int numItems = menu.getItemCount();
	for (int i = 0; i < numItems; i++)
	{
	    JMenuItem menuItem = menu.getItem(i);
	    if (menuItem != null && menuItem.isSelected())
	    {
		++retval;
	    }
	}
	return retval;
    }

    ///////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ///////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT_LOAD_TIME = true;
    private static final boolean DEBUG_PRINT_LOAD_MEMORY = true;
    private static final boolean DEBUG_CHECK_ID_MAPPINGS = true;

    private static final int DEFAULT_FRAME_WIDTH = 900;
    private static final int DEFAULT_FRAME_HEIGHT = 1000;

    private static final String WALRUS_TITLE = "Walrus";
    private static final String SPLASH_ICON_PATH = "walrus-splash.jpg";
    private static final String MSG_NO_GRAPH_LOADED = "No graph loaded.";
    private static final String MSG_GRAPH_LOADED = "Graph loaded.";
    private static final String MSG_LOADING_GRAPH = "Loading graph...";
    private static final String SPLASH_HTML_LABEL = "<html><table border=0><tr><td align=center><b><i><font color=#CAFF70 size=+4>Walrus 0.1 -- Graph Visualization</font></i></b></td></tr><tr><td align=center><font color=#1E9619 size=+2>Copyright 2001 CAIDA/UCSD</font></td></tr></table></html>";

    ///////////////////////////////////////////////////////////////////////

    // The following, m_renderingConfiguration, will be non-null if a graph
    // has been loaded and rendered (at least once).
    private RenderingConfiguration m_renderingConfiguration;
    private Graph m_backingGraph;  // Will be non-null if a graph is open.
    private H3Graph m_graph;  // ...non-null when a graph is being rendered.
    private H3DisplayPosition m_displayPosition;
    private H3Canvas3D m_canvas; // Always non-null; one per program run.
    private H3ViewParameters m_viewParameters; // Always non-null.
    private H3RenderLoop m_renderLoop; // ...non-null when ... being rendered.
    private EventHandler m_eventHandler; // ...non-null when ...being rendered.
    private MemoryUsage m_memoryUsage = new MemoryUsage();
    private H3GraphLoader m_graphLoader = new H3GraphLoader();

    private JFrame m_frame;
    private JTextField m_statusBar;
    private JLabel m_splashLabel;
    private JFileChooser m_fileChooser = new JFileChooser();

    private JMenu m_fileMenu;
    private JMenuItem m_saveWithLayoutMenuItem;
    private JMenuItem m_saveWithLayoutAsMenuItem;
    private JMenuItem m_closeMenuItem;

    private JMenu m_renderingMenu;
    private JMenuItem m_startMenuItem;
    private JMenuItem m_stopMenuItem;
    private JMenuItem m_updateMenuItem;
    private JMenuItem m_refreshMenuItem;
    private JCheckBoxMenuItem m_adaptiveMenuItem;
    private JCheckBoxMenuItem m_multipleNodeSizesMenuItem;
    private JCheckBoxMenuItem m_depthCueingMenuItem;
    private JCheckBoxMenuItem m_axesMenuItem;
    private JCheckBoxMenuItem m_screenCaptureMenuItem;
    private JCheckBoxMenuItem m_automaticRefreshMenuItem;

    private JMenu m_spanningTreeMenu;
    private ButtonGroup m_spanningTreeButtonGroup;
    private List m_spanningTreeQualifiers;

    private ColorSchemeMenu m_colorSchemeMenu;

    private JMenu m_nodeLabelMenu;
    private List m_nodeLabelAttributes;

    private H3GraphLoader.AttributeTypeMatcher
	m_allAttributeTypeMatcher = new AllAttributeTypeMatcher();

    private float[] m_float3Temporary = new float[3];
    private double[] m_double3Temporary = new double[3];

    ///////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES
    ///////////////////////////////////////////////////////////////////////

    private static class EventHandler
	implements KeyListener, MouseListener, MouseMotionListener
    {
	public EventHandler
	    (H3Canvas3D canvas, H3RenderLoop renderLoop,
	     CapturingManager manager, int rootNode,
	     H3Graph graph, Graph backingGraph,
	     int[] nodeLabelAttributes, JTextField statusBar,
	     boolean automaticRefresh)
	{
	    m_canvas = canvas;
	    m_canvas.addKeyListener(this);
	    m_canvas.addMouseListener(this);
	    m_canvas.addMouseMotionListener(this);

	    // This is a tradeoff between seeing flicker and having to
	    // manually refresh the display in some cases.  There's some
	    // problem deep in Java3D which makes a satisfactory solution
	    // impossible.
	    m_automaticRefresh = automaticRefresh;
	    if (automaticRefresh)
	    {
		m_canvas.addPaintObserver(m_paintObserver);
	    }
	    else
	    {
		// Resize events are generated during the initial layout
		// and when the enclosing frame is resized.
		m_canvas.addComponentListener(m_resizeListener);
	    }

	    m_renderLoop = new H3CapturingRenderLoop(renderLoop);
	    m_capturingManager = manager;
	    m_rootNode = m_currentNode = m_previousNode = rootNode;

	    m_graph = graph;
	    m_backingGraph = backingGraph;
	    m_nodeLabelAttributes = nodeLabelAttributes;
	    m_statusBar = statusBar;
	}

	public void dispose()
	{
	    m_canvas.removeKeyListener(this);
	    m_canvas.removeMouseListener(this);
	    m_canvas.removeMouseMotionListener(this);

	    if (m_automaticRefresh)
	    {
		m_canvas.removePaintObserver(m_paintObserver);
	    }
	    else
	    {
		m_canvas.removeComponentListener(m_resizeListener);
	    }
	}

	// MouseListener - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e)
	{
	    System.out.println("mousePressed");

	    int modifiers = e.getModifiers();
	    switch (m_state)
	    {
	    case STATE_IDLE:
		if (checkModifiers(modifiers, InputEvent.BUTTON1_MASK))
		{
		    if (checkModifiers(modifiers, InputEvent.SHIFT_MASK))
		    {
			// Continuous rotations...
		    }
		    else
		    {
			// Interactive rotation...
		    }
		}
		else if (checkModifiers(modifiers, InputEvent.BUTTON2_MASK))
		{
		    // Attribute display...
		}
		else if (checkModifiers(modifiers, InputEvent.BUTTON3_MASK))
		{
		    if (checkModifiers(modifiers, InputEvent.SHIFT_MASK))
		    {
			// Attribute display...
		    }
		    else
		    {
			// Picking for translation...
		    }
		}
		break;

	    case STATE_WOBBLING:
		// Stop wobbling...
		break;

	    case STATE_REPLAYING:
		// Cancel replay...
		break;

	    case STATE_ROTATING_CONTINUOUS:
		// Stop rotations...
		break;

	    case STATE_DISPLAYING_ATTRIBUTES:
		//FALLTHROUGH
	    case STATE_ROTATING_INTERACTIVE:
		//FALLTHROUGH
	    default:
		throw new RuntimeException
		    ("Invalid state in EventHandler: mousePressed in state "
		     + m_state);
	    }
	}

	public void mouseReleased(MouseEvent e)
	{
	    System.out.println("mouseReleased");

	    int modifiers = e.getModifiers();
	    switch (m_state)
	    {
	    case STATE_IDLE:
		//IGNORE
		break;

	    case STATE_DISPLAYING_ATTRIBUTES:
		// Stop displaying...
		break;

	    case STATE_ROTATING_INTERACTIVE:
		// Stop rotating...
		break;

	    case STATE_ROTATING_CONTINUOUS:
		//IGNORE
		break;

	    case STATE_WOBBLING:
		//FALLTHROUGH
	    case STATE_REPLAYING:
		//FALLTHROUGH
	    default:
		throw new RuntimeException
		    ("Invalid state in EventHandler: mouseReleased in state "
		     + m_state);
	    }
	}

	// MouseMotionListener  - - - - - - - - - - - - - - - - - - - - - - -

	public void mouseDragged(MouseEvent e)
	{
	    System.out.println("mouseDragged");

	    switch (m_state)
	    {
	    case STATE_IDLE:
		//IGNORE
		break;

	    case STATE_DISPLAYING_ATTRIBUTES:
		// Display attributes...
		break;

	    case STATE_ROTATING_INTERACTIVE:
		// Rotate display...
		break;

	    case STATE_ROTATING_CONTINUOUS:
		// Adjust rotation parameters...
		break;

	    case STATE_WOBBLING:
		//FALLTHROUGH
	    case STATE_REPLAYING:
		//FALLTHROUGH
	    default:
		throw new RuntimeException
		    ("Invalid state in EventHandler: mouseDragged in state "
		     + m_state);
	    }
	}

	public void mouseMoved(MouseEvent e) {}

	// KeyListener - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}

	// Supported key events:
	//
	//  CTRL-R: refresh display
	public void keyTyped(KeyEvent e)
	{
	    if (e.isControlDown())
	    {
		// KeyEvent.getKeyChar() does not quite return what you would
		// expect if the cause of the event was a user's typing a
		// letter while holding down the control key.  What you would
		// expect is KeyEvent.getKeyChar()'s returning the UNICODE
		// encoding of the letter--e.g., 114 for the letter 'r'.
		// What you get instead is the encoding of the character
		// represented by the control sequence.  That is, you would
		// get '\r' for CTRL-M, for example.

		char c = e.getKeyChar();
		if (c == CTRL_R)
		{
		    if (!m_isRotating)
		    {
			m_renderLoop.refreshDisplay();
		    }
		}
	    }
	}

	//---------------------------------------------------------------

	private void highlightNode(int x, int y)
	{
	    //System.out.println("Highlighting ...");
	    m_renderLoop.highlightNode(x, y);
	    if (m_nodeLabelAttributes.length > 0)
	    {
		int node = m_renderLoop.pickNode(x, y, m_center);
		if (node >= 0)
		{
		    showNodeLabels(node);
		}
	    }
	}

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

	private void showNodeLabels(int node)
	{
	    StringBuffer buffer = new StringBuffer();

	    for (int i = 0; i < m_nodeLabelAttributes.length; i++)
	    {
		if (i > 0)
		{
		    buffer.append("  ");
		}

		int attribute = m_nodeLabelAttributes[i];
		AttributeDefinitionIterator iterator =
		    m_backingGraph.getAttributeDefinition(attribute);
		buffer.append(iterator.getName());
		buffer.append(": ");

		ValueType type = iterator.getType();
		if (type.isListType())
		{
		    buffer.append("<<skipped>>"); // XXX Implement this.
		}
		else
		{
		    int nodeID = m_graph.getNodeID(node);
		    addAttributeValue(buffer, type, nodeID, attribute);
		}
	    }

	    m_statusBar.setText(buffer.toString());
	}

	// The parameter {node} should be the ID of a node in the libsea Graph.
	private void addAttributeValue(StringBuffer buffer, ValueType type,
				       int node, int attribute)
	{
	    try
	    {
		switch (type.getBaseType())
		{
		case ValueType._BOOLEAN:
		    {
			boolean value = m_backingGraph.getBooleanAttribute
			    (ObjectType.NODE, node, attribute);
			buffer.append(value ? 'T' : 'F');
		    }
		    break;

		case ValueType._INTEGER:
		    {
			int value = m_backingGraph.getIntegerAttribute
			    (ObjectType.NODE, node, attribute);
			buffer.append(value);
		    }
		    break;

		case ValueType._FLOAT:
		    {
			float value = m_backingGraph.getFloatAttribute
			    (ObjectType.NODE, node, attribute);
			buffer.append(value);
		    }
		    break;

		case ValueType._DOUBLE:
		    {
			double value = m_backingGraph.getDoubleAttribute
			    (ObjectType.NODE, node, attribute);
			buffer.append(value);
		    }
		    break;

		case ValueType._STRING:
		    {
			String value = m_backingGraph.getStringAttribute
			    (ObjectType.NODE, node, attribute);
			buffer.append('"');
			buffer.append(value);
			buffer.append('"');
		    }
		    break;

		case ValueType._FLOAT3:
		    m_backingGraph.getFloat3Attribute
			(ObjectType.NODE, node, attribute, m_float3LabelData);
		    buffer.append('{');
		    buffer.append(m_float3LabelData[0]);
		    buffer.append(", ");
		    buffer.append(m_float3LabelData[1]);
		    buffer.append(", ");
		    buffer.append(m_float3LabelData[2]);
		    buffer.append('}');
		    break;

		case ValueType._DOUBLE3:
		    m_backingGraph.getDouble3Attribute
			(ObjectType.NODE, node, attribute, m_double3LabelData);
		    buffer.append('{');
		    buffer.append(m_double3LabelData[0]);
		    buffer.append(", ");
		    buffer.append(m_double3LabelData[1]);
		    buffer.append(", ");
		    buffer.append(m_double3LabelData[2]);
		    buffer.append('}');
		    break;

		case ValueType._ENUMERATION:
		    {
			int value = m_backingGraph.getEnumerationAttribute
			    (ObjectType.NODE, node, attribute);
			ReadOnlyEnumeratorIterator iterator =
			    m_backingGraph.getEnumerator(value);
			buffer.append(iterator.getName());
		    }
		    break;

		default: throw new RuntimeException();
		}
	    }
	    catch (AttributeUnavailableException e)
	    {
		buffer.append("<<unavailable>>");
	    }
	}

	//---------------------------------------------------------------

	private static final int STATE_IDLE = 0;
	private static final int STATE_DISPLAYING_ATTRIBUTES = 1;
	private static final int STATE_ROTATING_INTERACTIVE = 2;
	private static final int STATE_ROTATING_CONTINUOUS = 3;
	private static final int STATE_WOBBLING = 4;
	private static final int STATE_REPLAYING = 5;

	private static final char CTRL_R = 'r' - 'a' + 1;

	private static final int MOUSE_SENSITIVITY = 2;

	private int m_state;
	private H3Canvas3D m_canvas;
	private H3CapturingRenderLoop m_renderLoop;
	private CapturingManager m_capturingManager;

	private int m_rootNode;
	private int m_currentNode;
	private int m_previousNode;

	private H3Graph m_graph;
	private Graph m_backingGraph;
	private int[] m_nodeLabelAttributes;
	private JTextField m_statusBar;
	private float[] m_float3LabelData = new float[3];
	private double[] m_double3LabelData = new double[3];

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

	private boolean m_automaticRefresh;
	private PaintObserver m_paintObserver = new PaintObserver();
	private ComponentResizeListener m_resizeListener =
	    new ComponentResizeListener();

	////////////////////////////////////////////////////////////////////

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

	////////////////////////////////////////////////////////////////////

	private class ComponentResizeListener
	    extends ComponentAdapter
	{
	    public void componentResized(ComponentEvent e)
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

    ///////////////////////////////////////////////////////////////////////

    private static class ColorSchemeMenu
    {
	////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	////////////////////////////////////////////////////////////////////

	public ColorSchemeMenu()
	{
	    createFixedColors();

	    Map nodeMenuMap = new HashMap();
	    m_nodeColorMenu = new JMenu("Node Color");
	    m_nodeColorMenu.setMnemonic(KeyEvent.VK_N);
	    m_nodeColorSelection = new ColorSelection
		(m_nodeColorMenu, nodeMenuMap, m_fixedColors);

	    Map treeLinkMenuMap = new HashMap();
	    m_treeLinkColorMenu = new JMenu("Tree Link Color");
	    m_treeLinkColorMenu.setMnemonic(KeyEvent.VK_T);
	    m_treeLinkColorSelection = new ColorSelection
		(m_treeLinkColorMenu, treeLinkMenuMap, m_fixedColors);

	    Map nontreeLinkMenuMap = new HashMap();
	    m_nontreeLinkColorMenu = new JMenu("Nontree Link Color");
	    m_nontreeLinkColorMenu.setMnemonic(KeyEvent.VK_E);
	    m_nontreeLinkColorSelection = new ColorSelection
		(m_nontreeLinkColorMenu, nontreeLinkMenuMap, m_fixedColors);

	    createPredefinedColorSchemes
		(nodeMenuMap, treeLinkMenuMap, nontreeLinkMenuMap);
	    {
		m_predefinedColorSchemeMenu = new JMenu("Predefined");
		m_predefinedColorSchemeMenu.setMnemonic(KeyEvent.VK_P);
		ListIterator iterator =
		    m_predefinedColorSchemes.listIterator();
		while (iterator.hasNext())
		{
		    PredefinedColorScheme scheme =
			(PredefinedColorScheme)iterator.next();
		    JMenuItem menuItem = new JMenuItem(scheme.name);
		    menuItem.addActionListener(scheme);
		    m_predefinedColorSchemeMenu.add(menuItem);
		}
	    }

	    m_colorSchemeMenu = new JMenu("Color Scheme");
	    m_colorSchemeMenu.setMnemonic(KeyEvent.VK_C);
	    m_colorSchemeMenu.add(m_predefinedColorSchemeMenu);
	    m_colorSchemeMenu.addSeparator();
	    m_colorSchemeMenu.add(m_nodeColorMenu);
	    m_colorSchemeMenu.add(m_treeLinkColorMenu);
	    m_colorSchemeMenu.add(m_nontreeLinkColorMenu);
	}

	////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	////////////////////////////////////////////////////////////////////

	public JMenu getColorSchemeMenu()
	{
	    return m_colorSchemeMenu;
	}

	public void populateAttributeMenus(H3GraphLoader graphLoader,
					   Graph graph)
	{
	    m_attributeCache = new AttributeCache(graphLoader, graph);
	    m_nodeColorSelection.populateAttributeMenus(m_attributeCache);
	    m_treeLinkColorSelection.populateAttributeMenus(m_attributeCache);
	    m_nontreeLinkColorSelection.populateAttributeMenus
		(m_attributeCache);
	}

	public void removeAttributeMenus()
	{
	    m_attributeCache = null;
	    m_nodeColorSelection.removeAttributeMenus();
	    m_treeLinkColorSelection.removeAttributeMenus();
	    m_nontreeLinkColorSelection.removeAttributeMenus();
	}

	// NOTE: Call this only after the menus are completely constructed
	//       in this class (and aggregated classes).
	public void enableDefaultColorScheme()
	{
	    if (m_predefinedColorSchemes.size() > 0)
	    {
		// XXX: Preserve the default scheme in properties.
		PredefinedColorScheme scheme =
		    (PredefinedColorScheme)m_predefinedColorSchemes.get(0);
		m_nodeColorSelection.enableDefaultSelection
		    (scheme.nodeMenuItem);
		m_treeLinkColorSelection.enableDefaultSelection
		    (scheme.treeLinkMenuItem);
		m_nontreeLinkColorSelection.enableDefaultSelection
		    (scheme.nontreeLinkMenuItem);
	    }
	}

	// NOTE: Call this only after the menus are completely constructed
	//       in this class (and aggregated classes).
	public void enableReasonableColorScheme()
	{
	    m_nodeColorSelection.enableReasonableSelection();
	    m_treeLinkColorSelection.enableReasonableSelection();
	    m_nontreeLinkColorSelection.enableReasonableSelection();
	}

	public ColorConfiguration createNodeColorConfigurationSnapshot()
	{
	    return m_nodeColorSelection.createColorConfigurationSnapshot();
	}

	public ColorConfiguration createTreeLinkColorConfigurationSnapshot()
	{
	    return m_treeLinkColorSelection.createColorConfigurationSnapshot();
	}

	public ColorConfiguration createNontreeLinkColorConfigurationSnapshot()
	{
	    return m_nontreeLinkColorSelection
		.createColorConfigurationSnapshot();
	}

	////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	////////////////////////////////////////////////////////////////////

	private void createPredefinedColorSchemes
	    (Map nodeMenuMap, Map treeLinkMenuMap, Map nontreeLinkMenuMap)
	{
	    ColorSchemeMaker maker = new ColorSchemeMaker
		(nodeMenuMap, treeLinkMenuMap, nontreeLinkMenuMap);
	    m_predefinedColorSchemes = new ArrayList();
	    m_predefinedColorSchemes.add
		(maker.make("Yellow-Green", "Yellow", "Green", "Green"));
	    m_predefinedColorSchemes.add
		(maker.make("Yellow-Green/Invisible", "Yellow", "Green",
			    ColorSelection.INVISIBLE));
	    m_predefinedColorSchemes.add
		(maker.make("Yellow-Green/Transparent", "Yellow", "Green",
			    ColorSelection.TRANSPARENT));
	    m_predefinedColorSchemes.add
		(maker.make("Yellow-Violet", "Yellow", "Violet", "Violet"));
	    m_predefinedColorSchemes.add
		(maker.make("Grey-Violet", "Grey", "Violet", "Violet"));
	    m_predefinedColorSchemes.add
		(maker.make("Gold-Violet/Transparent", "Gold", "Violet",
			    ColorSelection.TRANSPARENT));
	    m_predefinedColorSchemes.add
		(maker.make("Orange-Gold/Transparent", "Orange", "Gold",
			    ColorSelection.TRANSPARENT));
	    m_predefinedColorSchemes.add
		(maker.make("Snow-Gold/Transparent", "Snow", "Gold",
			    ColorSelection.TRANSPARENT));
	    m_predefinedColorSchemes.add
		(maker.make("Violet-Gold/Transparent", "Violet", "Gold",
			    ColorSelection.TRANSPARENT));
	}

	private void createFixedColors()
	{
	    m_fixedColors = new ArrayList();
	    m_fixedColors.add
		(new FixedColor("Yellow", packRGB(255, 255, 0)));
	    m_fixedColors.add
		(new FixedColor("Green", packRGB(30, 150, 25)));
	    m_fixedColors.add
		(new FixedColor("Grey", packRGB(178, 178, 178)));
	    m_fixedColors.add
		(new FixedColor("Pink", packRGB(255, 48, 48)));
	    m_fixedColors.add
		(new FixedColor("Beige", packRGB(255, 246, 143)));
	    m_fixedColors.add
		(new FixedColor("Violet", packRGB(199, 21, 133)));
	    m_fixedColors.add
		(new FixedColor("Olive Green", packRGB(202, 255, 112)));
	    m_fixedColors.add
		(new FixedColor("Orange", packRGB(255, 140, 0)));
	    m_fixedColors.add
		(new FixedColor("Snow", packRGB(255, 225, 255)));
	    m_fixedColors.add
		(new FixedColor("Gold", packRGB(255, 215, 0)));
	    m_fixedColors.add
		(new FixedColor("Red", packRGB(255, 0, 0)));
	}

	private int packRGB(int r, int g, int b)
	{
	    return (r << 16) | (g << 8) | b;
	}

	////////////////////////////////////////////////////////////////////
	// PRIVATE FIELDS
	////////////////////////////////////////////////////////////////////

	private JMenu m_colorSchemeMenu;
	private JMenu m_nodeColorMenu;
	private JMenu m_treeLinkColorMenu;
	private JMenu m_nontreeLinkColorMenu;
	private JMenu m_predefinedColorSchemeMenu;

	private ColorSelection m_nodeColorSelection;
	private ColorSelection m_treeLinkColorSelection;
	private ColorSelection m_nontreeLinkColorSelection;
	private List m_fixedColors; // ArrayList<FixedColor>
	// ArrayList<PredefinedColorScheme>
	private List m_predefinedColorSchemes;

	private AttributeCache m_attributeCache;

	////////////////////////////////////////////////////////////////////
	// PRIVATE INNER CLASSES
	////////////////////////////////////////////////////////////////////

	private class PredefinedColorScheme
	    implements ActionListener
	{
	    public PredefinedColorScheme(String name, JMenuItem nodeMenuItem,
					 JMenuItem treeLinkMenuItem,
					 JMenuItem nontreeLinkMenuItem)
	    {
		this.name = name;
		this.nodeMenuItem = nodeMenuItem;
		this.treeLinkMenuItem = treeLinkMenuItem;
		this.nontreeLinkMenuItem = nontreeLinkMenuItem;
	    }

	    public void actionPerformed(ActionEvent e)
	    {
		nodeMenuItem.doClick();
		treeLinkMenuItem.doClick();
		nontreeLinkMenuItem.doClick();
	    }

	    public String name;
	    public JMenuItem nodeMenuItem;
	    public JMenuItem treeLinkMenuItem;
	    public JMenuItem nontreeLinkMenuItem;
	}

	////////////////////////////////////////////////////////////////////

	private class ColorSchemeMaker
	{
	    public ColorSchemeMaker
		(Map nodeMenuMap, Map treeLinkMenuMap, Map nontreeLinkMenuMap)
	    {
		m_nodeMenuMap = nodeMenuMap;
		m_treeLinkMenuMap = treeLinkMenuMap;
		m_nontreeLinkMenuMap = nontreeLinkMenuMap;
	    }

	    public PredefinedColorScheme make
		(String name, String nodeColor, String treeLinkColor,
		 String nontreeLinkColor)
	    {
		JMenuItem nodeMenuItem = findColor(m_nodeMenuMap, nodeColor);
		JMenuItem treeLinkMenuItem =
		    findColor(m_treeLinkMenuMap, treeLinkColor);
		JMenuItem nontreeLinkMenuItem =
		    findColor(m_nontreeLinkMenuMap, nontreeLinkColor);

		return new PredefinedColorScheme
		    (name, nodeMenuItem, treeLinkMenuItem,
		     nontreeLinkMenuItem);
	    }

	    private JMenuItem findColor(Map map, String color)
	    {
		JMenuItem retval = (JMenuItem)map.get(color);
		if (retval == null)
		{
		    String msg = "INTERNAL ERROR: color[" + color
			+ "] not found";
		    throw new RuntimeException(msg);
		}
		return retval;
	    }

	    private Map m_nodeMenuMap;
	    private Map m_treeLinkMenuMap;
	    private Map m_nontreeLinkMenuMap;
	}
    }

    ////////////////////////////////////////////////////////////////////

    private static class FixedColor
    {
	public FixedColor(String name, int color)
	{
	    this.name = name;
	    this.color = color;
	}

	public String name;
	public int color;   // packed RGB
    }

    ///////////////////////////////////////////////////////////////////////

    private static class ColorSelection
    {
	private static final boolean COLOR_SCALES = false;

	////////////////////////////////////////////////////////////////////
	// PUBLIC CONSTANTS
	////////////////////////////////////////////////////////////////////

	public static final String INVISIBLE = "Invisible";
	public static final String TRANSPARENT = "Transparent";
	public static final String HOT_TO_COLD = "Hot to Cold";
	public static final String LOG_HOT_TO_COLD = "Logarithmic HtoC";
	public static final String HUE = "Hue";
	public static final String RGB = "RGB";

	////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	////////////////////////////////////////////////////////////////////

	// List<FixedColor> fixedColors
	public ColorSelection(JMenu menu, Map map, List fixedColors)
	{
	    m_invisibleMenuItem = new JRadioButtonMenuItem(INVISIBLE);
	    m_invisibleMenuItem.setMnemonic(KeyEvent.VK_I);
	    m_invisibleMenuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e)
		    {
			handleInvisibleColorRequest();
		    }
		});

	    m_transparentMenuItem = new JRadioButtonMenuItem(TRANSPARENT);
	    m_transparentMenuItem.setMnemonic(KeyEvent.VK_T);
	    m_transparentMenuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e)
		    {
			handleTransparentColorRequest();
		    }
		});

	    m_hotToColdMenuItem = new JRadioButtonMenuItem(HOT_TO_COLD);
	    m_hotToColdMenuItem.setMnemonic(KeyEvent.VK_H);
	    m_hotToColdMenuItem.setEnabled(false);
	    m_hotToColdMenuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e)
		    {
			handleHotToColdColorRequest();
		    }
		});

	    m_logHotToColdMenuItem = new JRadioButtonMenuItem(LOG_HOT_TO_COLD);
	    m_logHotToColdMenuItem.setMnemonic(KeyEvent.VK_L);
	    m_logHotToColdMenuItem.setEnabled(false);
	    m_logHotToColdMenuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e)
		    {
			handleLogHotToColdColorRequest();
		    }
		});

	    m_hueMenuItem = new JRadioButtonMenuItem(HUE);
	    m_hueMenuItem.setMnemonic(KeyEvent.VK_U);
	    m_hueMenuItem.setEnabled(false);
	    m_hueMenuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e)
		    {
			handleHueColorRequest();
		    }
		});

	    m_RGBMenuItem = new JRadioButtonMenuItem(RGB);
	    m_RGBMenuItem.setMnemonic(KeyEvent.VK_R);
	    m_RGBMenuItem.setEnabled(false);
	    m_RGBMenuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e)
		    {
			handleRGBColorRequest();
		    }
		});

	    m_colorAttributeMenu = new JMenu("Color Attribute");
	    m_colorAttributeMenu.setMnemonic(KeyEvent.VK_C);
	    m_colorAttributeMenu.setEnabled(false);

	    m_selectionAttributeMenu = new JMenu("Selection Attribute");
	    m_selectionAttributeMenu.setMnemonic(KeyEvent.VK_S);
	    m_selectionAttributeMenu.setEnabled(false);

	    m_colorSchemeButtonGroup = new ButtonGroup();
	    m_colorSchemeButtonGroup.add(m_invisibleMenuItem);
	    m_colorSchemeButtonGroup.add(m_transparentMenuItem);
	    if (COLOR_SCALES)
	    {
	    m_colorSchemeButtonGroup.add(m_hotToColdMenuItem);
	    m_colorSchemeButtonGroup.add(m_logHotToColdMenuItem);
	    m_colorSchemeButtonGroup.add(m_hueMenuItem);
	    }
	    m_colorSchemeButtonGroup.add(m_RGBMenuItem);

	    putChecked(map, INVISIBLE, m_invisibleMenuItem);
	    putChecked(map, TRANSPARENT, m_transparentMenuItem);
	    if (COLOR_SCALES)
	    {
	    putChecked(map, HOT_TO_COLD, m_hotToColdMenuItem);
	    putChecked(map, LOG_HOT_TO_COLD, m_logHotToColdMenuItem);
	    putChecked(map, HUE, m_hueMenuItem);
	    }
	    putChecked(map, RGB, m_RGBMenuItem);

	    menu.add(m_invisibleMenuItem);
	    menu.add(m_transparentMenuItem);
	    menu.addSeparator();
	    {
		int numFixedColors = fixedColors.size();
		m_fixedColors = new int[numFixedColors];
		m_fixedColorMenuItems =
		    new JRadioButtonMenuItem[numFixedColors];

		int index = 0;
		ListIterator iterator = fixedColors.listIterator();
		while (iterator.hasNext())
		{
		    FixedColor fixedColor = (FixedColor)iterator.next();
		    JRadioButtonMenuItem menuItem =
			new JRadioButtonMenuItem(fixedColor.name);
		    menuItem.addActionListener(new FixedColorListener(index));

		    putChecked(map, fixedColor.name, menuItem);
		    m_colorSchemeButtonGroup.add(menuItem);
		    menu.add(menuItem);

		    m_fixedColors[index] = fixedColor.color;
		    m_fixedColorMenuItems[index] = menuItem;

		    ++index;
		}
	    }
	    menu.addSeparator();
	    if (COLOR_SCALES)
	    {
	    menu.add(m_hotToColdMenuItem);
	    menu.add(m_logHotToColdMenuItem);
	    menu.add(m_hueMenuItem);
	    }
	    menu.add(m_RGBMenuItem);
	    menu.add(m_colorAttributeMenu);
	    menu.addSeparator();
	    menu.add(m_selectionAttributeMenu);

	    m_defaultSelection = m_invisibleMenuItem;
	    m_defaultSelection.setSelected(true);
	}

	////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	////////////////////////////////////////////////////////////////////

	public void populateAttributeMenus(AttributeCache attributeCache)
	{
	    removeColorAttributeMenu();
	    removeSelectionAttributeMenu();

	    m_scalarColorAttributeButtonGroup = new ButtonGroup();
	    m_scalarColorAttributeMenus = createColorAttributeMenuCache
		(attributeCache.getScalarColorAttributes(),
		 m_scalarColorAttributeButtonGroup);

	    m_RGBColorAttributeButtonGroup = new ButtonGroup();
	    m_RGBColorAttributeMenus = createColorAttributeMenuCache
		(attributeCache.getRGBColorAttributes(),
		 m_RGBColorAttributeButtonGroup);

	    populateSelectionAttributeMenu
		(attributeCache.getSelectionAttributes());
	    updateMenuInterdependencies();
	}

	public void removeAttributeMenus()
	{
	    removeColorAttributeMenu();
	    removeSelectionAttributeMenu();
	    updateMenuInterdependencies();
	}

	public void enableDefaultSelection(JMenuItem menuItem)
	{
	    m_defaultSelection = menuItem;
	    m_defaultSelection.setSelected(true);
	    updateSelectedFixedColorIndex(m_defaultSelection);
	    updateMenuInterdependencies();
	}

	public void enableReasonableSelection()
	{
	    if (m_hotToColdMenuItem.isSelected()
		|| m_logHotToColdMenuItem.isSelected()
		|| m_hueMenuItem.isSelected()
		|| m_RGBMenuItem.isSelected())
	    {
		m_defaultSelection.setSelected(true);
		updateSelectedFixedColorIndex(m_defaultSelection);
		updateMenuInterdependencies();
	    }
	}

	public ColorConfiguration createColorConfigurationSnapshot()
	{
	    ColorConfiguration retval = new ColorConfiguration();

	    if (m_invisibleMenuItem.isSelected())
	    {
		retval.scheme = ColorConfiguration.INVISIBLE;
	    }
	    else if (m_transparentMenuItem.isSelected())
	    {
		retval.scheme = ColorConfiguration.TRANSPARENT;
	    }
	    else if (m_hotToColdMenuItem.isSelected())
	    {
		retval.scheme = ColorConfiguration.HOT_TO_COLD;
		retval.colorAttribute =
		    findSelectedMenuItem(m_colorAttributeMenu).getText();
	    }
	    else if (m_logHotToColdMenuItem.isSelected())
	    {
		retval.scheme = ColorConfiguration.LOG_HOT_TO_COLD;
		retval.colorAttribute =
		    findSelectedMenuItem(m_colorAttributeMenu).getText();
	    }
	    else if (m_hueMenuItem.isSelected())
	    {
		retval.scheme = ColorConfiguration.HUE;
		retval.colorAttribute =
		    findSelectedMenuItem(m_colorAttributeMenu).getText();
	    }
	    else if (m_RGBMenuItem.isSelected())
	    {
		retval.scheme = ColorConfiguration.RGB;
		retval.colorAttribute =
		    findSelectedMenuItem(m_colorAttributeMenu).getText();
	    }
	    else
	    {
		retval.scheme = ColorConfiguration.FIXED_COLOR;
		retval.fixedColor = m_fixedColors[m_selectedFixedColorIndex];
	    }

	    if (m_selectionAttributeMenu.isEnabled())
	    {
		JMenuItem menuItem = 
		    findSelectedMenuItem(m_selectionAttributeMenu);
		// NOTE: One menu item is always selected.
		// The zeroth element in the menu stands for no-choice.
		if (menuItem != m_selectionAttributeMenu.getItem(0))
		{
		    retval.selectionAttribute = menuItem.getText();
		}
	    }

	    return retval;
	}

	////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	////////////////////////////////////////////////////////////////////

	private void handleInvisibleColorRequest()
	{
	    setupForMinimalColorChoice();
	}

	private void handleTransparentColorRequest()
	{
	    setupForMinimalColorChoice();
	}

	private void handleHotToColdColorRequest()
	{
	    installScalarAttributeMenu();
	    setupForArbitraryColorChoice();
	}

	private void handleLogHotToColdColorRequest()
	{
	    installScalarAttributeMenu();
	    setupForArbitraryColorChoice();
	}

	private void handleHueColorRequest()
	{
	    installScalarAttributeMenu();
	    setupForArbitraryColorChoice();
	}

	private void handleRGBColorRequest()
	{
	    installRGBAttributeMenu();
	    setupForArbitraryColorChoice();
	}

	private void handleFixedColorRequest(int index)
	{
	    setupForFixedColorChoice();
	    m_selectedFixedColorIndex = index;
	}

	private void setupForMinimalColorChoice()
	{
	    m_colorAttributeMenu.setEnabled(false);
	    m_selectionAttributeMenu.setEnabled(false);
	}

	private void setupForFixedColorChoice()
	{
	    m_colorAttributeMenu.setEnabled(false);
	    if (m_selectionAttributeMenu.getItemCount() > 0)
	    {
		m_selectionAttributeMenu.setEnabled(true);
	    }
	}

	private void setupForArbitraryColorChoice()
	{
	    m_colorAttributeMenu.setEnabled(true);
	    if (m_selectionAttributeMenu.getItemCount() > 0)
	    {
		m_selectionAttributeMenu.setEnabled(true);
	    }
	}

	// List<String> attributes
	private JRadioButtonMenuItem[] createColorAttributeMenuCache
	    (List attributes, ButtonGroup buttonGroup)
	{
	    JRadioButtonMenuItem[] retval = null;
	    int numAttributes = attributes.size();
	    if (numAttributes > 0)
	    {
		retval = new JRadioButtonMenuItem[numAttributes];

		int i = 0;
		ListIterator iterator = attributes.listIterator();
		while (iterator.hasNext())
		{
		    String name = (String)iterator.next();
		    JRadioButtonMenuItem menuItem =
			new JRadioButtonMenuItem(name);
		    retval[i++] = menuItem;
		    buttonGroup.add(menuItem);
		}

		retval[0].setSelected(true);
	    }
	    return retval;
	}

	// List<String> attributes
	private void populateSelectionAttributeMenu(List attributes)
	{
	    if (attributes.size() > 0)
	    {
		m_selectionAttributeButtonGroup = new ButtonGroup();

		JRadioButtonMenuItem noneMenuItem =
		    new JRadioButtonMenuItem("None");
		noneMenuItem.setSelected(true);
		m_selectionAttributeButtonGroup.add(noneMenuItem);
		m_selectionAttributeMenu.add(noneMenuItem);
		m_selectionAttributeMenu.addSeparator();

		ListIterator iterator = attributes.listIterator();
		while (iterator.hasNext())
		{
		    String name = (String)iterator.next();
		    JRadioButtonMenuItem menuItem =
			new JRadioButtonMenuItem(name);
		    m_selectionAttributeButtonGroup.add(menuItem);
		    m_selectionAttributeMenu.add(menuItem);
		}
	    }
	}

	private void removeColorAttributeMenu()
	{
	    m_colorAttributeMenu.removeAll();
	    m_scalarColorAttributeButtonGroup = null;
	    m_RGBColorAttributeButtonGroup = null;
	    m_scalarColorAttributeMenus = null;
	    m_RGBColorAttributeMenus = null;
	}

	private void removeSelectionAttributeMenu()
	{
	    m_selectionAttributeMenu.removeAll();
	    m_selectionAttributeButtonGroup = null;
	}

	private void updateMenuInterdependencies()
	{
	    if (m_scalarColorAttributeMenus == null
		&& m_RGBColorAttributeMenus == null)
	    {
		if (m_hotToColdMenuItem.isSelected()
		    || m_logHotToColdMenuItem.isSelected()
		    || m_hueMenuItem.isSelected()
		    || m_RGBMenuItem.isSelected())
		{
		    m_defaultSelection.setEnabled(true);
		}
		setColorAttributesRelatedEnabled(false, false);
		m_colorAttributeMenu.setEnabled(false);
	    }
	    else if (m_scalarColorAttributeMenus != null)
	    {
		// By necessity, we must have m_RGBColorAttributeMenus != null.

		if (m_hotToColdMenuItem.isSelected()
		    || m_logHotToColdMenuItem.isSelected()
		    || m_hueMenuItem.isSelected())
		{
		    installScalarAttributeMenu();
		    m_colorAttributeMenu.setEnabled(true);
		}
		else if (m_RGBMenuItem.isSelected())
		{
		    installRGBAttributeMenu();
		    m_colorAttributeMenu.setEnabled(true);
		}
		else
		{
		    m_colorAttributeMenu.setEnabled(false);
		}
		setColorAttributesRelatedEnabled(true, true);

	    }
	    else
	    {
		// We must have m_scalarColorAttributesMenus == null
		//               && m_RGBColorAttributeMenus != null.

		if (m_hotToColdMenuItem.isSelected()
		    || m_logHotToColdMenuItem.isSelected()
		    || m_hueMenuItem.isSelected())
		{
		    m_defaultSelection.setEnabled(true);
		    m_colorAttributeMenu.setEnabled(false);
		}
		else if (m_RGBMenuItem.isSelected())
		{
		    installRGBAttributeMenu();
		    m_colorAttributeMenu.setEnabled(true);
		}	
		else
		{
		    m_colorAttributeMenu.setEnabled(false);
		}
		setColorAttributesRelatedEnabled(false, true);
	    }

	    boolean enableSelection = 
		m_selectionAttributeMenu.getItemCount() > 0
		&& !m_invisibleMenuItem.isSelected()
		&& !m_transparentMenuItem.isSelected();
	    m_selectionAttributeMenu.setEnabled(enableSelection);
	}

	// NOTE: The caller should enable m_colorAttributeMenu.
	private void installScalarAttributeMenu()
	{
	    m_colorAttributeMenu.removeAll();
	    for (int i = 0; i < m_scalarColorAttributeMenus.length; i++)
	    {
		m_colorAttributeMenu.add(m_scalarColorAttributeMenus[i]);
	    }
	}

	// NOTE: The caller should enable m_colorAttributeMenu.
	private void installRGBAttributeMenu()
	{
	    m_colorAttributeMenu.removeAll();
	    for (int i = 0; i < m_RGBColorAttributeMenus.length; i++)
	    {
		m_colorAttributeMenu.add(m_RGBColorAttributeMenus[i]);
	    }
	}

	private void setColorAttributesRelatedEnabled(boolean scalar,
						      boolean RGB)
	{
	    m_hotToColdMenuItem.setEnabled(scalar);
	    m_logHotToColdMenuItem.setEnabled(scalar);
	    m_hueMenuItem.setEnabled(scalar);
	    m_RGBMenuItem.setEnabled(RGB);
	}

	/**
	 * Update <code>m_selectedFixedColorIndex</code> if the given
	 * selected menu item is that for a fixed color.
	 *
	 * <p>
	 * Usually, this value is automatically updated by an action
	 * listener added to the menu item representing a fixed
	 * color.  That works well in general, but during program
	 * initialization, when we want to setup the default color scheme,
	 * we need a different mechanism.  Even during initialization, we
	 * could conceivably call <code>JMenuItem.doClick()</code>
	 * (which is inherited from <code>AbstractButton</code>) and
	 * simply take advantage of the mechanism involving action listeners.
	 * But despite its attractiveness at first glance, this approach
	 * is not really tenable, owing to the assumptions that an action
	 * listener may make about which parts of the program are fully
	 * initialized.  That is to say, though it could work in many cases,
	 * it could also be the source of subtle bugs.  So it seems much
	 * better to be cautious and have this other mechanism for keeping
	 * <code>m_selectedFixedColorIndex</code> up-to-date in special
	 * circumstances.
	 * </p>
	 */
	private void updateSelectedFixedColorIndex(JMenuItem selectedMenuItem)
	{
	    for (int i = 0; i < m_fixedColorMenuItems.length; i++)
	    {
		if (selectedMenuItem == m_fixedColorMenuItems[i])
		{
		    m_selectedFixedColorIndex = i;
		    break;
		}
	    }
	}

	private JMenuItem findSelectedMenuItem(JMenu menu)
	{
	    JMenuItem retval = null;

	    int numItems = menu.getItemCount();
	    for (int i = 0; i < numItems && retval == null; i++)
	    {
		JMenuItem menuItem = menu.getItem(i);
		// NOTE: getItem() returns null for menu separators.
		if (menuItem != null && menuItem.isSelected())
		{
		    retval = menuItem;
		}
	    }

	    return retval;
	}

	private void putChecked(Map map, String name, Object data)
	{
	    if (map.put(name, data) != null)
	    {
		String msg = "INTERNAL ERROR: mapping already exists for `"
		    + name + "'";
		throw new RuntimeException(msg);
	    }
	}

	////////////////////////////////////////////////////////////////////
	// PRIVATE FIELDS
	////////////////////////////////////////////////////////////////////

	private ButtonGroup m_colorSchemeButtonGroup;
	private JRadioButtonMenuItem m_invisibleMenuItem;
	private JRadioButtonMenuItem m_transparentMenuItem;
	private JRadioButtonMenuItem m_hotToColdMenuItem;
	private JRadioButtonMenuItem m_logHotToColdMenuItem;
	private JRadioButtonMenuItem m_hueMenuItem;
	private JRadioButtonMenuItem m_RGBMenuItem;
	private JMenu m_colorAttributeMenu;
	private JMenu m_selectionAttributeMenu;
	private ButtonGroup m_selectionAttributeButtonGroup;

	// This is the menu item which will be in the selected state when
	// no menu item has yet been selected by the user or when the
	// currently selected menu item must be disabled (as a result of
	// removing all color attributes, for example).  This must point
	// to a menu item for a fixed color or to one of the invisible
	// or transparent choices.  By doing so, we ensure that this menu
	// item is selectable in all situations.
	private JMenuItem m_defaultSelection;

	private ButtonGroup m_scalarColorAttributeButtonGroup;
	private ButtonGroup m_RGBColorAttributeButtonGroup;
	private JRadioButtonMenuItem[] m_scalarColorAttributeMenus;
	private JRadioButtonMenuItem[] m_RGBColorAttributeMenus;

	private int[] m_fixedColors; // packed RGB
	private JRadioButtonMenuItem[] m_fixedColorMenuItems;
	private int m_selectedFixedColorIndex;

	////////////////////////////////////////////////////////////////////
	// PRIVATE INNER CLASSES
	////////////////////////////////////////////////////////////////////

	private class FixedColorListener
	    implements ActionListener
	{
	    public FixedColorListener(int index)
	    {
		m_index = index;
	    }

	    public void actionPerformed(ActionEvent e)
	    {
		handleFixedColorRequest(m_index);
	    }

	    private int m_index;
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private static class RenderingConfiguration
    {
	public String spanningTree;
	public boolean adaptiveRendering;
	public boolean multipleNodeSizes;
	public boolean depthCueing;
	public boolean axes;
	public boolean supportScreenCapture;
	public boolean automaticRefresh;

	public ColorConfiguration nodeColor;
	public ColorConfiguration treeLinkColor;
	public ColorConfiguration nontreeLinkColor;

	public int[] nodeLabelAttributes;

	public void print()
	{
	    System.out.println("------------------------------------------\n");
	    System.out.println("RenderingConfiguration:");
	    System.out.println("\tspanningTree = " + spanningTree);
	    System.out.println("\tadaptiveRendering = " + adaptiveRendering);
	    System.out.println("\tmultipleNodeSizes = " + multipleNodeSizes);
	    System.out.println("\tdepthCueing = " + depthCueing);
	    System.out.println("\taxes = " + axes);
	    System.out.println("\tsupportScreenCapture = "
			       + supportScreenCapture);
	    System.out.println("\tautomaticRefresh = " + automaticRefresh);

	    System.out.print("(Node) ");
	    nodeColor.print();
	    System.out.print("(Tree Link) ");
	    treeLinkColor.print();
	    System.out.print("(Nontree Link) ");
	    nontreeLinkColor.print();

	    System.out.println("Node Label Attributes:");
	    for (int i = 0; i < nodeLabelAttributes.length; i++)
	    {
		System.out.println("\t" + nodeLabelAttributes[i]);
	    }
	    System.out.println("------------------------------------------\n");
	}
    }

    private static class ColorConfiguration
    {
	public static final int INVISIBLE = 0;
	public static final int TRANSPARENT = 1;
	public static final int FIXED_COLOR = 2;
	public static final int HOT_TO_COLD = 3;
	public static final int LOG_HOT_TO_COLD = 4;
	public static final int HUE = 5;
	public static final int RGB = 6;

	public int scheme;
	public int fixedColor;
	public String colorAttribute;
	public String selectionAttribute;

	public boolean equalColoring(ColorConfiguration rhs)
	{
	    boolean retval = (rhs.scheme == scheme);
	    if (retval)
	    {
		if (scheme == FIXED_COLOR)
		{
		    retval = (rhs.fixedColor == fixedColor);
		}
		else if (scheme == HOT_TO_COLD || scheme == LOG_HOT_TO_COLD
			 || scheme == HUE || scheme == RGB)
		{
		    retval = (rhs.colorAttribute.equals(colorAttribute));
		}
	    }

	    if (retval)
	    {
		boolean lhsIsNull = (selectionAttribute == null);
		boolean rhsIsNull = (rhs.selectionAttribute == null);
		if (!lhsIsNull || !rhsIsNull)
		{
		    if (!lhsIsNull && !rhsIsNull)
		    {
			retval = (rhs.selectionAttribute
				  .equals(selectionAttribute));
		    }
		    else
		    {
			retval = false;
		    }
		}
	    }
	    return retval;
	}

	public void print()
	{
	    System.out.println("ColorConfiguration:");
	    System.out.println("\tscheme = " + getSchemeName());
	    System.out.println("\tfixedColor = " + fixedColor);
	    System.out.println("\tcolorAttribute = " + colorAttribute);
	    System.out.println("\tselectionAttribute = " + selectionAttribute);
	}

	private String getSchemeName()
	{
	    return m_schemeNames[scheme];
	}

	private static final String[] m_schemeNames = {
	    "INVISIBLE", "TRANSPARENT", "FIXED_COLOR", "HOT_TO_COLD",
	    "LOG_HOT_TO_COLD", "HUE", "RGB"
	};
    }

    ///////////////////////////////////////////////////////////////////////

    private static class AttributeCache
    {
	public AttributeCache(H3GraphLoader graphLoader, Graph graph)
	{
	    m_scalarColorAttributes = graphLoader.loadAttributes
		(graph, m_scalarColorAttributeTypeMatcher);
	    m_RGBColorAttributes = graphLoader.loadAttributes
		(graph, m_RGBColorAttributeTypeMatcher);
	    m_selectionAttributes = graphLoader.loadAttributes
		(graph, m_selectionAttributeTypeMatcher);
	}

	public List getScalarColorAttributes()
	{
	    return m_scalarColorAttributes;
	}

	public List getRGBColorAttributes()
	{
	    return m_RGBColorAttributes;
	}

	public List getSelectionAttributes()
	{
	    return m_selectionAttributes;
	}

	private List m_scalarColorAttributes;
	private List m_RGBColorAttributes;
	private List m_selectionAttributes;

	private H3GraphLoader.AttributeTypeMatcher
	    m_scalarColorAttributeTypeMatcher =
	    new ScalarColorAttributeTypeMatcher();

	private H3GraphLoader.AttributeTypeMatcher
	    m_RGBColorAttributeTypeMatcher =
	    new RGBColorAttributeTypeMatcher();

	private H3GraphLoader.AttributeTypeMatcher
	    m_selectionAttributeTypeMatcher =
	    new SelectionAttributeTypeMatcher();

	////////////////////////////////////////////////////////////////////

	private class ScalarColorAttributeTypeMatcher
	    implements H3GraphLoader.AttributeTypeMatcher
	{
	    public boolean match(ValueType type)
	    {
		return type == ValueType.INTEGER || type == ValueType.FLOAT
		    || type == ValueType.DOUBLE;
	    }
	}

	private class RGBColorAttributeTypeMatcher
	    implements H3GraphLoader.AttributeTypeMatcher
	{
	    public boolean match(ValueType type)
	    {
		return type == ValueType.INTEGER || type == ValueType.FLOAT3
		    || type == ValueType.DOUBLE3;
	    }
	}

	private class SelectionAttributeTypeMatcher
	    implements H3GraphLoader.AttributeTypeMatcher
	{
	    public boolean match(ValueType type)
	    {
		return type == ValueType.BOOLEAN;
	    }
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private class AllAttributeTypeMatcher
	implements H3GraphLoader.AttributeTypeMatcher
    {
	public boolean match(ValueType type)
	{
	    return true;
	}
    }

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
}

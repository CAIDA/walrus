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
	retval.onScreenLabels = m_onScreenLabelsMenuItem.isSelected();
	retval.automaticRefresh = m_automaticRefreshMenuItem.isSelected();
	retval.automaticExtendedPrecision =
	    m_automaticExtendedPrecisionMenuItem.isSelected();
	retval.nodeColor =
	    m_colorSchemeMenu.createNodeColorConfigurationSnapshot();
	retval.treeLinkColor =
	    m_colorSchemeMenu.createTreeLinkColorConfigurationSnapshot();
	retval.nontreeLinkColor =
	    m_colorSchemeMenu.createNontreeLinkColorConfigurationSnapshot();

	int numSelected = countNumSelectedItems(m_nodeLabelMenu);
	retval.nodeLabelAttributes = new int[numSelected];
	retval.nodeLabelAttributeNames = new String[numSelected];

	int numAdded = 0;
	int numAttributes = m_nodeLabelMenu.getItemCount();
	for (int i = 0; i < numAttributes; i++)
	{
	    JMenuItem menuItem = m_nodeLabelMenu.getItem(i);
	    if (menuItem != null && menuItem.isSelected())
	    {
		String name = menuItem.getText();
		AttributeDefinitionIterator iterator =
		    m_backingGraph.getAttributeDefinition(name);
		if (iterator.atEnd())
		{
		    String msg = "no attribute named `" + name + "' found";
		    throw new RuntimeException(msg);
		}
		retval.nodeLabelAttributes[numAdded] = iterator.getID();
		retval.nodeLabelAttributeNames[numAdded] = name;
		++numAdded;
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
		JRadioButtonMenuItem menuItem =
		    new JRadioButtonMenuItem(name);
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
	if (m_renderLoop != null)
	{
	    stopRendering();
	}

	m_renderingConfiguration = null;
	m_rootNode = -1;
	m_currentNode = -1;
	m_previousNode = -1;
	m_backingGraph = null;
	m_graph = null;
	m_displayPosition = null;
	m_savedDisplayPosition = null;
	m_isDisplayNarrowed = false;

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
	m_resetRenderingMenuItem.setEnabled(false);
	m_recomputeLayoutExtendedMenuItem.setEnabled(false);

	// Display menu.
	m_narrowToSubtreeMenuItem.setEnabled(false);
	m_widenSubtreeMenuItem.setEnabled(false);
	m_widenTowardRootMenuItem.setEnabled(false);
	m_widenToGraphMenuItem.setEnabled(false);
	m_pruneSubtreeMenuItem.setEnabled(false);
	m_pruneToChildrenMenuItem.setEnabled(false);
	m_pruneToNeighborhoodMenu.setEnabled(false);
	m_zoomInMenuItem.setEnabled(false);
	m_zoomOutMenuItem.setEnabled(false);
	m_zoomResetMenuItem.setEnabled(false);
	m_refreshDisplayMenuItem.setEnabled(false);
	m_wobbleDisplayMenuItem.setEnabled(false);
	m_showRootNodeMenuItem.setEnabled(false);
	m_showParentNodeMenuItem.setEnabled(false);
	m_showPreviousNodeMenuItem.setEnabled(false);
	m_savePositionMenuItem.setEnabled(false);
	m_restorePositionMenuItem.setEnabled(false);

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

	// NOTE: Java3D behaves erratically if transparency is set in the
	//       Appearance of a geometry which also includes per point/vertex
	//       colors.  When a geometry includes both data, Java3D doesn't
	//       draw with transparency, and furthermore, it occasionally
	//       draws lines (and perhaps points) in black.
	//
	//       PointArray seems to be an exception to this, but we take
	//       care to disable transparency even for this case just to
	//       be sure.
	setNodeTransparencyEnabled
	    (configuration.isTransparent
	     && configuration.scheme != ColorConfiguration.RGB);

	switch (configuration.scheme)
	{
	case ColorConfiguration.INVISIBLE:
	    // No explicit coloring needed.
	    // startRendering() will take care of setting things up.
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

	case ColorConfiguration.HUE:
	    throw new RuntimeException("NOT IMPLEMENTED");

	case ColorConfiguration.RGB:
	    colorNodesRGB(configuration.colorAttribute);
	    break;

	default: throw new RuntimeException();
	}
    }

    private void setNodeTransparencyEnabled(boolean state)
    {
	TransparencyAttributes attributes =
	    (state ? m_viewParameters.getTransparencyAttributes() : null);

	m_viewParameters.getNodeAppearance()
	    .setTransparencyAttributes(attributes);
	m_viewParameters.getNearNodeAppearance()
	    .setTransparencyAttributes(attributes);
	m_viewParameters.getMiddleNodeAppearance()
	    .setTransparencyAttributes(attributes);
	m_viewParameters.getFarNodeAppearance()
	    .setTransparencyAttributes(attributes);
    }

    private void colorTreeLinks(ColorConfiguration configuration)
    {
	System.out.print("(colorTreeLinks) ");
	configuration.print();

	// NOTE: Java3D behaves erratically if transparency is set in the
	//       Appearance of a geometry which also includes per point/vertex
	//       colors.  When a geometry includes both data, Java3D doesn't
	//       draw with transparency, and furthermore, it occasionally
	//       draws lines (and perhaps points) in black.
	setTreeLinkTransparencyEnabled
	    (configuration.isTransparent
	     && configuration.scheme != ColorConfiguration.RGB);

	switch (configuration.scheme)
	{
	case ColorConfiguration.INVISIBLE:
	    // No explicit coloring needed.
	    // startRendering() will take care of setting things up.
	    break;

	case ColorConfiguration.FIXED_COLOR:
	    {
		ColoringAttributes attributes =
		    makeColoringAttributes(configuration.fixedColor);
		m_viewParameters.getTreeLinkAppearance()
		    .setColoringAttributes(attributes);
	    }	    
	    break;

	case ColorConfiguration.HUE:
	    throw new RuntimeException("NOT IMPLEMENTED");

	case ColorConfiguration.RGB:
	    colorLinksRGB(configuration.colorAttribute, true);
	    break;

	default: throw new RuntimeException();
	}
    }

    private void setTreeLinkTransparencyEnabled(boolean state)
    {
	TransparencyAttributes attributes =
	    (state ? m_viewParameters.getTransparencyAttributes() : null);

	m_viewParameters.getTreeLinkAppearance()
	    .setTransparencyAttributes(attributes);
    }

    private void colorNontreeLinks(ColorConfiguration configuration)
    {
	System.out.print("(colorNontreeLinks) ");
	configuration.print();

	// NOTE: Java3D behaves erratically if transparency is set in the
	//       Appearance of a geometry which also includes per point/vertex
	//       colors.  When a geometry includes both data, Java3D doesn't
	//       draw with transparency, and furthermore, it occasionally
	//       draws lines (and perhaps points) in black.
	setNontreeLinkTransparencyEnabled
	    (configuration.isTransparent
	     && configuration.scheme != ColorConfiguration.RGB);

	switch (configuration.scheme)
	{
	case ColorConfiguration.INVISIBLE:
	    // No explicit coloring needed.
	    // startRendering() will take care of setting things up.
	    break;

	case ColorConfiguration.FIXED_COLOR:
	    {
		ColoringAttributes attributes =
		    makeColoringAttributes(configuration.fixedColor);
		m_viewParameters.getNontreeLinkAppearance()
		    .setColoringAttributes(attributes);
	    }	    
	    break;

	case ColorConfiguration.HUE:
	    throw new RuntimeException("NOT IMPLEMENTED");

	case ColorConfiguration.RGB:
	    colorLinksRGB(configuration.colorAttribute, false);
	    break;

	default: throw new RuntimeException();
	}
    }

    private void setNontreeLinkTransparencyEnabled(boolean state)
    {
	TransparencyAttributes attributes =
	    (state ? m_viewParameters.getTransparencyAttributes() : null);

	m_viewParameters.getNontreeLinkAppearance()
	    .setTransparencyAttributes(attributes);
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

    private void selectNodes(ColorConfiguration configuration)
    {
	if (configuration.selectionAttribute == null)
	{
	    m_graph.setNodeSelectivity(true);
	}
	else
	{
	    int attribute = m_backingGraph.getAttributeDefinition
		(configuration.selectionAttribute).getID();

	    int numNodes = m_graph.getNumNodes();
	    for (int i = 0; i < numNodes; i++)
	    {
		boolean isSelected = true;
		try
		{
		    int nodeID = m_graph.getNodeID(i);
		    isSelected = m_backingGraph.getBooleanAttribute
			(ObjectType.NODE, nodeID, attribute);
		}
		catch (AttributeUnavailableException e)
		{
		    // Assume selected.
		}
		m_graph.setNodeSelectivity(i, isSelected);
	    }
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private void selectLinks
	(ColorConfiguration configuration, boolean treeLink)
    {
	if (configuration.selectionAttribute == null)
	{
	    m_graph.setLinkSelectivity(treeLink, true);
	}
	else
	{
	    int attribute = m_backingGraph.getAttributeDefinition
		(configuration.selectionAttribute).getID();

	    int numLinks = m_graph.getTotalNumLinks();
	    for (int i = 0; i < numLinks; i++)
	    {
		if (m_graph.checkTreeLink(i) == treeLink)
		{
		    boolean isSelected = true;
		    try
		    {
			int linkID = m_graph.getLinkID(i);
			isSelected = m_backingGraph.getBooleanAttribute
			    (ObjectType.LINK, linkID, attribute);
		    }
		    catch (AttributeUnavailableException e)
		    {
			// Assume selected.
		    }
		    m_graph.setLinkSelectivity(i, isSelected);
		}
	    }
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleStopRenderingRequest()
    {
	stopRendering();
	reinstateSplashScreenContentPane();
	setupIdleRenderingMenu();
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleUpdateRenderingRequest()
    {
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

    private void handleResetRenderingRequest()
    {
	boolean isRendering = (m_renderLoop != null);
	if (isRendering)
	{
	    stopRendering();
	}

	m_rootNode = m_currentNode = m_previousNode = m_graph.getRootNode();
	m_displayPosition = null;
	m_savedDisplayPosition = null;
	m_isDisplayNarrowed = false;

	m_graph.setNodeDisplayability(true);
	m_graph.setLinkDisplayability(true);
	m_graph.computeVisibility();

	m_graph.transformNodes(H3Transform.I4);
	m_viewParameters.resetObjectTransform();

	// Display menu.
	m_widenSubtreeMenuItem.setEnabled(false);
	m_widenTowardRootMenuItem.setEnabled(false);
	m_widenToGraphMenuItem.setEnabled(false);
	m_restorePositionMenuItem.setEnabled(false);

	if (isRendering)
	{
	    startRendering(m_renderingConfiguration);
	}
    }

    ///////////////////////////////////////////////////////////////////////

    // NOTE: Assumes that a graph has been loaded and laid out already
    //       (that is, m_renderingConfiguration != null).
    //
    //       This can be called both while some graph is being rendered
    //       and when rendering has been stopped (but with some graph loaded
    //       and previously laid out).
    private void handleRecomputeLayoutExtendedRequest()
    {
	if (m_renderLoop != null)
	{
	    stopRendering();
	    reinstateSplashScreenContentPane();
	    setupIdleRenderingMenu();
	}

	if (layoutGraph(m_renderingConfiguration, true))
	{
	    reinstateCanvasContentPane();
	    setupActiveRenderingMenu();
	    startRendering(m_renderingConfiguration);
	}
	else
	{
	    m_renderingConfiguration = null;
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleNarrowToSubtreeRequest(int node)
    {
	m_graph.narrowVisibility(node);
	updateDisplayNarrowingMenusAndRefresh();
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleWidenSubtreeRequest(int node)
    {
	m_graph.widenSubtreeVisibility(node);
	updateDisplayNarrowingMenusAndRefresh();
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleWidenTowardRootRequest(int node)
    {
	m_graph.widenVisibilityTowardRoot(node);
	updateDisplayNarrowingMenusAndRefresh();
    }

    ///////////////////////////////////////////////////////////////////////

    private void handleWidenToGraphRequest()
    {
	m_graph.widenVisibility();
	updateDisplayNarrowingMenusAndRefresh();
    }

    ///////////////////////////////////////////////////////////////////////

    private void handlePruneSubtreeRequest(int node)
    {
	m_graph.pruneSubtreeVisibility(node);
	updateDisplayNarrowingMenusAndRefresh();
    }

    ///////////////////////////////////////////////////////////////////////

    private void handlePruneToNeighborhoodRequest(int node, int distance)
    {
	m_graph.pruneVisibilityToNeighborhood(node, distance);
	updateDisplayNarrowingMenusAndRefresh();
    }

    ///////////////////////////////////////////////////////////////////////

    private void updateDisplayNarrowingMenusAndRefresh()
    {
	setupDisplayNarrowingMenus(!m_graph.checkNodesVisible());
	m_eventHandler.forceIdleState();
	m_eventHandler.refreshDisplay();
    }

    ///////////////////////////////////////////////////////////////////////

    private void setupDisplayNarrowingMenus(boolean status)
    {
	m_isDisplayNarrowed = status;
	m_widenSubtreeMenuItem.setEnabled(status);
	m_widenTowardRootMenuItem.setEnabled(status);
	m_widenToGraphMenuItem.setEnabled(status);
    }

    ///////////////////////////////////////////////////////////////////////

    private boolean setupRendering
	(RenderingConfiguration renderingConfiguration)
    {
	boolean retval = true;
	try
	{
	    if (m_renderingConfiguration == null
		|| !renderingConfiguration.spanningTree.equals
		(m_renderingConfiguration.spanningTree))
	    {
		m_displayPosition = null;
		m_savedDisplayPosition = null;
		m_isDisplayNarrowed = false;

		m_graph = m_graphLoader.load
		    (m_backingGraph, renderingConfiguration.spanningTree);

		m_rootNode = m_graph.getRootNode();
		m_currentNode = m_previousNode = m_rootNode;

		if (DEBUG_CHECK_ID_MAPPINGS)
		{
		    checkGraphIDMappings(m_graph, m_backingGraph);
		}

		retval = false;

		int numNodes = m_graph.getNumNodes();
		int numReachable = m_graph.checkSpanningTree();
		if (numReachable == numNodes)
		{
		    if (layoutGraph(renderingConfiguration, false))
		    {
			retval = true;

			colorNodes(renderingConfiguration.nodeColor);
			colorTreeLinks(renderingConfiguration.treeLinkColor);
			colorNontreeLinks
			    (renderingConfiguration.nontreeLinkColor);

			selectNodes(renderingConfiguration.nodeColor);
			selectLinks
			    (renderingConfiguration.treeLinkColor, true);
			selectLinks
			    (renderingConfiguration.nontreeLinkColor, false);

			m_graph.setNodeDisplayability(true);
			m_graph.setLinkDisplayability(true);
			m_graph.computeVisibility();
		    }
		}
		else
		{
		    String msg;
		    if (numReachable == -1)
		    {
			msg = "The selected spanning tree does not specify"
			    + " a tree.\nA node is reachable along at least"
			    + " two paths, which indicates a cycle,\n"
			    + "a parallel edge, or the convergence of"
			    + " two branches.";
		    }
		    else
		    {
			msg = "The selected spanning tree does not cover"
			    + " all nodes.\nOnly " + numReachable + " of "
			    + numNodes + " nodes are reachable through the"
			    + " spanning tree.";
		    }

		    JOptionPane.showMessageDialog
			(null, msg, "Malformed Spanning Tree",
			 JOptionPane.ERROR_MESSAGE);
		}
	    }
	    else
	    {
		boolean visibilityChanged = false;

		if (!renderingConfiguration.nodeColor
		    .equalColoring(m_renderingConfiguration.nodeColor))
		{
		    visibilityChanged = true;
		    colorNodes(renderingConfiguration.nodeColor);
		    selectNodes(renderingConfiguration.nodeColor);
		}

		if (!renderingConfiguration.treeLinkColor
		    .equalColoring(m_renderingConfiguration.treeLinkColor))
		{
		    visibilityChanged = true;
		    colorTreeLinks(renderingConfiguration.treeLinkColor);
		    selectLinks(renderingConfiguration.treeLinkColor, true);
		}

		if (!renderingConfiguration.nontreeLinkColor
		    .equalColoring(m_renderingConfiguration.nontreeLinkColor))
		{
		    visibilityChanged = true;
		    colorNontreeLinks(renderingConfiguration.nontreeLinkColor);
		    selectLinks(renderingConfiguration.nontreeLinkColor,false);
		}

		if (visibilityChanged)
		{
		    if (!m_isDisplayNarrowed)
		    {
			m_graph.setNodeDisplayability(true);
			m_graph.setLinkDisplayability(true);
		    }

		    m_graph.computeVisibility();
		}
	    }

	    if (retval)
	    {
		m_renderingConfiguration = renderingConfiguration;
	    }
	}
	catch (H3GraphLoader.InvalidGraphDataException e)
	{
	    retval = false;
	    String msg = "Graph file lacks needed data: " + e.getMessage();
	    JOptionPane dialog = new JOptionPane();
	    dialog.showMessageDialog(null, msg, "Rendering Setup Failed",
				     JOptionPane.ERROR_MESSAGE);
	}
	return retval;
    }

    private boolean layoutGraph
	(RenderingConfiguration renderingConfiguration,
	 boolean useExtendedPrecision)
    {
	H3GraphLayout layout = new H3GraphLayout
	    (renderingConfiguration.automaticExtendedPrecision);

	H3GraphLayout.LayoutState layoutState =
	    layout.layoutHyperbolic(m_graph, useExtendedPrecision);

	int numNodes = m_graph.getNumNodes();
	int numGoodNodes = m_graph.checkLayoutCoordinates();

	boolean retval = (numGoodNodes == numNodes);
	if (!retval)
	{
	    int numBadNodes = numNodes - numGoodNodes;

	    if (useExtendedPrecision)
	    {
		String msg = "Graph could not be laid out within the"
		    + " limits of extended floating-point precision.\n"
		    + "Layout failed for "  + numBadNodes
		    + " of " + numNodes + " nodes.\n\n"
		    + "Would you like to proceed anyway by arbitrarily"
		    + " placing these nodes at the origin?"; 
		JOptionPane dialog = new JOptionPane();
		int response = dialog.showConfirmDialog
		    (null, msg, "Graph Layout Failed",
		     JOptionPane.YES_NO_OPTION);
		retval = (response == JOptionPane.YES_OPTION);
		if (retval)
		{
		    m_graph.sanitizeLayoutCoordinates();
		}
	    }
	    else
	    {
		String msg = "Graph could not be laid out within the"
		    + " limits of floating-point precision.\n"
		    + "Layout failed for "  + numBadNodes
		    + " of " + numNodes + " nodes.\n\n"
		    + "You may retry using extended precision arithmetic,"
		    + " but layout can take several hours for large graphs.\n"
		    + "Or you may proceed by arbitrarily placing the"
		    + " problematic nodes at the origin.\n"
		    + "Or you may cancel the layout attempt and not render"
		    + " the graph.\n";
		JOptionPane dialog = new JOptionPane();
		int response = dialog.showOptionDialog
		    (null, msg, "Graph Layout Failed",
		     JOptionPane.YES_NO_CANCEL_OPTION,
		     JOptionPane.QUESTION_MESSAGE, null,
		     new String[] { "Retry with Extended Precision",
				    "Place Nodes at Origin", "Cancel" },
		     null);

		if (response == JOptionPane.YES_OPTION)
		{
		    retval = layoutGraph(renderingConfiguration, true);
		}
		else if (response == JOptionPane.NO_OPTION)
		{
		    retval = true;
		    m_graph.sanitizeLayoutCoordinates();
		}
		// else do nothing and fail
	    }
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

	H3PointRenderList renderList = new H3PointRenderList
	    (m_graph, useNodeSizes,
	     includeNodes, includeNodeColor,
	     includeTreeLinks, includeTreeLinkColor,
	     includeNontreeLinks, includeNontreeLinkColor);

	renderList.setNearNodeAppearance
	    (useNodeSizes
	     ? m_viewParameters.getNearNodeAppearance()
	     : m_viewParameters.getMiddleNodeAppearance());
	renderList.setMiddleNodeAppearance
	    (m_viewParameters.getMiddleNodeAppearance());
	renderList.setFarNodeAppearance
	    (m_viewParameters.getFarNodeAppearance());
	renderList.setTreeLinkAppearance
	    (m_viewParameters.getTreeLinkAppearance());
	renderList.setNontreeLinkAppearance
	    (m_viewParameters.getNontreeLinkAppearance());

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

	    H3AdaptiveRenderer renderer = null;

	    if (true)
	    {
		renderer = new H3LineRenderer(m_graph, queue, renderList);
	    }
	    else
	    {
		// H3CircleRenderer draws circles (lying on the plane of
		// the screen) around each node.  Every node has a radius
		// which is inversely proportional to the distance between
		// the node and the center of the screen.  Currently, this
		// radius is used primarily as a sorting key by which the
		// adaptive rendering algorithm tries to render the display
		// from the center of the screen outward (since the nodes
		// with the greatest interest lie near the center).
		//
		// This renderer is mainly a debugging aid, but it also
		// serves as an example of how the architecture supports
		// variation in the rendering of nodes.
		renderer = new H3CircleRenderer
		    (m_graph, m_viewParameters, queue, renderList);
	    }

	    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	    H3AdaptiveRenderLoop adaptive = new H3AdaptiveRenderLoop
		(m_graph, m_canvas, m_viewParameters,
		 transformer, queue, renderer);

	    new Thread(adaptive).start();
	    m_renderLoop = adaptive;

	    // This duration is not a hard upper limit but only a strong
	    // suggestion to the renderer.  The frame rate will be
	    // approximately 1000/DURATION, under ideal conditions, but
	    // in practice, the frame rate is usually only a quarter to
	    // a half this calculation.
	    //
	    // There is no upper bound on the frame rate given a sufficiently
	    // powerful computer.  The following only specifies the lower
	    // bound.
            final int DURATION = 50; // in milliseconds
	    adaptive.setMaxRotationDuration(DURATION);
	    adaptive.setMaxTranslationDuration(DURATION);
	    adaptive.setMaxCompletionDuration(DURATION);

	    System.out.println("Started H3AdaptiveRenderLoop.");
	}
	else
	{
	    m_graph.transformNodes(H3Transform.I4);

	    H3NonadaptiveRenderLoop nonadaptive = new H3NonadaptiveRenderLoop
		(m_graph, m_canvas, m_viewParameters,
		 renderList, useNodeSizes);

	    new Thread(nonadaptive).start();
	    m_renderLoop = nonadaptive;

	    System.out.println("Started H3NonadaptiveRenderLoop.");
	}

	NarrowingEventHandler narrowingHandler = new NarrowingEventHandler()
	    {
		public void narrowToSubtree(int node)
		{ handleNarrowToSubtreeRequest(node); }

		public void widenSubtree(int node)
		{ handleWidenSubtreeRequest(node); }

		public void widenTowardRoot(int node)
		{ handleWidenTowardRootRequest(node); }

		public void widenToGraph()
		{ handleWidenToGraphRequest(); }

		public void pruneSubtree(int node)
		{ handlePruneSubtreeRequest(node); }

		public void pruneToNeighborhood(int node, int distance)
		{ handlePruneToNeighborhoodRequest(node, distance); }
	    };

	m_eventHandler = new EventHandler
	    (m_viewParameters, m_canvas, m_renderLoop,
	     narrowingHandler,
	     m_rootNode, m_currentNode, m_previousNode,
	     m_graph, m_backingGraph,
	     renderingConfiguration.nodeLabelAttributes,
	     renderingConfiguration.nodeLabelAttributeNames,
	     m_statusBar,
	     renderingConfiguration.onScreenLabels,
	     renderingConfiguration.automaticRefresh);

	System.out.println("EventHandler installed.");

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
	m_eventHandler.forceIdleState();
	m_currentNode = m_eventHandler.getCurrentNode();
	m_previousNode = m_eventHandler.getPreviousNode();
	m_displayPosition = m_renderLoop.getDisplayPosition();

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

    // Changes the enabled state of various menu items to reflect an idle
    // rendering state (when the splash screen is being shown).
    private void setupIdleRenderingMenu()
    {
	// Rendering menu.
	m_startMenuItem.setEnabled(true);
	m_stopMenuItem.setEnabled(false);
	m_updateMenuItem.setEnabled(false);
	m_resetRenderingMenuItem.setEnabled(m_renderingConfiguration != null);
	m_recomputeLayoutExtendedMenuItem.setEnabled
	    (m_renderingConfiguration != null);

	// Display menu.
	m_narrowToSubtreeMenuItem.setEnabled(false);
	m_widenSubtreeMenuItem.setEnabled(false);
	m_widenTowardRootMenuItem.setEnabled(false);
	m_widenToGraphMenuItem.setEnabled(false);
	m_pruneSubtreeMenuItem.setEnabled(false);
	m_pruneToChildrenMenuItem.setEnabled(false);
	m_pruneToNeighborhoodMenu.setEnabled(false);
	m_zoomInMenuItem.setEnabled(false);
	m_zoomOutMenuItem.setEnabled(false);
	m_zoomResetMenuItem.setEnabled(false);
	m_refreshDisplayMenuItem.setEnabled(false);
	m_wobbleDisplayMenuItem.setEnabled(false);
	m_showRootNodeMenuItem.setEnabled(false);
	m_showParentNodeMenuItem.setEnabled(false);
	m_showPreviousNodeMenuItem.setEnabled(false);
	m_savePositionMenuItem.setEnabled(false);
	m_restorePositionMenuItem.setEnabled(false);
    }

    ///////////////////////////////////////////////////////////////////////

    // Changes the enabled state of various menu items to reflect an active
    // rendering state (when a Canvas3D is being shown).
    private void setupActiveRenderingMenu()
    {
	// Rendering menu.
	m_startMenuItem.setEnabled(false);
	m_stopMenuItem.setEnabled(true);
	m_updateMenuItem.setEnabled(true);
	m_resetRenderingMenuItem.setEnabled(true);
	m_recomputeLayoutExtendedMenuItem.setEnabled(true);

	// Display menu.
	m_narrowToSubtreeMenuItem.setEnabled(true);
	m_widenSubtreeMenuItem.setEnabled(m_isDisplayNarrowed);
	m_widenTowardRootMenuItem.setEnabled(m_isDisplayNarrowed);
	m_widenToGraphMenuItem.setEnabled(m_isDisplayNarrowed);
	m_pruneSubtreeMenuItem.setEnabled(true);
	m_pruneToChildrenMenuItem.setEnabled(true);
	m_pruneToNeighborhoodMenu.setEnabled(true);
	m_zoomInMenuItem.setEnabled(true);
	m_zoomOutMenuItem.setEnabled(true);
	m_zoomResetMenuItem.setEnabled(true);
	m_refreshDisplayMenuItem.setEnabled(true);
	m_wobbleDisplayMenuItem.setEnabled(true);
	m_showRootNodeMenuItem.setEnabled(true);
	m_showParentNodeMenuItem.setEnabled(true);
	m_showPreviousNodeMenuItem.setEnabled(true);
	m_savePositionMenuItem.setEnabled(true);
	m_restorePositionMenuItem.setEnabled(m_savedDisplayPosition != null);
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
	// Without this, the ordinarily lightweight menus would be drawn
	// behind the heavyweight Canvas3D and thus be obscured.
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

	m_resetRenderingMenuItem = new JMenuItem("Reset Rendering");
	m_resetRenderingMenuItem.setMnemonic(KeyEvent.VK_T);
	m_resetRenderingMenuItem.setEnabled(false);
	m_resetRenderingMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    handleResetRenderingRequest();
		}
	    });

	m_recomputeLayoutExtendedMenuItem =
	    new JMenuItem("Recompute Layout With Extended Precision");
	m_recomputeLayoutExtendedMenuItem.setMnemonic(KeyEvent.VK_R);
	m_recomputeLayoutExtendedMenuItem.setEnabled(false);
	m_recomputeLayoutExtendedMenuItem.addActionListener
	    (new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    handleRecomputeLayoutExtendedRequest();
		}
	    });

	m_adaptiveMenuItem = new JCheckBoxMenuItem("Adaptive Rendering");
	m_adaptiveMenuItem.setMnemonic(KeyEvent.VK_A);
	m_adaptiveMenuItem.setSelected(true);

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

	m_onScreenLabelsMenuItem =
	    new JCheckBoxMenuItem("On-Screen Labels");
	m_onScreenLabelsMenuItem.setMnemonic(KeyEvent.VK_L);
	m_onScreenLabelsMenuItem.setSelected(true);

	m_automaticRefreshMenuItem
	    = new JCheckBoxMenuItem("Automatic Refresh");
	m_automaticRefreshMenuItem.setMnemonic(KeyEvent.VK_F);
	m_automaticRefreshMenuItem.setSelected(true);

	m_automaticExtendedPrecisionMenuItem
	    = new JCheckBoxMenuItem("Automatic Extended Precision Layout");
	m_automaticExtendedPrecisionMenuItem.setMnemonic(KeyEvent.VK_E);
	m_automaticExtendedPrecisionMenuItem.setSelected(true);

	m_renderingMenu = new JMenu("Rendering");
	m_renderingMenu.setMnemonic(KeyEvent.VK_R);
	m_renderingMenu.add(m_startMenuItem);
	m_renderingMenu.add(m_stopMenuItem);
	m_renderingMenu.add(m_updateMenuItem);
	m_renderingMenu.addSeparator();
	m_renderingMenu.add(m_resetRenderingMenuItem);
	m_renderingMenu.add(m_recomputeLayoutExtendedMenuItem);
	m_renderingMenu.addSeparator();
	m_renderingMenu.add(m_adaptiveMenuItem);
	m_renderingMenu.add(m_multipleNodeSizesMenuItem);
	m_renderingMenu.add(m_depthCueingMenuItem);
	m_renderingMenu.add(m_axesMenuItem);
	m_renderingMenu.add(m_onScreenLabelsMenuItem);
	m_renderingMenu.add(m_automaticRefreshMenuItem);
	m_renderingMenu.add(m_automaticExtendedPrecisionMenuItem);

	// Create "Display" menu. ------------------------------------------

	m_narrowToSubtreeMenuItem = new JMenuItem("Narrow To Subtree");
	m_narrowToSubtreeMenuItem.setMnemonic(KeyEvent.VK_N);
	m_narrowToSubtreeMenuItem.setEnabled(false);
	m_narrowToSubtreeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    int node = m_eventHandler.getCurrentNode();
		    handleNarrowToSubtreeRequest(node);
		}
	    });

	m_widenSubtreeMenuItem = new JMenuItem("Widen Subtree");
	m_widenSubtreeMenuItem.setMnemonic(KeyEvent.VK_U);
	m_widenSubtreeMenuItem.setEnabled(false);
	m_widenSubtreeMenuItem.addActionListener
	    (new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    int node = m_eventHandler.getCurrentNode();
		    handleWidenSubtreeRequest(node);
		}
	    });

	m_widenTowardRootMenuItem = new JMenuItem("Widen Toward Root");
	m_widenTowardRootMenuItem.setMnemonic(KeyEvent.VK_W);
	m_widenTowardRootMenuItem.setEnabled(false);
	m_widenTowardRootMenuItem.addActionListener
	    (new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    int node = m_eventHandler.getCurrentNode();
		    handleWidenTowardRootRequest(node);
		}
	    });

	m_widenToGraphMenuItem = new JMenuItem("Widen To Entire Graph");
	m_widenToGraphMenuItem.setMnemonic(KeyEvent.VK_G);
	m_widenToGraphMenuItem.setEnabled(false);
	m_widenToGraphMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    handleWidenToGraphRequest();
		}
	    });

	m_pruneSubtreeMenuItem = new JMenuItem("Prune Subtree");
	m_pruneSubtreeMenuItem.setMnemonic(KeyEvent.VK_P);
	m_pruneSubtreeMenuItem.setEnabled(false);
	m_pruneSubtreeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    int node = m_eventHandler.getCurrentNode();
		    handlePruneSubtreeRequest(node);
		}
	    });

	m_pruneToChildrenMenuItem = new JMenuItem("Prune To Children");
	m_pruneToChildrenMenuItem.setMnemonic(KeyEvent.VK_C);
	m_pruneToChildrenMenuItem.setEnabled(false);
	m_pruneToChildrenMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    int node = m_eventHandler.getCurrentNode();
		    handlePruneToNeighborhoodRequest(node, 1);
		}
	    });

	m_pruneToNeighborhoodMenu = new JMenu("Prune To Neighborhood");
	m_pruneToNeighborhoodMenu.setMnemonic(KeyEvent.VK_E);
	m_pruneToNeighborhoodMenu.setEnabled(false);
	addPruneToNeighborhoodSubmenus(m_pruneToNeighborhoodMenu);

	m_zoomInMenuItem = new JMenuItem("Zoom In");
	m_zoomInMenuItem.setMnemonic(KeyEvent.VK_I);
	m_zoomInMenuItem.setEnabled(false);
	m_zoomInMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_eventHandler.increaseMagnification();
		}
	    });

	m_zoomOutMenuItem = new JMenuItem("Zoom Out");
	m_zoomOutMenuItem.setMnemonic(KeyEvent.VK_O);
	m_zoomOutMenuItem.setEnabled(false);
	m_zoomOutMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_eventHandler.decreaseMagnification();
		}
	    });

	m_zoomResetMenuItem = new JMenuItem("Zoom 1:1");
	m_zoomResetMenuItem.setMnemonic(KeyEvent.VK_Z);
	m_zoomResetMenuItem.setEnabled(false);
	m_zoomResetMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_eventHandler.resetMagnification();
		}
	    });

	m_refreshDisplayMenuItem = new JMenuItem("Refresh");
	m_refreshDisplayMenuItem.setMnemonic(KeyEvent.VK_R);
	m_refreshDisplayMenuItem.setEnabled(false);
	m_refreshDisplayMenuItem.setAccelerator
	    (KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
	m_refreshDisplayMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_eventHandler.refreshDisplay();
		}
	    });

	m_wobbleDisplayMenuItem = new JMenuItem("Wobble");
	m_wobbleDisplayMenuItem.setMnemonic(KeyEvent.VK_B);
	m_wobbleDisplayMenuItem.setEnabled(false);
	m_wobbleDisplayMenuItem.setAccelerator
	    (KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
	m_wobbleDisplayMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_wobbleDisplayMenuItem.setEnabled(false);
		    m_eventHandler.forceIdleState();
		    m_eventHandler.startWobbling(new CancellationListener()
			{
			    public void cancelled()
			    {
				m_wobbleDisplayMenuItem.setEnabled(true);
			    }
			});
		}
	    });

	m_showRootNodeMenuItem = new JMenuItem("Show Root Node");
	m_showRootNodeMenuItem.setMnemonic(KeyEvent.VK_H);
	m_showRootNodeMenuItem.setEnabled(false);
	m_showRootNodeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_eventHandler.forceIdleState();
		    m_eventHandler.showRootNode();
		}
	    });

	m_showParentNodeMenuItem = new JMenuItem("Show Parent Node");
	m_showParentNodeMenuItem.setMnemonic(KeyEvent.VK_A);
	m_showParentNodeMenuItem.setEnabled(false);
	m_showParentNodeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_eventHandler.forceIdleState();
		    m_eventHandler.showParentNode();
		}
	    });

	m_showPreviousNodeMenuItem = new JMenuItem("Show Previous Node");
	m_showPreviousNodeMenuItem.setMnemonic(KeyEvent.VK_V);
	m_showPreviousNodeMenuItem.setEnabled(false);
	m_showPreviousNodeMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_eventHandler.forceIdleState();
		    m_eventHandler.showPreviousNode();
		}
	    });

	m_savePositionMenuItem = new JMenuItem("Save Position");
	m_savePositionMenuItem.setMnemonic(KeyEvent.VK_S);
	m_savePositionMenuItem.setEnabled(false);
	m_savePositionMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_eventHandler.forceIdleState();
		    m_savedDisplayPosition = m_eventHandler.savePosition();
		    m_restorePositionMenuItem.setEnabled(true);
		}
	    });

	m_restorePositionMenuItem = new JMenuItem("Restore Position");
	m_restorePositionMenuItem.setMnemonic(KeyEvent.VK_T);
	m_restorePositionMenuItem.setEnabled(false);
	m_restorePositionMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		    m_eventHandler.forceIdleState();
		    m_eventHandler.restorePosition(m_savedDisplayPosition);
		}
	    });

	m_displayMenu = new JMenu("Display");
	m_displayMenu.setMnemonic(KeyEvent.VK_D);
	m_displayMenu.add(m_narrowToSubtreeMenuItem);
	m_displayMenu.add(m_widenSubtreeMenuItem);
	m_displayMenu.add(m_widenTowardRootMenuItem);
	m_displayMenu.add(m_widenToGraphMenuItem);
	m_displayMenu.add(m_pruneSubtreeMenuItem);
	m_displayMenu.add(m_pruneToChildrenMenuItem);
	m_displayMenu.add(m_pruneToNeighborhoodMenu);
	m_displayMenu.addSeparator();
	m_displayMenu.add(m_zoomInMenuItem);
	m_displayMenu.add(m_zoomOutMenuItem);
	m_displayMenu.add(m_zoomResetMenuItem);
	m_displayMenu.addSeparator();
	m_displayMenu.add(m_refreshDisplayMenuItem);
	m_displayMenu.add(m_wobbleDisplayMenuItem);
	m_displayMenu.add(m_showRootNodeMenuItem);
	m_displayMenu.add(m_showParentNodeMenuItem);
	m_displayMenu.add(m_showPreviousNodeMenuItem);
	m_displayMenu.addSeparator();
	m_displayMenu.add(m_savePositionMenuItem);
	m_displayMenu.add(m_restorePositionMenuItem);

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
	retval.add(m_displayMenu);
	retval.add(m_spanningTreeMenu);
	retval.add(m_colorSchemeMenu.getColorSchemeMenu());
	retval.add(m_nodeLabelMenu);
	return retval;
    }

    private void addPruneToNeighborhoodSubmenus(JMenu menu)
    {
	class PruningActionListener implements ActionListener
	{
	    public PruningActionListener(int distance)
	    {
		m_distance = distance;
	    }

	    public void actionPerformed(ActionEvent e)
	    {
		int node = m_eventHandler.getCurrentNode();
		handlePruneToNeighborhoodRequest(node, m_distance);
	    }

	    private final int m_distance;
	}

	int[] distances = { 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 30, 50 };
	for (int i = 0; i < distances.length; i++)
	{
	    int distance = distances[i];
	    JMenuItem menuItem =
		new JMenuItem("distance <= " + Integer.toString(distance));
	    menuItem.addActionListener(new PruningActionListener(distance));
	    menu.add(menuItem);
	}
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
    private static final boolean DEBUG_CHECK_ID_MAPPINGS = false;
    private static final boolean DEBUG_EVENT_HANDLING = false;

    private static final int DEFAULT_FRAME_WIDTH = 900;
    private static final int DEFAULT_FRAME_HEIGHT = 1000;

    private static final String WALRUS_TITLE = "Walrus 0.6.3";
    private static final String SPLASH_ICON_PATH = "walrus-splash.jpg";
    private static final String MSG_NO_GRAPH_LOADED = "No graph loaded.";
    private static final String MSG_GRAPH_LOADED = "Graph loaded.";
    private static final String MSG_LOADING_GRAPH = "Loading graph...";
    private static final String SPLASH_HTML_LABEL = "<html><table border=0><tr><td align=center><b><i><font color=#CAFF70 size=+4>Walrus 0.6.3 -- Graph Visualization</font></i></b></td></tr><tr><td align=center><font color=#1E9619 size=+2>Copyright (c) 2000,2001,2002 CAIDA/UCSD</font></td></tr></table></html>";

    ///////////////////////////////////////////////////////////////////////

    // The following, m_renderingConfiguration, will be non-null if a graph
    // has been loaded and rendered (at least once).
    private RenderingConfiguration m_renderingConfiguration;
    private int m_rootNode;
    private int m_currentNode;
    private int m_previousNode;
    private Graph m_backingGraph;  // Will be non-null if a graph is open.
    private H3Graph m_graph;  // ...non-null when a graph is being rendered.
    private H3DisplayPosition m_displayPosition; // Saved while updating disp..
    private H3DisplayPosition m_savedDisplayPosition; // Saved by user...
    private boolean m_isDisplayNarrowed;
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
    private JMenuItem m_resetRenderingMenuItem;
    private JMenuItem m_recomputeLayoutExtendedMenuItem;
    private JCheckBoxMenuItem m_adaptiveMenuItem;
    private JCheckBoxMenuItem m_multipleNodeSizesMenuItem;
    private JCheckBoxMenuItem m_depthCueingMenuItem;
    private JCheckBoxMenuItem m_axesMenuItem;
    private JCheckBoxMenuItem m_onScreenLabelsMenuItem;
    private JCheckBoxMenuItem m_automaticRefreshMenuItem;
    private JCheckBoxMenuItem m_automaticExtendedPrecisionMenuItem;

    private JMenu m_displayMenu;
    private JMenuItem m_narrowToSubtreeMenuItem;
    private JMenuItem m_widenSubtreeMenuItem;
    private JMenuItem m_widenTowardRootMenuItem;
    private JMenuItem m_widenToGraphMenuItem;
    private JMenuItem m_pruneSubtreeMenuItem;
    private JMenuItem m_pruneToChildrenMenuItem;
    private JMenu m_pruneToNeighborhoodMenu;
    private JMenuItem m_zoomInMenuItem;
    private JMenuItem m_zoomOutMenuItem;
    private JMenuItem m_zoomResetMenuItem;
    private JMenuItem m_refreshDisplayMenuItem;
    private JMenuItem m_wobbleDisplayMenuItem;
    private JMenuItem m_showRootNodeMenuItem;
    private JMenuItem m_showParentNodeMenuItem;
    private JMenuItem m_showPreviousNodeMenuItem;
    private JMenuItem m_savePositionMenuItem;
    private JMenuItem m_restorePositionMenuItem;

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

    private static interface CancellationListener
    {
	void cancelled();
    }

    private static interface NarrowingEventHandler
    {
	void narrowToSubtree(int node);
	void widenSubtree(int node);
	void widenTowardRoot(int node);
	void widenToGraph();
	void pruneSubtree(int node);
	void pruneToNeighborhood(int node, int distance);
    }

    ///////////////////////////////////////////////////////////////////////

    // NOTE: The public methods of EventHandler aren't synchronized.
    //       Because this class listens for AWT events, any threads
    //       accessing this class should take care not to collide with
    //       the AWT event dispatching thread.  To be absolutely safe,
    //       only the AWT thread should access this class.

    private static class EventHandler
	implements KeyListener, MouseListener, MouseMotionListener
    {
	public EventHandler
	    (H3ViewParameters parameters,
	     H3Canvas3D canvas, H3RenderLoop renderLoop,
	     NarrowingEventHandler narrowingHandler,
	     int rootNode, int currentNode, int previousNode,
	     H3Graph graph, Graph backingGraph,
	     int[] nodeLabelAttributes, String[] nodeLabelAttributeNames,
	     JTextField statusBar,
	     boolean onScreenLabels, boolean automaticRefresh)
	{
	    m_parameters = parameters;
	    m_canvas = canvas;
	    m_canvas.addKeyListener(this);
	    m_canvas.addMouseListener(this);
	    m_canvas.addMouseMotionListener(this);

	    m_renderLoop = renderLoop;
	    m_narrowingHandler = narrowingHandler;
	    m_rootNode = rootNode;
	    m_currentNode = currentNode;
	    m_previousNode = previousNode;

	    m_graph = graph;
	    m_backingGraph = backingGraph;
	    m_nodeLabelAttributes = nodeLabelAttributes;
	    m_nodeLabelAttributeNames = nodeLabelAttributeNames;
	    m_statusBar = statusBar;
	    m_onScreenLabels = onScreenLabels;
	    m_labelConstructor =
		new NodeLabelConstructor(backingGraph, nodeLabelAttributes);

	    // This is a tradeoff between seeing flicker and having to
	    // manually refresh the display in some cases.  There's some
	    // problem deep in Java3D which makes a satisfactory solution
	    // impossible.
	    m_automaticRefresh = automaticRefresh;
	    if (automaticRefresh)
	    {
		m_canvas.addPaintObserver(m_paintObserver);
	    }

	    // Resize events are generated during the initial layout
	    // and when the enclosing frame is resized.
	    m_canvas.addComponentListener(m_resizeListener);
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

	    m_canvas.removeComponentListener(m_resizeListener);
	}

	public int getCurrentNode()
	{
	    return m_currentNode;
	}

	public int getPreviousNode()
	{
	    return m_previousNode;
	}

	// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void forceIdleState()
	{
	    switch (m_state)
	    {
	    case STATE_IDLE:
		//IGNORE
		break;

	    case STATE_DISPLAYING_ATTRIBUTES:
		//IGNORE
		break;

	    case STATE_ROTATING_INTERACTIVE:
		m_interactiveRequest.end();
		break;

	    case STATE_ROTATING_INTERACTIVE_START:
		//IGNORE
		break;

	    case STATE_ROTATING_CONTINUOUS:
		m_repeatingRequest.end();
		break;

	    case STATE_ROTATING_CONTINUOUS_START:
		//IGNORE
		break;

	    case STATE_ROTATING_TRACKING:
		m_repeatingRequest.end();
		break;

	    case STATE_WOBBLING:
		m_wobblingListener.cancelled();
		m_wobblingListener = null;
		m_wobblingRequest.end();
		break;

	    default:
		throw new RuntimeException
		    ("Invalid state in EventHandler: invalid state "+ m_state);
	    }
	    m_state = STATE_IDLE;
	}

	public void refreshDisplay()
	{
	    if (m_state == STATE_IDLE
		|| m_state == STATE_ROTATING_INTERACTIVE_START)
	    {
		m_labelZOffsetCounter = 0;
		m_renderLoop.refreshDisplay();		
	    }
	}

	public void startWobbling(CancellationListener listener)
	{
	    if (m_state == STATE_IDLE) 
	    {
		m_labelZOffsetCounter = 0;
		m_state = STATE_WOBBLING;
		m_wobblingListener = listener;
		m_wobblingRequest.start();
		m_renderLoop.rotateDisplay(m_wobblingRequest);
	    }
	    else
	    {
		listener.cancelled();
	    }
	}

	public void stopWobbling()
	{
	    if (m_state == STATE_WOBBLING)
	    {
		m_state = STATE_IDLE;
		m_wobblingListener = null;
		m_wobblingRequest.end();
	    }
	}

	public void showRootNode()
	{
	    if (m_state == STATE_IDLE)
	    {
		m_labelZOffsetCounter = 0;
		m_renderLoop.translate(m_rootNode);
		shiftCenterNodes(m_rootNode);
	    }
	}

	public void showParentNode()
	{
	    if (m_state == STATE_IDLE)
	    {
		int parent = m_graph.getNodeParent(m_currentNode);
		if (parent != -1)
		{
		    m_labelZOffsetCounter = 0;
		    m_renderLoop.translate(parent);
		    shiftCenterNodes(parent);
		}
	    }
	}

	public void showPreviousNode()
	{
	    if (m_state == STATE_IDLE)
	    {
		m_labelZOffsetCounter = 0;
		m_renderLoop.translate(swapCenterNodes());
	    }
	}

	public H3DisplayPosition savePosition()
	{
	    H3DisplayPosition retval = null;
	    if (m_state == STATE_IDLE)
	    {
		retval = m_renderLoop.getDisplayPosition();
	    }
	    return retval;
	}

	public void restorePosition(H3DisplayPosition position)
	{
	    if (m_state == STATE_IDLE)
	    {
		m_labelZOffsetCounter = 0;
		m_renderLoop.setDisplayPosition(position);
		shiftCenterNodes(position.getCenterNode());
	    }
	}

	public void increaseMagnification()
	{
	    forceIdleState();
	    m_parameters.increaseMagnification();
	    m_renderLoop.resizeDisplay();
	    refreshDisplay();

	    // Java3D has a bug/feature in which updating the depth-cueing
	    // distances often doesn't take effect until the *SECOND* display
	    // refresh after the change.  Thus the need for this second call
	    // here.
	    refreshDisplay();
	}

	public void decreaseMagnification()
	{
	    forceIdleState();
	    m_parameters.decreaseMagnification();
	    m_renderLoop.resizeDisplay();
	    refreshDisplay();
	    refreshDisplay();  // See comments at increaseMagnification();
	}

	public void resetMagnification()
	{
	    forceIdleState();
	    m_parameters.resetMagnification();
	    m_renderLoop.resizeDisplay();
	    refreshDisplay();
	    refreshDisplay();  // See comments at increaseMagnification();
	}

	// MouseListener - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e)
	{
	    // XXX: Workaround for some AWT/Swing bug.
	    m_canvas.requestFocus();
	}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e)
	{
	    if (DEBUG_EVENT_HANDLING)
	    {
		dumpMouseEvent("mousePressed", e);
	    }

	    int x = e.getX();
	    int y = e.getY();
	    int modifiers = e.getModifiers();

	    switch (m_state)
	    {
	    case STATE_IDLE:
		if (checkModifiers(modifiers, InputEvent.BUTTON1_MASK))
		{
		    m_lastX = x;
		    m_lastY = y;

		    if (checkModifiers(modifiers, InputEvent.SHIFT_MASK))
		    {
			m_labelZOffsetCounter = 0;
			m_state = STATE_ROTATING_CONTINUOUS_START;
		    }
		    else if (checkModifiers(modifiers, InputEvent.CTRL_MASK))
		    {
			m_labelZOffsetCounter = 0;
			m_state = STATE_ROTATING_TRACKING;
			computeTrackingAngles(x, y);
			m_repeatingRequest.start();
			m_repeatingRequest.rotate(m_dxRadians, m_dyRadians);
			m_renderLoop.rotateDisplay(m_repeatingRequest);
		    }
		    else
		    {
			// We get here under two very different situations.
			// The user might have pressed the left mouse button
			// to start interactive rotations, or the user might
			// be simply clicking (pressing and immediately
			// releasing the button without an intervening drag)
			// in the window.  The user might do the latter, for
			// example, to bring the Walrus window to the front.
			//
			// The difference matters to us because we should
			// attempt to refresh the display only if we're idle.
			// By refreshing the display, I mean calling
			// m_renderLoop.refreshDisplay().  If we call this
			// method at any other time, such as when some type
			// of rotation is active, we could lock up the
			// display [because the AWT thread calling
			// refreshDisplay() would block waiting for the
			// renderer thread to stop rotating, while
			// the renderer thread would never stop rotating
			// until the AWT thread told it to stop, which leads
			// to a deadlock].
			//
			// For these reasons, we switch to the intermediate
			// state STATE_ROTATING_INTERACTIVE_START rather
			// than directly to STATE_ROTATING_INTERACTIVE so
			// that we can hold off on taking any action until
			// we get the subsequent AWT event, which will then
			// unambiguously determine our course.  Because we
			// have a named state (namely
			// STATE_ROTATING_INTERACTIVE_START) for this
			// ambiguous situation, which we know is safe with
			// respect to refreshDisplay(), this makes it
			// possible for us to easily determine at any time
			// whether it is safe to call refreshDisplay()--it
			// is safe if the state is either this or STATE_IDLE.
			m_labelZOffsetCounter = 0;
			m_state = STATE_ROTATING_INTERACTIVE_START;
		    }
		}
		else if (checkModifiers(modifiers, InputEvent.BUTTON2_MASK))
		{
		    if (checkModifiers(modifiers, InputEvent.SHIFT_MASK))
		    {
			int node = m_renderLoop.pickNode(x, y, m_center);
			if (node >= 0)
			{		
			    m_narrowingHandler.pruneSubtree(node);
			}
		    }
		    else if (checkModifiers(modifiers, InputEvent.CTRL_MASK))
		    {
			int node = m_renderLoop.pickNode(x, y, m_center);
			if (node >= 0)
			{		
			    m_narrowingHandler.narrowToSubtree(node);
			}
		    }
		    else
		    {
			m_state = STATE_DISPLAYING_ATTRIBUTES;
			displayAttributes(x, y);
		    }
		}
		else //if (checkModifiers(modifiers, InputEvent.BUTTON3_MASK))
		{
		    if (checkModifiers(modifiers, InputEvent.SHIFT_MASK))
		    {
			m_state = STATE_DISPLAYING_ATTRIBUTES;
			displayAttributes(x, y);
		    }
		    else if (checkModifiers(modifiers, InputEvent.CTRL_MASK))
		    {
			m_labelZOffsetCounter = 0;
		        m_renderLoop.translate(swapCenterNodes());
		    }
		    else
		    {
			m_labelZOffsetCounter = 0;
			translateDisplay(x, y);
		    }
		}
		break;

	    case STATE_ROTATING_CONTINUOUS:
		m_state = STATE_IDLE;
		m_repeatingRequest.end();
		break;

	    case STATE_ROTATING_TRACKING:
		m_state = STATE_IDLE;
		m_repeatingRequest.end();
		break;

	    case STATE_WOBBLING:
		m_state = STATE_IDLE;
		m_wobblingListener.cancelled();
		m_wobblingListener = null;
		m_wobblingRequest.end();
		break;

		// Under normal circumstances, a mousePressed event shouldn't
		// occur in STATE_DISPLAYING_ATTRIBUTES,
		// STATE_ROTATING_INTERACTIVE, or
		// STATE_ROTATING_CONTINUOUS_START.  Such an event has no
		// meaning.  However, the user can cause them nonetheless
		// by pressing down on unrelated mouse buttons while
		// the relavant button is being held down.  For example,
		// while the user is dragging with the middle mouse button,
		// s/he can click the other buttons to generate mousePressed
		// events.
		// 
		// My tendency is to handle this situation gracefully by
		// ignoring the events (both here and in mouseReleased()),
		// but the MouseEvent mechanism makes this difficult to do
		// without tracking changes in successive MouseEvent objects.
		// Specifically, examining a single MouseEvent doesn't reveal
		// which mouse buttons were released.  For example, the
		// following sequence of events is ambiguous:
		//
		//      mousePressed: 1--
		//      mousePressed: 1-3
		//      mouseReleased: 1-3
		//
		// Was the mouseReleased event for the release of button 1
		// or button 3, or even both buttons 1 and 3?  You can't tell.
		// You have to look at the next MouseEvent to see which
		// buttons were actually released.
		// 
		// Because of the difficulty, we don't bother trying to be
		// graceful.  Hence, the user can cause Walrus to enter
		// unanticipated states.

	    case STATE_DISPLAYING_ATTRIBUTES:
		//FALLTHROUGH
	    case STATE_ROTATING_INTERACTIVE:
		//FALLTHROUGH
	    case STATE_ROTATING_INTERACTIVE_START:
		//FALLTHROUGH
	    case STATE_ROTATING_CONTINUOUS_START:
		//FALLTHROUGH
		break;
	    default:
		throw new RuntimeException
		    ("Invalid state in EventHandler: mousePressed in state "
		     + m_state);
	    }
	}

	public void mouseReleased(MouseEvent e)
	{
	    if (DEBUG_EVENT_HANDLING)
	    {
		dumpMouseEvent("mouseReleased", e);
	    }

	    int modifiers = e.getModifiers();
	    switch (m_state)
	    {
	    case STATE_IDLE:
		//IGNORE
		break;

	    case STATE_DISPLAYING_ATTRIBUTES:
		m_state = STATE_IDLE;
		break;

	    case STATE_ROTATING_INTERACTIVE:
		m_state = STATE_IDLE;
		m_interactiveRequest.end();
		break;

	    case STATE_ROTATING_INTERACTIVE_START:
		// The user simply clicked the left mouse button in the
		// window without holding down any keys and without
		// dragging.  It could have been the start of an interactive
		// rotation, but it wasn't, so we simply return to idle.
		m_state = STATE_IDLE;
		break;

	    case STATE_ROTATING_CONTINUOUS:
		//IGNORE -- We ignore the mouseReleased event that matches
		// the mousePressed that initiated the continuous rotations.
		break;

	    case STATE_ROTATING_CONTINUOUS_START:
		m_state = STATE_IDLE;
		break;

	    case STATE_ROTATING_TRACKING:
		//IGNORE -- We ignore the mouseReleased event that matches
		// the mousePressed that initiated the tracking rotations.
		break;

	    case STATE_WOBBLING:
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
	    if (DEBUG_EVENT_HANDLING)
	    {
		dumpMouseEvent("mouseDragged", e);
	    }

	    int x = e.getX();
	    int y = e.getY();

	    switch (m_state)
	    {
	    case STATE_IDLE:
		//IGNORE
		break;

	    case STATE_DISPLAYING_ATTRIBUTES:
		displayAttributes(x, y);
		break;

	    case STATE_ROTATING_INTERACTIVE:
		computeDragAngles(x, y);
		m_interactiveRequest.rotate(m_dxRadians, m_dyRadians);
		break;

	    case STATE_ROTATING_INTERACTIVE_START:
		m_state = STATE_ROTATING_INTERACTIVE;

		// m_lastX and m_lastY were set in the STATE_IDLE case
		// of mousePressed().
		computeDragAngles(x, y);
		m_interactiveRequest.start();
		m_interactiveRequest.rotate(m_dxRadians, m_dyRadians);
		m_renderLoop.rotateDisplay(m_interactiveRequest);
		break;

	    case STATE_ROTATING_CONTINUOUS:
		//IGNORE -- The rotation vector is determined solely from
		// the first mouseDragged event in the gesture initiating
		// a continuous rotation.  However, this determining
		// mouseDragged event is caught in
		// STATE_ROTATING_CONTINUOUS_START below.
		break;

	    case STATE_ROTATING_CONTINUOUS_START:
		m_state = STATE_ROTATING_CONTINUOUS;
		computeDragAngles(x, y);
		m_repeatingRequest.start();
		m_repeatingRequest.rotate(m_dxRadians, m_dyRadians);
		m_renderLoop.rotateDisplay(m_repeatingRequest);
		break;

	    case STATE_ROTATING_TRACKING:
		//IGNORE -- We care only about mouseMoved events in this state.
		// However, a user may have entered this state by dragging
		// rather than simply clicking, so we should be prepared to
		// ignore the resulting mouseDragged events.
		break;

	    case STATE_WOBBLING:
		//FALLTHROUGH
	    default:
		throw new RuntimeException
		    ("Invalid state in EventHandler: mouseDragged in state "
		     + m_state);
	    }
	}

	public void mouseMoved(MouseEvent e)
	{
	    if (DEBUG_EVENT_HANDLING)
	    {
		dumpMouseEvent("mouseMoved", e);
	    }

	    switch (m_state)
	    {
	    case STATE_IDLE:
		//IGNORE
		break;

	    case STATE_DISPLAYING_ATTRIBUTES:
		//IGNORE
		break;

	    case STATE_ROTATING_INTERACTIVE:
		//IGNORE
		break;

	    case STATE_ROTATING_INTERACTIVE_START:
		//IGNORE
		break;

	    case STATE_ROTATING_CONTINUOUS:
		//IGNORE
		break;

	    case STATE_ROTATING_CONTINUOUS_START:
		//IGNORE
		break;

	    case STATE_ROTATING_TRACKING:
		computeTrackingAngles(e.getX(), e.getY());
		m_repeatingRequest.rotate(m_dxRadians, m_dyRadians);
		break;

	    case STATE_WOBBLING:
		//IGNORE
		break;

	    default:
		throw new RuntimeException
		    ("Invalid state in EventHandler: mouseMoved in state "
		     + m_state);
	    }
	}

	// KeyListener - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void keyPressed(KeyEvent e) {}
	public void keyReleased(KeyEvent e) {}

	// Supported key events:
	//
	//  CTRL-R: refresh display
	public void keyTyped(KeyEvent e)
	{
	    char c = e.getKeyChar();
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

		if (c == CTRL_R)
		{
		    if (m_state == STATE_IDLE
			|| m_state == STATE_ROTATING_INTERACTIVE_START)
		    {
			m_labelZOffsetCounter = 0;
			m_renderLoop.refreshDisplay();
		    }
		}
	    }
	    else
	    {
		if (m_state == STATE_IDLE
		    || m_state == STATE_ROTATING_INTERACTIVE_START)
		{
		    if (c == 'w')
		    {
			m_narrowingHandler.widenSubtree(m_currentNode);
		    }
		    else if (c == 'n')
		    {
			m_narrowingHandler.narrowToSubtree(m_currentNode);
		    }
		    else if (c == 'p')
		    {
			showParentNode();
		    }
		    else if (c >= '0' && c <= '9')
		    {
			int distance = c - '0';
			m_narrowingHandler.pruneToNeighborhood
			    (m_currentNode, distance);
		    }
		    else if (c == ',')
		    {
			decreaseMagnification();
		    }
		    else if (c == '.')
		    {
			increaseMagnification();
		    }
		    else if (c == '/')
		    {
			resetMagnification();
		    }
		}
	    }
	}

	//---------------------------------------------------------------

	private void translateDisplay(int x, int y)
	{
	    System.out.println("Picking ...");
	    int node = m_renderLoop.pickNode(x, y, m_center);
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

	private void displayAttributes(int x, int y)
	{
	    m_renderLoop.highlightNode(x, y);
	    if (m_nodeLabelAttributes.length > 0)
	    {
		int node = m_renderLoop.pickNode(x, y, m_center);
		if (node >= 0)
		{
		    String statusBarLabel = createStatusBarLabel(node);
		    m_statusBar.setText(statusBarLabel);

		    if (m_onScreenLabels)
		    {
			String onScreenLabel = createOnScreenLabel(node);
			displayOnScreenLabel(x, y, onScreenLabel);
		    }
		}
	    }
	}

	private void displayOnScreenLabel(int x, int y, String label)
	{
	    GraphicsContext3D gc = m_canvas.getGraphicsContext3D();
	    Point3d position = new Point3d();
	    m_canvas.getPixelLocationInImagePlate(x, y, position);
	    m_parameters.drawLabel
		(gc, position.x, position.y, m_labelZOffsetCounter++, label);
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

	private void computeDragAngles(int x, int y)
	{
	    int dx = (x - m_lastX) / MOUSE_SENSITIVITY;
	    int dy = (y - m_lastY) / MOUSE_SENSITIVITY;

	    m_lastX = x;
	    m_lastY = y;

	    m_dxRadians = transformDeltas(Math.toRadians(dx));
	    m_dyRadians = transformDeltas(Math.toRadians(dy));
	}

	private void computeTrackingAngles(int x, int y)
	{
	    double dx = (x - m_canvas.getWidth() / 2) / TRACKING_SCALE;
	    double dy = (y - m_canvas.getHeight() / 2) / TRACKING_SCALE;

	    m_dxRadians = Math.toRadians(dx);
	    m_dyRadians = Math.toRadians(dy);
	}

	private double transformDeltas(double x)
	{
	    // The following equation was empirically derived.
	    // It's an arctan translated into the upper-right (positive x
	    // and positive y) quadrant such that the bottom leg crosses
            // the origin (it looks like an oblique S in the upper-right
	    // quadrant).  The curve lies entirely underneath y=x.
	    //
	    // There were two goals: 1) make it possible to easily specify
	    // a small delta (which implies a small dragging angle), and
	    // 2) smoothly cap the maximum delta, since large values are
	    // useless, and intermediate values the most useful.

	    double t = Math.atan(10.0 * (Math.abs(x) - 0.3))
		/ ROTATION_SCALE + 0.1987918;

	    return (x >= 0.0 ? t : -t);
	}

	//---------------------------------------------------------------

	private String createStatusBarLabel(int node)
	{
	    int nodeID = m_graph.getNodeID(node);
	    String[] values = m_labelConstructor.extractValues(node, true);

	    StringBuffer buffer = new StringBuffer();
	    for (int i = 0; i < m_nodeLabelAttributes.length; i++)
	    {
		if (i > 0)
		{
		    buffer.append("  ");
		}

		buffer.append(m_nodeLabelAttributeNames[i]);
		buffer.append(": ");
		buffer.append(values[i]);
	    }
	    return buffer.toString();
	}

	private String createOnScreenLabel(int node)
	{
	    int nodeID = m_graph.getNodeID(node);
	    String[] values = m_labelConstructor.extractValues(node, false);

	    StringBuffer buffer = new StringBuffer();
	    for (int i = 0; i < m_nodeLabelAttributes.length; i++)
	    {
		if (i > 0)
		{
		    buffer.append("; ");
		}
		buffer.append(values[i]);
	    }
	    return buffer.toString();
	}

	private void dumpMouseEvent(String name, MouseEvent e)
	{
	    System.out.print(name + " in state " + m_state + ": ");

	    int modifiers = e.getModifiers();
	    System.out.print
		(checkModifiers(modifiers, InputEvent.BUTTON1_MASK)
		 ? "1" : "-");
	    System.out.print
		(checkModifiers(modifiers, InputEvent.BUTTON2_MASK)
		 ? "2" : "-");
	    System.out.print
		(checkModifiers(modifiers, InputEvent.BUTTON3_MASK)
		 ? "3" : "-");

	    System.out.print
		(checkModifiers(modifiers, InputEvent.SHIFT_MASK)
		 ? "S" : "-");
	    System.out.print
		(checkModifiers(modifiers, InputEvent.CTRL_MASK)
		 ? "C" : "-");

	    System.out.println();
	}

	//---------------------------------------------------------------

	private static final double ROTATION_SCALE = 2.0 * Math.PI;
	private static final double TRACKING_SCALE = 50.0;

	private static final int STATE_IDLE = 0;
	private static final int STATE_DISPLAYING_ATTRIBUTES = 1;
	private static final int STATE_ROTATING_INTERACTIVE = 2;
	private static final int STATE_ROTATING_INTERACTIVE_START = 3;
	private static final int STATE_ROTATING_CONTINUOUS = 4;
	private static final int STATE_ROTATING_CONTINUOUS_START = 5;
	private static final int STATE_ROTATING_TRACKING = 6;
	private static final int STATE_WOBBLING = 7;

	private static final char CTRL_R = 'r' - 'a' + 1;

	private static final int MOUSE_SENSITIVITY = 2;

	private int m_state;
	private H3ViewParameters m_parameters;
	private H3Canvas3D m_canvas;
	private H3RenderLoop m_renderLoop;
	private NarrowingEventHandler m_narrowingHandler;

	private int m_rootNode;
	private int m_currentNode;
	private int m_previousNode;

	private H3Graph m_graph;
	private Graph m_backingGraph;
	private int[] m_nodeLabelAttributes;
	private String[] m_nodeLabelAttributeNames;
	private JTextField m_statusBar;
	private boolean m_onScreenLabels;
	private NodeLabelConstructor m_labelConstructor;

	// This is a workaround for dealing with the transparency issues
	// associated with drawing successive labels.  Without this,
	// successive labels are drawn *behind* earlier labels.
	// See comments for H3ViewParameters.java:drawLabel().
	//
	// This should be reset to zero at the beginning (or end) of
	// each 'labelling session'.  Getting the timing perfect isn't
	// important; ensuring that it never grows too large (say > 200)
	// is all that matters.
	private int m_labelZOffsetCounter = 0;

	private Point2d m_center = new Point2d();

	private int m_lastX;
	private int m_lastY;
	private double m_dxRadians;
	private double m_dyRadians;

	private H3InteractiveRotationRequest m_interactiveRequest
	    = new H3InteractiveRotationRequest();

	private H3RepeatingRotationRequest m_repeatingRequest
	    = new H3RepeatingRotationRequest();

	private H3WobblingRotationRequest m_wobblingRequest
	    = new H3WobblingRotationRequest();

	private CancellationListener m_wobblingListener;

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
		if (m_state == STATE_IDLE
		    || m_state == STATE_ROTATING_INTERACTIVE_START)
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
		if (m_state == STATE_IDLE
		    || m_state == STATE_ROTATING_INTERACTIVE_START)
		{
		    m_renderLoop.resizeDisplay();
		}
	    }
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private static class NodeLabelConstructor
    {
	public NodeLabelConstructor
	    (Graph backingGraph, int[] nodeLabelAttributes)
	{
	    m_backingGraph = backingGraph;
	    m_nodeLabelAttributes = nodeLabelAttributes;
	}

	// The parameter {node} should be the ID of a node in the
	// backing libsea Graph.  It should not be the ID (index) of a node
	// in H3Graph.
	public String[] extractValues(int node, boolean quoteStrings)
	{
	    String[] retval = new String[m_nodeLabelAttributes.length];

	    for (int i = 0; i < m_nodeLabelAttributes.length; i++)
	    {
		StringBuffer buffer = new StringBuffer();

		try
		{
		    int attribute = m_nodeLabelAttributes[i];
		    ValueIterator iterator =
			m_backingGraph.getNodeAttribute(node, attribute);

		    boolean isListType = iterator.getType().isListType();
		    if (isListType)
		    {
			buffer.append('[');
		    }

		    int k = 0;
		    while (!iterator.atEnd())
		    {
			if (k++ > 0)
			{
			    buffer.append(", ");
			}
			addAttributeValue(buffer, iterator, quoteStrings);
			iterator.advance();
		    }

		    if (isListType)
		    {
			buffer.append(']');
		    }
		}
		catch (AttributeUnavailableException e)
		{
		    buffer.append("<<unavailable>>");
		}

		retval[i] = buffer.toString();
	    }

	    return retval;
	}

	private void addAttributeValue
	    (StringBuffer buffer, ValueIterator iterator,
	     boolean quoteStrings)
	{
	    switch (iterator.getType().getBaseType())
	    {
	    case ValueType._BOOLEAN:
		buffer.append(iterator.getBooleanValue() ? 'T' : 'F');
		break;

	    case ValueType._INTEGER:
		buffer.append(iterator.getIntegerValue());
		break;

	    case ValueType._FLOAT:
		buffer.append(iterator.getFloatValue());
		break;

	    case ValueType._DOUBLE:
		buffer.append(iterator.getDoubleValue());
		break;

	    case ValueType._STRING:
		{
		    String value = iterator.getStringValue();
		    if (quoteStrings)
		    {
			buffer.append('"');
			buffer.append(value);
			buffer.append('"');
		    }
		    else
		    {
			buffer.append(value);
		    }
		}
		break;

	    case ValueType._FLOAT3:
		iterator.getFloat3Value(m_float3LabelData);
		buffer.append('{');
		buffer.append(m_float3LabelData[0]);
		buffer.append(", ");
		buffer.append(m_float3LabelData[1]);
		buffer.append(", ");
		buffer.append(m_float3LabelData[2]);
		buffer.append('}');
		break;

	    case ValueType._DOUBLE3:
		iterator.getDouble3Value(m_double3LabelData);
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
		    int value = iterator.getEnumerationValue();
		    ReadOnlyEnumeratorIterator enumerator =
			m_backingGraph.getEnumerator(value);
		    buffer.append(enumerator.getName());
		}
		break;

	    default: throw new RuntimeException();
	    }
	}

	private Graph m_backingGraph;
	private int[] m_nodeLabelAttributes;
	private float[] m_float3LabelData = new float[3];
	private double[] m_double3LabelData = new double[3];
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

	// NOTE: Call this only after the menus are completely constructed,
	//       since we're taking advantage of the ActionListener plumbing
	//       already set up for the menus.
	public void enableDefaultColorScheme()
	{
	    if (m_predefinedColorSchemes.size() > 0)
	    {
		// XXX: We might want to preserve the default color scheme
		//      in the system properties.
		PredefinedColorScheme scheme =
		    (PredefinedColorScheme)m_predefinedColorSchemes.get(0);
		scheme.activate();
	    }
	}

	// NOTE: This may be called even before the menus are completely
	//       constructed.  However, it's perhaps safer to call this
	//       only after construction has finished.
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
		(maker.make("Yellow-Green-[Grey]",
			    "Yellow", ColorSchemeMaker.DISABLE_TRANSPARENCY,
			    "Green", ColorSchemeMaker.DISABLE_TRANSPARENCY,
			    "Grey", ColorSchemeMaker.ENABLE_TRANSPARENCY));
	    m_predefinedColorSchemes.add
		(maker.make("Green-Olive-[Summer Sky]",
			    "Green", ColorSchemeMaker.DISABLE_TRANSPARENCY,
			    "Olive", ColorSchemeMaker.DISABLE_TRANSPARENCY,
			    "Summer Sky",
			    ColorSchemeMaker.ENABLE_TRANSPARENCY));
	    m_predefinedColorSchemes.add
		(maker.make("Invisible-Olive-[Summer Sky]",
			    ColorSelection.INVISIBLE,
			    ColorSchemeMaker.IGNORE_TRANSPARENCY,
			    "Olive", ColorSchemeMaker.DISABLE_TRANSPARENCY,
			    "Summer Sky",
			    ColorSchemeMaker.ENABLE_TRANSPARENCY));
	    m_predefinedColorSchemes.add
		(maker.make("Beige-Gold-[Green]",
			    "Beige", ColorSchemeMaker.DISABLE_TRANSPARENCY,
			    "Gold", ColorSchemeMaker.DISABLE_TRANSPARENCY,
			    "Green",
			    ColorSchemeMaker.ENABLE_TRANSPARENCY));
	}

	private void createFixedColors()
	{
	    m_fixedColors = new ArrayList();
	    m_fixedColors.add
		(new FixedColor("Magenta", packRGB(199, 21, 133)));
	    m_fixedColors.add
		(new FixedColor("Red", packRGB(255, 0, 0)));
	    m_fixedColors.add
		(new FixedColor("Pink", packRGB(255, 48, 48)));
	    m_fixedColors.add
		(new FixedColor("Orange", packRGB(255, 140, 0)));
	    m_fixedColors.add
		(new FixedColor("Brown", packRGB(205, 127, 50)));
	    m_fixedColors.add
		(new FixedColor("Gold", packRGB(255, 215, 0)));
	    m_fixedColors.add
		(new FixedColor("Yellow", packRGB(255, 255, 0)));
	    m_fixedColors.add
		(new FixedColor("Beige", packRGB(255, 255, 160)));
	    m_fixedColors.add
		(new FixedColor("Olive", packRGB(202, 255, 112)));
	    m_fixedColors.add
		(new FixedColor("Green", packRGB(30, 150, 25)));
	    m_fixedColors.add
		(new FixedColor("Aquamarine", packRGB(112, 219, 147)));
	    m_fixedColors.add
		(new FixedColor("Summer Sky", packRGB(56, 176, 222)));
	    m_fixedColors.add
		(new FixedColor("Turquoise", packRGB(173, 234, 234)));
	    m_fixedColors.add
		(new FixedColor("Grey", packRGB(178, 178, 178)));
	    m_fixedColors.add
		(new FixedColor("White", packRGB(255, 225, 255)));
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
	    public PredefinedColorScheme
		(String name, JMenuItem nodeMenuItem,
		 JMenuItem nodeTransparencyMenuItem,
		 boolean nodeTransparency,
		 JMenuItem treeLinkMenuItem,
		 JMenuItem treeLinkTransparencyMenuItem,
		 boolean treeLinkTransparency,
		 JMenuItem nontreeLinkMenuItem,
		 JMenuItem nontreeLinkTransparencyMenuItem,
		 boolean nontreeLinkTransparency)
	    {
		this.name = name;
		this.nodeMenuItem = nodeMenuItem;
		this.nodeTransparencyMenuItem = nodeTransparencyMenuItem;
		this.nodeTransparency = nodeTransparency;
		this.treeLinkMenuItem = treeLinkMenuItem;
		this.treeLinkTransparencyMenuItem =
		    treeLinkTransparencyMenuItem;
		this.treeLinkTransparency = treeLinkTransparency;
		this.nontreeLinkMenuItem = nontreeLinkMenuItem;
		this.nontreeLinkTransparencyMenuItem =
		    nontreeLinkTransparencyMenuItem;
		this.nontreeLinkTransparency = nontreeLinkTransparency;
	    }

	    public void actionPerformed(ActionEvent e)
	    {
		activate();
	    }

	    public void activate()
	    {
		nodeMenuItem.doClick();
		if (nodeTransparencyMenuItem != null)
		{
		    nodeTransparencyMenuItem.setSelected(nodeTransparency);
		}

		treeLinkMenuItem.doClick();
		if (treeLinkTransparencyMenuItem != null)
		{
		    treeLinkTransparencyMenuItem.setSelected
			(treeLinkTransparency);
		}

		nontreeLinkMenuItem.doClick();
		if (nontreeLinkTransparencyMenuItem != null)
		{
		    nontreeLinkTransparencyMenuItem.setSelected
			(nontreeLinkTransparency);
		}
	    }

	    public String name;
	    public JMenuItem nodeMenuItem;
	    public JMenuItem nodeTransparencyMenuItem;
	    public boolean nodeTransparency;
	    public JMenuItem treeLinkMenuItem;
	    public JMenuItem treeLinkTransparencyMenuItem;
	    public boolean treeLinkTransparency;
	    public JMenuItem nontreeLinkMenuItem;
	    public JMenuItem nontreeLinkTransparencyMenuItem;
	    public boolean nontreeLinkTransparency;
	}

	////////////////////////////////////////////////////////////////////

	private class ColorSchemeMaker
	{
	    public static final int IGNORE_TRANSPARENCY = 0;
	    public static final int ENABLE_TRANSPARENCY = 1;
	    public static final int DISABLE_TRANSPARENCY = 2;

	    public ColorSchemeMaker
		(Map nodeMenuMap, Map treeLinkMenuMap, Map nontreeLinkMenuMap)
	    {
		m_nodeMenuMap = nodeMenuMap;
		m_treeLinkMenuMap = treeLinkMenuMap;
		m_nontreeLinkMenuMap = nontreeLinkMenuMap;

		m_nodeTransparencyMenuItem =
		    findColor(nodeMenuMap, ColorSelection.TRANSPARENT);
		m_treeLinkTransparencyMenuItem =
		    findColor(treeLinkMenuMap, ColorSelection.TRANSPARENT);
		m_nontreeLinkTransparencyMenuItem =
		    findColor(nontreeLinkMenuMap, ColorSelection.TRANSPARENT);
	    }

	    public PredefinedColorScheme make
		(String name, String nodeColor, int nodeTransparencyType,
		 String treeLinkColor, int treeLinkTransparencyType,
		 String nontreeLinkColor, int nontreeLinkTransparencyType)
	    {
		JMenuItem nodeMenuItem = findColor(m_nodeMenuMap, nodeColor);
		JMenuItem treeLinkMenuItem =
		    findColor(m_treeLinkMenuMap, treeLinkColor);
		JMenuItem nontreeLinkMenuItem =
		    findColor(m_nontreeLinkMenuMap, nontreeLinkColor);

		JMenuItem nodeTransparencyMenuItem =
		    (nodeTransparencyType == IGNORE_TRANSPARENCY
		     ? null : m_nodeTransparencyMenuItem);
		JMenuItem treeLinkTransparencyMenuItem =
		    (treeLinkTransparencyType == IGNORE_TRANSPARENCY
		     ? null : m_treeLinkTransparencyMenuItem);
		JMenuItem nontreeLinkTransparencyMenuItem =
		    (nontreeLinkTransparencyType == IGNORE_TRANSPARENCY
		     ? null : m_nontreeLinkTransparencyMenuItem);

		boolean nodeTransparency =
		    (nodeTransparencyType == ENABLE_TRANSPARENCY);
		boolean treeLinkTransparency =
		    (treeLinkTransparencyType == ENABLE_TRANSPARENCY);
		boolean nontreeLinkTransparency =
		    (nontreeLinkTransparencyType == ENABLE_TRANSPARENCY);

		return new PredefinedColorScheme
		    (name, nodeMenuItem, nodeTransparencyMenuItem,
		     nodeTransparency, treeLinkMenuItem,
		     treeLinkTransparencyMenuItem, treeLinkTransparency,
		     nontreeLinkMenuItem, nontreeLinkTransparencyMenuItem,
		     nontreeLinkTransparency);
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

	    private JMenuItem m_nodeTransparencyMenuItem;
	    private JMenuItem m_treeLinkTransparencyMenuItem;
	    private JMenuItem m_nontreeLinkTransparencyMenuItem;
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
	// Compile out the HUE color choice for now.
	private static final boolean COLOR_SCALES = false;

	////////////////////////////////////////////////////////////////////
	// PUBLIC CONSTANTS
	////////////////////////////////////////////////////////////////////

	public static final String TRANSPARENT = "Transparent";
	public static final String INVISIBLE = "Invisible";
	public static final String HUE = "Hue";
	public static final String RGB = "RGB";

	////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	////////////////////////////////////////////////////////////////////

	// List<FixedColor> fixedColors
	public ColorSelection(JMenu menu, Map map, List fixedColors)
	{
	    // NOTE: In the following, we arbitrarily choose the invisible
	    //       color to be the default selection.  As a result, all
	    //       menu items excluded by this choice are manually disabled
	    //       in the following initialization.

	    m_transparentMenuItem = new JCheckBoxMenuItem(TRANSPARENT);
	    m_transparentMenuItem.setEnabled(false);
	    m_transparentMenuItem.setMnemonic(KeyEvent.VK_T);

	    m_invisibleMenuItem = new JRadioButtonMenuItem(INVISIBLE);
	    m_invisibleMenuItem.setMnemonic(KeyEvent.VK_I);
	    m_invisibleMenuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e)
		    {
			handleInvisibleColorRequest();
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
	    if (COLOR_SCALES)
	    {
	    m_colorSchemeButtonGroup.add(m_hueMenuItem);
	    }
	    m_colorSchemeButtonGroup.add(m_RGBMenuItem);

	    putChecked(map, TRANSPARENT, m_transparentMenuItem);
	    putChecked(map, INVISIBLE, m_invisibleMenuItem);
	    if (COLOR_SCALES)
	    {
	    putChecked(map, HUE, m_hueMenuItem);
	    }
	    putChecked(map, RGB, m_RGBMenuItem);

	    menu.add(m_transparentMenuItem);
	    menu.addSeparator();
	    menu.add(m_invisibleMenuItem);
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

	// Set the color selection to one that is independent of the content
	// of a graph.  One of the fixed colors or the invisible choice will
	// be selected.
	// 
	// This step is necessary after unloading a graph so that the color
	// selection doesn't refer to nonexistent attribute data.
	public void enableReasonableSelection()
	{
	    if (m_hueMenuItem.isSelected() || m_RGBMenuItem.isSelected())
	    {
		m_defaultSelection.setSelected(true);
		updateSelectedFixedColorIndex(m_defaultSelection);
		updateMenuInterdependencies();
	    }
	}

	public ColorConfiguration createColorConfigurationSnapshot()
	{
	    ColorConfiguration retval = new ColorConfiguration();

	    retval.isTransparent = m_transparentMenuItem.isSelected();

	    if (m_invisibleMenuItem.isSelected())
	    {
		retval.scheme = ColorConfiguration.INVISIBLE;
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
	    m_transparentMenuItem.setEnabled(false);
	    m_colorAttributeMenu.setEnabled(false);
	    m_selectionAttributeMenu.setEnabled(false);
	}

	private void setupForFixedColorChoice()
	{
	    m_transparentMenuItem.setEnabled(true);
	    m_colorAttributeMenu.setEnabled(false);
	    if (m_selectionAttributeMenu.getItemCount() > 0)
	    {
		m_selectionAttributeMenu.setEnabled(true);
	    }
	}

	private void setupForArbitraryColorChoice()
	{
	    m_transparentMenuItem.setEnabled(false);
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

	// The color menu items aren't independent of each other.  Thus,
	// when some of the items change, others are affected.  The purpose
	// of this method is to make the menus consistent with each other
	// after some prior change.  It does so by enabling or disabling,
	// or by selecting or de-selecting, any affected menu items.
	//
	// The most common initiating change is the addition or removal
	// of the various menus listing the available color attributes
	// in a loaded graph.  This typically happens when a graph is
	// loaded or unloaded.  However, other lesser changes can initiate
	// this method, which indeed makes no assumptions about the cause.
	//
	// Currently, there are three types of inconsistencies that need
	// to be addressed.  The first concerns the color attribute menus,
	// such as m_scalarColorAttributeMenus and m_RGBMenuItem.  These
	// should be enabled or selected if, and only if, there are suitable
	// attributes in the loaded graph.  For example, if there are no
	// attributes that could possibly hold RGB values, then m_RGBMenuItem
	// should be disabled.  The second type of inconsistency concerns
	// the selection attribute menu.  This menu should be enabled if,
	// and only if, there are suitable attributes, and the current
	// color choice is not "invisible."  The final consistency concerns
	// the transparency checkbox.  This checkbox is only applicable when
	// one of the fixed colors is selected.
	private void updateMenuInterdependencies()
	{
	    boolean enableColorAttributes = false;

	    m_hueMenuItem.setEnabled(m_scalarColorAttributeMenus != null);
	    if (m_hueMenuItem.isEnabled() && m_hueMenuItem.isSelected())
	    {
		enableColorAttributes = true;
		installScalarAttributeMenu();
	    }

	    m_RGBMenuItem.setEnabled(m_RGBColorAttributeMenus != null);
	    if (m_RGBMenuItem.isEnabled() && m_RGBMenuItem.isSelected())
	    {
		enableColorAttributes = true;
		installRGBAttributeMenu();
	    }

	    m_colorAttributeMenu.setEnabled(enableColorAttributes);

	    if ((m_hueMenuItem.isSelected() && !m_hueMenuItem.isEnabled())
		|| (m_RGBMenuItem.isSelected() && !m_RGBMenuItem.isEnabled()))
	    {
		m_defaultSelection.setSelected(true);		
	    }

	    boolean enableSelection = 
		m_selectionAttributeMenu.getItemCount() > 0
		&& !m_invisibleMenuItem.isSelected();
	    m_selectionAttributeMenu.setEnabled(enableSelection);

	    boolean enableTransparency =
		!m_invisibleMenuItem.isSelected()
		&& !m_hueMenuItem.isSelected()
		&& !m_RGBMenuItem.isSelected();
	    m_transparentMenuItem.setEnabled(enableTransparency);
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
	private JCheckBoxMenuItem m_transparentMenuItem;
	private JRadioButtonMenuItem m_invisibleMenuItem;
	private JRadioButtonMenuItem m_hueMenuItem;
	private JRadioButtonMenuItem m_RGBMenuItem;
	private JMenu m_colorAttributeMenu;
	private JMenu m_selectionAttributeMenu;
	private ButtonGroup m_selectionAttributeButtonGroup;

	// This is the menu item which will be in the selected state when
	// no menu item has yet been selected by the user or when the
	// currently selected menu item must be disabled (as a result of
	// removing all color attributes, for example).  This must point
	// to a menu item for a fixed color or the invisible choice.
	// By doing so, we ensure that this menu item is selectable in
        // all situations (such as just after a graph is unloaded).
	private JMenuItem m_defaultSelection;

	// The following ButtonGroup and JRadioButtonMenuItem instances
	// only serve as a cache (or as a backing store).  Depending on
	// the current menu selection, either m_scalarColorAttributeMenus or
	// m_RGBColorAttributeMenus will be installed in m_colorAttributeMenu
	// (above).  For example, if m_RGBMenuItem is selected, then the
	// latter set of menu items will be so installed.  If m_hueMenuItem
	// is selected, then the former set is installed.
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
	public boolean onScreenLabels;
	public boolean automaticRefresh;
	public boolean automaticExtendedPrecision;

	public ColorConfiguration nodeColor;
	public ColorConfiguration treeLinkColor;
	public ColorConfiguration nontreeLinkColor;

	public int[] nodeLabelAttributes;
	public String[] nodeLabelAttributeNames;

	public void print()
	{
	    System.out.println("------------------------------------------\n");
	    System.out.println("RenderingConfiguration:");
	    System.out.println("\tspanningTree = " + spanningTree);
	    System.out.println("\tadaptiveRendering = " + adaptiveRendering);
	    System.out.println("\tmultipleNodeSizes = " + multipleNodeSizes);
	    System.out.println("\tdepthCueing = " + depthCueing);
	    System.out.println("\taxes = " + axes);
	    System.out.println("\tonScreenLabels = " + onScreenLabels);
	    System.out.println("\tautomaticRefresh = " + automaticRefresh);
	    System.out.println("\tautomaticExtendedPrecision = "
			       + automaticExtendedPrecision);

	    System.out.print("(Node) ");
	    nodeColor.print();
	    System.out.print("(Tree Link) ");
	    treeLinkColor.print();
	    System.out.print("(Nontree Link) ");
	    nontreeLinkColor.print();

	    System.out.println("Node Label Attributes:");
	    for (int i = 0; i < nodeLabelAttributes.length; i++)
	    {
		System.out.println("\t" + nodeLabelAttributeNames[i]
				   + " (" + nodeLabelAttributes[i] + ")");
	    }
	    System.out.println("------------------------------------------\n");
	}
    }

    private static class ColorConfiguration
    {
	public static final int INVISIBLE = 0;
	public static final int FIXED_COLOR = 1;
	public static final int HUE = 2;
	public static final int RGB = 3;

	public int scheme;
	public int fixedColor;
	public boolean isTransparent;  // Only applies to FIXED_COLOR.
	public String colorAttribute;
	public String selectionAttribute;

	public boolean equalColoring(ColorConfiguration rhs)
	{
	    boolean retval = (rhs.scheme == scheme);
	    if (retval)
	    {
		if (scheme == FIXED_COLOR)
		{
		    retval = (rhs.fixedColor == fixedColor
			      && rhs.isTransparent == isTransparent);
		}
		else if (scheme == HUE || scheme == RGB)
		{
		    retval = (rhs.colorAttribute.equals(colorAttribute));
		}
	    }

	    if (retval)
	    {
		boolean hasLHS = (selectionAttribute != null);
		boolean hasRHS = (rhs.selectionAttribute != null);
		if (hasLHS && hasRHS)
		{
		    retval =
			(rhs.selectionAttribute.equals(selectionAttribute));
		}
		else
		{
		    retval = (!hasLHS && !hasRHS);
		}
	    }

	    return retval;
	}

	public void print()
	{
	    System.out.println("ColorConfiguration:");
	    System.out.println("\tscheme = " + getSchemeName());
	    System.out.println("\tfixedColor = " + fixedColor);
	    System.out.println("\tisTransparent = " + isTransparent);
	    System.out.println("\tcolorAttribute = " + colorAttribute);
	    System.out.println("\tselectionAttribute = " + selectionAttribute);
	}

	private String getSchemeName()
	{
	    return m_schemeNames[scheme];
	}

	private static final String[] m_schemeNames = {
	    "INVISIBLE", "FIXED_COLOR", "HUE", "RGB"
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

    ///////////////////////////////////////////////////////////////////////

    private interface GraphVisitor
    {
	void visit(int node, int level);
    }

    private class GraphTraversal
    {
	public GraphTraversal(H3Graph graph, GraphVisitor visitor)
	{
	    m_graph = graph;
	    m_visitor = visitor;
	}

	public void traverse()
	{
	    traverseNode(m_graph.getRootNode(), 0);
	}

	private void traverseNode(int node, int level)
	{
	    m_visitor.visit(node, level);

	    int start = m_graph.getNodeChildIndex(node);
	    int nontreeStart = m_graph.getNodeNontreeIndex(node);
    
	    for (int i = start; i < nontreeStart; i++)
	    {
		int child = m_graph.getLinkDestination(i);
		traverseNode(child, level + 1);
	    }
	}

	private H3Graph m_graph;
	private GraphVisitor m_visitor;
    }
}

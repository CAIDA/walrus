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


import java.io.*;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.caida.libsea.*;

public class H3GraphLoader
{
    ///////////////////////////////////////////////////////////////////////
    // PUBLIC INTERFACES
    ///////////////////////////////////////////////////////////////////////

    public interface AttributeTypeMatcher
    {
	boolean match(ValueType type);
    }

    ////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////////////////

    public H3GraphLoader() {}

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    ////////////////////////////////////////////////////////////////////////

    public H3Graph load(Graph graph, String spanningTree)
	throws InvalidGraphDataException
    {
	int numNodes = graph.getNumNodes();
	int numLinks = graph.getNumLinks();

	H3Graph retval = new H3Graph(numNodes, numLinks);

	IDMap map = populateNodeIDs(retval, graph);
	findSpanningTreeQualifierAttributes(graph, spanningTree);
	int rootID = findSpanningTreeRootNodeID(graph, m_rootAttribute);
	retval.setRootNode(map.map(rootID));
	populateLinks(retval, graph, map, m_treeLinkAttribute);

	return retval;
    }

    // Returns List<String>.
    public List loadSpanningTreeQualifiers(Graph graph)
    {
	List retval = new ArrayList();

	QualifierIterator iterator =
	    graph.getQualifiersByType(SPANNING_TREE_QUALIFIER);
	while (!iterator.atEnd())
	{
	    retval.add(iterator.getName());
	    iterator.advance();
	}

	Collections.sort(retval);
	return retval;
    }

    // Returns List<String>.
    public List loadAttributes(Graph graph, AttributeTypeMatcher matcher)
    {
	List retval = new ArrayList();

	AttributeDefinitionIterator iterator = graph.getAttributeDefinitions();
	while (!iterator.atEnd())
	{
	    if (matcher.match(iterator.getType()))
	    {
		retval.add(iterator.getName());
	    }
	    iterator.advance();
	}

	Collections.sort(retval);
	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    ////////////////////////////////////////////////////////////////////////

    private void findSpanningTreeQualifierAttributes
	(Graph graph, String spanningTree)
	throws InvalidGraphDataException
    {
	QualifierIterator qualifierIterator = graph.getQualifier(spanningTree);
	if (qualifierIterator.atEnd())
	{
	    String msg =
		"spanning tree qualifier `" + spanningTree + "' not found";
	    throw new IllegalArgumentException(msg);
	}

	boolean foundRootAttribute = false;
	boolean foundTreeLinkAttribute = false;

	QualifierAttributeIterator qualifierAttributeIterator =
	    qualifierIterator.getAttributes();
	while (!qualifierAttributeIterator.atEnd())
	{
	    String name = qualifierAttributeIterator.getName();
	    if (name.equals(ROOT_ATTRIBUTE))
	    {
		foundRootAttribute = true;
		m_rootAttribute =
		    qualifierAttributeIterator.getAttributeID();
		checkAttributeType
		    (graph, SPANNING_TREE_QUALIFIER, ROOT_ATTRIBUTE,
		     m_rootAttribute, ValueType.BOOLEAN);
	    }
	    else if (name.equals(TREE_LINK_ATTRIBUTE))
	    {
		foundTreeLinkAttribute = true;
		m_treeLinkAttribute =
		    qualifierAttributeIterator.getAttributeID();
		checkAttributeType
		    (graph, SPANNING_TREE_QUALIFIER, TREE_LINK_ATTRIBUTE,
		     m_treeLinkAttribute, ValueType.BOOLEAN);
	    }

	    qualifierAttributeIterator.advance();
	}

	if (!foundRootAttribute)
	{
	    String msg = "missing attribute `" + ROOT_ATTRIBUTE
		+ "' of qualifier type `" + SPANNING_TREE_QUALIFIER + "'";
	    throw new InvalidGraphDataException(msg);
	}

	if (!foundTreeLinkAttribute)
	{
	    String msg = "missing attribute `" + TREE_LINK_ATTRIBUTE
		+ "' of qualifier type `" + SPANNING_TREE_QUALIFIER + "'";
	    throw new InvalidGraphDataException(msg);
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private int findSpanningTreeRootNodeID(Graph graph, int attribute)
	throws InvalidGraphDataException
    {
	AttributesByAttributeIterator iterator =
	    graph.getAttributeDefinition(attribute).getNodeAttributes();
	while (!iterator.atEnd()
	       && !iterator.getAttributeValues().getBooleanValue())
	{
	    iterator.advance();
	}

	if (iterator.atEnd())
	{
	    String msg = "no root node found for spanning tree";
	    throw new InvalidGraphDataException(msg);
	}

	return iterator.getObjectID();
    }

    ///////////////////////////////////////////////////////////////////////

    private void checkAttributeType
	(Graph graph, String qualifierType, String attributeName,
	 int attribute, ValueType type)
	throws InvalidGraphDataException
    {
	AttributeDefinitionIterator iterator =
	    graph.getAttributeDefinition(attribute);
	if (iterator.getType() != type)
	{
	    String msg = "attribute `" + attributeName
		+ "' of qualifier type `" + qualifierType
		+ "' must have type " + type.getName()
		+ "; found " + iterator.getType().getName();
	    throw new InvalidGraphDataException(msg);
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private void populateLinks(H3Graph retval, Graph graph, IDMap map,
			       int treeLinkAttribute)
    {
	BitSet treeLinksMap = createTreeLinksMap(graph, treeLinkAttribute);

	NodeIterator nodeIterator = graph.getNodes();
	while (!nodeIterator.atEnd())
	{
	    int node = map.map(nodeIterator.getObjectID());
	    retval.startChildLinks(node);
	    {
		LinkIterator linkIterator =
		    nodeIterator.getOutgoingLinks();
		while (!linkIterator.atEnd())
		{
		    int link = linkIterator.getObjectID();
		    if (treeLinksMap.get(link))
		    {
			int destination =
			    map.map(linkIterator.getDestination());
			retval.addChildLink(node, destination, link);
		    }
		    linkIterator.advance();
		}
	    }
	    retval.startNontreeLinks(node);
	    {
		LinkIterator linkIterator =
		    nodeIterator.getOutgoingLinks();
		while (!linkIterator.atEnd())
		{
		    int link = linkIterator.getObjectID();
		    if (!treeLinksMap.get(link))
		    {
			int destination =
			    map.map(linkIterator.getDestination());
			retval.addNontreeLink(node, destination, link);
		    }
		    linkIterator.advance();
		}
	    }
	    retval.endNodeLinks(node);

	    nodeIterator.advance();
	}
    }

    ///////////////////////////////////////////////////////////////////////

    private BitSet createTreeLinksMap(Graph graph, int treeLinkAttribute)
    {
	BitSet retval = new BitSet(graph.getLinkIDRange());

	AttributesByAttributeIterator iterator =
	    graph.getAttributeDefinition(treeLinkAttribute)
	    .getLinkAttributes();
	while (!iterator.atEnd())
	{
	    if (iterator.getAttributeValues().getBooleanValue())
	    {
		retval.set(iterator.getObjectID());
	    }
	    iterator.advance();
	}

	return retval;
    }
    
    ///////////////////////////////////////////////////////////////////////

    private IDMap populateNodeIDs(H3Graph retval, Graph graph)
    {
	int[] mapping = extractSortedNodeIDs(graph);
	for (int i = 0; i < mapping.length; i++)
	{
	    retval.setNodeID(i, mapping[i]);
	}
	return new IDMap(mapping);
    }

    private int[] extractSortedNodeIDs(Graph graph)
    {
	int numNodes = graph.getNumNodes();
	int[] retval = new int[numNodes];

	boolean isSorted = true;
	int previousID = -1; // Must be less than all valid IDs.

	int i = 0;
	NodeIterator iterator = graph.getNodes();
	while (!iterator.atEnd())
	{
	    int id = iterator.getObjectID();
	    retval[i++] = id;
	    isSorted = isSorted && (previousID <= id);
	    previousID = id;

	    iterator.advance();
	}

	if (!isSorted)
	{
	    Arrays.sort(retval);
	}

	return retval;
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE FIELDS
    ////////////////////////////////////////////////////////////////////////

    private static final boolean DEBUG_PRINT = true;

    private static final String SPANNING_TREE_QUALIFIER = "spanning_tree";
    private static final String ROOT_ATTRIBUTE = "root";
    private static final String TREE_LINK_ATTRIBUTE = "tree_link";

    private int m_rootAttribute;
    private int m_treeLinkAttribute;

    ////////////////////////////////////////////////////////////////////////
    // PUBLIC CLASSES
    ////////////////////////////////////////////////////////////////////////

    public static class InvalidGraphDataException extends Exception
    {
	public InvalidGraphDataException()
	{
	    super();
	}

	public InvalidGraphDataException(String s)
	{
	    super(s);
	}
    }

    ////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES
    ////////////////////////////////////////////////////////////////////////

    private static class IDMap
    {
	public IDMap(int[] mapping)
	{
	    m_mapping = mapping;
	}

	public int map(int id)
	{
	    int retval = Arrays.binarySearch(m_mapping, id);
	    if (retval < 0)
	    {
		String msg = "id[" + id + "] not found";
		throw new RuntimeException(msg);
	    }
	    return retval;
	}

	private int[] m_mapping;
    }
}

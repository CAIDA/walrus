# The LibSea Graph File Format and Java Graph Library

There are many graph file formats and graph libraries available today. However, most are proprietary, ad-hoc, limited in expressiveness, too verbose, or lacking in scalability. LibSea is both a file format and a Java library for representing large directed graphs on disk and in memory. Scalability to graphs with as many as one million nodes has been the primary goal. Additional goals have been expressiveness, compactness, and support for application-specific conventions and policies.

## Source Code 

The source code to LibSea is available under the [GNU Lesser GPL](http://www.gnu.org/copyleft/lesser.html).

## About the LibSea Graph File Format

The LibSea file format allows you to

*   specify the topology of directed graphs using nodes, links, and paths (paths are sequences of links)
*   attach data to nodes, links, and paths in a flexible manner
*   implement application-specific conventions and semantics

Users can attach data to topological elements (nodes, links, and paths) using _attributes_. A file may contain any number of attributes, and each attribute may have one of sixteen types. The scalar types are boolean, integer, float, double, string, triples of floats ('float3'), triples of doubles ('double3'), and user-defined enumerations. The remaining supported types are lists of the scalar types. Lists of lists are not supported, and all elements in a list must have the same type. Attributes may have default values, and the user can explicitly override the default value on a per object basis.

LibSea graph files can also contain _qualifiers_. Qualifiers group together attributes and provide a general mechanism for specifying application-specific information. Qualifiers have a type (an arbitrary user-defined identifier such as "spanning_tree"), a name, and a list of constituent attributes. Unlike attribute types (boolean, integer, etc.), qualifier types are not defined by LibSea itself. It is up to the applications making use of LibSea graph files to dictate what qualifier types are valid, what they mean, and what attributes should be supplied for a given qualifier. This intentional under specification allows applications to implement their conventions and semantics on top of the basic LibSea graph format without having to create a new format.

For example, there is no direct method of specifying a spanning tree in a LibSea graph file, but this can be easily accomplished with a qualifier. We might define the qualifier type to be "spanning_tree" and require that the qualifier contain two boolean attributes, named "root" and "tree_link". The "root" attribute is true for the node that is the root of the spanning tree and false for all other nodes. The "tree_link" attribute is true for the links that are part of the spanning tree and false for all others. An application that knows about this "spanning_tree" qualifier can search for it in a LibSea graph file while applications that do not know about it can simply ignore it. The separation of qualifier types from qualifier names permits the inclusion of multiple instances of a qualifier in a single file, such as multiple spanning trees in the case of the "spanning_tree" qualifier. In the special case of a qualifier type that does not require any attributes, the presence of a qualifier itself may communicate all the information. For example, the qualifier types "acyclic", "connected", and "minimal" could act as descriptions of the graph contained in a file, and an application may look for (any) instances of these qualifiers to learn _a priori_ that the contained graph has these properties.

## LibSea Java Graph Library

It is relatively easy to design a data structure that can compactly represent the _topology_ of graphs, even graphs with millions of nodes. However, for many problems, there will be large amounts of data associated with the nodes and links that must also be accessible. Designing a data structure that can represent this additional data in a flexible, compact, and scalable manner is challenging. The LibSea Java graph library was designed from the ground up to meet exactly these goals. In contrast, ad-hoc data structures, designed for simplicity over all else, will typically

*   have a poorly designed API for traversing, accessing, or modifying graph topology and associated data
*   support a severly limited number of data types, perhaps just strings
*   miss opportunities for optimization: optimizations based on data type, such as using bit vectors to represent boolean values, and optimizations based on the sparsity of data, such as using default values or alternative low-level structures with different space-time tradeoffs

The LibSea Java graph library

*   parses LibSea graph files
*   provides data structures for representing large directed graphs with many attributes compactly
*   provides a clean, type-safe API for traversing graphs and for accessing or modifying attributes

## Applicability

Please note that LibSea currently has the following requirements, restrictions, or limitations which may render it unsuitable for a given problem domain or dataset:

*   Only _directed_ links are supported. Thus, undirected graphs may be inconvenient or impractical to deal with.
*   Although the LibSea Java library was designed to handle large graphs, the graphs themselves must fit entirely in memory. In contrast, database management systems can provide access to a dataset that is many times larger than available physical memory.
*   The LibSea Java library uses an adjacency list representation of graphs. This representation is not suitable for certain algorithms (e.g., shortest path) that require a matrix representation for optimal running time.
*   The LibSea Java library does not provide graph algorithms of any kind. Graph analysis, transformation, layout, and display are outside the scope of the library.

## Implemented Features (Java library)

*   nodes, links, and paths
*   scalar and list attributes of all sixteen types
*   optimizations based on data type and sparsity of data
*   mutable and immutable attributes
*   immutable graphs (topology is immutable, permitting the use of a very compact internal representation)

## Requirements

The LibSea Java library requires _[JDK](http://java.sun.com/j2se)_ and was tested with JDK 1.2 to 1.5.

## Acknowledgments

Support for this work was provided by the NSF CAIDA and Internet Atlas grants (ANI-9711092 and ANI-9996248), the DARPA NGI and NMS programs (N66001-98-2-8922 and N66001-01-1-8909), and CAIDA members.


import argparse

links = []
tree_link_attributes = []
total_links = 0

def indent(line, indents):
  """
  Indents the given string by a multiple of two spaces.

  Args:
    line (str): The string that must be indented.
    indents (int): The number of times the string should be indented.
  """
  count = 0
  indented = ""
  while count < indents:
    indented +=  "  "
    count += 1
  return indented + line

def comment(message):
  """
  Creates a comment with the # symbol and the given message.
  
  Args:
    message (str): The message that needs to be formatted as a comment.

  Returns:
    The message enclosed in triple pound signs and spaces ending with a
    newline character.
  """
  return "### " + message + " ###\n"

def formatCodeString(phrase):
  """
  Creates a string enclosed in double quotes and ends it with a semi-colon.

  Args:
    phrase (str): The string that needs to be surrounded by ".

  Returns:
    The phrase enclosed in double quotes ending with a semi-colon and then
    a newline character.
  """
  return "\"" + phrase + "\";\n"

def writeEmptyLine(file):
  """
  Writes an empty line to the given file.

  Args:
    file (file object): The file that requires an empty line.
  """
  file.write("\n")

def writeAttribute(file, name, type, default, nodeValues, linkValues,
    pathValues, isLast):
  """
  Writes an attribute in the correct format for the attribute data section.

  Args:
    file (File): The .graph file being generated for the graph.
    name (str): The name of the attribute.
    type (str): The type of the attribute.
    default (str): The default value of the attribute if not explicitly
                   assigned.
    nodeValues (str): Details values for specific nodes in the graph, or ";" if
                      no nodes are explicitly assigned a value.
    linkValues (str): Details values for specific links in the graph, or ";" if
                      no links are explicitly assigned a value.
    pathValues (str): Details values for specific paths in the graph, or ";" if
                      no paths are explicitly assigned a value.
    isLast (bool): True if this will be the last attribute listed, false
                   otherwise.
  """
  file.write( indent( "{\n", 2 ) )
  file.write( indent( "%s;\n" % (name), 3 ) )
  file.write( indent( "%s;\n" % (type), 3 ) )
  file.write( indent( "%s;\n" % (default), 3 ) )
  
  if nodeValues == ";":
    file.write( indent( "%s\n" % nodeValues, 3 ) )
  else:
    file.write( indent( "[\n", 3 ) )
    file.write( "%s\n" % nodeValues )
    file.write( indent( "];\n", 3 ) )

  if linkValues == ";":
    file.write( indent( "%s\n" % linkValues, 3 ) )
  else:
    file.write( indent( "[\n", 3 ) )
    file.write( "%s\n" % linkValues )
    file.write( indent( "];\n", 3 ) )

  if pathValues == ";":
    file.write( indent( "%s\n" % pathValues, 3 ) )
  else:
    file.write( indent( "[\n", 3 ) )
    file.write( "%s\n" % pathValues )
    file.write( indent( "];\n", 3 ) )

  if isLast:
    file.write( indent( "}\n", 2 ) )
  else:
    file.write( indent( "},\n", 2 ) )

def writeQualifier(file, type, name, description, attributes, isLast):
  """
  Writes a qualifier in the correct format for the attribute data section.

  Args:
    file (File): The .graph file being generated for the graph.
    name (str): The name of the qualifier.
    type (str): The type of the qualifier.
    description (str): A description of the qualifier.
    attributes (str): The attributes of the qualifier.
    isLast (bool): True if this will be the last attribute listed, false
                   otherwise.
  """
  file.write( indent( "{\n", 2 ) )
  file.write( indent( "%s;\n" % (type), 3 ) )
  file.write( indent( "%s;\n" % (name), 3 ) )
  file.write( indent( "%s;\n" % (description), 3 ) )
  file.write( indent( "[\n", 3 ) )
  file.write( "%s\n" % attributes )
  file.write( indent( "];\n", 3 ) )

  if isLast:
    file.write( indent( "}\n", 2 ) )
  else:
    file.write( indent( "},\n", 2 ) )

def sortedInsert(list, insertion, is_list_of_tuples, low, high):
  """
  Inserts the given value in the list while maintaining ascending order with
  ASNs.

  Args:
    list (List): The list of elements to search through.
    insertion (int): The autonomous system tuple or autonomous system number
                     to be inserted.
    is_list_of_tuples (bool): True if list is a list of tuple elements, false
                              otherwise.
    low (int): The lowest index of the list that search might be located.
    high (int): The highest index of the list that search might be located.
  """
  if len(list) == 0:
    return list.append(insertion)

  search = low + (high - low) / 2

  if is_list_of_tuples:
    value_comparison = list[search][0]
    prev_elem_value_comparison = list[search - 1][0]
    insert_value = insertion[0]
  else:
    value_comparison = list[search]
    prev_elem_value_comparison = list[search - 1]
    insert_value = insertion

  if value_comparison == insert_value:
    return
  elif value_comparison > insert_value and search == 0:
    return list.insert(search, insertion)
  elif value_comparison > insert_value and \
      prev_elem_value_comparison < insert_value:
    return list.insert(search, insertion)
  elif value_comparison > insert_value:
    return sortedInsert(list, insertion, is_list_of_tuples, low, search - 1)
  elif value_comparison < insert_value and search == (len(list) - 1):
    return list.append(insertion)
  else:
    return sortedInsert(list, insertion, is_list_of_tuples, search + 1, high)

def find(list, asn, is_list_of_tuples, low, high):
  """
  Finds the index of the given ASN.

  Args:
    list (List): The list of elements to search through.
    asn (int): The autonomous system number to be looked for.
    is_list_of_tuples (bool): True if list is a list of tuple elements, false
                              otherwise.
    low (int): The lowest index of the list that search might be located.
    high (int): The highest index of the list that search might be located.

  Returns:
    The index of the given ASN in the list, or -1 if not found.
  """
  if low <= high:
    search = low + (high - low) / 2

    if is_list_of_tuples:
      asn_comparison = list[search][0]
    else:
      asn_comparison = list[search]

    if asn_comparison == asn:
      return search
    elif asn_comparison > asn:
      return find(list, asn, is_list_of_tuples, low, search - 1)
    else:
      return find(list, asn, is_list_of_tuples, search + 1, high)
  else:
    return -1

def insertRelationship(autonomous_systems, rel_type, asn1, asn2):
  """
  Inserts an ASN into an Autonomous System's list of providers or list of
  siblings.

  Args:
    autonomous_systems (List): The list containing all autonomous systems.
    rel_type (int): -1 if there is a provider-customer relationship between
                    the first AS and the second AS, 1 if there is a customer-
                    provider relationship, 0 if there is a sibling-sibling
                    relationship.
    asn1 (int): The Autonomous System Number of the first autonomous system
                in the relationship.
    asn2 (int): The Autonomous System Number of the second autonomous system
                in the relationship.
  """
  # insert the first autonomous system in the list of providers for the
  # second autonomous system if they possess a provider-customer
  # relationship
  if rel_type == -1:
    insertProvider(autonomous_systems, asn1, asn2)

  # insert the second autonomous system in the list of providers for the
  # first autonomous system if they possess a customer-provider
  # relationship
  if rel_type == 1:
    insertProvider(autonomous_systems, asn2, asn1)

  # insert the first autonomous system in the list of siblings for the
  # second autonomous system listed if they possess a sibling-sibling
  # relationship
  if rel_type == 0:
    insertSibling(autonomous_systems, asn1, asn2)

def insertProvider(autonomous_systems, provider, customer):
  """
  Inserts an ASN into an Autonomous System's list of providers.

  Args:
    autonomous_systems (List): The list containing all autonomous systems.
    provider (int): The ASN of the provider.
    customer (int): The ASN of the customer.
  """
  as_index = find(autonomous_systems, customer, True, 0,
      len(autonomous_systems) - 1)
  as_providers = autonomous_systems[as_index][1]
  sortedInsert(as_providers, provider, False, 0, len(as_providers) - 1)

def insertSibling(autonomous_systems, sibling1, sibling2):
  """
  Inserts an ASN into an Autonomous System's list of siblings.

  Args:
    autonomous_systems (List): The list containing all autonomous systems.
    sibling1 (int): The ASN of an Autonomous System in a sibling-sibling
                    relationship.
    sibling2 (int): The ASN of an Autonomous System in a sibling-sibling
                    relationship, assumed to be different from sibling1.
  """
  as_index = find(autonomous_systems, sibling2, True, 0,
        len(autonomous_systems) - 1)
  as_siblings = autonomous_systems[as_index][2]
  sortedInsert(as_siblings, sibling1, False, 0, len(as_siblings) - 1)

def parseRelationships(file):
  """
  Creates a list of all relationships for all Autonomous Systems from
  the given file.

  Args:
    file (File): The file containing the provider-customer, customer-
                 provider, and sibling-sibling relationships for
                 Autonomous Systems.

  Returns:
    A list of tuples where each tuple contains three elements: the ASN
    of an Autonomous System, a list of that Autonomous System's providers,
    and a list of that Autonomous System's siblings.
  """
  autonomous_systems = []

  rel_lines = file.readlines()
  for i in range(0, len(rel_lines)):
    if rel_lines[i].find("#") == -1:
      rel = rel_lines[i].strip().split("|")
      asn1 = int(rel[0])
      asn2 = int(rel[1])
      rel_type = int(rel[2])

      rel_tuple = (asn1, [], [])
      sortedInsert(autonomous_systems, rel_tuple, True, 0,
          len(autonomous_systems) - 1)

      rel_tuple = (asn2, [], [])
      sortedInsert(autonomous_systems, rel_tuple, True, 0,
          len(autonomous_systems) - 1)

      insertRelationship(autonomous_systems, rel_type, asn1, asn2)

  return autonomous_systems

def parseCustomerCones(file, autonomous_systems):
  """
  Adds relationships to the given list of Autonomous Systems from parsing
  the given file.

  Args:
    file (File): The file containing the customer cones of all members of
                 the clique.
    autonomous_systems (List): The list containing all autonomous systems.
  """
  cone_lines = file.readlines()
  for i in range(0, len(cone_lines)):
    if cone_lines[i].find("#") == -1:
      cone = cone_lines[i].strip()
      #print cone

def addLinks(autonomous_systems):
  """
  From the relationships provided for each autonomous system, strings
  representing their links and attributes are added to the respective
  lists.

  Args:
    autonomous_systems (List): The list containing all autonomous systems
                               and their providers and siblings.
  """
  global links
  global tree_link_attributes
  global total_links

  # each autonomous system is mapped to its index plus one
  for i in range(0, len(autonomous_systems)):
    if len(autonomous_systems[i][1]) == 0:
      links.append( indent( "{ 0; %d; }," % (i + 1), 2 ) )
      total_links += 1
    for prov in range(0, len(autonomous_systems[i][1])):
      links.append( indent( "{ %d; %d; }," % (find(autonomous_systems,
            autonomous_systems[i][1][prov], True, 0,
            len(autonomous_systems) - 1), i + 1), 2 ) )
      tree_link_attributes.append( indent ( "{ %d; T; }," % (total_links),
            4 ) )
      total_links += 1
    for sib in range(0, len(autonomous_systems[i][2])):
      links.append( indent( "{ %d; %d; }," % (find(autonomous_systems,
            autonomous_systems[i][2][sib], True, 0,
            len(autonomous_systems) - 1), i + 1), 2 ) )
      total_links += 1

  # format the last link string
  last_link = links[len(links) - 1]
  links[len(links) - 1] = last_link[0:(len(last_link) - 1)] + "\n"

  # format the last tree link attribute string
  last_tla = tree_link_attributes[len(tree_link_attributes) - 1]
  tree_link_attributes[len(tree_link_attributes) - 1] = \
    last_tla[0:(len(last_tla) - 1)]

def writeMetadataSection(file, c_args, num_nodes, num_links, num_paths,
    num_path_links):
  """
  Writes the basic information of the graph to the file.

  Args:
    file (file object): The .graph file being generated for the graph.
    c_args (argparse.Namespace object): The command line arguments.
    num_nodes (int): The amount of nodes in the graph.
    num_links (int): The amount of links in the graph.
    num_paths (int): The amount of paths in the graph.
    num_path_links (int): The amount of path links in the graph.
  """
  file.write( indent( comment("metadata"), 1 ) );

  graph_name = ";\n"
  if c_args.n:
    graph_name = formatCodeString(c_args.n)
  file.write( indent( graph_name, 1 ) );

  graph_description = ";\n"
  if c_args.d:
    graph_description = formatCodeString(c_args.d)
  file.write( indent( graph_description, 1 ) )

  # add one node for the ghost node that acts as the root
  file.write( indent( "%d;\n" % (num_nodes + 1), 1) )
  file.write( indent( "%d;\n" % num_links, 1) )
  file.write( indent( "%d;\n" % num_paths, 1) )
  file.write( indent( "%d;\n" % num_path_links, 1) )

def writeStructuralDataSection(file):
  """
  Writes the information about the links and paths to the file.

  Args:
    file (file object): The .graph file being generated for the graph.
  """
  global links

  file.write( indent( comment("structural data"), 1 ) )
  file.write( indent( "[\n", 1 ) )
 
  file.write("\n".join(links))
  file.write( indent("];\n", 1) )
  file.write( indent( ";\n", 1 ) )
  

def writeAttributeDataSection(file):
  """
  Writes the information about the attributes of the nodes, links, and paths
  in the graph and the qualifiers to the file.

  Args:
    file (File): The .graph file being generated for the graph.
  """
  global tree_link_attributes

  file.write( indent( comment("attribute data"), 1 ) )
  file.write( indent( ";\n", 1 ) )
  file.write( indent( "[\n", 1 ) )

  writeAttribute( file, "$root", "bool", "|| false ||",
      indent( "{ 0; T; }", 4 ), ";", ";", False )

  writeAttribute( file, "$tree_link", "bool", "|| false ||", ";",
      "\n".join(tree_link_attributes), ";", True )

  file.write( indent( "];\n", 1 ) )
  file.write( indent( "[\n", 1 ) )

  attributes = indent( "{ 0; $root; },\n        { 1; $tree_link; }", 4 )
  writeQualifier( file, "$spanning_tree", "$main_spanning_tree", "",
      attributes, True )

  file.write( indent ( "];\n", 1 ) )
  
def main():
  """
  Generates the .graph file for the autonomous systems graph.
  """
  global links

  parser = argparse.ArgumentParser()
  parser.add_argument("-g",
      help="Desired name of the .graph file that will be generated",
      metavar="graphFileName")
  parser.add_argument("-n",
      help="Desired name to title the graph",
      metavar="graphName")
  parser.add_argument("-d",
      help="Description of the graph",
      metavar="graphDescription")
  parser.add_argument("-r",
      help="File containing relationships between autonomous systems",
      metavar="relFile", required=True)
  parser.add_argument("-c",
      help="File containing information about the customer cones",
      metavar="conesFile", required=True)
  args = parser.parse_args()

  graph_file_name = "autonomousSystems.graph"

  if args.g:
    graph_file_name = args.g + ".graph"

  graph_file = open(graph_file_name, "w")
  graph_file.write("Graph\n")
  graph_file.write("{\n")

  # parse files for information about Autonomous Systems
  relationships_file = open(args.r, "r")
  cones_file = open(args.c, "r")

  autonomous_systems = parseRelationships(relationships_file)
  parseCustomerCones(cones_file, autonomous_systems)
  addLinks(autonomous_systems)

  relationships_file.close()
  cones_file.close()

  writeMetadataSection(graph_file, args, len(autonomous_systems), len(links), 0,
      0)
  writeEmptyLine(graph_file)
  writeStructuralDataSection(graph_file)
  writeEmptyLine(graph_file)
  writeAttributeDataSection(graph_file)
  writeEmptyLine(graph_file)

  graph_file.write( indent( comment( "visualization hints" ), 1 ) )
  graph_file.write( "  ;\n  ;\n  ;\n  ;\n" )
  writeEmptyLine(graph_file)
  
  graph_file.write( indent( comment( "interface hints" ), 1 ) )
  graph_file.write( "  ;\n  ;\n  ;\n  ;\n  ;\n" )

  graph_file.write("}") 

  graph_file.close()

if __name__ == "__main__":
  main()

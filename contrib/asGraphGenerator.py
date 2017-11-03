import argparse
import copy
import random

clique = []
links = []
tree_link_attributes = []
node_asn_attributes = []
specified_labels = []
label_attributes = []
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

def format_code_string(phrase):
  """
  Creates a string enclosed in double quotes and ends it with a semi-colon.

  Args:
    phrase (str): The string that needs to be surrounded by ".

  Returns:
    The phrase enclosed in double quotes ending with a semi-colon and then
    a newline character.
  """
  return "\"" + phrase + "\";\n"

def write_empty_line(file):
  """
  Writes an empty line to the given file.

  Args:
    file (file object): The file that requires an empty line.
  """
  file.write("\n")

def write_attribute(file, name, type, default, nodeValues, linkValues,
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

def write_qualifier(file, type, name, description, attributes, isLast):
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

def sorted_insert(list, insertion, is_list_of_tuples, low, high):
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
    return sorted_insert(list, insertion, is_list_of_tuples, low, search - 1)
  elif value_comparison < insert_value and search == (len(list) - 1):
    return list.append(insertion)
  else:
    return sorted_insert(list, insertion, is_list_of_tuples, search + 1, high)

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

def insert_relationship(autonomous_systems, rel_type, asn1, asn2):
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
    insert_provider(autonomous_systems, asn1, asn2)

  # insert the second autonomous system in the list of providers for the
  # first autonomous system if they possess a customer-provider
  # relationship
  if rel_type == 1:
    insert_provider(autonomous_systems, asn2, asn1)

  # insert the first autonomous system in the list of siblings for the
  # second autonomous system listed if they possess a sibling-sibling
  # relationship
  #if rel_type == 0:
    #insert_sibling(autonomous_systems, asn1, asn2)

def insert_provider(autonomous_systems, provider, customer):
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
  sorted_insert(as_providers, provider, False, 0, len(as_providers) - 1)

def insert_sibling(autonomous_systems, sibling1, sibling2):
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
  sorted_insert(as_siblings, sibling1, False, 0, len(as_siblings) - 1)

def parse_labels(file_lines):
  """
  Parses the desired labels for certain autonomous systems from the file.

  Args:
    file_lines (List): The lines of the file containing labels for specific
                       autonomous system numbers.

  Returns:
    The desired attribute name for the labels if specified in the files.
  """
  global specified_labels
  label_name = ""

  for i in range(0, len(file_lines)):
    if file_lines[i].find("# name:") > -1:
      desired_name_start_index = file_lines[i].find(":")
      desired_name_start_index += 2
      label_name = file_lines[i][desired_name_start_index:].strip()
    elif file_lines[i].find("#") == -1:
      label = file_lines[i].strip().split("|")
      label[0] = int(label[0])
      specified_labels.append(label)

  return label_name

def parse_clique(file_lines):
  """
  Searches the given file for the members of the clique.

  Args:
    file_lines (List): The lines of the file containing information about the
                       clique.
  """
  global clique

  info_lines = file_lines
  for i in range(0, len(info_lines)):
    if info_lines[i].find("# c1:") > -1:
      clique_listing_start_index = info_lines[i].find(":")
      # shift index over to the start of the first member of the clique
      clique_listing_start_index += 2
      clique = info_lines[i][clique_listing_start_index:].strip().split(" ")
      # turn each element of clique into the type int
      for j in range(0, len(clique)):
        clique[j] = int(clique[j])
      return

def parse_relationships(file_lines):
  """
  Creates a list of all relationships for all Autonomous Systems from
  the given file.

  Args:
    file_lines (List): The lines of the file containing the provider-customer,
                 customer-provider, and sibling-sibling relationships for
                 Autonomous Systems.

  Returns:
    A list of tuples where each tuple contains three elements: the ASN
    of an Autonomous System, a list of that Autonomous System's providers,
    and a list of that Autonomous System's siblings.
  """
  autonomous_systems = []

  rel_lines = file_lines
  for i in range(0, len(rel_lines)):
    if rel_lines[i].find("#") == -1:
      rel = rel_lines[i].strip().split("|")
      asn1 = int(rel[0])
      asn2 = int(rel[1])
      rel_type = int(rel[2])

      rel_tuple = (asn1, [], [])
      sorted_insert(autonomous_systems, rel_tuple, True, 0,
          len(autonomous_systems) - 1)

      rel_tuple = (asn2, [], [])
      sorted_insert(autonomous_systems, rel_tuple, True, 0,
          len(autonomous_systems) - 1)

      insert_relationship(autonomous_systems, rel_type, asn1, asn2)

  return autonomous_systems

def quicksort(unsorted_list, low, high):
  """
  Sorts all elements in the given list to be in ascending order.

  Args:
    unsorted_list (List): The list needed to be sorted.
    low (int): The lowest index of the list being sorted.
    high (int): The highest index of the list being sorted.
  """
  if low < high:
    split = partition(unsorted_list, low, high)
    quicksort(unsorted_list, low, split - 1)
    quicksort(unsorted_list, split + 1, high)

def partition(unsorted_list, low, high):
  """
  Sorts the list of lists according to a selected pivot, using the first element
  of a list that is an element of the given unsorted list as a point of
  comparison.

  Args:
    unsorted_list (List of Lists): The list of lists needed to be sorted.
    low (int): The lowest index of the list being sorted.
    high (int): The highest index of the list being sorted.
  """
  random_index = random.randint(low, high)
  pivot = unsorted_list[random_index][0]

  # swap the element containing the pivot with the last element
  unsorted_list[random_index], unsorted_list[high] = \
      unsorted_list[high], unsorted_list[random_index]

  i = low - 1
  # place all elements of unsorted_list with first elements less than the
  # pivot before i + 1
  for j in range(low, high):
    if unsorted_list[j][0] < pivot:
      i += 1
      unsorted_list[i], unsorted_list[j]  = unsorted_list[j], unsorted_list[i]

  # swap the element containing the pivot to the index where the first elements
  # of all previous elements are less than the pivot
  unsorted_list[i + 1], unsorted_list[high] = unsorted_list[high], \
     unsorted_list[i + 1]
  return i + 1

def topological_sort(autonomous_systems, cone_lines):
  """
  Sorts all given Autonomous Systems into a list ordered so that no customer
  is before its provider.

  Args:
    autonomous_systems (List): The list containing all autonomous systems and
                               their providers and siblings.
    cone_lines (List): The list of lines from the file containing information
                       about each autonomous system's customer cone.

  Returns:
    A list of ASNs that are ordered so no exists at a lower index than its
    provider.
  """
  global clique

  sorted = []
  no_providers = copy.deepcopy(clique)
  autonomous_system_info = copy.deepcopy(autonomous_systems)
  customer_cones = copy.deepcopy(cone_lines)

  # filter customer_cone_lines so that it only contains lines related to the
  # customer cone of an autonomous system
  k = 0
  while k < len(customer_cones):
    if customer_cones[k].find("#") > -1:
      customer_cones.pop(k)
    else:
      customer_cones[k] = customer_cones[k].strip().split(" ")
      # convert each asn in each customer cone into type int
      for l in range(0, len(customer_cones[k])):
        customer_cones[k][l] = int(customer_cones[k][l])
      k += 1

  # sort the customer cones so the indices match the autonomous system info list
  quicksort(customer_cones, 0, len(customer_cones) - 1)

  # append autonomous systems to sorted in topological order
  while len(no_providers) > 0:
    no_provs_asn = no_providers.pop(0)
    sorted.append(no_provs_asn)
    no_provs_index = find(autonomous_system_info, no_provs_asn, True,
        0, len(autonomous_system_info) - 1)
    no_provs_cone = customer_cones[no_provs_index]
    # remove this provider from each of its customer's provider list
    for i in range(1, len(no_provs_cone)):
      current_as_index = find(autonomous_system_info, no_provs_cone[i], True,
          0, len(autonomous_system_info) - 1)
      providers_list = autonomous_system_info[current_as_index][1]
      if find(providers_list, no_provs_asn, False, 0,
          len(providers_list) - 1) > -1:
        providers_list.remove(no_provs_asn)
        if len(providers_list) == 0:
          no_providers.append(autonomous_system_info[current_as_index][0])

  return sorted


def add_links_and_attributes(top_sorted_asns, autonomous_systems):
  """
  From the relationships provided for each autonomous system, strings
  representing their links and attributes are added to the respective
  lists.

  Args:
    top_sorted_asns (List): The list containing all autonomous systems
                            sorted so that no customer appears before its
                            provider.
    autonomous_systems (List): The list containing all autonomous systems
                               and information about their providers and
                               siblings for each one.
  """
  global links
  global tree_link_attributes
  global total_links
  global node_asn_attributes
  global specified_labels
  global label_attributes

  # each autonomous system is mapped to a node number equal to its index plus
  # one
  for i in range(len(top_sorted_asns) - 1, -1, -1):
    if top_sorted_asns[i] in clique:
      links.append( indent( "{ 0; %d; }," % (i + 1), 2 ) )
      tree_link_attributes.append( indent( "{ %d; T; }," % (total_links), 4 ) )
      total_links += 1

    else:
      as_index = find(autonomous_systems, top_sorted_asns[i], True, 0,
          len(autonomous_systems) - 1)
      provider_list = autonomous_systems[as_index][1]
      # find the first provider with greatest depth to link together
      for j in range(i - 1, -1, -1):
        if find(provider_list, top_sorted_asns[j], False, 0,
            len(provider_list) - 1) > -1:
          links.append( indent( "{ %d; %d; }," % (j + 1, i + 1), 2 ) )
          tree_link_attributes.append( indent( "{ %d; T; }," % (total_links),
                4 ) )
          total_links += 1
          break

    # provide node label for autonomous system number represented
    node_asn_attributes.append( indent( "{ %d; %d; }," % (i + 1,
        top_sorted_asns[i]), 4 ) )

    # add specified labels as attributes for certain nodes
    for j in range(0, len(specified_labels)):
      if top_sorted_asns[i] == specified_labels[j][0]:
        label_attributes.append( indent( "{ %d; \"%s\"; }," % (i + 1,
            specified_labels[j][1]), 4 ) )

  # format the last link string
  last_link = links[len(links) - 1]
  links[len(links) - 1] = last_link[0:(len(last_link) - 1)] + "\n"

  # format the last tree link attribute string
  last_tla = tree_link_attributes[len(tree_link_attributes) - 1]
  tree_link_attributes[len(tree_link_attributes) - 1] = \
      last_tla[0:(len(last_tla) - 1)]

  # format the last node asn attribute string
  last_node_asn = node_asn_attributes[len(node_asn_attributes) - 1]
  node_asn_attributes[len(node_asn_attributes) - 1] = \
      last_node_asn[0:(len(last_node_asn) - 1)]

  # format the last label attribute string
  if len(label_attributes) > 0:
    last_label = label_attributes[len(label_attributes) - 1]
    label_attributes[len(label_attributes) - 1] = \
        last_label[0:(len(last_label) - 1)]

def write_metadata_section(file, c_args, num_nodes, num_links, num_paths,
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
    graph_name = format_code_string(c_args.n)
  file.write( indent( graph_name, 1 ) );

  graph_description = ";\n"
  if c_args.d:
    graph_description = format_code_string(c_args.d)
  file.write( indent( graph_description, 1 ) )

  # add one node for the ghost node that acts as the root
  file.write( indent( "%d;\n" % (num_nodes + 1), 1) )
  file.write( indent( "%d;\n" % num_links, 1) )
  file.write( indent( "%d;\n" % num_paths, 1) )
  file.write( indent( "%d;\n" % num_path_links, 1) )

def write_structural_data_section(file):
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
  

def write_attribute_data_section(file, label_name):
  """
  Writes the information about the attributes of the nodes, links, and paths
  in the graph and the qualifiers to the file.

  Args:
    file (File): The .graph file being generated for the graph.
    label_name (str): The desired name of the specified labels
  """
  global tree_link_attributes
  global node_asn_attributes
  global label_attributes

  file.write( indent( comment("attribute data"), 1 ) )
  file.write( indent( ";\n", 1 ) )
  file.write( indent( "[\n", 1 ) )

  write_attribute( file, "$root", "bool", "|| false ||",
      indent( "{ 0; T; }", 4 ), ";", ";", False )

  write_attribute( file, "$tree_link", "bool", "|| false ||", ";",
      "\n".join(tree_link_attributes), ";", False )

  # check if any labels were specified
  if len(label_attributes) > 0:
    write_attribute( file, "$asn", "int", "|| 0 ||",
        "\n".join(node_asn_attributes), ";", ";", False )

    write_attribute( file, "$" + label_name, "string", "|| \"\" ||",
        "\n".join(label_attributes), ";", ";", True )
  else:
    write_attribute( file, "$asn", "int", "|| 0 ||",
        "\n".join(node_asn_attributes), ";", ";", True )

  file.write( indent( "];\n", 1 ) )
  file.write( indent( "[\n", 1 ) )

  attributes = indent( "{ 0; $root; },\n        { 1; $tree_link; }", 4 )
  write_qualifier( file, "$spanning_tree", "$main_spanning_tree", "",
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
  parser.add_argument("-l",
      help="File containing desired labels for certain autonomous systems",
      metavar="asLabelFile")
  args = parser.parse_args()

  graph_file_name = "autonomousSystems.graph"

  if args.g:
    graph_file_name = args.g + ".graph"

  graph_file = open(graph_file_name, "w")
  graph_file.write("Graph\n")
  graph_file.write("{\n")

  label_name = ""

  if args.l:
    labels_file = open(args.l, "r")
    label_lines = labels_file.readlines()
    label_name = parse_labels(label_lines)
    if label_name == "":
      label_name = "label"
    labels_file.close()

  relationships_file = open(args.r, "r")
  info_lines = relationships_file.readlines()

  # parse relationships file for information about Autonomous Systems
  parse_clique(info_lines)
  autonomous_systems = parse_relationships(info_lines)

  cones_file = open(args.c, "r")
  cone_lines = cones_file.readlines()

  top_sorted_asns = topological_sort(autonomous_systems, cone_lines)
  add_links_and_attributes(top_sorted_asns, autonomous_systems)

  relationships_file.close()
  cones_file.close()

  write_metadata_section(graph_file, args, len(top_sorted_asns), len(links), 0,
      0)
  write_empty_line(graph_file)
  write_structural_data_section(graph_file)
  write_empty_line(graph_file)
  write_attribute_data_section(graph_file, label_name)
  write_empty_line(graph_file)

  graph_file.write( indent( comment( "visualization hints" ), 1 ) )
  graph_file.write( "  ;\n  ;\n  ;\n  ;\n" )
  write_empty_line(graph_file)
  
  graph_file.write( indent( comment( "interface hints" ), 1 ) )
  graph_file.write( "  ;\n  ;\n  ;\n  ;\n  ;\n" )

  graph_file.write("}") 

  graph_file.close()

if __name__ == "__main__":
  main()

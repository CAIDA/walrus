import argparse

def indent(line):
  """
  Indents the given string by two spaces.

  Args:
    line (str): The string that must be indented.
  """
  return "  " + line

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

def writeMetadataSection(file, c_args):
  """
  Writes the basic information of the graph to the file.

  Args:
    file (file object): The .graph file being generated for the graph.
    c_args (argparse.Namespace object): The command line arguments.
  """
  file.write( indent( comment("metadata") ) );

  graph_name = ";\n"
  if c_args.n:
    graph_name = formatCodeString(c_args.n)
  file.write( indent( graph_name ) );

  graph_description = ";\n"
  if c_args.d:
    graph_description = formatCodeString(c_args.d)
  file.write( indent( graph_description ) )

def writeStructuralDataSection(file, c_args):
  """
  Writes the information about the links and paths to the file.

  Args:
    file (file object): The .graph file being generated for the graph.
    c_args (argparse.Namespace object): The command line arguments.
  """
  file.write( indent( comment("structural data") ) )

  relationships_file = open(c_args.r, "r")
  clique_file = open(c_args.c, "r")

  autonomous_systems = []

  rel_lines = relationships_file.readlines()
  for i in range (0, len(rel_lines)):
    if rel_lines[i].find("#") == -1:
      rel = rel_lines[i].strip().split("|")
      asn1 = int(rel[0])
      asn2 = int(rel[1])

      rel_tuple = (asn1, [], [])
      sortedInsert(autonomous_systems, rel_tuple, True, 0,
          len(autonomous_systems) - 1)

      rel_tuple = (asn2, [], [])
      sortedInsert(autonomous_systems, rel_tuple, True, 0,
          len(autonomous_systems) - 1)

      # insert the first autonomous system in the list of providers for the
      # second autonomous system if they possess a provider-customer
      # relationship
      if rel[2] == '-1':
        as_index = find(autonomous_systems, asn2, True, 0,
            len(autonomous_systems) - 1)
        as_providers = autonomous_systems[as_index][1]
        sortedInsert(as_providers, asn1, False, 0, len(as_providers) - 1)

      # insert the second autonomous system in the list of providers for the
      # first autonomous system if they possess a customer-provider
      # relationship
      if rel[2] == '1':
        as_index = find(autonomous_systems, asn1, True, 0,
            len(autonomous_systems) - 1)
        as_providers = autonomous_systems[as_index][1]
        sortedInsert(as_providers, asn2, False, 0, len(as_providers) - 1)

      # insert the first autonomous system in the list of siblings for the
      # second autonomos system listed if they possess a sibling-sibling
      # relationship
      if rel[2] == '0':
        as_index = find(autonomous_systems, asn2, True, 0,
            len(autonomous_systems) - 1)
        as_siblings = autonomous_systems[as_index][2]
        sortedInsert(as_siblings, asn1, False, 0, len(as_siblings) - 1)

  for i in range(0, len(autonomous_systems)):
    print autonomous_systems[i]

def main():
  """
  Generates the .graph file for the autonomous systems graph.
  """
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
      help="File containing information about the members of the clique",
      metavar="cliqueFile", required=True)
  args = parser.parse_args()

  graph_file_name = "autonomousSystems.graph"

  if args.g:
    graph_file_name = args.g + ".graph"

  graph_file = open(graph_file_name, "w")
  graph_file.write("Graph\n")
  graph_file.write("{\n")

  writeMetadataSection(graph_file, args)
  writeEmptyLine(graph_file)
  writeStructuralDataSection(graph_file, args)
  
  graph_file.close()

if __name__ == "__main__":
  main()

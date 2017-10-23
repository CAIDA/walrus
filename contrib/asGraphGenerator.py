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
  """
  return "### " + message + " ###\n"

def formatCodeString(phrase):
  """
  Creates a string enclosed in double quotes and ends it with a semi-colon.

  Args:
    phrase (str): The string that needs to be surrounded by ".
  """
  return "\"" + phrase + "\";\n"


def writeMetadataSection(file, c_args):
  """
  Writes the basic information of the graph to the file.

  Args:
    file (file object): The .graph file being generated for the graph
    c_args (argparse.Namespace object): The command line arguments
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
  args = parser.parse_args()

  graph_file_name = "autonomousSystems.graph"

  if args.g:
    graph_file_name = args.g + ".graph"

  graph_file = open(graph_file_name, "w")
  graph_file.write("Graph\n")
  graph_file.write("{\n")

  writeMetadataSection(graph_file, args)
  
  graph_file.close()

if __name__ == "__main__":
  main()

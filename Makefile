#############################################################################
# The Walrus Graph Visualization Tool.
#############################################################################

# There are no configuration options in this file.
# See Makefile.nodep for configuration options and the real makefile rules.
#
# The only purpose of the additional lines in this file is to automatically
# generate makefile dependency files (named *.u) with jikes.  These dependency
# files help Make determine which additional source files need to be recompiled
# whenever some source file changes.
#
# You may get warnings like the following the first time you run Make on this
# file, but the warnings can be ignored:
#
#    Makefile:11: H3AdaptivePicker.u: No such file or directory

include Makefile.nodep

%.u: %.java
	$(JIKES) $(JIKES_FLAGS) $(JIKES_DEPENDENCY_FLAGS) $<

include $(walrus_sources:.java=.u)
include $(tester_sources:.java=.u)

#############################################################################
# The Walrus Graph Visualization Tool
#
# See Makefile.nodep for the real makefile rules and for additional
# configuration options.
#
# This file is optional.  If you don't want to install or use jikes to build
# dependency files, then you don't have to use this makefile at all--use
# Makefile.nodep instead (either with "make -f Makefile.nodep" or by copying
# Makefile.nodep over this file).  The caveat to using Makefile.nodep directly
# is that some source files may not get properly recompiled when files are
# modified.
#
# The only purpose of this file is to add rules for automatically generating
# makefile dependency files (named *.u) with jikes.  These dependency files
# help Make determine which additional source files need to be recompiled
# whenever some source file changes.
#
# You may get warnings like the following the first time you run Make on this
# file, but the warnings can be ignored:
#
#   Makefile:77: H3AdaptivePicker.u: No such file or directory
#
# $Id: Makefile,v 1.11 2005/03/25 00:38:49 youngh Exp $
#############################################################################

include Makefile.nodep

#############################################################################
# CONFIGURATION OPTIONS
#############################################################################

# The top-level installation directory of Java.
#
# For Linux and other Unix you want something like the following (but check
# your system for the actual path):
#JAVA_INSTALL = /usr/local/jdk1.4.1
#JAVA_INSTALL = /usr/j2se  # Solaris
#
# For MacOS X:
JAVA_INSTALL = /System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home

# The following two symbols, giving the path of various system jar files,
# are needed to get Jikes to recognize the Java3D standard extension.
#
# For Linux and other Unix you want something like the following (but check
# your system for the actual path):
#JAVA_RUNTIME = $(JAVA_INSTALL)/jre/lib/rt.jar
#JAVA_EXT_DIR = $(JAVA_INSTALL)/jre/lib/ext
#
# For MacOS X:
JAVA_RUNTIME = /System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Classes/classes.jar
JAVA_EXT_DIR = /System/Library/Java/Extensions

J3D_JARS = $(JAVA_EXT_DIR)/vecmath.jar:$(JAVA_EXT_DIR)/j3dcore.jar:$(JAVA_EXT_DIR)/j3daudio.jar:$(JAVA_EXT_DIR)/j3dutils.jar

JIKES = jikes
JIKES_FLAGS = -classpath $(JAVA_RUNTIME):$(J3D_JARS):$$CLASSPATH
JIKES_DEPENDENCY_FLAGS = +B +M

#############################################################################
# NO FURTHER CONFIGURATION OPTIONS BELOW
#############################################################################

# Jikes occasionally includes the fully-qualified path of source and
# class files when generating dependencies.  This almost always makes
# these dependency files useless for anyone other than the current developer.
#
# Hence, for occasions when one wishes to distribute these dependency files,
# so that an installation of Jikes is not a requirement to building LibSea,
# this rule strips away the directory parts.  For day-to-day development,
# this step is not necessary, of course.
#
# This assumes that stripping away the directory part of paths does not
# cause ambiguity or cause files not to be found.  To ensure this, you
# should especially take care to use ANTLR in a jar file, so that Jikes
# elides dependencies on ANTLR classes.
sanitize:
	for F in *.u; do perl -i -n -MFile::Basename -e 's/\s//g; my ($$a,$$b) = split(/:/, $$_); print(basename($$a), " : ", basename($$b), "\n");' $$F; done

%.u: %.java
	$(JIKES) $(JIKES_FLAGS) $(JIKES_DEPENDENCY_FLAGS) $<

include $(walrus_sources:.java=.u)
include $(tester_sources:.java=.u)

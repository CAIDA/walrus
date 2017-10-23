#############################################################################
# The Walrus Graph Visualization Tool
#
# $Id: Makefile.nodep,v 1.5 2005/03/25 00:39:24 youngh Exp $
#############################################################################

# You must have mp.jar, antlrall-mod.jar, and libsea.jar in your CLASSPATH.

JAVA = java
JAVAC = javac
JAVAC_FLAGS = #-classpath $$CLASSPATH

#############################################################################
# NO FURTHER CONFIGURATION OPTIONS BELOW
#############################################################################

walrus_sources = H3AdaptivePicker.java \
	H3AdaptiveRenderLoop.java \
	H3AdaptiveRenderer.java \
	H3Axes.java \
	H3Canvas3D.java \
	H3Circle.java \
	H3CircleRenderer.java \
	H3DisplayPosition.java \
	H3Graph.java \
	H3GraphLayout.java \
	H3GraphLoader.java \
	H3InteractiveRotationRequest.java \
	H3LineRenderer.java \
	H3Main.java \
	H3Math.java \
	H3Matrix4d.java \
	H3MouseInputAdapter.java \
	H3NonadaptivePicker.java \
	H3NonadaptiveRenderLoop.java \
	H3PickViewer.java \
	H3Picker.java \
	H3PickerCommon.java \
	H3Point4d.java \
	H3PointRenderList.java \
	H3RenderList.java \
	H3RenderLoop.java \
	H3RenderQueue.java \
	H3RepeatingRotationRequest.java \
	H3RotationRequest.java \
	H3Transform.java \
	H3TransformQueue.java \
	H3Transformer.java \
	H3ViewParameters.java \
	H3WobblingRotationRequest.java

walrus_classes = $(walrus_sources:.java=.class)

tester_sources = H3TransformQueueTester.java

tester_classes = $(tester_sources:.java=.class)

%.class: %.java
	$(JAVAC) $(JAVAC_FLAGS) $<

.PHONY: walrus tester all atonce pedantic clean distclean

walrus: $(walrus_classes)

tester: $(tester_classes)

all: walrus tester

atonce: $(walrus_sources) $(tester_sources)
	$(JAVAC) $(JAVAC_FLAGS) $^

pedantic:
	$(MAKE) JAVAC=jikes JAVAC_FLAGS=+P

jar:
	jar cvmf distrib/manifest-supplement walrus.jar *.class walrus-splash.jpg

clean:
	-rm *.class *~

distclean: clean
	-rm *.u

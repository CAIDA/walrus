JAVA_INSTALL = /usr/j2se

JAVA = $(JAVA_INSTALL)/bin/java
JAVAC = $(JAVA_INSTALL)/bin/javac
JAVAC_FLAGS =

# The following two symbols, giving the path of various "system" jar files,
# are needed to get Jikes to recognize the Java3D standard extension.
JAVA_RUNTIME = $(JAVA_INSTALL)/jre/lib/rt.jar
JAVA_EXT_DIR = $(JAVA_INSTALL)/jre/lib/ext

J3D_JARS = $(JAVA_EXT_DIR)/vecmath.jar:$(JAVA_EXT_DIR)/j3dcore.jar:$(JAVA_EXT_DIR)/j3daudio.jar:$(JAVA_EXT_DIR)/j3dutils.jar

JIKES = jikes
JIKES_FLAGS = -classpath $(JAVA_RUNTIME):$(J3D_JARS):$$CLASSPATH
JIKES_DEPENDENCY_FLAGS = +B +M

walrus_sources = H3AdaptivePicker.java \
	H3AdaptiveRenderLoop.java \
	H3AdaptiveRenderer.java \
	H3Axes.java \
	H3Canvas3D.java \
	H3CapturingRenderLoop.java \
	H3CapturingRotationRequest.java \
	H3Circle.java \
	H3CircleRenderer.java \
	H3Graph.java \
	H3GraphLayout.java \
	H3GraphLoader.java \
	H3InteractiveRotationRequest.java \
	H3LineRenderer.java \
	H3Main.java \
	H3Math.java \
	H3MouseInputAdapter.java \
	H3NonadaptivePicker.java \
	H3NonadaptiveRenderLoop.java \
	H3PickViewer.java \
	H3Picker.java \
	H3PickerCommon.java \
	H3PointRenderList.java \
	H3RenderList.java \
	H3RenderLoop.java \
	H3RenderQueue.java \
	H3RepeatingRotationRequest.java \
	H3RotationRequest.java \
	H3ScreenCapturer.java \
	H3Transform.java \
	H3TransformQueue.java \
	H3Transformer.java \
	H3ViewParameters.java \
	H3WobblingRotationRequest.java

walrus_classes = $(walrus_sources:.java=.class)

tester_sources = H3TransformQueueTester.java

tester_classes = $(tester_sources:.java=.class)

%.u: %.java
	$(JIKES) $(JIKES_FLAGS) $(JIKES_DEPENDENCY_FLAGS) $<

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

clean:
	-rm *.class *~

distclean: clean
	-rm *.u

include $(walrus_sources:.java=.u)
include $(tester_sources:.java=.u)

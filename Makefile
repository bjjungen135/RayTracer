JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        Raytracer.java\
	Light.java\
	Camera.java\
	Ray.java\
	Material.java\
	Triangle.java\
	Model.java\
	RayObject.java\
	Intersect.java\
	Sphere.java\
	MonteCarloRayTracer.java\
	RaytracerThreaded.java

default: all

all: $(CLASSES:.java=.class)

clean:
	$(RM) *.class


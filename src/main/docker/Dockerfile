FROM ogndocker/ogn-gateway-java:latest

ENV JVM_OPTS="-Xms128m -Xmx256m"

# add plugin
ADD ${project.build.finalName}-shaded.jar ${OGN_HOME}/ogn-gateway/plugins
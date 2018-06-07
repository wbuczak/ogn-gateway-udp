FROM ogndocker/ogn-gateway-java:latest

# default values
ENV OGN_CLIENT_SRV_NAME="aprs.glidernet.org"
ENV OGN_CLIENT_USE_SSL=true
ENV OGN_CLIENT_SRV_PORT_FILTERED=10018

# add plugin
ADD ${project.build.finalName}-shaded.jar ${OGN_HOME}/ogn-gateway/plugins
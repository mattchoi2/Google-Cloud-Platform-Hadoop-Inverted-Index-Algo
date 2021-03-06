# Cloud-Final-Project

This repository contains the Maven Java project that queries the GCP API and creates a job.  The input is a large text file containing plays from Shakespeare, Hugo, and Tolstoy.  The Hadoop spark job that runs is contained in the hadoop file in this repo.  It runs an inverted index algorithm, mapping each term (key) to the file it was found in and the frequency of how many times it was found within that file (value).

# Video Demo

[![Thumbnail of video](http://img.youtube.com/vi/G3q4798JNbA/0.jpg)](http://www.youtube.com/watch?v=G3q4798JNbA)

# Tasks Completed

1. Java Application Implementation and Execution on Docker
2. Docker to GCP Cluster Communication
3. Inverted Indexing MapReduce Implementation and Execution on the Cluster (GCP)

# Requirements

- Docker Desktop Client

# Docker Run Instructions

1. Start up Xming on machine
2. Ensure it is running on port 0.0
3. set-variable -name DISPLAY -value 172.18.7.17:0.0
    The IP is located in the logs of Xming (under heading XdmcpRegisterConnection: newAddress 172.18.7.17)
4. docker build -t cloud-project-gui:?.?
5. docker images (get the IMAGE ID) 
6. docker run -it --privileged -e DISPLAY=$DISPLAY -v $PWD/data:/data <docker-img-id> bash

# Dockerfile

```
FROM openkbs/jdk-mvn-py3

MAINTAINER DrSnowbird "DrSnowbird@openkbs.org"

ARG DISPLAY=${DISPLAY:-":0.0"}
ENV DISPLAY=${DISPLAY}

USER root

## ---- X11 ----
RUN apt-get update && \
    # apt-get install -y sudo xauth xorg openbox && \
    apt-get install -y sudo xauth xorg fluxbox && \
    # apt-get install -y libxext-dev libxrender-dev libxtst-dev firefox-esr && \
    apt-get install -y libxext-dev libxrender-dev libxtst-dev firefox && \
    apt-get install -y apt-transport-https ca-certificates libcurl3-gnutls

RUN apt-get install -y apt-utils packagekit-gtk3-module libcanberra-gtk3-module
RUN apt-get install -y dbus-x11 
RUN apt-get install -y xdg-utils --fix-missing

#=================================
# DBus setup
#=================================
##  ---- dbus setup ----
ENV DBUS_SYSTEM_BUS_ADDRESS=unix:path=/host/run/dbus/system_bus_socket
ENV unix:runtime=yes

RUN sudo apt-get update -y && \
    sudo rm -f /var/run/firefox-restart-required && \
    #sudo mkdir -p /var/run/dbus/system_bus_socket && chmod -R 0777 /var/run/dbus/system_bus_socket && \
    sudo mkdir -p /host/run/dbus/system_bus_socket && sudo chmod -R 0777 /host/run/dbus/system_bus_socket && \
    sudo ln -s ${INST_SCRIPTS}/docker-entrypoint.sh /usr/local/docker-entrypoint.sh && \
    sudo rm -rf ${HOME}/.cache && \
    sudo chown -R ${USER}:${USER} ${HOME}
    # sudo mkdir -p /host/run/dbus/system_bus_socket
    # sudo apt-get install -qqy x11-apps

WORKDIR ${HOME}
COPY . ${HOME}
USER ${USER}

#ENTRYPOINT ["/usr/local/docker-entrypoint.sh"]
#CMD ["/usr/bin/firefox"]
# CMD ["/usr/bin/google-chrome","--no-sandbox","--disable-gpu", "--disable-extensions"]
#CMD ["/usr/bin/google-chrome"]
# -- test --
CMD xeyes
```

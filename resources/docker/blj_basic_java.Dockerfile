# Deployment path:  $BLJ/resources/docker/blj_basic_java.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get install -y software-properties-common && \
	apt-get upgrade -y && \
   	apt-get install -y openjdk-8-jre-headless

#3.) ============== Cleanup =================
RUN	rm -rf /tmp/* && \
	rm -rf /var/log/* 

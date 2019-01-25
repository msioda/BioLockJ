# Deployment path:  $BLJ/resources/docker/blj_basic_python2.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y python2.7-dev python-pip python-tk 
	
#3.) =======================  Cleanup =================
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/* 

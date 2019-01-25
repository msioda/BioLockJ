# Deployment path:  $BLJ/resources/docker/blj_basic_py2.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get install -y python2.7-dev python-pip python-tk 
	
#3.) =======================  Cleanup =================
RUN	rm -rf /tmp/* && \
	rm -rf /var/log/* 

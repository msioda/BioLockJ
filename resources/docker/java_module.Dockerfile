# Deployment path:  $BLJ/resources/docker/java_module.Dockerfile

FROM blj_basic_biolockj

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get install -y software-properties-common && \
	apt-get upgrade -y && \
   	apt-get install -y openjdk-8-jre-headless

#3.) =======================  Cleanup  ===========
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
	
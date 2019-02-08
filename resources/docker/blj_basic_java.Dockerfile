# Deployment path:  $BLJ/resources/docker/blj_basic_java.Dockerfile

FROM biolockj/blj_basic
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Ubuntu Software 
RUN apt-get install -y software-properties-common && \
	apt-get upgrade -y && \
   	apt-get install -y openjdk-8-jre-headless

#2.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /var/log/* 

#3.) Run BioLockJ Command
CMD [ "/bin/bash", "biolockj", "$BLJ_OPTIONS" ]
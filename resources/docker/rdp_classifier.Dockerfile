# Deployment path:  $BLJ/resources/docker/dock.Dockerfile

FROM biolockj/blj_basic_java
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install RDP
ENV RDP="rdp_classifier_2.12"
ENV RDP_URL="https://sourceforge.net/projects/rdp-classifier/files/rdp-classifier"
RUN cd /usr/local/bin && \
	wget -qO- $RDP_URL/$RDP.zip | bsdtar -xf-  && \
	mv ./rdp*/dist/* . && \
	rm -rf ./rdp*

#2.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
	
#3.) Set Default Command
CMD /bin/bash $COMPUTE_SCRIPT
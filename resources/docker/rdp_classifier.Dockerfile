# Deployment path:  $BLJ/resources/docker/rdp_classifier.Dockerfile

FROM biolockj/blj_basic_java
ARG DEBIAN_FRONTEND=noninteractive

#1.) Activate Ubuntu Software
RUN apt-get update

#2.) Install RDP
ENV RDP="rdp_classifier_2.12"
ENV RDP_URL="https://sourceforge.net/projects/rdp-classifier/files/rdp-classifier"
RUN cd /usr/local/bin && \
	wget -qO- $RDP_URL/$RDP.zip | bsdtar -xf-  && \
	mv ./rdp*/dist/* . && \
	rm -rf ./rdp*

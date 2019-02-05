# Deployment path:  $BLJ/resources/docker/rdp_classifier.Dockerfile

FROM biolockj/blj_basic_java

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ================= Setup Env =================
RUN apt-get update

#3.) =======================  RDP  ========================== 
ENV RDP="rdp_classifier_2.12"
ENV RDP_URL="https://sourceforge.net/projects/rdp-classifier/files/rdp-classifier"
RUN cd /app && \
	wget -qO- $RDP_URL/$RDP.zip | bsdtar -xf-  && \
	rm -rf ./rdp*/[LRbilmnst]*

#4.) ================= Container Command =================
CMD [ "/bin/bash", "$COMPUTE_SCRIPT" ]
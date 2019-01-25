# Deployment path:  $BLJ/resources/docker/rdp_classifier.Dockerfile

FROM blj_basic_java

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) =======================  RDP  ========================== 
ENV RDP="rdp_classifier_2.12"
ENV RDP_URL="https://sourceforge.net/projects/rdp-classifier/files/rdp-classifier"
RUN cd /app && \
	wget -qO- $RDP_URL/$RDP.zip | bsdtar -xf- && \
	rm $RDP_ZIP && rm -rf ./rdp*/[LRbilmnst]*

#3.) ================= Container Command =================
CMD [ "$COMPUTE_SCRIPT" ]
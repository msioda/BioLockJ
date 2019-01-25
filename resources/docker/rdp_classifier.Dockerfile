# Deployment path:  $BLJ/resources/docker/rdp_classifier.Dockerfile

FROM java_module

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ENV RDP_ZIP="rdp_classifier_2.12.zip"
ENV RDP_URL="https://sourceforge.net/projects/rdp-classifier/files/rdp-classifier"

#2.) =======================  RDP  ========================== 
RUN cd /app && \
	wget $RDP_URL/$RDP_ZIP && \
	unzip $RDP_ZIP && \
	rm $RDP_ZIP && \
	chmod -R 777 /app/rdp*
	
#3.) ================= Container Command =================
ENTRYPOINT [ "bash", $COMPUTE_SCRIPT ]
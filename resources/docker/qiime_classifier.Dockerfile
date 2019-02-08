# Deployment path:  $BLJ/resources/docker/qiime_classifier.Dockerfile

FROM biolockj/qiime_classifier
ARG DEBIAN_FRONTEND=noninteractive

#1.) Add Default QIIME DB 
RUN apt-get update && \
	pip install qiime-default-reference 

#2.) Cleanup 
#RUN	apt-get clean && \
#	rm -rf /tmp/* && \
#	rm -rf /usr/share/* && \
#	rm -rf /var/cache/* && \
#	rm -rf /var/lib/apt/lists/* && \
#	rm -rf /var/log/*
# Deployment path:  $BLJ/resources/docker/kraken_classifier.Dockerfile

FROM biolockj/kraken_classifier_dbfree
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Ubuntu Software 
RUN apt-get update

#2.) Download 8GB Dustmasked miniKraken DB
ENV KRAKEN_DB_URL="https://ccb.jhu.edu/software/kraken/dl/minikraken_20171101_8GB_dustmasked.tgz"
RUN cd /db && \
	wget -qO- $KRAKEN_DB_URL | bsdtar -xzf- && \
	mv minikraken*/* . && \
	rm -rf minikraken*

#3.) Cleanup
#RUN	 apt-get clean && \
#	rm -rf /tmp/* && \
#	rm -rf /usr/share/* && \
#	rm -rf /var/cache/* && \
#	rm -rf /var/lib/apt/lists/* && \
#	rm -rf /var/log/*

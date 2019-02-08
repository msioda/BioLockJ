# Deployment path:  $BLJ/resources/docker/kraken2_classifier.Dockerfile

FROM biolockj/kraken2_classifier_dbfree
ARG DEBIAN_FRONTEND=noninteractive
  
#1.) Update Ubuntu Software 
RUN apt-get update
 
#2.) Download 8GB miniKraken2 DB
ENV KRAKEN_DB_URL="https://ccb.jhu.edu/software/kraken2/dl/minikraken2_v1_8GB.tgz"
RUN cd /db && \
	wget -qO- $KRAKEN_DB_URL | bsdtar -xzf- && \
	chmod o+x *

#3.) Cleanup
#RUN apt-get clean && \
#	rm -rf /tmp/* && \
#	rm -rf /usr/share/* && \
#	rm -rf /var/cache/* && \
#	rm -rf /var/lib/apt/lists/* && \
#	rm -rf /var/log/*

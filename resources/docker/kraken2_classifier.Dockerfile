# Deployment path:  $DOCKER_FILE_PATH/kraken2_classifier.Dockerfile

FROM biolockj/kraken2_classifier_dbfree
 
#1.) Download 8GB miniKraken2 DB
ENV KRAKEN_DB_URL="https://ccb.jhu.edu/software/kraken2/dl/minikraken2_v1_8GB.tgz"
RUN cd /db && \
	wget -qO- $KRAKEN_DB_URL | bsdtar -xzf- && \
	chmod o+x *

#2.) Cleanup
RUN	 rm -rf /usr/share/* 

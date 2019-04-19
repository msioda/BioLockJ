# Deployment path:  $DOCKER_FILE_PATH/kraken_classifier.Dockerfile

FROM biolockj/kraken_classifier_dbfree

#1.) Download 8GB Dustmasked miniKraken DB
ENV KRAKEN_DB_URL="https://ccb.jhu.edu/software/kraken/dl/minikraken_20171101_8GB_dustmasked.tgz"
RUN cd $BLJ_DB && \
	wget -qO- $KRAKEN_DB_URL | bsdtar -xzf- && \
	mv minikraken*/* . && \
	rm -rf minikraken*

#2.) Cleanup
RUN	 rm -rf /usr/share/* 

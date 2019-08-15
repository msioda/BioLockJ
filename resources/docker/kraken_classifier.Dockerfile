# Deployment path:  $DOCKER_DIR/kraken_classifier.Dockerfile

FROM biolockj/kraken_classifier_dbfree

#1.) Download 8GB Dustmasked miniKraken DB
RUN cd "${BLJ_DEFAULT_DB}" && \
	wget -qO- "https://ccb.jhu.edu/software/kraken/dl/minikraken_20171101_8GB_dustmasked.tgz" | bsdtar -xzf- && \
	mv minikraken*/* . && rm -rf minikraken* && chmod -R 777 "${BLJ_DEFAULT_DB}"

#2.) Cleanup
RUN	 rm -rf /usr/share/* 

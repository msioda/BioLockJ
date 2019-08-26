# Deployment path: $DOCKER_DIR/kraken2_classifier.Dockerfile

FROM biolockj/kraken2_classifier_dbfree
 
#1.) Download 8GB miniKraken2 DB
RUN cd "${BLJ_DEFAULT_DB}" && \
	wget -qO- "ftp://ftp.ccb.jhu.edu/pub/data/kraken2_dbs/minikraken2_v1_8GB_201904_UPDATE.tgz" | bsdtar -xzf- && \
	chmod -R 777 "${BLJ_DEFAULT_DB}" && mv "${BLJ_DEFAULT_DB}"/minikraken2*/* . && rm -rf "${BLJ_DEFAULT_DB}"/minikraken2*

#2.) Cleanup
RUN	 rm -rf /usr/share/* 

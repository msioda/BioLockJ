# Deployment path: $DOCKER_DIR/kraken2_classifier_dbfree.Dockerfile

FROM biolockj/blj_basic
  
#1.) Install Kraken
ENV KRAKEN_VER=2.0.7-beta
ENV BASE_URL="https://github.com/DerrickWood/kraken2/archive/v"
ENV K2_URL=${BASE_URL}${KRAKEN_VER}.tar.gz
ENV K2="kraken2-${KRAKEN_VER}"
RUN cd $BIN && wget -qO- $K2_URL | bsdtar -xzf- && chmod o+x -R $K2 && \
	cd $K2 && ./install_kraken2.sh $BIN && chmod o+x -R $BIN && rm -rf $K2

#2.) Cleanup - save ca-certificates so kraken2_classifier can download from Internet
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	mv /usr/share/ca-certificates ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates /usr/share && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

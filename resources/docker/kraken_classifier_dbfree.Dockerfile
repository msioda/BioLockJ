# Deployment path:  $DOCKER_FILE_PATH/kraken_classifier_dbfree.Dockerfile

FROM biolockj/blj_basic
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Kraken
ENV KRAKEN_VER=0.10.5-beta
ENV BASE_URL="https://github.com/DerrickWood/kraken/archive/v"
ENV KRAKEN_URL=${BASE_URL}${KRAKEN_VER}.tar.gz
ENV KRAKEN=kraken-${KRAKEN_VER}
ENV BUILD_DIR=/usr/local/bin
RUN cd $BUILD_DIR && \
	wget -qO- $KRAKEN_URL | bsdtar -xf- && \
	chmod o+x -R $KRAKEN && \
	cd $KRAKEN && \
  	./install_kraken.sh $BUILD_DIR && \
  	chmod o+x -R $BUILD_DIR && \
  	rm -rf $KRAKEN

#2.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	mv /usr/share/ca-certificates ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates /usr/share && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

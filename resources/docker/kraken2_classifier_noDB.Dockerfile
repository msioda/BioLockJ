# Deployment path:  $BLJ/resources/docker/kraken2_classifier_noDB.Dockerfile

FROM biolockj/blj_basic
ARG DEBIAN_FRONTEND=noninteractive
  
#1.) Install Kraken
ENV KRAKEN_VER=2.0.7-beta
ENV BASE_URL="https://github.com/DerrickWood/kraken2/archive/v"
ENV KRAKEN2_URL=${BASE_URL}${KRAKEN_VER}.tar.gz
ENV KRAKEN2="kraken2-${KRAKEN_VER}"
ENV BUILD_DIR=/usr/local/bin
RUN cd $BUILD_DIR && \
	wget -qO- $KRAKEN2_URL | bsdtar -xzf- && \
	chmod o+x -R $KRAKEN2 && \
	cd $KRAKEN2 && \
	./install_kraken2.sh $BUILD_DIR && \
	chmod o+x -R $BUILD_DIR && \
	rm -rf $KRAKEN2

#2.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

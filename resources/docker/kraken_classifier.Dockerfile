# Deployment path:  $BLJ/resources/docker/kraken_classifier.Dockerfile

FROM biolockj/blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ================ Install Kraken ================ 
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

#3.) ================ Install Kraken DB ================
ENV KRAKEN_DB_URL="https://ccb.jhu.edu/software/kraken/dl/minikraken_20171101_8GB_dustmasked.tgz"
RUN cd /db && \
	wget -qO- $KRAKEN_DB_URL | bsdtar -xzf- && \
	mv minikraken*/* . && \
	rm -rf minikraken*

#4. ) =============== Cleanup ================================
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
		
#5.) ================= Container Command =================
CMD [ "/bin/bash", "$COMPUTE_SCRIPT" ]

# Deployment path:  $BLJ/resources/docker/kraken_classifier.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ================ Install Kraken ================ 
ENV KRAKEN_VER=0.10.5-beta
ENV KRAKEN_URL="https://github.com/DerrickWood/kraken/archive/v"
ENV BUILD_DIR=/usr/local/bin
RUN cd /app && \
	wget -qO- ${KRAKEN_URL}${KRAKEN_VER}.zip | bsdtar -xf- && \
	cd kraken-${KRAKEN_VER} && \
  	./install_kraken.sh $BUILD_DIR && \
  	chmod o+x -R $BUILD_DIR && \
  	rm -rf /app/kraken2-${KRAKEN_VER}

#3.) ================ Install Kraken DB ================ 
ENV KRAKEN_DB_URL="https://ccb.jhu.edu/software/kraken/dl"
ENV KRAKEN_DB="minikraken_20171019_4GB"
RUN cd /db && \
	wget -qO- ${KRAKEN_URL}/${KRAKEN_DB}.tgz | bsdtar -xzf-

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

# Deployment path:  $BLJ/resources/docker/classifier/wgs/Kraken2Classifier

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
  
#2.) ================ Install Kraken ================ 
ENV KRAKEN_VER=2.0.7-beta
ENV KRAKEN_URL="https://github.com/DerrickWood/kraken2/archive/v"
ENV BUILD_DIR=/usr/local/bin
RUN cd /app && \
	wget -qO- ${KRAKEN_URL}${KRAKEN_VER}.tar.gz | bsdtar -xf- && \
	cd kraken2-${KRAKEN_VER} && \
	./install_kraken2.sh $BUILD_DIR && \
	chmod o+x -R $BUILD_DIR && \
	rm -rf /app/kraken2-${KRAKEN_VER}
    
#3.) ================= Copy 4GB database =================
COPY miniKraken2_20181027 /db/miniKraken2

#4.) =============== Cleanup ================================
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#5.) ================= Container Command =================
CMD [ "$COMPUTE_SCRIPT" ]
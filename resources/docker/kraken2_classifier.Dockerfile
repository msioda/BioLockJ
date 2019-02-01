# Deployment path:  $BLJ/resources/docker/classifier/wgs/Kraken2Classifier

FROM ubuntu:18.04

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
  
#3.) ================ Install Kraken ================ 
ENV KRAKEN_VER=2.0.7-beta
ENV KB=/app/kraken2
RUN cd /app && \
  wget -qO- https://github.com/DerrickWood/kraken2/archive/v${KRAKEN_VER}.tar.gz | bsdtar -xf- && \
  cd kraken2-${KRAKEN_VER} && \
  mkdir $KB && \
  ./install_kraken2.sh $KB && \
  chmod o+x -R $KB && \
  rm -rf /app/kraken2-${KRAKEN_VER}

    
#5.) ================= Copy 4GB database =================
COPY miniKraken2_20181027 /db/miniKraken2

#6.) =============== Cleanup ================================
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#7.) ================= Container Command =================
CMD [ "$COMPUTE_SCRIPT" ]
# Deployment path:  $BLJ/resources/docker/classifier/wgs/Kraken2Classifier

FROM ubuntu:18.04

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir /input && \
    mkdir /pipeline && \
    mkdir /app && \
    mkdir /meta && \
    mkdir /primer && \
    mkdir /config && \
    mkdir /db
    

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
  apt-get install -y build-essential \
  checkinstall \
  apt-utils \
  wget \
  gzip \
  bsdtar \
  rsync
  
  
#3.) ================ Install Kraken ================ 
ENV KRAKEN_VER=2.0.7-beta
ENV K2=/app/kraken2
ENV KB=/app/kraken2-${KRAKEN_VER}/build
RUN cd /app && \
  wget -qO- https://github.com/DerrickWood/kraken2/archive/v${KRAKEN_VER}.tar.gz | bsdtar -xf- && \
  cd kraken2-${KRAKEN_VER} && \
  mkdir $K2 && \
  ./install_kraken2.sh $K2 && \
  chmod o+x -R $K2 && \
  rm -rf /app/kraken2-${KRAKEN_VER}

#4.) ============ Update Ubuntu ~/.bashrc =================
RUN echo ' '  >> ~/.bashrc && \
  echo 'force_color_prompt=yes' >> ~/.bashrc && \
  echo 'alias ..="cd .."' >> ~/.bashrc && \
  echo 'alias ls="ls -lh --color=auto"' >> ~/.bashrc && \
  echo 'alias h="head -n 8"' >> ~/.bashrc && \
  echo 'alias t="tail -n 8"' >> ~/.bashrc && \
  echo 'alias f="find . -name"' >> ~/.bashrc && \
  echo 'alias cab="cat ~/.bashrc"' >> ~/.bashrc && \
  echo 'alias tlog="tail -1000f *.log"' >> ~/.bashrc && \
  echo 'alias rf="source ~/.bashrc"' >> ~/.bashrc && \
  echo ' ' >> ~/.bashrc && \
  echo 'if [ -f /etc/bash_completion ] && ! shopt -oq posix; then' >> ~/.bashrc && \
  echo '    . /etc/bash_completion' >> ~/.bashrc && \
  echo 'fi' >> ~/.bashrc
  
  	
#5.) ================= Copy 4GB database =================
COPY miniKraken2_20181027 /db/miniKraken2

#6.) =============== Cleanup ================================
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	find / -name *python* | xargs rm -rf && \
	rm -rf var/cache/* && \
	rm -rf var/log/* && \
	rm -rf tmp/* && \
	rm -rf usr/games && \
	rm -rf usr/share/*

#7.) ================= Container Command =================
CMD bash $COMPUTE_SCRIPT
# Deployment path:  $BLJ/resources/docker/classifier/wgs/KrakenClassifier/Dockerfile

FROM ubuntu:18.04

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir /input && mkdir /pipeline && mkdir /app && mkdir /meta && mkdir /primer && mkdir /config  

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y build-essential \
	checkinstall \
	apt-utils \
	wget \
	gzip \
	bsdtar

#2.1) ============ Update Ubuntu ~/.bashrc =================
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

#3.) ================ Install Kraken ================ 
RUN cd /app && wget -qO- https://github.com/DerrickWood/kraken/archive/v0.10.5-beta.zip | bsdtar -xf- && \
	cd kraken-0.10.5-beta && mkdir build && echo "echo 0" >> install_kraken.sh && sh install_kraken.sh build && chmod o+x -R /app/kraken-0.10.5-beta/build

#4.) ================ Install Kraken DB ================ 
# Kraken Databases available here: https://ccb.jhu.edu/software/kraken/ 
# uncomment for MiniKraken DB_4GB
RUN mkdir /db && cd /db && wget -qO- https://ccb.jhu.edu/software/kraken/dl/minikraken_20171019_4GB.tgz | bsdtar -xzf-
# uncomment for MiniKraken DB_8GB
# RUN mkdir /db && cd /db && wget -qO- https://ccb.jhu.edu/software/kraken/dl/minikraken_20171019_8GB.tgz | bsdtar -xzf-

#5. ) =============== Cleanup ================================
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	find / -name *python* | xargs rm -rf && \
	rm -rf var/cache/* && \
	rm -rf var/log/* && \
	rm -rf tmp/* && \
	rm -rf usr/games && \
	rm -rf usr/share/*
		
#6.) ================= Container Command =================
CMD bash $COMPUTE_SCRIPT

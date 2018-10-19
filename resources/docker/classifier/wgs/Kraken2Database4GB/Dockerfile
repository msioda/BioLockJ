# Deployment path:  $BLJ/resources/docker/classifier/wgs/Kraken2Database4GB/Dockerfile
# This build requires that the kraken2 database is built and saved to this folder before building

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
RUN cd /app && \
	wget -qO- https://github.com/DerrickWood/kraken2/archive/v2.0.7-beta.tar.gz | bsdtar -xf- && \
	cd kraken2-2.0.7-beta && \
	mkdir build && \
	./install_kraken2.sh build && \
	chmod o+x -R /app/kraken2-2.0.7-beta/build
	
#4.) ================= Copy 4GB database =================
COPY miniKraken2_20181027 /db/miniKraken2_20181027
	
#5.) ================= Container Command =================
CMD bash $BLJ_SCRIPT

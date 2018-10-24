# Deployment path:  $BLJ/resources/docker/seq/PearMergeReads/Dockerfile

FROM ubuntu:18.04

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ENV PEAR_VERSION=pear-0.9.10-bin-64
RUN mkdir /input && mkdir /pipeline && mkdir /app && mkdir /meta && mkdir /primer && mkdir /config  

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y build-essential \
	apt-utils \
	wget \
	gzip && \
	apt-get clean

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
	echo 'fi' >> ~/.bashrc && \
	echo ' ' >> ~/.bashrc 
	
#3.) ================= Install PEAR =================
RUN mkdir -p /app/pear && cd /app/pear && \  
	wget https://github.com/mikesioda/BioLockJ_Dev/releases/download/v1.0/${PEAR_VERSION}.gz && \
	gzip -df ${PEAR_VERSION}.gz && \
	chmod +x /app/pear/${PEAR_VERSION} 
	
#4.) ================= Container Command =================
CMD bash $COMPUTE_SCRIPT

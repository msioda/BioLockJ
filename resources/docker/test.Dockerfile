# Deployment path: $BLJ/resources/docker/test.Dockerfile

FROM ubuntu:18.04

#1.) ================= Setup Env ==================================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y build-essential apt-utils bsdtar

#3.) =======================  profile updates  ==========================
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

	
#4.) =======================  Cleanup  ==========================
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/games && \
	rm -rf /var/log/*
	
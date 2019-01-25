# Deployment path: $BLJ/resources/docker/blj_basic.Dockerfile

FROM ubuntu:18.04

#1.) ================= Setup Env ==================================
ARG DEBIAN_FRONTEND=noninteractive
ENV BLJ=/app/biolockj
ENV BLJ_PROJ=/pipeline
ENV BLJ_JAR=$BLJ/dist/BioLockJ.jar

#2.) ============ Make standard dirs 
RUN mkdir /config && \
	mkdir /input && \
	mkdir /log && \
	mkdir /meta && \
	mkdir /pipeline && \
	mkdir /primer

#2.) ============ Update Ubuntu ~/.bashrc =================
RUN echo ' '  >> ~/.bashrc && \
	echo 'force_color_prompt=yes' >> ~/.bashrc && \
	echo 'alias ..="cd .."' >> ~/.bashrc && \
	echo 'alias ls="ls -lh --color=auto"' >> ~/.bashrc && \
	echo 'alias h="head -n 8"' >> ~/.bashrc && \
	echo 'alias t="tail -n 8"' >> ~/.bashrc && \
	echo 'alias f="find . -name"' >> ~/.bashrc && \
	echo 'alias cab="cat ~/.bashrc"' >> ~/.bashrc && \
	echo 'alias tlog="tail -1000 *.log"' >> ~/.bashrc && \
	echo 'alias tlogf="tail -1000f *.log"' >> ~/.bashrc && \
	echo 'alias rf="source ~/.bashrc"' >> ~/.bashrc && \
	echo ' ' >> ~/.bashrc && \
	echo 'if [ -f /etc/bash_completion ] && ! shopt -oq posix; then' >> ~/.bashrc && \
	echo '    . /etc/bash_completion' >> ~/.bashrc && \
	echo 'fi' >> ~/.bashrc

#3.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y \
		build-essential \
		apt-utils \
		bsdtar \
		gawk \
		tzdata \
		wget

#4.) ================= Set the timezone =================
RUN ln -fs /usr/share/zoneinfo/US/Eastern /etc/localtime && \
	dpkg-reconfigure -f noninteractive tzdata

#5.) =======================  Cleanup  ==========================
RUN	rm -rf /tmp/* && \
	rm -rf /usr/games && \
	rm -rf /var/log/*

#6.) =======================  Command  ==========================
ENTRYPOINT [ "/bin/bash" ]
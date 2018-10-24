# Deployment path:  $BLJ/resources/docker/classifier/r16s/RdpClassifier/Dockerfile

FROM ubuntu:18.04

#1.) ================= Setup Env =================
ENV RDP=rdp_classifier_2.12.zip
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir /input && mkdir /pipeline && mkdir /app && mkdir /meta && mkdir /primer && mkdir /config  

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install --no-install-recommends -y build-essential \
	software-properties-common \
	apt-utils \
	unzip \
	wget 

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

#3.) ================= Install Java   =================
RUN apt-get upgrade -y && \
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update && \
    echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y oracle-java8-installer
    
	
#4.) =======================  RDP  ========================== 
RUN cd /app && \
	wget https://sourceforge.net/projects/rdp-classifier/files/rdp-classifier/$RDP && \
	unzip $RDP && \
	rm $RDP && \
	chmod -R 777 /app/rdp*

#5.) =======================  Cleanup  ========================== 
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* 
	
#6.) ================= Container Command =================
CMD bash $COMPUTE_SCRIPT

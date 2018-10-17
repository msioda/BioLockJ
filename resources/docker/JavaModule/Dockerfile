# Deployment path:  $BLJ/resources/docker/JavaModule/Dockerfile

FROM ubuntu:18.04

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir /input && mkdir /pipeline && mkdir /app && mkdir /meta && mkdir /primer && mkdir /config  

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y build-essential \
	apt-utils \
	software-properties-common \
	tar \
	wget \
	gzip \
	tzdata
	
#3.) ================= Install Java   =================
RUN apt-get upgrade -y && \
   add-apt-repository ppa:webupd8team/java -y && \
   apt-get update && \
   echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
   apt-get install -y oracle-java8-installer 

#4.) ================= Install BioLockJ =================
ARG BLJ_VERSION
ARG BLJ_DATE
ENV BLJ=/app/${BLJ_VERSION}
ENV BLJ_TAR=${BLJ_VERSION}.tar.gz
ENV BLJ_RELEASE_URL=https://github.com/msioda/BioLockJ/releases/download/${BLJ_VERSION}/$BLJ_TAR
RUN echo ${BLJ_DATE} && \
	mkdir $BLJ && \
	cd $BLJ && \
	wget $BLJ_RELEASE_URL && \
	tar -xzf $BLJ_TAR && \
	chmod -R 777 $BLJ && \
	rm -f $BLJ_TAR

#5.) ============ Update Ubuntu ~/.bashrc =================
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

#6.) ================= Set the timezone =================
RUN ln -fs /usr/share/zoneinfo/US/Eastern /etc/localtime && dpkg-reconfigure -f noninteractive tzdata

#7.) =======================  Cleanup  ========================== 
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* 

#8.) ================= Container Command =================
CMD java -jar $BLJ/dist/BioLockJ.jar $BLJ_OPTIONS

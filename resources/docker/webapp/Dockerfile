# Deployment path: $BLJ/resources/docker/webapp

FROM ubuntu:18.04

ENV NODE_VERSION 8.11.3

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir /pipeline && mkdir /app && mkdir /config && mkdir /log 

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y build-essential \
	checkinstall \
	apt-utils \
	software-properties-common \
	ca-certificates \
	tar \
	wget \
	gzip \
    nodejs \
    aptitude && \
    apt-get update && \
    wget https://deb.nodesource.com/setup_8.x | bash - && \
	apt-get update 

#3.) ============ Update Ubuntu ~/.bashrc =================
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

#4.) ================= Install BioLockJ =================
ARG BLJ_VERSION
ARG BLJ_DATE
ENV BLJ=/app/biolockj
ENV BLJ_PROJ=/pipeline
ENV BLJ_TAR=biolockj_${BLJ_VERSION}.tgz
ENV BLJ_RELEASE_URL=https://github.com/msioda/BioLockJ/releases/download/${BLJ_VERSION}/$BLJ_TAR
RUN echo ${BLJ_DATE} && \
	mkdir $BLJ && \
	cd $BLJ && \
	wget $BLJ_RELEASE_URL && \
	tar -xzf $BLJ_TAR && \
	chmod -R 770 $BLJ && \
	rm -f $BLJ_TAR && rm -rf $BLJ/[lip]* && rm -rf $BLJ/src && rm -rf $BLJ/resources/[blid]*

#5.) ============ Install  Docker Client =================
ARG DOCKER_CLIENT
RUN cd /tmp && mkdir -p /usr/local/bin && \
	wget https://download.docker.com/linux/static/stable/x86_64/${DOCKER_CLIENT} && \
	tar --strip-components=1 -zxf ${DOCKER_CLIENT} -C /usr/local/bin && \
	chmod +x /usr/local/bin/docker && \
	rm -f /tmp/$DOCKER_CLIENT	

#6.) ======== Move json packages to container root dir =======
RUN cp $BLJ/web_app/package*.json ./

#7.) ======== Install npm  =======
RUN apt-get install -y npm && \
	npm install --only=production 
    #Remove "--only=production" if adding new packages (maybe) MS 11/1

	
#8.) ================= Expose Port 8080 =================
#I used https://nodejs.org/en/docs/guides/nodejs-docker-webapp/ initially.
#Now, I'm copying from https://github.com/nodejs/docker-node/blob/master/Dockerfile-alpine.template //didn't work
#Your app binds to port 8080 so you'll use the EXPOSE instruction to have it mapped by the docker daemon:
EXPOSE 8080

#9.) =======================  Bundle app source  ==========================
RUN cp -r $BLJ/web_app/* ./

#10.) =======================  Cleanup  ==========================
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf var/cache/* && \
	rm -rf var/log/* && \
	rm -rf tmp/* && \
	rm -rf usr/games 
	## && \ rm -rf usr/share/* <-- selectively delete content, but don't remove /usr/share/npm* packages (maybe more)

#11.) ================= Define command = npm start =================

#define the command to run your app using CMD which defines your runtime. Here we will use the basic npm start which will run node server.js to start your server:
CMD [ "npm", "start" ]

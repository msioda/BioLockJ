# Deployment path: $BLJ/web_app

FROM ubuntu:18.04

ENV NODE_VERSION 8.11.3

#1.) ================= Setup Env =================
ENV BLJ=/app/biolockj_v1.0
ARG DEBIAN_FRONTEND=noninteractive
#RUN mkdir /input && mkdir /pipeline && mkdir /app && mkdir /meta && mkdir /primer && mkdir /config
RUN mkdir /output && mkdir /pipeline && mkdir /app && mkdir $BLJ


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
    curl \
    nodejs \
    aptitude && \
    apt-get update && \
    curl --silent --location https://deb.nodesource.com/setup_8.x | bash -


##########Dockerize Node
#3.) NODE (from Malcolm) & npm install
# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./
RUN apt-get update && apt-get install -y npm
RUN npm install

# If you are building your code for production
# I THINK WE WANT TO USE THIS ONE  -noteL: MS 9/2
# RUN npm install --only=production

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

#5.) ================= Install BioLockJ =================
ARG BLJ_VERSION
ARG BLJ_DATE
ENV BLJ=/app/biolockj
ENV BLJ_TAR=biolockj_${BLJ_VERSION}.tgz
#ENV BLJ_RELEASE_URL=https://github.com/msioda/BioLockJ/releases/download/${BLJ_VERSION}/$BLJ_TAR
#RUN echo ${BLJ_DATE} && \
	#mkdir $BLJ && \
	#cd $BLJ && \
	#wget $BLJ_RELEASE_URL && \
	#tar -xzf $BLJ_TAR && \
	#chmod -R 770 $BLJ && \
	#rm -f $BLJ_TAR && rm -rf $BLJ/[slip]* && rm -rf $BLJ/docs && rm -rf $BLJ/resources/[blid]*

#6.) ================= Expose Port 8080 =================
#I used https://nodejs.org/en/docs/guides/nodejs-docker-webapp/ initially.
#Now, I'm copying from https://github.com/nodejs/docker-node/blob/master/Dockerfile-alpine.template //didn't work
#Your app binds to port 8080 so you'll use the EXPOSE instruction to have it mapped by the docker daemon:
EXPOSE 8080

#7.) =======================  Cleanup  ==========================
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	find / -name *python* | xargs rm -rf && \
	rm -rf var/cache/* && \
	rm -rf var/log/* && \
	rm -rf tmp/* && \
	rm -rf usr/games && \
	rm -rf usr/share/*


#8.) ================ Addi the local webapp folder ================
COPY . .

#9.) ================= Define command = npm start =================
#define the command to run your app using CMD which defines your runtime. Here we will use the basic npm start which will run node server.js to start your server:
CMD [ "bin/www" ]

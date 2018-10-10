# Deployment path: $BLJ/resources/docker/Dockerfile

FROM ubuntu:18.04

# Expose port for Email
Expose 25

ENV NODE_VERSION 8.11.3

#1.) ================= Setup Env =================
ENV BLJ=/app/biolockj_v1.0
ENV PATH=$PATH:$BLJ/dist
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir /output && mkdir /pipeline && mkdir /app && mkdir $BLJ

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y build-essential \
	checkinstall \
	apt-utils \
	software-properties-common \
	git \
	unzip \
	ca-certificates \
	tar \
	wget \
	gzip \
	nano \
  curl

##########Dockerize Node
#32.) NODE (from Malcom)
RUN apt-get update && apt-get install --yes curl
RUN curl --silent --location https://deb.nodesource.com/setup_8.x | bash -
RUN apt-get update && apt-get install --yes nodejs
RUN apt-get install --yes aptitude
#RUN apt-get update && apt-get install -y npm

# Install app dependencies
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./
RUN npm install
# If you are building your code for production
# RUN npm install --only=production

#2.1) ============ Update Ubuntu ~/.bashrc =================
RUN echo ' '  >> ~/.bashrc
RUN echo 'force_color_prompt=yes' >> ~/.bashrc
RUN echo 'alias ..="cd .."' >> ~/.bashrc
RUN echo 'alias ls="ls -lh --color=auto"' >> ~/.bashrc
RUN echo 'alias h="head -n 8"' >> ~/.bashrc
RUN echo 'alias t="tail -n 8"' >> ~/.bashrc
RUN echo 'alias f="find . -name"' >> ~/.bashrc
RUN echo 'alias vib="nano ~/.bashrc"' >> ~/.bashrc
RUN echo 'alias cab="cat ~/.bashrc"' >> ~/.bashrc
RUN echo 'alias tlog="tail -1000f *.log"' >> ~/.bashrc
RUN echo 'alias rf="source ~/.bashrc"' >> ~/.bashrc
RUN echo ' ' >> ~/.bashrc
RUN echo 'if [ -f /etc/bash_completion ] && ! shopt -oq posix; then' >> ~/.bashrc
RUN echo '    . /etc/bash_completion' >> ~/.bashrc
RUN echo 'fi' >> ~/.bashrc

#3.) ============ Install  Docker Client =================
ARG DOCKER_CLIENT="docker-18.06.0-ce.tgz"
RUN cd /tmp && mkdir -p /usr/local/bin && \
	wget https://download.docker.com/linux/static/stable/x86_64/${DOCKER_CLIENT} && \
	tar --strip-components=1 -zxf ${DOCKER_CLIENT} -C /usr/local/bin && \
	chmod +x /usr/local/bin/docker && \
	rm -f /tmp/$DOCKER_CLIENT

#4.) ================= Install Java   =================
RUN apt-get update && \
   apt-get upgrade -y && \
   apt-get install -y  software-properties-common && \
   add-apt-repository ppa:webupd8team/java -y && \
   apt-get update && \
   echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
   apt-get install -y oracle-java8-installer && \
   apt-get clean

#5.) ================= Install BioLockJ =================
ARG BLJ_VER=1
ENV BLJ_TAR=biolockj_v1.0.tar.gz
ENV BLJ_RELEASE_URL="https://github.com/mikesioda/BioLockJ_dev/raw/master/dist/$BLJ_TAR"
RUN echo $BLJ_VER && \
	cd $BLJ && \
	wget $BLJ_RELEASE_URL && \
	tar -xzf $BLJ_TAR && \
	chmod -R 777 $BLJ && \
	rm -f $BLJ/$BLJ_TAR

#I used https://nodejs.org/en/docs/guides/nodejs-docker-webapp/ initially.

#Now, I'm copying from https://github.com/nodejs/docker-node/blob/master/Dockerfile-alpine.template //didn't work

#Your app binds to port 8080 so you'll use the EXPOSE instruction to have it mapped by the docker daemon:
EXPOSE 8080

# Bundle app source
COPY . .



#define the command to run your app using CMD which defines your runtime. Here we will use the basic npm start which will run node server.js to start your server:
CMD [ "npm", "start" ]

# Deployment path: $BLJ/resources/docker/blj_webapp.Dockerfile

FROM biolockj/blj_basic_py2

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ENV NODE_VERSION 8.11.3
ENV NODE_URL="https://deb.nodesource.com/setup_8.x"

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get install -y \
	ca-certificates \
	nodejs \
	aptitude \
	npm && \
    wget $NODE_URL | bash -

#3.) ============ Install  Docker Client =================
ARG DOCKER_CLIENT
ENV DOCKER_URL="https://download.docker.com/linux/static/stable/x86_64"
RUN cd /usr/local/bin && \
	wget -qO- $DOCKER_URL/${DOCKER_CLIENT}.tgz | bsdtar -xzf- && \
	mv docker tempDocker && \
	mv tempDocker/* . && \
	rm -rf tempDocker

#4.) ================= Install BioLockJ =================
ARG BLJ_DATE
ARG VER
ENV BLJ_TAR=biolockj_${VER}.tgz
RUN echo ${BLJ_DATE} && mkdir $BLJ && cd $BLJ && \
	wget -qO- $BLJ_URL/${VER}/$BLJ_TAR | bsdtar -xzf- && \
	rm -rf $BLJ/[bilp]* && rm -rf $BLJ/resources/[bdil]* && rm -rf $BLJ/src \
	cp $BLJ/script/* /usr/local/bin

#5.) ================= Install npm  =================
RUN npm install --only=production

#6.) =======================  Cleanup  ==========================
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#7.) ================= Define command = npm start =================
WORKDIR $BLJ/web_app/
EXPOSE 8080
CMD [ "npm", "start" ]

# Deployment path: $DOCKER_FILE_PATH/blj_webapp.Dockerfile

FROM biolockj/blj_basic_py2
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Ubuntu Software
ENV NODE_VERSION 8.11.3
ENV NODE_URL="https://deb.nodesource.com/setup_8.x"
RUN apt-get install -y \
	ca-certificates \
	nodejs \
	aptitude \
	npm && \
    wget $NODE_URL | bash - && \
    pip install awscli

#2.) Install Docker Client
ARG DOCKER_CLIENT
ENV DOCKER_URL="https://download.docker.com/linux/static/stable/x86_64"
RUN cd /usr/local/bin && \
	wget -qO- $DOCKER_URL/${DOCKER_CLIENT}.tgz | bsdtar -xzf- && \
	mv docker tempDocker && \
	mv tempDocker/* . && \
	rm -rf tempDocker

#3.) Install BioLockJ
ARG BLJ_DATE
ARG VER
ENV BLJ_TAR=biolockj_${VER}.tgz
ENV WGET_URL="$BLJ_URL/${VER}/$BLJ_TAR"
RUN echo ${BLJ_DATE} && \
	mkdir $BLJ && \
	cd $BLJ && \
	wget -qO- $WGET_URL | bsdtar -xzf- && \
	rm -rf $BLJ/[bil]* && rm -rf $BLJ/resources/[bdil]* && \
	cp $BLJ/script/* /usr/local/bin
	
#4.) Install npm
RUN cp $BLJ/web_app/package*.json ./
RUN npm install --only=production
RUN cp -r $BLJ/web_app/* ./

#5.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	#rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#6.) Update  ~/.bashrc
RUN echo '[ -f "$BLJ/script/blj_config" ] && . $BLJ/script/blj_config' >> ~/.bashrc && \
	echo 'export BLJ_PROJ=/pipelines' >> ~/.bashrc && \
	echo 'alias goblj=blj_go' >> ~/.bashrc
		
#7.) Start npm Command (Ready to open web-browser localhost:8080)
WORKDIR $BLJ/web_app/
EXPOSE 8080
CMD [ "npm", "start" ]

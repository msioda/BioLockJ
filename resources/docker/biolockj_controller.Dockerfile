# Deployment path: $DOCKER_DIR/biolockj_controller.Dockerfile

FROM biolockj/blj_basic_py2
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Ubuntu Software
ENV NODE_VERSION 8.11.3
RUN apt-get update && \
	apt-get install -y ca-certificates software-properties-common nodejs aptitude npm && \
	apt-get upgrade -y && \
   	apt-get install -y openjdk-8-jre-headless && \
    wget "https://deb.nodesource.com/setup_8.x" | bash -

#2.) Install Nextflow Client
RUN cd /usr/local/bin && wget -qO- https://get.nextflow.io | bash

#3.) Install Docker Client
ARG DOCKER_CLIENT
RUN cd /usr/local/bin && \
	wget -qO- "https://download.docker.com/linux/static/stable/x86_64/${DOCKER_CLIENT}.tgz" | bsdtar -xzf- && \
	mv docker tempDocker && mv tempDocker/* . && rm -rf tempDocker

#4.) Install BioLockJ
ARG BLJ_DATE
ARG VER
RUN echo "${BLJ_DATE}" && cd $BLJ && \
	wget -qO- "https://github.com/msioda/BioLockJ/releases/download/${VER}/biolockj_${VER}.tgz" | bsdtar -xzf- && \
	rm -rf $BLJ/[bil]* && rm -rf $BLJ/resources/[bdil]*

#5.) Install npm
RUN cp "${BLJ}/web_app/package-lock.json" ./ && \
	cp "${BLJ}/web_app/package.json" ./ && \
	npm install --only=production && \
	cp -r $BLJ/web_app/* ./

#6.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	mv /usr/share/ca-certificates* ~ && \
	mv /usr/share/npm ~ && \
	rm -rf /usr/share/* && \
	mv ~/npm /usr/share && \
	mv ~/ca-certificates* /usr/share && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#7.) Update  ~/.bashrc
RUN echo '[ -f "$BLJ/script/blj_config" ] && . $BLJ/script/blj_config' >> ~/.bashrc && \
	echo 'alias goblj=blj_go' >> ~/.bashrc
		
#8.) Setup environment and assign default command
CMD java -jar $BLJ/dist/BioLockJ.jar $BLJ_OPTIONS

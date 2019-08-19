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
#NF_URL="https://get.nextflow.io"
ENV NF_URL="https://github.com/nextflow-io/nextflow/releases/download/v19.04.0/nextflow"
RUN cd $BIN && wget -qO- $NF_URL | bash

#3.) Install Docker Client
ARG DOCKER_CLIENT
ENV DOCK_URL="https://download.docker.com/linux/static/stable/x86_64/${DOCKER_CLIENT}.tgz"
RUN cd $BIN && \
	wget -qO- $DOCK_URL  | bsdtar -xzf- && \
	mv docker tempDocker && mv tempDocker/* . && rm -rf tempDocker

#4.) Install BioLockJ
ARG BLJ_DATE
ARG VER
ENV BLJ_URL="https://github.com/msioda/BioLockJ/releases/download/${VER}/biolockj_${VER}.tgz"
RUN echo "${BLJ_DATE}" && cd "${BLJ}" && \
	wget -qO- $BLJ_URL | bsdtar -xzf- && \
	rm -rf ${BLJ}/[bil]* && rm -rf ${BLJ}/resources/[bdil]* && \
	cd "${BLJ}/web_app" && npm install --only=production

#5.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#6.) Remove shares (except npm & ca-certificates)
RUN	mv /usr/share/ca-certificates* ~ && mv /usr/share/npm ~ && \
	rm -rf /usr/share/* && \
	mv ~/npm /usr/share && mv ~/ca-certificates* /usr/share

#7.) Update  ~/.bashrc
RUN echo '[ -f "$BLJ/script/blj_config" ] && . $BLJ/script/blj_config' >> ~/.bashrc && \
	echo 'alias goblj=blj_go' >> ~/.bashrc
		
#8.) Setup environment and assign default command
CMD java -cp $BLJ/dist/BioLockJ.jar:$BLJ_MODS/* biolockj.BioLockJ $BLJ_OPTIONS

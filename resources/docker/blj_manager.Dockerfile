# Deployment path: $BLJ/resources/docker/blj_manager.Dockerfile

FROM biolockj/blj_basic_java

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ============ Install  Docker Client =================
ARG DOCKER_CLIENT
ENV DOCKER_URL="https://download.docker.com/linux/static/stable/x86_64"
RUN cd /usr/local/bin && \
	wget -qO- $DOCKER_URL/${DOCKER_CLIENT}.tgz | bsdtar -xzf- && \
	mv docker tempDocker && \
	mv tempDocker/* . && \
	rm -rf tempDocker

#3.) ================= Install BioLockJ =================
ARG BLJ_DATE
ARG VER
ENV BLJ_TAR=biolockj_${VER}.tgz
RUN echo ${BLJ_DATE} && mkdir $BLJ && cd $BLJ && \
	wget -qO- $BLJ_URL/${VER}/$BLJ_TAR | bsdtar -xzf- && \
	rm -rf $BLJ/[bilpw]* && rm -rf $BLJ/resources/[bdil]* && rm -rf $BLJ/docs && rm -rf $BLJ/src \
	cp $BLJ/script/* /usr/local/bin
	
#4.) =======================  Cleanup  ===========
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#5.) ================= Container Command =================
CMD [ "biolockj", "$BLJ_OPTIONS" ]
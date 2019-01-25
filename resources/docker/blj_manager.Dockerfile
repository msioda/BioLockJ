# Deployment path: $BLJ/resources/docker/blj_manager.Dockerfile

FROM java_module

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ARG DOCKER_CLIENT
ENV DOCKER_URL="https://download.docker.com/linux/static/stable/x86_64"

#2.) ============ Install  Docker Client =================
RUN cd /usr/local/bin && \
	wget -qO- $DOCKER_URL/${DOCKER_CLIENT}.tgz | bsdtar -xzf- && \
	mv docker tempDocker && \
	mv tempDocker/* . && \
	rm -rf tempDocker
	
#3.) ================= Install BioLockJ =================
RUN echo ${BLJ_DATE} && mkdir $BLJ && cd $BLJ && \
	wget -qO- $BLJ_URL/${VER}/$BLJ_TAR | bsdtar -xzf- && \
	rm -rf $BLJ/[bilpw]* && rm -rf $BLJ/resources/[bdil]* && rm -rf $BLJ/docs && rm -rf $BLJ/src && \
	cp $BLJ/script/* /usr/local/bin

#4.) ================= Container Command =================
CMD [ "biolockj", "$BLJ_OPTIONS" ]
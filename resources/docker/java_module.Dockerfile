# Deployment path: $DOCKER_DIR/java_module.Dockerfile

FROM biolockj/blj_basic_java

#1.) Install BioLockJ
ARG BLJ_DATE
ARG VER
ENV BLJ_URL="https://github.com/msioda/BioLockJ/releases/download/${VER}/biolockj_${VER}.tgz"
RUN echo ${BLJ_DATE} && cd $BLJ && wget -qO- $BLJ_URL | bsdtar -xzf- && \
	rm -rf $BLJ/[bilw]* && rm -rf $BLJ/resources/[bdil]* && rm -rf $BLJ/docs && rm -rf $BLJ/src 

#2.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

# Deployment path:  $BLJ/resources/docker/java_module.Dockerfile

FROM biolockj/blj_basic_java

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ================= Install BioLockJ =================
ARG BLJ_DATE
ARG VER
ENV BLJ_TAR=biolockj_${VER}.tgz
ENV WGET_URL="$BLJ_URL/${VER}/$BLJ_TAR"
RUN echo ${BLJ_DATE} && \
	mkdir $BLJ && \
	cd $BLJ && \
	wget -qO- $WGET_URL | bsdtar -xzf- && \
	rm -rf $BLJ/[bilpw]* && rm -rf $BLJ/resources/[bdil]* && rm -rf $BLJ/docs && rm -rf $BLJ/src && \
	cp $BLJ/script/* /usr/local/bin 

#3.) =======================  Cleanup  ===========
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#4.) ================= Container Command =================
CMD [ "/bin/bash", "biolockj", "$BLJ_OPTIONS" ]

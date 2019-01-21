# Deployment path:  $BLJ/resources/docker/JavaModule

FROM base

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ARG BLJ_DATE
ARG BLJ_VERSION
ENV BLJ_TAR=biolockj_${BLJ_VERSION}.tgz
ENV BLJ_RELEASE_URL=https://github.com/msioda/BioLockJ/releases/download/${BLJ_VERSION}/$BLJ_TAR

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y software-properties-common && \
	apt-get upgrade -y && \
   	apt-get install -y openjdk-8-jre-headless

#3.) ================= Install BioLockJ =================
RUN echo ${BLJ_DATE} && \
	mkdir $BLJ && \
	cd $BLJ && \
	wget $BLJ_RELEASE_URL && \
	tar -xzf $BLJ_TAR && \
	chmod -R 770 $BLJ && \
	rm -f $BLJ_TAR && rm -rf $BLJ/[lip]* && rm -rf $BLJ/src && rm -rf $BLJ/resources/[blid]*

#4.) =======================  Cleanup  ========================== 
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf var/log/* && \
	rm -rf tmp/* && \
	rm -rf usr/games

#5.) ================= Container Command =================
CMD java -jar $BLJ/dist/BioLockJ.jar $BLJ_OPTIONS

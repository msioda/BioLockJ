# Deployment path:  $BLJ/resources/docker/java_module.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ARG BLJ_DATE
ARG BLJ_VERSION
ENV BLJ_URL="https://github.com/msioda/BioLockJ/releases/download"
ENV BLJ_TAR=biolockj_${BLJ_VERSION}.tgz
ENV BLJ_RELEASE=$BLJ_URL/${BLJ_VERSION}/$BLJ_TAR

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
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/* && \
	find / -name *python* | xargs rm -rf

#5.) ================= Container Command =================
CMD java -jar $BLJ/dist/BioLockJ.jar $BLJ_OPTIONS

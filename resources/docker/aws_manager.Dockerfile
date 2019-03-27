# Deployment path: $DOCKER_FILE_PATH/aws_manager.Dockerfile

FROM biolockj/blj_basic_java
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Ubuntu Software (including AWS Client)
RUN apt-get install -y \
		python2.7 \
		python-pip && \
	pip install awscli

#2.) Install Nextflow Client
RUN cd /usr/local/bin && \
	wget -qO- https://get.nextflow.io | bash

#3.) Install BioLockJ
ARG BLJ_DATE
ARG VER
ENV BLJ_TAR=biolockj_${VER}.tgz
ENV WGET_URL="$BLJ_URL/${VER}/$BLJ_TAR"
RUN echo ${BLJ_DATE} && \
	mkdir $BLJ && \
	cd $BLJ && \
	wget -qO- $WGET_URL | bsdtar -xzf- && \
	rm -rf $BLJ/[bilw]* && rm -rf $BLJ/resources/[bdil]* && rm -rf $BLJ/docs && rm -rf $BLJ/src && \
	cp $BLJ/script/* /usr/local/bin
	
#4.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
	
#5.) Update  ~/.bashrc
RUN echo '[ -f "$BLJ/script/blj_config" ] && . $BLJ/script/blj_config' >> ~/.bashrc && \
	echo 'export BLJ_PROJ=/pipelines' >> ~/.bashrc && \
	echo 'alias goblj=blj_go' >> ~/.bashrc

#6.) Set Default Command
CMD java -jar $BLJ/dist/BioLockJ.jar $BLJ_OPTIONS

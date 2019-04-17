# Deployment path: $DOCKER_FILE_PATH/aws_manager.Dockerfile

FROM biolockj/blj_basic_java
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Nextflow Client
RUN cd /usr/local/bin && wget -qO- https://get.nextflow.io | bash

#2.) Install BioLockJ
ARG BLJ_DATE
ARG VER
ENV BLJ_TAR=biolockj_${VER}.tgz
ENV WGET_URL="$BLJ_URL/${VER}/$BLJ_TAR"
ENV BLJ_PROJ=/pipelines
ENV PATH="/home/ec2-user/miniconda/bin:$PATH"
RUN echo ${BLJ_DATE} && \
	mkdir $BLJ && \
	cd $BLJ && \
	wget -qO- $WGET_URL | bsdtar -xzf- && \
	rm -rf $BLJ/[bilw]* && rm -rf $BLJ/resources/[bdil]* && rm -rf $BLJ/docs && rm -rf $BLJ/src && \
	cp $BLJ/script/* /usr/local/bin
	
#3.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	#rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
	
#4.) Update  ~/.bashrc
RUN echo '[ -f "$BLJ/script/blj_config" ] && . $BLJ/script/blj_config' >> ~/.bashrc && \
	echo 'alias goblj=blj_go' >> ~/.bashrc

#5.) Set Default Command
CMD java -jar $BLJ/dist/BioLockJ.jar $BLJ_OPTIONS

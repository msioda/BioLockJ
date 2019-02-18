# Deployment path: $DOCKER_FILE_PATH/blj_bash.Dockerfile

FROM biolockj/blj_basic

#1.) Install PEAR
ENV VER="pear-0.9.10-bin-64"
ENV PEAR_URL="https://github.com/mikesioda/BioLockJ_Dev/releases/download/v1.0"
RUN cd /usr/local/bin && \
	wget $PEAR_URL/$VER.gz && \
	gunzip $VER.gz && \
	chmod o+x $VER && \
	mv $VER pear
	
#2.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* 

# Deployment path: $DOCKER_FILE_PATH/blj_bash.Dockerfile

FROM biolockj/blj_basic

#1.) Install PEAR
ENV PEAR_URL="https://github.com/msioda/BioLockJ/releases/download/pear-0.9.10/pear.zip"
RUN cd /usr/local/bin && wget -qO- $PEAR_URL | bsdtar -xf- && chmod 777 /usr/local/bin/pear
	
#2.) Cleanup
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* 

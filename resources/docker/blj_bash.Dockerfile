# Deployment path: $DOCKER_DIR/blj_bash.Dockerfile

FROM biolockj/blj_basic

#1.) Install PEAR
RUN cd /usr/local/bin && wget -qO- "https://github.com/msioda/BioLockJ/releases/download/pear-0.9.10/pear.zip" \
	| bsdtar -xf- && chmod 777 /usr/local/bin/pear
	
#2.) Cleanup
RUN	apt-get clean && find / -name *python* | xargs rm -rf && \
	rm -rf /usr/share/* && rm -rf /var/cache/* && rm -rf /var/lib/apt/lists/* 

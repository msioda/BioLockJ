# Deployment path:  $BLJ/resources/docker/kraken_classifier.Dockerfile

FROM blj_bash_script

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#3.) ================ Install Kraken ================ 
RUN cd /app && wget -qO- https://github.com/DerrickWood/kraken/archive/v0.10.5-beta.zip | bsdtar -xf- && \
	cd kraken-0.10.5-beta && mkdir build && echo "echo 0" >> install_kraken.sh && sh install_kraken.sh build && chmod o+x -R /app/kraken-0.10.5-beta/build

#4.) ================ Install Kraken DB ================ 
# Kraken Databases available here: https://ccb.jhu.edu/software/kraken/ 
# uncomment for MiniKraken DB_4GB
RUN mkdir /db && cd /db && wget -qO- https://ccb.jhu.edu/software/kraken/dl/minikraken_20171019_4GB.tgz | bsdtar -xzf-
# uncomment for MiniKraken DB_8GB
# RUN mkdir /db && cd /db && wget -qO- https://ccb.jhu.edu/software/kraken/dl/minikraken_20171019_8GB.tgz | bsdtar -xzf-

#5. ) =============== Cleanup ================================
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
		
#6.) ================= Container Command =================
CMD [ "$COMPUTE_SCRIPT" ]

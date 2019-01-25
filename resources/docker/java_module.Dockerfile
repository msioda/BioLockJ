# Deployment path:  $BLJ/resources/docker/java_module.Dockerfile

FROM blj_basic_java

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ARG BLJ_DATE
ARG VER
ENV BLJ=/app/biolockj
ENV BLJ_PROJ=/pipelines
ENV BLJ_URL="https://github.com/msioda/BioLockJ/releases/download"
ENV BLJ_TAR=biolockj_${VER}.tgz

#2.) ================= Install BioLockJ =================
RUN echo ${BLJ_DATE} && \
	mkdir $BLJ && \
	cd $BLJ && \
	wget -qO- $BLJ_URL/${VER}/$BLJ_TAR | bsdtar -xzf- && \
	rm -rf $BLJ/[lip]* && rm -rf $BLJ/resources/[blid]* && rm -rf $BLJ/src && \
	cp $BLJ/script/* /usr/local/bin
	
	
	
RUN cd /usr/local/bin && \
	wget -qO- $PEAR_URL/${VER}.tgz | bsdtar -xzf- && \
	mv pear/* . && \
	rm -rf pear && \
	mv ${VER} pear
	
#3.) ============ Update Ubuntu ~/.bashrc =================
echo '[ -x "$BLJ/script/blj_config" ] && . $BLJ/script/blj_config' >> ~/.bashrc
echo 'alias goblj=blj_go' >> ~/.bashrc

#4.) =======================  Cleanup  ==========================
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
	

#5.) ================= Container Command =================
CMD [ "biolockj", "$BLJ_OPTIONS" ]

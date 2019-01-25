# Deployment path:  $BLJ/resources/docker/blj_basic_biolockj.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ARG BLJ_DATE
ARG VER
ENV BLJ=/app/biolockj
ENV BLJ_PROJ=/pipelines
ENV BLJ_URL="https://github.com/msioda/BioLockJ/releases/download"
ENV BLJ_TAR=biolockj_${VER}.tgz

#2.) ============ Update Ubuntu ~/.bashrc =================
RUN echo '[ -x "$BLJ/script/blj_config" ] && . $BLJ/script/blj_config' >> ~/.bashrc && \
	echo 'alias goblj=blj_go' >> ~/.bashrc

#3.) ================= Install BioLockJ =================
RUN echo ${BLJ_DATE} && mkdir $BLJ && cd $BLJ && \
	wget -qO- $BLJ_URL/${VER}/$BLJ_TAR | bsdtar -xzf- && \
	rm -rf $BLJ/[bilp]* && rm -rf $BLJ/resources/[bdil]* && rm -rf $BLJ/src \
	cp $BLJ/script/* /usr/local/bin

#4.) ================= Container Command =================
CMD [ "biolockj", "$BLJ_OPTIONS" ]
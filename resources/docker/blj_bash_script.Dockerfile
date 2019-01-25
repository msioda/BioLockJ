# Deployment path: $BLJ/resources/docker/blj_bash_script.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ARG VER
ENV PEAR_URL="https://github.com/mikesioda/BioLockJ_Dev/releases/download/v1.0"

#2.) ================= Install PEAR =================
RUN cd /usr/local/bin && \
	wget -qO- $PEAR_URL/${VER}.tgz | bsdtar -xzf- && \
	mv pear/* . && \
	rm -rf pear && \
	mv ${VER} pear
	
#3.) =======================  Cleanup =================
RUN	find / -name *python* | xargs rm -rf && \
	rm -rf /usr/share/*

#4.) ================= Container Command =================
CMD [ "$COMPUTE_SCRIPT" ]

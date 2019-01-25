# Deployment path: $BLJ/resources/docker/blj_bash_script.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ENV PEAR_VERSION="pear-0.9.10-bin-64"
ENV PEAR_URL="https://github.com/mikesioda/BioLockJ_Dev/releases/download/v1.0"

#2.) ================= Install PEAR =================
RUN cd /usr/local/bin && \
	wget -qO- $PEAR_URL/${PEAR_VERSION}.tgz | bsdtar -xzf- && \
	mv pear/* . && \
	rm -rf pear && \
	mv ${PEAR_VERSION} pear
	

#3.) =======================  Cleanup =================
RUN	find / -name *python* | xargs rm -rf && \
	rm -rf /usr/share/*

#4.) ================= Container Command =================
ENTRYPOINT [ "bash", "$COMPUTE_SCRIPT" ]

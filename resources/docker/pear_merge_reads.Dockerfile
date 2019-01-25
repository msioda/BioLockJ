# Deployment path:  $BLJ/resources/docker/pear_merge_reads.Dockerfile

FROM blj_bash_script

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ENV PEAR_VERSION="pear-0.9.10-bin-64"
ENV PEAR_URL="https://github.com/mikesioda/BioLockJ_Dev/releases/download/v1.0"

#2.) ================= Install PEAR =================
RUN mkdir -p /app/pear && cd /app/pear && \
	wget $PEAR_URL/$PEAR_VERSION.gz && \
	gzip -df $PEAR_VERSION.gz && \
	chmod +x /app/pear/$PEAR_VERSION

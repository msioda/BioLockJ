# Deployment path:  $BLJ/resources/docker/seq/PearMergeReads

FROM bash_script

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ENV PEAR_VERSION=pear-0.9.10-bin-64

#3.) ================= Install PEAR =================
RUN mkdir -p /app/pear && cd /app/pear && \
	wget https://github.com/mikesioda/BioLockJ_Dev/releases/download/v1.0/${PEAR_VERSION}.gz && \
	gzip -df ${PEAR_VERSION}.gz && \
	chmod +x /app/pear/${PEAR_VERSION}

#4. ) =============== Cleanup ================================
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /var/cache/* && \
	rm -rf /var/log/* && \
	rm -rf /tmp/* && \
	rm -rf /usr/games && \
	rm -rf /usr/share/*

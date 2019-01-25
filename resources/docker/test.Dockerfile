# Deployment path: $BLJ/resources/docker/test.Dockerfile

FROM ubuntu:18.04

#1.) ================= Setup Env ==================================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y build-essential apt-utils bdstar

#3.) =======================  Cleanup  ==========================
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /usr/games && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

# Deployment path: $BLJ/resources/docker/blj_manager.Dockerfile

FROM java_module

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ARG DOCKER_CLIENT
ENV DOCKER_URL="https://download.docker.com/linux/static/stable/x86_64"

#2.) ============ Install  Docker Client =================
RUN cd /usr/local/bin && \
	wget -qO- $DOCKER_URL/${DOCKER_CLIENT}.tgz | bsdtar -xzf-


	wget -qO- https://download.docker.com/linux/static/stable/x86_64/docker-18.09.1.tgz | bsdtar -xzf-

	
	
	

# Deployment path: $BLJ/resources/docker

FROM biolockj/javamodule

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ARG DOCKER_CLIENT

#2.) ============ Install  Docker Client =================
RUN cd /tmp && mkdir -p /usr/local/bin && \
	wget https://download.docker.com/linux/static/stable/x86_64/${DOCKER_CLIENT} && \
	tar --strip-components=1 -zxf ${DOCKER_CLIENT} -C /usr/local/bin && \
	chmod +x /usr/local/bin/docker && \
	rm -f /tmp/$DOCKER_CLIENT

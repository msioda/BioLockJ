# Deployment path: $BLJ/resources/docker/blj_bash_script.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) =======================  Cleanup =================
RUN	find / -name *python* | xargs rm -rf && \
	rm -rf /usr/share/*

#3.) ================= Container Command =================
ENTRYPOINT [ "bash", "$COMPUTE_SCRIPT" ]

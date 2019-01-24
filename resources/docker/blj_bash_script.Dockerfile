# Deployment path: $BLJ/resources/docker/bash_script.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ================= Container Command =================
ENTRYPOINT [ "bash", "$COMPUTE_SCRIPT" ]

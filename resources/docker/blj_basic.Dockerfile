# Deployment path: $DOCKER_DIR/blj_basic.Dockerfile

FROM ubuntu:18.04
ARG DEBIAN_FRONTEND=noninteractive

#1.) Setup Standard Dirs (used by some but not all ancestors)
ENV APP="/app"
ENV APP_BIN="${APP}/bin"
ENV BLJ="${APP}/biolockj"
ENV BLJ_SUP="${APP}/blj_support"
ENV EFS="/mnt/efs"
ENV BLJ_CONFIG="${EFS}/config"
ENV BLJ_DB="${EFS}/db"
ENV BLJ_DEFAULT_DB="/mnt/db"
ENV BLJ_INPUT="${EFS}/input"
ENV BLJ_HOST_HOME="/home/ec2-user"
ENV BLJ_META="${EFS}/metadata"
ENV BLJ_PROJ="${EFS}/pipelines"
ENV BLJ_PRIMER="${EFS}/primer"
ENV BLJ_SCRIPT="${EFS}/script"
ENV PATH="$PATH:${BLJ_HOST_HOME}/miniconda/bin:${APP_BIN}"

#2.) Build Standard Directories 
RUN mkdir -p "${BLJ}" && mkdir "${BLJ_SUP}" && mkdir -p "${BLJ_PROJ}" && \
	mkdir "${BLJ_CONFIG}" && mkdir "${BLJ_DB}" && mkdir "${BLJ_INPUT}" && \
	mkdir "${BLJ_META}" && mkdir "${BLJ_PRIMER}" && mkdir "${BLJ_SCRIPT}" && \
	mkdir "${BLJ_DEFAULT_DB}" && mkdir -p "${BLJ_HOST_HOME}"

#3.) Install Ubuntu Software 
RUN apt-get update && \
	apt-get install -y build-essential apt-utils bsdtar gawk nano tzdata wget

#4.) Set the timezone to EST
RUN ln -fs /usr/share/zoneinfo/US/Eastern /etc/localtime && \
	dpkg-reconfigure -f noninteractive tzdata

#5.) Update  ~/.bashrc
RUN echo ' '  >> ~/.bashrc && \
	echo 'force_color_prompt=yes' >> ~/.bashrc && \
	echo 'alias ..="cd .."' >> ~/.bashrc && \
	echo 'alias ls="ls -lh --color=auto"' >> ~/.bashrc && \
	echo 'alias h="head -n 8"' >> ~/.bashrc && \
	echo 'alias t="tail -n 8"' >> ~/.bashrc && \
	echo 'alias f="find . -name"' >> ~/.bashrc && \
	echo 'alias cab="cat ~/.bashrc"' >> ~/.bashrc && \
	echo 'alias tlog="tail -1000 *.log"' >> ~/.bashrc && \
	echo 'alias tlogf="tail -1000f *.log"' >> ~/.bashrc && \
	echo 'alias rf="source ~/.bashrc"' >> ~/.bashrc && \
	echo 'alias rd="rm -rf"' >> ~/.bashrc && \
	echo ' ' >> ~/.bashrc && \
	echo 'if [ -f /etc/bash_completion ] && ! shopt -oq posix; then' >> ~/.bashrc && \
	echo '    . /etc/bash_completion' >> ~/.bashrc && \
	echo 'fi' >> ~/.bashrc && \
	echo 'export PS1="${debian_chroot:+($debian_chroot)}\[\033[01;32m\]\u@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ "' >> ~/.bashrc	 && \
	echo '[ -f "$BLJ/script/blj_config" ] && . $BLJ/script/blj_config' >> ~/.bashrc

#6.) Cleanup
RUN	rm -rf /tmp/* && rm -rf /usr/games && rm -rf /var/log/*

#7.) Set Default Command
CMD /bin/bash $COMPUTE_SCRIPT

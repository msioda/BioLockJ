# Deployment path: $DOCKER_DIR/humann2_classifier_dbfree.Dockerfile

FROM ubuntu:18.04
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install HumanN2 + dependencies
RUN apt-get update && \
	apt-get install -y build-essential apt-utils bsdtar gawk nano tzdata wget && \
	apt-get update && \ 
	apt-get install -y python2.7-dev python-pip python-tk && \
	pip install numpy && \
	pip install biopython && \
	pip install biom-format && \
	pip install humann2

#2.) Install bowtie 2.3.4.3
ENV BOWTIE_URL="https://github.com/BenLangmead/bowtie2/releases/download/v"
ENV BOWTIE_VER=2.3.4.3
ENV BOWTIE=bowtie2-${BOWTIE_VER}-linux-x86_64
ENV B_URL=${BOWTIE_URL}${BOWTIE_VER}/${BOWTIE}.zip
RUN cd /usr/local/bin && \
	wget -qO- $B_URL | bsdtar -xf- && \
	chmod o+x -R /usr/local/bin/${BOWTIE} && \
	rm -rf /usr/local/bin/${BOWTIE}/*-debug && \
	mv /usr/local/bin/${BOWTIE}/[bs]* . && \
	rm -rf /usr/local/bin/${BOWTIE}

#==================================================================================================
#  To avoid coupling with blj_basic files (which change sometimes) 
#  Recode blj_basic_py2 here so rebuilds won't have to repull DBs
#==================================================================================================

#3.) Setup Standard Dirs (used by some but not all ancestors)
ENV BLJ="/app/biolockj"
ENV BLJ_SUP="/app/blj_support"
ENV EFS="/mnt/efs"
ENV BLJ_CONFIG="${EFS}/config"
ENV BLJ_DB="${EFS}/db"
ENV BLJ_DEFAULT_DB="/mnt/db"
#ENV BLJ_HOST_HOME="/mnt/host_home"
ENV BLJ_HOST_HOME="/home/ec2-user"
ENV BLJ_INPUT="${EFS}/input"
ENV BLJ_META="${EFS}/metadata"
ENV BLJ_PROJ="${EFS}/pipelines"
ENV BLJ_PRIMER="${EFS}/primer"
ENV BLJ_SCRIPT="${EFS}/script"
ENV PATH="${BLJ_HOST_HOME}/miniconda/bin:$PATH"

#4.) Build Standard Directories 
RUN mkdir -p "${BLJ}" && mkdir "${BLJ_SUP}" && mkdir -p "${BLJ_PROJ}" && \
	mkdir "${BLJ_CONFIG}" && mkdir "${BLJ_DB}" && mkdir "${BLJ_INPUT}" && \
	mkdir "${BLJ_META}" && mkdir "${BLJ_PRIMER}" && mkdir "${BLJ_SCRIPT}" && \
	mkdir "${BLJ_DEFAULT_DB}" && mkdir -p "${BLJ_HOST_HOME}"

#5.) Set the timezone to EST
RUN ln -fs /usr/share/zoneinfo/US/Eastern /etc/localtime && \
	dpkg-reconfigure -f noninteractive tzdata


#6.) Install MetaPhlAn2
ENV mpa_dir=/usr/local/bin
RUN cd /app && \
	wget -qO- "https://www.dropbox.com/s/ztqr8qgbo727zpn/metaphlan2.zip" | bsdtar -xf- && \
	chmod -R 774 /app/metaphlan2 && \
	mv /app/metaphlan2/* ${mpa_dir} && \
	rm -rf /app/metaphlan2 && \
	cd ${mpa_dir} && \
	ln -s metaphlan2.py metaphlan2
	
#7.) Update  ~/.bashrc
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

#8.) Cleanup - save ca-certificates so humann2_classifier can download from internet
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	mv /usr/share/ca-certificates ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates /usr/share && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#9.) Set Default Command
CMD /bin/bash $COMPUTE_SCRIPT
	
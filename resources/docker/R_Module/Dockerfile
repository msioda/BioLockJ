# Deployment path: $BLJ/resources/docker/R_Module

FROM ubuntu:18.04

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir /input && mkdir /pipeline && mkdir /app && mkdir /meta && mkdir /primer && mkdir /config  

#2.) ============ Install Ubuntu Prereqs & R =================
RUN apt-get update && \
	apt-get install -y build-essential \
	checkinstall \
	apt-utils \
	r-base-dev

#3.) ============ Install R Packages =================
RUN Rscript -e "install.packages('Kendall', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('coin', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('vegan', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('ggpubr', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('properties', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('stringr', dependencies=TRUE, repos = 'http://cran.us.r-project.org')"

#4.) ============ Update Ubuntu ~/.bashrc =================
RUN echo ' '  >> ~/.bashrc && \
	echo 'force_color_prompt=yes' >> ~/.bashrc && \
	echo 'alias ..="cd .."' >> ~/.bashrc && \
	echo 'alias ls="ls -lh --color=auto"' >> ~/.bashrc && \
	echo 'alias h="head -n 8"' >> ~/.bashrc && \
	echo 'alias t="tail -n 8"' >> ~/.bashrc && \
	echo 'alias f="find . -name"' >> ~/.bashrc && \
	echo 'alias cab="cat ~/.bashrc"' >> ~/.bashrc && \
	echo 'alias tlog="tail -1000f *.log"' >> ~/.bashrc && \
	echo 'alias rf="source ~/.bashrc"' >> ~/.bashrc && \
	echo ' ' >> ~/.bashrc && \
	echo 'if [ -f /etc/bash_completion ] && ! shopt -oq posix; then' >> ~/.bashrc && \
	echo '    . /etc/bash_completion' >> ~/.bashrc && \
	echo 'fi' >> ~/.bashrc

#5.) =======================  Cleanup  ========================== 
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	find / -name *python* | xargs rm -rf && \
	rm -rf var/cache/* && \
	rm -rf var/log/* && \
	rm -rf tmp/* && \
	rm -rf usr/games && \
	rm -rf usr/share/*
	
#6.) ================= Container Command =================
CMD bash $COMPUTE_SCRIPT

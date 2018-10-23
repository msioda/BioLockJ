# Deployment path:  $BLJ/resources/docker/classifier/wgs/SlimmClassifier/Dockerfile

FROM ubuntu:18.04

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir /input && mkdir /pipeline && mkdir /app && mkdir /meta && mkdir /primer && mkdir /config  

#2.) ============ Install Ubuntu Prereqs & python/metaphlan =================
RUN apt-get update && \
	apt-get install -y build-essential \
	apt-utils \
	wget \
	gzip \
	unzip  \
	python2.7-dev \
	python-pip \
	python-tk

#3.) ============ Update Ubuntu ~/.bashrc =================
RUN echo 'force_color_prompt=yes' >> ~/.bashrc && \
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

#4.) ============ Install numpy & biopython =================
RUN pip install numpy && \
	pip install biopython && \
	pip install biom-format

#5.) ============ Install bowtie 2.2.9  =================
ENV BOWTIE_VER=bowtie2-2.2.9
ENV BOWTIE_ZIP=$BOWTIE_VER-linux-x86_64.zip
RUN cd /app && \
	wget https://sourceforge.net/projects/bowtie-bio/files/bowtie2/2.2.9/$BOWTIE_VER-linux-x86_64.zip && \
	unzip $BOWTIE_ZIP && \
	rm -f $BOWTIE_ZIP && \
	rm -rf $BOWTIE_VER/example && \
	echo 'export PATH=$PATH:/app/$BOWTIE_VER' >> ~/.bashrc 
	
#6.) ============ Install metaphlan2 =================
ENV META_ZIP=default.zip
RUN cd /app && \
	wget https://bitbucket.org/biobakery/metaphlan2/get/$META_ZIP && \
	unzip $META_ZIP && \
	rm -f $META_ZIP && \
	mv biobakery-metaphlan2* biobakery && \
	echo 'export PATH=$PATH:/app/biobakery' >> ~/.bashrc 
	

#7.) ============ Configure DB  ========================== 
ENV MPA_DIR=/db/mpa
ENV MPA_DB=mpa_v20_m200
RUN	mkdir -p $MPA_DIR && \ 
	cd $MPA_DIR && \ 
	wget https://bitbucket.org/biobakery/metaphlan2/downloads/$MPA_DB.tar && \
	tar -xf $MPA_DB.tar && \ 
	bzip2 -d $MPA_DB.fna.bz2 && \ 
	echo 'export mpa_dir=$MPA_DIR' >> ~/.bashrc

#8.) =======================  Cleanup  ========================== 
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/*

#9.) ================= Container Command =================
CMD bash $COMPUTE_SCRIPT

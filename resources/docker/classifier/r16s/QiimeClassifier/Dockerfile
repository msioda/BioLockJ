# Deployment path:  $BLJ/resources/docker/classifier/r16s/QiimeClassifier/Dockerfile

FROM ubuntu:18.04

#1.) ================= Setup Env =================
ENV QIIME_VERSION=1.9.1
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir /input && mkdir /pipeline && mkdir /app && mkdir /meta && mkdir /primer && mkdir /config  

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y build-essential \
	checkinstall \
	apt-utils \
	gzip \
	wget \ 
	tar \
	gawk \
	python2.7-dev \
	python-pip \
	python-tk 

#2.1) ============ Update Ubuntu ~/.bashrc =================
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

#3.) ============ Install numpy/qiime  =================
RUN pip install numpy && \
	pip install --upgrade qiime==$QIIME_VERSION && \
	pip install qiime-default-reference && \
	apt-get clean

#4.) ============ Install vSearch  =================
RUN cd /app && \  
	wget https://github.com/torognes/vsearch/releases/download/v2.8.1/vsearch-2.8.1-linux-x86_64.tar.gz && \
	tar -xzf vsearch-2.8.1-linux-x86_64.tar.gz && \
	rm vsearch-2.8.1-linux-x86_64.tar.gz

#5. ) ============ Optional Install Silva DB  =================
#RUN cd /db && \  
#	wget https://www.arb-silva.de/fileadmin/silva_databases/qiime/Silva_132_release.zip && \
#	unzip Silva_132_release.zip && \
#	echo 'pick_otus_reference_seqs_fp /db/SILVA_132_QIIME_release/rep_set/rep_set_16S_only/97/silva_132_97_16S.fna' >> ~/.qiime_config && \
#	echo 'pynast_template_alignment_fp /db/SILVA_132_QIIME_release/core_alignment/80_core_alignment.fna' >> ~/.qiime_config && \
#	echo 'assign_taxonomy_reference_seqs_fp /db/SILVA_132_QIIME_release/rep_set/rep_set_16S_only/97/silva_132_97_16S.fna' >> ~/.qiime_config && \
#	echo 'assign_taxonomy_id_to_taxonomy_fp /db/SILVA_132_QIIME_release/taxonomy/16S_only/97/majority_taxonomy_7_levels.txt' >> ~/.qiime_config

#6. ) =============== Cleanup ================================
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf var/cache/* && \
	rm -rf var/log/* && \
	rm -rf tmp/* && \
	rm -rf usr/games && \
	rm -rf usr/share/*

#7.) ================= Container Command =================
CMD bash $COMPUTE_SCRIPT

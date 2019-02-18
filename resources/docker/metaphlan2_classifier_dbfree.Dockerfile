# Deployment path:  $DOCKER_FILE_PATH/metaphlan2_classifier_dbfree.Dockerfile

FROM biolockj/blj_basic_py2

#1.) Install numpy & biopython
RUN pip install numpy && \
	pip install biopython && \
	pip install biom-format

#2.) Install bowtie 2.2.9
ENV BOWTIE_URL="https://sourceforge.net/projects/bowtie-bio/files/bowtie2"
ENV BOWTIE_VER=2.2.9
ENV BOWTIE=bowtie2-${BOWTIE_VER}
ENV B_URL=$BOWTIE_URL/$BOWTIE_VER/${BOWTIE}-linux-x86_64.zip
RUN cd /usr/local/bin && \
	wget -qO- $B_URL | bsdtar -xf- && \
	chmod o+x -R /usr/local/bin/$BOWTIE && \
	rm -rf /usr/local/bin/$BOWTIE/*-debug && \
	mv /usr/local/bin/$BOWTIE/[bs]* . && \
	rm -rf /usr/local/bin/$BOWTIE

#3.) Install MetaPhlAn2
ENV mpa_dir=/usr/local/bin
ENV META_URL="https://bitbucket.org/biobakery/metaphlan2/get/default.zip"
RUN cd $mpa_dir && \
	wget -qO- $META_URL | bsdtar -xf- && \
	mv biobakery*/* . && \
	rm -rf biobakery*  && \
	chmod o+x -R *.py && \
	ln -s metaphlan2.py metaphlan2
	
#4.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	mv /usr/share/ca-certificates ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates /usr/share && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
	
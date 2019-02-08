# Deployment path:  $BLJ/resources/docker/metaphlan_classifier.Dockerfile

FROM biolockj/blj_basic_py2
ARG DEBIAN_FRONTEND=noninteractive

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
	wget  -qO- $B_URL | bsdtar -xf- && \
	chmod 777 -R $BOWTIE && \
	rm -rf $BOWTIE/*-debug && \
	mv $BOWTIE/[bs]* . && \
	rm -rf $BOWTIE

#3.) Install metaphlan2
ENV mpa_dir=/usr/local/bin
ENV META_URL="https://www.dropbox.com/s/ztqr8qgbo727zpn/metaphlan2.zip"
ENV M_URL=$META_URL
RUN cd $mpa_dir && \
	wget -qO- $META_URL | bsdtar -xf- && \
	chmod 777 -R metaphlan2 && \
	mv metaphlan2/* . && \
	rm -rf metaphlan2  
	
#4.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

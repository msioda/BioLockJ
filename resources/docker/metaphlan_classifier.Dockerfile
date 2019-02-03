# Deployment path:  $BLJ/resources/docker/classifier/wgs/metaphlan_classifier

FROM blj_basic_python2

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ============ Install numpy & biopython =================
RUN pip install numpy && \
	pip install biopython && \
	pip install biom-format

#3.) ============ Install bowtie 2.2.9  =================
ENV BOWTIE_URL="https://sourceforge.net/projects/bowtie-bio/files/bowtie2"
ENV BOWTIE_VER=2.2.9
ENV BOWTIE_PREFIX=bowtie2-${BOWTIE_VER}
ENV BOWTIE_ZIP=${BOWTIE_PREFIX}-linux-x86_64.zip
RUN cd /app && \
	wget -qO- $BOWTIE_URL/$BOWTIE_VER/$BOWTIE_ZIP | bsdtar -xf- && \
	rm -rf $BOWTIE_PREFIX/example


#4.) ============ Install metaphlan2 =================
ENV mpa_dir=/app/metaphlan2
ENV META_ZIP=metaphlan2.zip
ENV META_URL="https://www.dropbox.com/s/ztqr8qgbo727zpn/"
RUN cd /app && \
	wget -qO- $META_URL/${META_ZIP} | bsdtar -xf-
	
#5.) =======================  Cleanup  ========================== 
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#6.) ================= Container Command =================
ENV PATH=$PATH:/app/metaphlan2:/app/bowtie2-2.2.9
CMD [ "$COMPUTE_SCRIPT" ]

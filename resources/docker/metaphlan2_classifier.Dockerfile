# Deployment path:  $BLJ/resources/docker/classifier/wgs/metaphlan2_classifier

FROM blj_basic_python2

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

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
	rm $BOWTIE_ZIP && \
	rm -rf $BOWTIE_VER/example

#5.) ============ Install metaphlan2 =================
ENV mpa_dir=/app/metaphlan2
ENV META_ZIP=metaphlan2.zip
ENV META_URL="https://www.dropbox.com/s/ztqr8qgbo727zpn/metaphlan2.zip"
RUN cd /app && \
	wget $META_URL && \
	unzip $META_ZIP && \
	rm $META_ZIP 
	
#6.) =======================  Cleanup  ========================== 
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#7.) ================= Container Command =================
ENV PATH=$PATH:/app/metaphlan2:/app/bowtie2-2.2.9
CMD [ "$COMPUTE_SCRIPT" ]

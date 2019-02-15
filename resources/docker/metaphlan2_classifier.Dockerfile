# Deployment path:  $BLJ/resources/docker/metaphlan2_classifier.Dockerfile

FROM biolockj/metaphlan2_classifier_dbfree
ARG DEBIAN_FRONTEND=noninteractive

#1.) Download MetaPhlAn2 Bowtie2 DB
ENV MPA_V20_URL="https://bitbucket.org/biobakery/metaphlan2/downloads/mpa_v20_m200.tar"	
ENV MPA_DIR=/usr/local/bin/mpa
ENV DB_URL="https://bitbucket.org/nsegata/metaphlan/get/default.zip"

RUN cd /app && \
	wget -qO- $DB_URL | bsdtar -xf- && \
	mv nsegata*/bowtie2db/* /db && \
	mkdir $MPA_DIR && \
	cd $MPA_DIR && \
	wget -qO- $MPA_V20_URL | bsdtar -xf- && \
	bzip2 -d *.bz2

#2.) Cleanup
RUN	rm -rf /usr/share/*
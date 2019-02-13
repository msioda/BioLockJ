# Deployment path:  $BLJ/resources/docker/metaphlan2_classifier.Dockerfile

FROM biolockj/metaphlan2_classifier_dbfree
ARG DEBIAN_FRONTEND=noninteractive

#1.) Download MetaPhlAn2 Bowtie2 & MPA_v20 DBs
ENV BT_URL="https://bitbucket.org/nsegata/metaphlan/raw/f353151d84e317672a86eef624c51258888e9388/bowtie2db"
ENV MPA_V20_URL="https://bitbucket.org/biobakery/metaphlan2/downloads/mpa_v20_m200.tar"
ENV DB=/usr/local/bin/mpa_v20
RUN mkdir $DB && \
	cd $DB && \
	wget $BT_URL/mpa.1.bt2 && \
	wget $BT_URL/mpa.2.bt2 && \
	wget $BT_URL/mpa.3.bt2 && \
	wget $BT_URL/mpa.4.bt2 && \
	wget $BT_URL/mpa.rev.1.bt2 && \
	wget $BT_URL/mpa.rev.2.bt2 && \
	wget -qO- $MPA_V20_URL | bsdtar -xf- && \
	bzip2 -d *.bz2

#2.) Cleanup
RUN	rm -rf /usr/share/*
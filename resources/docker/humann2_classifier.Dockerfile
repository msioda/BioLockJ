# Deployment path: $DOCKER_DIR/humann2_classifier.Dockerfile

FROM biolockj/blj_basic_py2
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install HumanN2 + dependencies
RUN pip install numpy && \
	pip install biopython && \
	pip install biom-format && \
	pip install humann2

#2.) Install bowtie 2.3.4.3
ENV BOWTIE_URL="https://github.com/BenLangmead/bowtie2/releases/download/v"
ENV BOWTIE_VER=2.3.4.3
ENV BOWTIE=bowtie2-${BOWTIE_VER}-linux-x86_64
RUN cd /usr/local/bin && \
	wget -qO- ${BOWTIE_URL}${BOWTIE_VER}/${BOWTIE}.zip | bsdtar -xf- && \
	chmod o+x -R /usr/local/bin/${BOWTIE} && \
	rm -rf /usr/local/bin/${BOWTIE}/*-debug && \
	mv /usr/local/bin/${BOWTIE}/[bs]* . && \
	rm -rf /usr/local/bin/${BOWTIE}

#3.) Install MetaPhlAn2
ENV mpa_dir=/usr/local/bin
RUN cd /app && wget -qO- "https://www.dropbox.com/s/ztqr8qgbo727zpn/metaphlan2.zip" | bsdtar -xf- && \
	chmod -R 774 /app/metaphlan2 && mv /app/metaphlan2/* ${mpa_dir} && \
	rm -rf /app/metaphlan2 && cd ${mpa_dir} && ln -s metaphlan2.py metaphlan2
	
#4.) Cleanup - save ca-certificates so humann2_classifier can download from internet
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	mv /usr/share/ca-certificates ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates /usr/share && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
	
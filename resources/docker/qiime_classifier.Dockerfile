# Deployment path:  $DOCKER_DIR/qiime_classifier.Dockerfile

FROM biolockj/blj_basic_py2
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install numpy/QIIME + QIIME Default DB
ENV QIIME_VERSION=1.9.1
RUN pip install numpy && \
	pip install --upgrade qiime==$QIIME_VERSION

#2.) Install vSearch
ENV VSEARCH_URL="https://github.com/torognes/vsearch/releases/download/v"
ENV VSEARCH_VER="2.8.1"
ENV VSEARCH="vsearch-${VSEARCH_VER}-linux-x86_64"
ENV VSEARCH_TGZ="$VSEARCH.tar.gz"
ENV V_URL=${VSEARCH_URL}${VSEARCH_VER}/$VSEARCH_TGZ
RUN cd /usr/local/bin && \
	wget -qO- $V_URL | bsdtar -xzf- && \
	mv vsearch*/bin/* . && \
	rm -rf $VSEARCH

#3.) Cleanup
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

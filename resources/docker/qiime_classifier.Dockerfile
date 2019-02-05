# Deployment path:  $BLJ/resources/docker/qiime_classifier.Dockerfile

FROM blj_basic_python2

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ENV QIIME_VERSION=1.9.1
ENV VSEARCH_TGZ="vsearch-2.8.1-linux-x86_64.tar.gz"


#2.) ============ Install numpy/qiime  =================
RUN pip install --upgrade qiime==$QIIME_VERSION && \
	pip install qiime-default-reference 

#3.) ============ Install vSearch  =================
ENV VSEARCH_URL="https://github.com/torognes/vsearch/releases/download/v2.8.1"
ENV VSEARCH_VER="2.8.1"
ENV VSEARCH_TGZ="vsearch-${VSEARCH_VER}-linux-x86_64.tar.gz"
ENV VSEARCH_URL="https://github.com/torognes/vsearch/releases/download"
RUN cd /app && \  
	wget -qO- ${VSEARCH_URL}/v${VSEARCH_VER}/$VSEARCH_TGZ | bsdtar -xzf-

#4. ) =============== Cleanup ================================
RUN	apt-get clean && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* 

#5.) ================= Container Command =================
CMD [ "/bin/bash", "$COMPUTE_SCRIPT" ]

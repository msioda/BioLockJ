# Deployment path:  $DOCKER_FILE_PATH/knead_data_dbfree.Dockerfile

FROM biolockj/blj_basic_py2
ARG DEBIAN_FRONTEND=noninteractive

#1.) Install Java
RUN apt-get install -y software-properties-common && \
	apt-get upgrade -y && \
   	apt-get install -y openjdk-8-jre-headless

#2.) Install kneaddata
RUN pip install kneaddata

#3.) Install Trimmomatic
ENV TRIMMOMATIC_URL="http://www.usadellab.org/cms/uploads/supplementary/Trimmomatic/Trimmomatic"
ENV TRIMM_VER=0.38
ENV TRIM_APP=Trimmomatic-${TRIMM_VER}
ENV DL_URL=${TRIMMOMATIC_URL}-${TRIMM_VER}.zip
RUN cd /app && \
	wget -qO- $DL_URL | bsdtar -xf- 

#4.) Install bowtie 2.3.4.3
ENV BOWTIE_URL="https://github.com/BenLangmead/bowtie2/releases/download/v"
ENV BOWTIE_VER=2.3.4.3
ENV BOWTIE=bowtie2-${BOWTIE_VER}-linux-x86_64
ENV B_URL=${BOWTIE_URL}${BOWTIE_VER}/${BOWTIE}.zip
RUN cd /usr/local/bin && \
	wget -qO- $B_URL | bsdtar -xf- && \
	chmod o+x -R /usr/local/bin/$BOWTIE && \
	rm -rf /usr/local/bin/$BOWTIE/*-debug && \
	mv /usr/local/bin/$BOWTIE/[bs]* . && \
	rm -rf /usr/local/bin/$BOWTIE
	
#5.) Update $PATH
RUN echo 'export PATH=/app/$TRIM_APP:$PATH' >> ~/.bashrc

#6.) Cleanup - save ca-certificates so knead_data can download from internet
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	mv /usr/share/ca-certificates ~ && \
	rm -rf /usr/share/* && \
	mv ~/ca-certificates /usr/share && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*
	
# Deployment path:  $DOCKER_DIR/mag_dbfree.Dockerfile

FROM biolockj/blj_basic_py2

#checkM for checking quality and lineage of bins
RUN apt-get update && apt-get install -y gcc mono-mcs 
RUN python -m pip install --upgrade pip && alias pip="/usr/local/bin/pip"
RUN pip install numpy && \
	pip install scipy && \
	pip install matplotlib==1.5.3 && \
	pip install setuptools && \
	pip install Cython
	
RUN apt-get install -y zlib1g-dev libbz2-dev liblzma-dev libncurses5-dev

#GTDB-Tk is a software toolkit for assigning objective taxonomic classifications to bacterial and archaeal genomes. 
RUN pip install pysam && \
	pip install dendropy && \
	pip install checkm-genome && \
	pip install gtdbtk

#metaspades for assembling reads into contigs
ENV META_SPADE_URL="http://cab.spbu.ru/files/release3.13.0/SPAdes-3.13.0-Linux.tar.gz"
RUN cd /usr/local/bin && wget -qO- $META_SPADE_URL | bsdtar -xzf-
ENV PATH="/usr/local/bin/SPAdes-3.13.0-Linux:/usr/local/bin/SPAdes-3.13.0-Linux/bin:$PATH"
   
#metabat2 for binning contigs into bins
ENV META_BAT_URL="https://bitbucket.org/berkeleylab/metabat/downloads/metabat-static-binary-linux-x64_v2.12.1.tar.gz"
RUN cd /usr/local/bin && wget -qO- $META_BAT_URL | bsdtar -xzf- && \
	mv metabat matabat_temp && mv matabat_temp/* . && rm -rf matabat_temp
      
#mash for calculating distances between genomes/bins
ENV MASH_URL="https://github.com/marbl/Mash/releases/download/v2.2/mash-Linux64-v2.2.tar"
RUN cd /usr/local/bin && wget -qO- $MASH_URL | bsdtar -xf- && \
	mv mash-Linux64-v2.2/* . && rm -rf mash-Linux64-v2.2

#bwa for read alignment
ENV BWA_URL="https://github.com/lh3/bwa/releases/download/v0.7.17/bwa-0.7.17.tar.bz2"
RUN cd /usr/local/bin && wget -qO- $BWA_URL | bsdtar -xzf- && cd bwa-0.7.17 && make 
ENV PATH="/usr/local/bin/bwa-0.7.17:$PATH"

#samstools for processing bwa alignments
ENV SAM_DIR="/usr/local/bin/samtools"
ENV SAMTOOL_URL="https://github.com/samtools/samtools/releases/download/1.9/samtools-1.9.tar.bz2"
RUN cd /usr/local/bin && wget -qO- $SAMTOOL_URL | bsdtar -xzf- && cd samtools-1.9 && \
	mkdir $SAM_DIR && ./configure --prefix=$SAM_DIR --without-curses && make && make install
ENV PATH="$SAM_DIR/bin:$PATH"

#coverM for calculating relative abundance of genomes in reads based on alignments
ENV COVRM="coverm-x86_64-unknown-linux-musl-0.2.0-alpha7"
ENV COVRM_URL="https://github.com/wwood/CoverM/releases/download/v0.2.0-alpha7/${COVRM}.tar.gz"
RUN cd /usr/local/bin && wget -qO- ${COVRM_URL} | bsdtar -xzf- && \
	mv ${COVRM}/* . && rm -rf ${COVRM}

#orfM for calling open reading frames from genomes
ENV ORFM="orfm-0.7.1_Linux_x86_64"
ENV ORFM_URL="https://github.com/wwood/OrfM/releases/download/v0.7.1/${ORFM}.tar.gz"
RUN cd /usr/local/bin && wget -qO- ${ORFM_URL} | bsdtar -xzf- && \
	mv ${ORFM}/${ORFM} orfm && rm -rf ${ORFM}

#eggnog-mapper for functional annotation of genomes
ENV EGG_NOG_URL="https://github.com/eggnogdb/eggnog-mapper/archive/1.0.3.tar.gz"
RUN cd /usr/local/bin && wget -qO- $EGG_NOG_URL | bsdtar -xzf- && \
	alias emapper="/usr/local/bin/eggnog-mapper-1.0.3/emapper.py"
ENV PATH="/usr/local/bin/eggnog-mapper-1.0.3:$PATH"
	
#FastANI for fast alignment-free computation of whole-genome Average Nucleotide Identity (ANI)
ENV FAST_ANI_URL="https://github.com/ParBLiSS/FastANI/releases/download/v1.2/fastANI-Linux64-v1.2.zip"
RUN cd /usr/local/bin && wget -qO- $FAST_ANI_URL | bsdtar -xf-

#GTDBTK Expected DB Path (mapped in as Docker volume at runtime)
ENV GTDBTK_DATA_PATH="${BLJ_DB}/gtdbtk"

# Download the standard checkm DB
ENV CHECKM_DB_URL="https://data.ace.uq.edu.au/public/CheckM_databases/checkm_data_2015_01_16.tar.gz"
ENV CHECKM_DB_DIR="/usr/local/bin/checkm_data"
RUN mkdir ${CHECKM_DB_DIR} && cd ${CHECKM_DB_DIR} && \
	wget -qO- $CHECKM_DB_URL | bsdtar -xzf- && \
	echo ${CHECKM_DB_DIR} | checkm data setRoot ${CHECKM_DB_DIR}

# Set all bin data editable
RUN chmod -R 777 /usr/local/bin

# TODO: Install DBs on host machine & map as Docker volume w/ runtime param "-v"
#RUN wget https://data.ace.uq.edu.au/public/gtdb/data/releases/release89/89.0/gtdbtk_r89_data.tar.gz
#RUN download_eggnog_data.py none  -y
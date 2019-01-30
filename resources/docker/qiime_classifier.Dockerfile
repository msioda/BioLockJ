# Deployment path:  $BLJ/resources/docker/qiime_classifier.Dockerfile

FROM blj_basic_python2

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ENV QIIME_VERSION=1.9.1
ENV VSEARCH_TGZ="vsearch-2.8.1-linux-x86_64.tar.gz"
ENV VSEARCH_URL="https://github.com/torognes/vsearch/releases/download/v2.8.1"

#2.) ============ Install numpy/qiime  =================
RUN pip install --upgrade qiime==$QIIME_VERSION && \
	pip install qiime-default-reference 

#3.) ============ Install vSearch  =================
RUN cd /app && \  
	wget $VSEARCH_URL/$VSEARCH_TGZ && \
	tar -xzf $VSEARCH_TGZ && \
	rm $VSEARCH_TGZ

#3.5 ) ============ Optional Install Silva DB  =================
#RUN cd /db && \  
#	wget https://www.arb-silva.de/fileadmin/silva_databases/qiime/Silva_132_release.zip && \
#	unzip Silva_132_release.zip && \
#	echo 'pick_otus_reference_seqs_fp /db/SILVA_132_QIIME_release/rep_set/rep_set_16S_only/97/silva_132_97_16S.fna' >> ~/.qiime_config && \
#	echo 'pynast_template_alignment_fp /db/SILVA_132_QIIME_release/core_alignment/80_core_alignment.fna' >> ~/.qiime_config && \
#	echo 'assign_taxonomy_reference_seqs_fp /db/SILVA_132_QIIME_release/rep_set/rep_set_16S_only/97/silva_132_97_16S.fna' >> ~/.qiime_config && \
#	echo 'assign_taxonomy_id_to_taxonomy_fp /db/SILVA_132_QIIME_release/taxonomy/16S_only/97/majority_taxonomy_7_levels.txt' >> ~/.qiime_config

#4. ) =============== Cleanup ================================
RUN	rm -rf usr/share/*

#5.) ================= Container Command =================
CMD bash $COMPUTE_SCRIPT

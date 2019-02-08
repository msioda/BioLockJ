# Deployment path:  $BLJ/resources/docker/qiime_classifier_noDB.Dockerfile

FROM biolockj/qiime_classifier
ARG DEBIAN_FRONTEND=noninteractive

#1.) Remove Default QIIME DB 
RUN rm -rf /usr/local/lib/python2.7/dist-packages/qiime_default_reference/gg_13_8_otus && \
	echo "pick_otus_reference_seqs_fp /db" >> ~/.qiime_config && \
	echo "pynast_template_alignment_fp /db" >> ~/.qiime_config && \
	echo "assign_taxonomy_reference_seqs_fp /db" >> ~/.qiime_config && \
	echo "assign_taxonomy_id_to_taxonomy_fp /db" >> ~/.qiime_config


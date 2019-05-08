# Deployment path:  $DOCKER_DIR/knead_data.Dockerfile

FROM biolockj/knead_data_dbfree

#1.) Install kneaddata human DNA contaminant DB
RUN kneaddata_database --download human_genome bowtie2 "${BLJ_DEFAULT_DB}"

#2.) Cleanup
RUN	rm -rf /usr/share/*

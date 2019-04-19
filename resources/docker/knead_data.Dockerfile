# Deployment path:  $DOCKER_FILE_PATH/knead_data.Dockerfile

FROM biolockj/knead_data_dbfree

#1.) Install kneaddata human DNA contaminant DB
RUN kneaddata_database --download human_genome bowtie2 $BLJ_DB

#2.) Cleanup
RUN	rm -rf /usr/share/*

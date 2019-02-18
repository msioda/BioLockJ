# Deployment path:  $DOCKER_FILE_PATH/kneaddata.Dockerfile

FROM biolockj/kneaddata_dbfree

#1.) Install kneaddata human DNA contaminent DB
RUN kneaddata_database --download human_genome bowtie2 /db

#2.) Cleanup
RUN	rm -rf /usr/share/*

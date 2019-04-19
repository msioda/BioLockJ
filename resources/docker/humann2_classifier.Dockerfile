# Deployment path:  $DOCKER_FILE_PATH/humann2_classifier.Dockerfile

FROM biolockj/humann2_classifier_dbfree

#1.) Install HumanN2 chocophlan & UniRef90 Diamon DB
RUN humann2_databases --download chocophlan full $BLJ_DB && \
 	humann2_databases --download uniref uniref90_diamond $BLJ_DB

#2.) Cleanup
RUN	rm -rf /usr/share/*
	
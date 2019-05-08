# Deployment path:  $DOCKER_DIR/humann2_classifier.Dockerfile

FROM biolockj/humann2_classifier_dbfree

#1.) Install HumanN2 chocophlan & UniRef90 Diamon DB
RUN humann2_databases --download chocophlan full "${BLJ_DEFAULT_DB}" && \
 	humann2_databases --download uniref uniref90_diamond "${BLJ_DEFAULT_DB}"

#2.) Cleanup
RUN	rm -rf /usr/share/*
	
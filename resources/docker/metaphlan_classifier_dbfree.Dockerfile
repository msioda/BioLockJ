# Deployment path:  $BLJ/resources/docker/metaphlan_classifier_dbfree.Dockerfile

FROM biolockj/metaphlan_classifier
ARG DEBIAN_FRONTEND=noninteractive

#1.) Save a symlink where Metaphlan will look for the DB pointing to /db
#    Alternative DBs can be mapped as a volume to /db at startup
RUN cd /usr/local/bin && \
	rm -rf db_v20 && \
	ln -s /db db_v20

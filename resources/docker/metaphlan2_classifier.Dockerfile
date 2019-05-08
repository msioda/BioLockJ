# Deployment path:  $DOCKER_DIR/metaphlan2_classifier.Dockerfile

FROM biolockj/metaphlan2_classifier_dbfree

#1.) Remove DB-less MetaPhlAn2
RUN	cd /usr/local/bin && \
	rm -rf strain* && \
	rm -rf [_u]* && \
	rm -rf metaphlan2.py 
	
#2.) Download MetaPhlAn2 with DB
RUN cd /app && \
	wget -qO- "https://www.dropbox.com/s/ztqr8qgbo727zpn/metaphlan2.zip" | bsdtar -xf- && \
	chmod -R 774 * && \
	mv /app/metaphlan2/* $mpa_dir && \
	rm -rf /app/*

#3.) Cleanup
RUN	rm -rf /usr/share/*

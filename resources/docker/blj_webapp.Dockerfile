# Deployment path: $BLJ/resources/docker/blj_webapp.Dockerfile

FROM blj_basic_python2

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ARG BLJ_DATE
ARG BLJ_VERSION
ENV BLJ_URL="https://github.com/msioda/BioLockJ/releases/download"
ENV BLJ_TAR=biolockj_${BLJ_VERSION}.tgz
ENV BLJ_RELEASE=$BLJ_URL/${BLJ_VERSION}/$BLJ_TAR
ENV NODE_VERSION 8.11.3
ENV NODE_URL="https://deb.nodesource.com/setup_8.x"

#2.) ================= Install BioLockJ =================
RUN echo ${BLJ_DATE} && \
	mkdir $BLJ && \
	cd $BLJ && \
	wget $BLJ_RELEASE_URL && \
	tar -xzf $BLJ_TAR && \
	chmod -R 770 $BLJ && \
	rm -f $BLJ_TAR && rm -rf $BLJ/[lip]* && rm -rf $BLJ/src && rm -rf $BLJ/resources/[blid]*

#3.) ================= Move json packages to container root dir =================
RUN cp $BLJ/web_app/package*.json ./

#4.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y ca-certificates nodejs aptitude npm && \
    wget $NODE_URL | bash -

#5.) ================= Install npm  =================
RUN apt-get update && \
	apt-get install -y npm && \
	npm install --only=production
    #Remove "--only=production" if adding new packages (maybe) MS 11/1

#5.) ================= Expose Port 8080 =================
#I used https://nodejs.org/en/docs/guides/nodejs-docker-webapp/ initially.
#Now, I'm copying from https://github.com/nodejs/docker-node/blob/master/Dockerfile-alpine.template //didn't work
#Your app binds to port 8080 so you'll use the EXPOSE instruction to have it mapped by the docker daemon:
EXPOSE 8080


# 7.) =======================  Cleanup  ==========================
RUN	apt-get clean && \
	rm -rf /tmp/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#8.) ================= Define command = npm start =================
#define the command to run your app using CMD which defines your runtime. Here we will use the basic npm start which will run node server.js to start your server:
# ENTRYPOINT npm start  <-- try the [] way as in next line next build
ENTRYPOINT [ "npm", "start" ]

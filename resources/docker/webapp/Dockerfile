# Deployment path: $BLJ/resources/docker/webapp

FROM manager

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive
ENV NODE_VERSION 8.11.3
ENV NODE_URL=https://deb.nodesource.com/setup_8.x

#2.) ============ Install Ubuntu Prereqs =================
RUN apt-get update && \
	apt-get install -y build-essential ca-certificates nodejs aptitude && \
    wget $NODE_URL | bash -

#3.) ================= Move json packages to container root dir =================
RUN cp $BLJ/web_app/package*.json ./

#4.) ================= Install npm  =================
RUN apt-get update && \
	apt-get install -y npm && \
	npm install --only=production 
    #Remove "--only=production" if adding new packages (maybe) MS 11/1

#5.) ================= Expose Port 8080 =================
#I used https://nodejs.org/en/docs/guides/nodejs-docker-webapp/ initially.
#Now, I'm copying from https://github.com/nodejs/docker-node/blob/master/Dockerfile-alpine.template //didn't work
#Your app binds to port 8080 so you'll use the EXPOSE instruction to have it mapped by the docker daemon:
EXPOSE 8080

#6.) =================  Bundle app source  =================
RUN cp -r $BLJ/web_app/* ./

# 7.) =======================  Cleanup  ==========================
RUN	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf var/cache/* && \
	rm -rf var/log/* && \
	rm -rf tmp/* 

#8.) ================= Define command = npm start =================
#define the command to run your app using CMD which defines your runtime. Here we will use the basic npm start which will run node server.js to start your server:
# ENTRYPOINT npm start  <-- try the [] way as in next line next build
ENTRYPOINT [ "npm", "start" ]
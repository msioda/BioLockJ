# Deployment path: $BLJ/resources/docker/r_module.Dockerfile

FROM blj_basic

#1.) ================= Setup Env =================
ARG DEBIAN_FRONTEND=noninteractive

#2.) ============ Install Ubuntu Prereqs & R =================
RUN apt-get update && \
	apt-get install -y r-base-dev

#3.) ============ Install R Packages =================
RUN Rscript -e "install.packages('Kendall', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('coin', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('vegan', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('ggpubr', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('properties', dependencies=TRUE, repos = 'http://cran.us.r-project.org')" && \
	Rscript -e "install.packages('stringr', dependencies=TRUE, repos = 'http://cran.us.r-project.org')"

#4.) =======================  Cleanup  ========================== 
RUN	apt-get clean && \
	find / -name *python* | xargs rm -rf && \
	rm -rf /tmp/* && \
	rm -rf /usr/share/* && \
	rm -rf /var/cache/* && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/log/*

#5.) ================= Container Command =================
CMD [ "/bin/bash", "$COMPUTE_SCRIPT" ]

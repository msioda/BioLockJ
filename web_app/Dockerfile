#For webapp development
FROM biolockj/webapp
RUN rm -r $BLJ/web_app/*
COPY . $BLJ/web_app/
ENV PROJECT_DIRS=$BLJ/web_app
#RUN rm /package.json
#RUN rm /app.js
#if the new BLJ webapp doesn't pull the latest webapp, run these
# docker build --tag amyerke/webapp --no-cached .
# if mike updates his:
# docker pull biolockj/webapp && docker build --tag amyerke/webapp .
WORKDIR $BLJ/web_app/

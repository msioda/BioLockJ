#For webapp development
FROM biolockj/biolockj_controller
ENV PROJECT_DIRS=$BLJ/web_app


RUN rm -r $BLJ/web_app/*
COPY . $BLJ/web_app/

WORKDIR $BLJ/web_app/
#/usr/bin/env bash

#To make it clickable: chmod a+x (yourscriptname)
#from https://stackoverflow.com/questions/5125907/how-to-run-a-shell-script-in-os-x-by-double-clicking

function startBrowser() {
  sleep 2
  #usefull: https://stackoverflow.com/questions/3124556/clean-way-to-launch-the-web-browser-from-shell-script#3124750
  # if [ -n $BROWSER ]; then
  #   $BROWSER 'http://localhost:8080/'
  if which xdg-open > /dev/null; then
    xdg-open 'http://localhost:8080/'
  elif which gnome-open > /dev/null; then
    gnome-open 'http://localhost:8080/'
  elif which python > /dev/null; then
    python -mwebbrowser http://localhost:8080/
  else
    echo "Could not detect the web browser to use."
  fi
}
#removed --rm
docker run --name gui -p 8080:3000 \
  --rm \
  -v $BLJ/resources/config/gui:/config  \
  -v $BLJ_PROJ:/blj_proj \
  -v /var/run/docker.sock:/var/run/docker.sock  \
  -v $BLJ:/blj \
  -e BLJ_PROJ \
  -e BLJ \
  amyerke/node-web-app &
startBrowser

#-v /docs/pl/seq:/input/seq \

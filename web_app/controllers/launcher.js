/**
Author: Aaron Yerke
Purpose: Functions for launching BLJ from GUI
Notes: Backend server functions
*/

const fs = require('fs'),
  path = require('path'),
  indexAux = require('../lib/indexAux.js'),
  errorLogger = require('./errorLogger.js'),
  bljProjDir = process.env.BLJ_PROJ, //path to blj_proj
  bljDir = process.env.BLJ,
  HOST_BLJ = process.env.HOST_BLJ,
  events = require('events'),
  Stream = new events.EventEmitter(), // my event emitter instance
  { spawn } = require('child_process');//for running child processes

//For turning streamProgress on and off
let launchedPipeline = false;

exports.launch = function(req, res, next) {
  console.log('entered /launch');
  try {
    console.log('entered try catch');
    //console.log(req.body);
    const modules = req.body.modules;
    const paramKeys = req.body.paramKeys;
    const paramValues = req.body.paramValues;
    let launchArg = req.body.partialLaunchArg;
    let configName = paramValues[paramKeys.indexOf('pipeline.configFile')];
    if (!configName.endsWith('.properties')){
      configName = configName.concat('.properties');
    }

    const configText = indexAux.formatAsFlatFile(modules, paramKeys, paramValues);
    indexAux.saveConfigToLocal(configName,configText);

    //set host-path for the config:
    launchArg['c'] = path.join( HOST_BLJ ,'resources','config','gui', configName);

    var launchCommand;

    switch (req.body.launchAction) {
      case 'restartFromCheckPoint':
        console.log('restart request: ', req.body.restartProjectPath);
        const fullRestartPath = path.join(bljDir,req.body.restartProjectPath);
        console.log(fullRestartPath);
        launchCommand = indexAux.createFullLaunchCommand(launchArg, fullRestartPath);
        console.log('launching!');
        indexAux.runLaunchCommand(launchCommand, Stream);

        break;
      case 'eraseThenRestart':
      try {
        const eraseDir = req.body.restartProjectPath;
        console.log(eraseDir);
        //fs.rmdir(eraseDir, e => console.log(e));

        var deleteFolderRecursive = function(p) {
          if (p != '/'){
            if (fs.existsSync(p)) {
              fs.readdirSync(p).forEach(function(file, index){
                var curPath = path.join(p,file);
                if (fs.lstatSync(curPath).isDirectory()) { // recurse
                  deleteFolderRecursive(curPath);
                } else { // delete file
                  fs.unlinkSync(curPath);
                }
              });
              fs.rmdirSync(p);
            }
          }else{
            console.log('p cannot be root');
          }
        };
        deleteFolderRecursive(eraseDir);
        launchCommand = indexAux.createFullLaunchCommand(launchArg);
        console.log('launching!');
        indexAux.runLaunchCommand(launchCommand, Stream);

      } catch (e) {
        console.log(e);
        errorLogger.writeError(e.stack);
      }

        break;
      case 'launchNew':
        launchCommand = indexAux.createFullLaunchCommand(launchArg);
        console.log('launching!');
        indexAux.runLaunchCommand(launchCommand, Stream);
        //let fileModTime = new Map();

      break;
      default:
    }
    res.setHeader('Content-Type', 'text/html');
    res.write('Server Response: pipeline launched!');
    res.end();
    launchedPipeline = true;
  } catch (e) {
    errorLogger.writeError(e.stack);
    console.error(e);
  }
    console.log('leaving /launch post request');
}

exports.streamLog = function(req, res, next){
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });
  Stream.on("log", function(event) {
    console.log(`Stream: ${event}`);
    res.write("data: " + event + "\n\n");
    //response.write("event: " + String(event) + "\n" + "data: " + JSON.stringify(data) + "\n\n");
  });
}

exports.streamProgress = function(req, res, next){
  while (launchedPipeline === false) {
    res.status(400);
    res.send('Pipeline not yet launched');
    break;
  }
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });
  Stream.on("progresspush", function(event) {
    console.log(`Stream: ${event}`);
    res.write("data: " + event + "\n\n");
    //response.write("event: " + String(event) + "\n" + "data: " + JSON.stringify(data) + "\n\n");
  });
}
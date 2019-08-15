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
  BLJ_CONFIG = process.env.BLJ_CONFIG,
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
    let configName = paramValues[paramKeys.indexOf('pipeline.configFile')];
    if (!configName.endsWith('.properties')){
      configName = configName.concat('.properties');
    }
    console.log(configName, HOST_BLJ);
  
    const configText = indexAux.formatAsFlatFile(modules, paramKeys, paramValues);
    indexAux.saveConfigToLocal(configName,configText);

    //set host-path for the config:
    const configPath = path.join( HOST_BLJ, 'resources', 'config', 'gui', configName);

    switch (req.body.launchAction) {
      case 'restartFromCheckPoint':
        console.log('restart request: ', req.body.restartProjectPath);
        const fullRestartPath = path.join(bljDir,req.body.restartProjectPath);
        console.log(fullRestartPath);
        launchCommand = createLaunchCommand(configPath, paramKeys, paramValues, fullRestartPath);
        //configPath, keys, params, restartPath
        console.log('launching!');
        runLaunchCommand(launchCommand, Stream);
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
        launchCommand = createLaunchCommand(configPath, paramKeys, paramValues);
        console.log('launching!');
        runLaunchCommand(launchCommand, Stream);

      } catch (e) {
        console.log(e);
        errorLogger.writeError(e.stack);
      }

        break;
      case 'launchNew':
        launchCommand = createLaunchCommand(configPath, paramKeys, paramValues);
        console.log('launching!');
        runLaunchCommand(launchCommand, Stream);

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

runLaunchCommand = function(command, eventEmitter) {
  const bljProjDir = process.env.BLJ_PROJ; //path to blj_proj
  const { spawn } = require('child_process');//for running child processes
  const first = command.shift();
  console.log(first);
  console.log(command);
  try {
    const child = spawn(first, command);
    child.stdout.on('data', function(data){
      eventEmitter.emit('log',data);
      console.log('child.stdout: ' + data);
    });
    child.stderr.on('data', function (data) {
        //throw errors
        eventEmitter.emit('log',data);
        console.log('child.stderr: ' + data);
    });
    child.on('error', function (data) {
        //throw errors
        eventEmitter.emit('log',data);
        console.log('child.err: ' + data);
    });
    child.on('close', function (code) {
        console.log('child process exited with code ' + code);
    });
    //child.unref();//to run in background
  } catch (e) {
    console.error(`launch error: ${e}`);
  }

}//end runLaunchCommand

createLaunchCommand = function(configPath, keys, values, restartPath){//
  let command = ['biolockj', "--docker"];

  let env = '-c';//flag, -c

  for (var i = 0; i < keys.length; i++) {
    if (keys[i] == 'pipeline.env' && values[i] == 'aws'){
      command.push(`-aws ${configPath}`);
      env = 'aws';
    }
  }

  if (env == '-c') {
    command.push(`-c ${configPath}`);
  }

  if (restartPath !== undefined ){
    //note, change to make more universal
    command.push(`-r ${path.Dirname(restartPath)}`);
  }
  console.log('launch');
  console.log('full launch command: \n', command);
  return command;

}//end createLaunchCommand

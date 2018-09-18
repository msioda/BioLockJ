/**
Author: Aaron Yerke
Purpose: Route and return data for BLJ front end
Notes:
  For sending updates to the client:
  https://www.html5rocks.com/en/tutorials/eventsource/basics/
*/
var express = require('express'),
 router = express.Router(),
 indexAux = require('../lib/indexAux.js'),
 fs = require('fs'),
 path = require('path'),
 events = require('events');
var eventEmitter = new events.EventEmitter();//for making an event emitter
const { spawn } = require('child_process');//for running child processes

const bljProjDir = process.env.BLJ_PROJ; //path to blj_proj
const bljDir = process.env.BLJ;

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Welcome to BioLockJ' });
});

router.get('/config', function(req, res, next) {
  res.render('config', { title: 'Configuration' });
});

router.get('/progress', function(req, res, next) {
  res.render('progress', { title: 'BioLockJ' });
});

router.get('/results', function(req, res, next) {
  res.render('results', { title: 'BioLockJ' });
});

router.post('/launch', function(req, res, next) {
  const configLocal = path.join(bljDir,'resources','config','gui');
  try {
    console.log('entered try catch');
    //console.log(req.body);
    let currentConfig = req.body.config;
    let launchArg = req.body.partialLaunchArg;
    //console.log(launchArg);
    let configText = indexAux.parseBljJson(currentConfig);
    let configName = currentConfig['project.configFile'];
    indexAux.saveConfigToLocal(configName,configText);

    launchArg['config'] = path.join(configLocal,configName);
    const launchCommand = indexAux.createFullLaunchCommand(launchArg);
    //console.log(launchCommand);
    console.log('launching!');
    indexAux.runLaunchCommand(launchCommand);
    //console.log('launched?');
    let fileModTime = new Map();

    // const dirFiles = fs.readdirSync( '/blj_proj' );
    // dirFiles.forEach(file => {
    //   console.log(path.join( '/blj_proj/', file ));
    //   fs.stat(path.join( '/blj_proj/', file ), function(err, m) {
    //     if (err){
    //       console.error(err);
    //     }else{
    //       fileModTime.set(file,  m.mtime )}
    //       console.log(fileModTime.get(file));
    //     });
    //   });

    // var mtimeArr = [];
    // fileModTime.forEach(file => mtimeArr.push(file.mtime));
    // console.log('mtime');
    // console.log(mtimeArr);
    // console.log(fileModTime.get('test.properties').mtime);
    // console.log(fileModTime);
    // console.log(fs.readdirSync(bljDir.concat(configDir)));
    //progressReports(configPath + '*');

    res.setHeader('Content-Type', 'text/html');
    res.write('Server Response: Config recieved!');
    res.end();

    //postProgress('/Users/aaronyerke/Desktop/fodor_lab/blj_testing/');
    // res.redirect('/progress');
  } catch (e) {
    console.error(e);
  } finally {


}
    console.log('leaving /launch post request');

});


//begin serverside events
const Stream = new events.EventEmitter(); // my event emitter instance

router.get('/streamprogress', function(request, response){
  console.log('entered get');
  response.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });
  progressEmitter.on("progresspush", function(event) {
    console.log(`progressEmitter: ${event}`);
    response.write("data: " + event + "\n\n");
    //response.write("event: " + String(event) + "\n" + "data: " + JSON.stringify(data) + "\n\n");
  });
});

const progressEmitter = new events.EventEmitter();

  fs.watch('/config', (eventType, filename) => {
    console.log(`Filename: ${filename}, Event: ${eventType}`);
    console.log(`Filename: ${filename}, Event: ${eventType}`);
    progressEmitter.emit('progresspush',filename);
    //console.log(`FS.watch Filename: ${filename}`);
  // could be either 'rename' or 'change'. new file event and delete
  // also generally emit 'rename'
  })
module.exports = router;


// fs.watch(bljDir, (eventType, filename) => {
//   console.log(`Filename: ${filename}, Event: ${eventType}`);
//   console.log(fs.lstatSync('/Users/aaronyerke/Desktop/fodor_lab/blj_testing/' + filename).isDirectory());
// // could be either 'rename' or 'change'. new file event and delete
// // also generally emit 'rename'
// })




console.log(process.env.BLJ.toString());

console.log('index.js started');

//to actually run blj: https://stackoverflow.com/questions/1880198/how-to-execute-shell-command-in-javascript

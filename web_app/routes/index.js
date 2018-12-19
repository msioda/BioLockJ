/**
Author: Aaron Yerke
Purpose: Route and return data for BLJ front end
Notes:
  For sending updates to the client:
  https://www.html5rocks.com/en/tutorials/eventsource/basics/
*/
let express = require('express'),
 router = express.Router(),
 path = require('path'),
 indexAux = require('../lib/indexAux.js'),
 fs = require('fs'),
 events = require('events'),
 eventEmitter = new events.EventEmitter();//for making an event emitter
const { spawn } = require('child_process');//for running child processes
const Stream = new events.EventEmitter(); // my event emitter instance

const bljProjDir = process.env.BLJ_PROJ; //path to blj_proj
const bljDir = process.env.BLJ;
console.log('bljDir ', bljDir);
const HOST_BLJ = process.env.HOST_BLJ;

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Welcome to BioLockJ' });
});

router.get('/config', function(req, res, next) {
  res.render('config', { title: 'Configuration' });
});

// router.get('/javadocs', function(req, res, next) {
//   res.sendFile(path.join(__dirname,'..','views','docs','index.html'), { title: 'BioLockJ JavaDocs' });
// });
//__dirname resolves to /app/biolockj/web_app/routes

router.get('/results', function(req, res, next) {
  res.render('results', { title: 'BioLockJ' });
});

router.post('/defaultproperties', function(req, res, next) {
  // console.log(req.body);
  // const body = JSON.parse(req.body);
  // console.log('body', body);
  if (req.body.docker === true){
    fs.readFile(path.join('..','resources','config','default','docker.properties'), 'utf8', function (err,data) {
    if (err) {
      return console.log(err);
    }
    console.log(data);
    res.setHeader("Content-Type", "text/html");
    res.write(data);
    res.end();
    });//end fs.readFile
  }else if(req.body.standard === true){
    fs.readFile(path.join('..','resources','config','default','standard.properties'), 'utf8', function (err,data) {
    if (err) {
      return console.log(err);
    }
    console.log(data);
    res.setHeader("Content-Type", "text/html");
    res.write(data);
    res.end();
    });//end fs.readFile
  }else{
    console.log('else');
    console.log(req.body.file);

    var filePath = req.body.file;
    console.log("filePath: ", filePath);
    filePath = filePath.replace(/^\$BLJ/g, '');
    console.log(filePath);
    //filePath = path.parse(filePath);
    //console.log('filePath', filePath);
    console.log('path.join(bljDir, filePath)', path.join(bljDir, filePath));
    fs.readFile(path.join(bljDir, filePath), 'utf8', function (err,data) {
    if (err) {
      return console.log(err);
    }
    console.log(data);
    res.setHeader("Content-Type", "text/html");
    res.write(data);
    res.end();
    });//end fs.readFile
    }

});//end router.post('/defaultproperties'...

router.post('/launch', function(req, res, next) {
  console.log('entered /launch');
  const configLocal = path.join(HOST_BLJ,'resources','config','gui');
  //const configPath = 'config';
  try {
    console.log('entered try catch');
    //console.log(req.body);
    const modules = req.body.modules;
    const paramKeys = req.body.paramKeys;
    const paramValues = req.body.paramValues;
    let launchArg = req.body.partialLaunchArg;
    let configName = paramValues[paramKeys.indexOf('project.configFile')];
    if (req.body.check === true){
      if (fs.existsSync(pipelineProjectName(configName))){
        res.setHeader('Content-Type', 'text/html');
        res.write('previously started');
        res.end();
      }else{
        res.setHeader('Content-Type', 'text/html');
        res.write('not found in pipeline');
        res.end();
      }
    }else if (req.body.check === false && req.body.launch === true && req.body.deleteOldOrRestart === 'delete') {
      //console.log(launchArg);
      const configText = indexAux.formatAsFlatFile(modules, paramKeys, paramValues);
      let configName = paramValues[paramKeys.indexOf('project.configFile')];
      indexAux.saveConfigToLocal(configName,configText);

      launchArg['config'] = path.join(configLocal,configName);
      const launchCommand = indexAux.createFullLaunchCommand(launchArg);
      //console.log(launchCommand);
      console.log('launching!');
      indexAux.runLaunchCommand(launchCommand, Stream);
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
      // progressReports(configPath + '*');

        res.setHeader('Content-Type', 'text/html');
        res.write('Server Response: Config recieved!');
        res.end();


      // postProgress('/Users/aaronyerke/Desktop/fodor_lab/blj_testing/');
      // res.redirect('/progress');

    }else{

      const configText = indexAux.formatAsFlatFile(modules, paramKeys, paramValues);
      let configName = paramValues[paramKeys.indexOf('project.configFile')];
      indexAux.saveConfigToLocal(configName,configText);

      launchArg['config'] = path.join(configLocal,configName);
      const launchCommand = indexAux.createFullLaunchCommand(launchArg);
      //console.log(launchCommand);
      console.log('launching!');
      indexAux.runLaunchCommand(launchCommand, Stream);
      //console.log('launched?');
      let fileModTime = new Map();

      res.setHeader('Content-Type', 'text/html');
      res.write('Server Response: Config recieved!');
      res.end();
    }


  } catch (e) {
    console.error(e);
  }
    console.log('leaving /launch post request');
});

router.get('/streamLog', function(request, response){
  response.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });
  Stream.on("log", function(event) {
    console.log(`Stream: ${event}`);
    response.write("data: " + event + "\n\n");
    //response.write("event: " + String(event) + "\n" + "data: " + JSON.stringify(data) + "\n\n");
  });
});


//begin serverside events
router.get('/streamProgress', function(request, response){
  response.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });
  Stream.on("progresspush", function(event) {
    console.log(`Stream: ${event}`);
    response.write("data: " + event + "\n\n");
    //response.write("event: " + String(event) + "\n" + "data: " + JSON.stringify(data) + "\n\n");
  });
});

// fs.watch('/config', (eventType, filename) => {
//   console.log(`Filename: ${filename}, Event: ${eventType}`);
//   console.log(`Filename: ${filename}, Event: ${eventType}`);
//   Stream.emit('progresspush',filename);
  //console.log(`FS.watch Filename: ${filename}`);
// could be either 'rename' or 'change'. new file event and delete
// also generally emit 'rename'
// })
module.exports = router;


// fs.watch(bljDir, (eventType, filename) => {
//   console.log(`Filename: ${filename}, Event: ${eventType}`);
//   console.log(fs.lstatSync('/Users/aaronyerke/Desktop/fodor_lab/blj_testing/' + filename).isDirectory());
// // could be either 'rename' or 'change'. new file event and delete
// // also generally emit 'rename'
// })


function pipelineProjectName(configName){
  if (configName.endsWith('.properties')){
    configName = configName.replace('.properties', '')
  }
  let now = new Date();
  let year = now.getYear() + 1900;
  let month = now.toLocaleString("en-us", {
    month: "short"
    });
  let day = now.getDay();
  console.log(day);
  let c = configName.concat('_',year,month,day);
  const projectPipelinePath = path.join('/','pipeline', c );
  return projectPipelinePath;
}

console.log(process.env.BLJ.toString());

console.log('index.js started');

//to actually run blj: https://stackoverflow.com/questions/1880198/how-to-execute-shell-command-in-javascript

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
 eventEmitter = new events.EventEmitter();//for making an event emitter,
 sys = require('util');
 exec = require('child_process').exec;
const { spawn } = require('child_process');//for running child processes
const Stream = new events.EventEmitter(); // my event emitter instance

const bljProjDir = process.env.BLJ_PROJ; //path to blj_proj
const bljDir = process.env.BLJ;
console.log('bljDir ', bljDir);
const HOST_BLJ = process.env.HOST_BLJ;

//for error reports
const accessLogStream = fs.createWriteStream(createLogFile(), { flags: 'a' });

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Welcome to BioLockJ' });
});

router.get('/config', function(req, res, next) {
  res.render('config', { title: 'Configuration' });
});

//retrieve project and descriptions
router.post('/retrieveProjects', function(req, res, next) {
  console.log('retrieveProjects');
  try {
    let names = [];
    let descrip = [];
    fs.readdir(path.join('/', 'pipelines'), (err, files) => {
      if (err) {
        console.error(err);
        accessLogStream.write(e.stack + '\n');;
      }
      files.forEach(file => {
        console.log(file);
        const checkFile = fs.lstatSync(path.join('/','pipelines',file));
          if (checkFile.isDirectory()) {
            names.push(file);

            //go into folder
            const nestedFolderFiles = fs.readdirSync(path.join('/', 'pipelines', file));

            //get master config and read description
            for (var i = 0; i < nestedFolderFiles.length; i++) {
              if (nestedFolderFiles[i].startsWith('MASTER_') && nestedFolderFiles[i].endsWith('.properties')) {
                console.log(nestedFolderFiles[i]);

                const propFile = fs
                  .readFileSync(path.join('/', 'pipelines', file, nestedFolderFiles[i]), 'utf8')
                  .split('\n');

                let projDescrp = 'Project description is empty'

                for (var i = 0; i < propFile.length; i++) {
                  if (propFile[i].startsWith('project.description=')) {
                    console.log(propFile[i].slice(20));
                    projDescrp = propFile[i].slice(20);
                  }
                }//end propFile for loop
                descrip.push(projDescrp);
              }
            }//end nestedFolderFiles for loop
          };//end if dir
        });
        // console.log(projects);
        console.log(descrip);
        res.setHeader("Content-Type", "text/html");
        res.write(JSON.stringify({
          names : names,
          descrip : descrip,
        }));
        res.end();
      });
  } catch (e) {
    console.error(e);
    accessLogStream.write(e.stack + '\n');;
  }
});//end router.post('/retrieveProjects',

router.post('/retrieveConfigs', function(req, res, next) {
  console.log('/retrieveConfigs');
  try {
    let configs = [];
    fs.readdir(path.join('/', 'config'), (err, files) => {
      if (err) {
        console.error(err);
      }
      files.forEach(file => {
        console.log(file);
        // TODO: change isDirectory to check for .properties
        const checkFile = path.parse(file);
        if (checkFile.ext === '.properties'){
          configs.push(checkFile.name)
          }
        });
        console.log(configs);
        res.setHeader("Content-Type", "text/html");
        res.write(JSON.stringify(configs));
        res.end();
      });
  } catch (e) {
    console.error(e);
    accessLogStream.write(e.stack + '\n');;
  }
});//end router.post('/retrieveProjects',

router.post('/retrievePropertiesFile', function(req, res, next){
  try {
    const datum = fs.readFileSync(path.join('/','config', req.body.propertiesFile), 'utf8');
    res.setHeader("Content-Type", "text/html");
    res.write(JSON.stringify({data : datum}));
    res.end();
  } catch (e) {
    console.error(e);
    accessLogStream.write(e.stack + '\n');;
  }
})

router.post('/javadocsmodulegetter', function(req, res, next) {
  try {
    let modPathString = req.body.moduleJavaClassPath;
    modPathString = modPathString.concat('.java');
    const modPathArray = modPathString.split('/');
    // console.log(path.join.apply(null, modPathArray));
    const datum = fs.readFileSync(path.join(bljDir, 'src', path.join.apply(null, modPathArray)), 'utf8');
      res.setHeader("Content-Type", "text/html");
      res.write(datum);
      res.end();
  } catch (e) {
    console.error(e);
    accessLogStream.write(e.stack + '\n');;
  }
});
//__dirname resolves to /app/biolockj/web_app/routes

router.get('/results', function(req, res, next) {
  res.render('results', { title: 'BioLockJ' });
});

router.post('/saveConfigToGui', function(req, res, next) {
  console.log('made it to /saveConfigToGui');
  try {
    console.log(req.body.configName);
    indexAux.saveConfigToLocal(req.body.configName, req.body.configText);
    res.setHeader('Content-Type', 'text/html');
    res.write('Server Response: config saved!');
    res.end();
  } catch (e) {
    accessLogStream.write(e.stack + '\n');
    console.error(e);
  }
})

router.post('/defaultproperties', function(req, res, next) {
  try {
    console.log(req.body.file);
    var filePath = req.body.file;
    console.log("filePath: ", filePath);
    filePath = filePath.replace(/^\$BLJ/g, '');
    console.log(filePath);
    //filePath = path.parse(filePath);
    //console.log('filePath', filePath);
    // console.log('path.join(bljDir, filePath)', path.join(bljDir, filePath));
    const datum = fs.readFileSync(path.join(bljDir, filePath), 'utf8');
    res.setHeader("Content-Type", "text/html");
    res.write(datum);
    res.end();
  } catch (e) {
    accessLogStream.write(e.stack + '\n');;
    console.error(e);
  }
});//end router.post('/defaultproperties'...

//currently for docker only
router.post('/checkProjectExists', function(req, res, next) {
  try {
    const projectPath = pipelineProjectName(req.body.projectName);
    const projectName = path.parse(projectPath).base;
    console.log('checkingProject: ', projectName);
    if (fs.existsSync(projectPath)){
      console.log('exists');

      const resParams = {
        projectName : projectName,
        projectPath : projectPath,
      }

      res.setHeader('Content-Type', 'text/html');
      res.write(JSON.stringify(resParams));
      res.end();
    }else{
      console.log('not exists');
      res.setHeader('Content-Type', 'text/html');
      res.write('');
      res.end();
    }
  } catch (e) {
    console.log(e);
    accessLogStream.write(e.stack + '\n');;
  }
});// end outer.post('/checkProjectExists',

router.post('/launch', function(req, res, next) {
  console.log('entered /launch');
  const configHost = path.join(HOST_BLJ,'resources','config','gui');
  //const configPath = 'config';
  try {
    console.log('entered try catch');
    //console.log(req.body);
    const modules = req.body.modules;
    const paramKeys = req.body.paramKeys;
    const paramValues = req.body.paramValues;
    let launchArg = req.body.partialLaunchArg;
    let configName = paramValues[paramKeys.indexOf('project.configFile')];
    if (!configName.endsWith('.properties')){
      configName = configName.concat('.properties');
    }

    const configText = indexAux.formatAsFlatFile(modules, paramKeys, paramValues);
    indexAux.saveConfigToLocal(configName,configText);
    launchArg['c'] = path.join(configHost, configName);
    //console.log("launchArg['c']: ", launchArg['config']);

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
        accessLogStream.write(e.stack + '\n');;
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
    res.write('Server Response: project launched!');
    res.end();

  } catch (e) {
    accessLogStream.write(e.stack + '\n');;
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


router.post('/getMalcolmGitRepo', function(req, res, next) {
  /*
  The components of the formData should be:
  AWSACCESSKEYID, AWSSECRETACCESSKEY, REGION, OUTPUTFORMAT, PROFILE
  */
  console.log('in AWS');
  try {
    console.dir(req.body.formData);
    const sys = require('util');
    const exec = require('child_process').exec;
    exec(`
      git clone https://github.com/mjzapata/AWSBatchGenomicsStack.git ; cd AWSBatchGenomicsStack ;  ./installBatchDeployer.sh pwd
    `, function(err, stdout, stderr) {
      console.log(stdout);
      console.error(err);
      console.error(stderr);
    });
    res.setHeader('Content-Type', 'text/html');
    res.write('Server Response: AWS launched!');
    res.end();
  } catch (e) {
    accessLogStream.write(e.stack + '\n');;
    console.error(e);
  }
});
const batchAwsConfigFile = "~/.batchawsdeploy/config"

router.post('/configureAws', function(req, res, next) {
  /*
  The components of the formData should be:
  AWSACCESSKEYID, AWSSECRETACCESSKEY, REGION, OUTPUTFORMAT, PROFILE
  */
  console.log('configureAws');
  try {
    console.log('req.body.formData: ', req.body.formData);
    console.dir(req.body.formData);
    const sys = require('util');
    const exec = require('child_process').exec;
      //then write credentials
    var spawn = require('child_process').spawn,
        aws = spawn(`source ${batchAwsConfigFile} ; writeAWScredentials.sh ${req.body.formData.PROFILE} ${req.body.formData.REGION} ${req.body.formData.OUTPUTFORMAT} ${req.body.formData.AWSACCESSKEYID} ${req.body.formData.AWSSECRETACCESSKEY}`, {shell: '/bin/bash'});

aws.stdout.on('data', function (data) {
  console.log('stdout: ' + data.toString());
});

aws.stderr.on('data', function (data) {
  console.log('stderr: ' + data.toString());
});

aws.on('exit', function (code) {
  console.log('child process exited with code ' + code.toString());
});
    // exec(`source ${batchAwsConfigFile} ; writeAWScredentials.sh ${req.body.formData.PROFILE} ${req.body.formData.REGION} ${req.body.formData.OUTPUTFORMAT} ${req.body.formData.AWSACCESSKEYID} ${req.body.formData.AWSSECRETACCESSKEY}`, {shell: '/bin/bash'},  function(err, stdout, stderr) {
    //   console.log(stdout);
    //   console.error(err);
    //   console.error(stderr);
    //   res.setHeader('Content-Type', 'text/html');
    //   res.write(`Server Response: ${stdout}`);
    //   res.end();
    // });
  } catch (e) {
    accessLogStream.write(e.stack + '\n');;
    console.error(e);
  }
});
router.post('/deployBatchEnv', function(req, res, next) {
  /**
  stackname: string,
  dockerRepository: string,
  s3bucket: string,
  docker run --rm   --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 -v /Users/aaronyerke/git/postgres:/var/lib/postgresql/data  postgres
  */
  console.log('req.body.formData: ', req.body.formData);
  console.log('deployBatchEnv');
  try {
    if (req.body.formData.deployArg = 'delete'){
      var spawn = require('child_process').spawn,
          aws = spawn(`source ${batchAwsConfigFile} ; deployBatchEnv.sh delete ${req.body.formData.stackname}`, {shell: '/bin/bash'});

      aws.stdout.on('data', function (data) {
        console.log('stdout: ' + data.toString());
      });

      aws.stderr.on('data', function (data) {
        console.log('stderr: ' + data.toString());
      });

      aws.on('exit', function (code) {
        console.log('child process exited with code ' + code.toString());
      });
    }else{
      var spawn = require('child_process').spawn,
          aws = spawn(`source ${batchAwsConfigFile} ; deployBatchEnv.sh create ${req.body.formData.stackname} ${req.body.formData.dockerRepository}`, {shell: '/bin/bash'});

      aws.stdout.on('data', function (data) {
        console.log('stdout: ' + data.toString());
      });

      aws.stderr.on('data', function (data) {
        console.log('stderr: ' + data.toString());
      });

      aws.on('exit', function (code) {
        console.log('child process exited with code ' + code.toString());
    })
  }
    // exec(`source ${batchAwsConfigFile} ; deployBatchEnv.sh create ${req.body.formData.stackname} ${req.body.formData.dockerRepository}`, {shell: '/bin/bash'}, function(err, stdout, stderr) {
    //   console.log(stdout);
    //   console.error(err);
    //   console.error(stderr);
    //   res.setHeader('Content-Type', 'text/html');
    //   res.write(`Server Response: ${stdout}`);
    //   res.end();
    // });

  } catch (e) {
    accessLogStream.write(e.stack + '\n');;
    console.error(e);
  }
});
router.post('/launchEc2HeadNode', function(req, res, next) {
  /**
  launchAction
  awsInstanceType
  */
  console.log('launchEC2HeadNode.sh');
  try {
    exec(`source ${batchAwsConfigFile} ; launchEC2HeadNode.sh ${req.body.formData.instanceType} ${req.body.formData.scriptName}`, {shell: '/bin/bash'}, function(err, stdout, stderr) {
      console.log(stdout);
      console.error(err);
      console.error(stderr);
      res.setHeader('Content-Type', 'text/html');
      res.write(`Server Response: ${stdout}`);
      res.end();
    });

  } catch (e) {
    accessLogStream.write(e.stack + '\n');;
    console.error(e);
  }
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

function pipelineProjectName(configName){//gets the name of the BLJ created pipeline
  if (configName.endsWith('.properties')){
    configName = configName.replace('.properties', '')
  }
  let now = new Date();
  let year = now.getYear() + 1900;
  let month = now.toLocaleString("en-us", {
    month: "short"
    });
  let day = now.getDate();
  console.log(day);
  let c = configName.concat('_',year,month,day);
  const projectPipelinePath = path.join('/','pipelines', c );
  return projectPipelinePath;
}

function createLogFile(){//creates file and returns file name
  let now = new Date();
  let year = now.getYear() + 1900;
  let month = now.toLocaleString("en-us", {
    month: "short"
    });
  let day = now.getDate();
  let hour = now.getHours();
  let min = now.getMinutes();
  let c = ['webapp_log', year, month, day, hour, min].join('_');
  let p = path.join('/','log', c)
  fs.writeFile(p, "Log for BLJ webapp\n", function(err) {
    if(err) {
      accessLogStream.write(err);
      return console.log(err);
    }
    console.log(`created ${c}`);
});
  return p;
}// end of function loggerFileName()

console.log(process.env.BLJ.toString());

console.log('index.js started');

//to actually run blj: https://stackoverflow.com/questions/1880198/how-to-execute-shell-command-in-javascript
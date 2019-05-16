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
  sys = require('util'),
  exec = require('child_process').exec,
  pipelineIo = require(path.join('..','controllers','pipelineIo.js')),
  errorLogger = require(path.join('..','controllers','errorLogger.js')),
  propertiesIo = require(path.join('..','controllers','propertiesIo.js')),
  awsUtil = require(path.join('..','controllers','awsUtil.js')),
  launcher = require(path.join('..','controllers','launcher.js')),
  javaDocs = require(path.join('..','controllers','javaDocs.js'));

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

router.get('/config/:configPath', function(req, res, next) {
  console.log(req.params.configPath);
  let configPath = req.params.configPath;
  if (configPath === "" || configPath === " "){
    res.redirect('/config');
  }
  console.log("__dirname: ", __dirname);
  res.render('config', { title: 'Configuration', configPath : configPath });
});

//retrieve project and descriptions
router.post('/retrievePipelines', pipelineIo.retrieveAllPipelines);

router.post('/retrieveConfigs', propertiesIo.retrievePropertiesFiles);

router.post('/retrievePropertiesFile', propertiesIo.retrievePropertiesFile);

router.post('/javadocsmodulegetter', javaDocs.javaDocsModGetter);

router.get('/results', function(req, res, next) {
  res.render('results', { title: 'BioLockJ' });
});

router.post('/saveConfigToGui', propertiesIo.saveConfig);

router.post('/defaultproperties', propertiesIo.getDefaultProperties);//end router.post('/defaultproperties'...

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
    errorLogger.writeError(e.stack);
  }
});// end outer.post('/checkProjectExists',

router.post('/launch', launcher.launch);

router.get('/streamLog', launcher.streamLog);

//begin serverside events
router.get('/streamProgress', launcher.streamProgress );

router.post('/listAwsProfiles', awsUtil.listAwsProfiles);

router.post('/listS3Buckets', awsUtil.listS3Buckets);

router.post('/listEc2InstanceIds', awsUtil.listEc2InstanceIds);


// source ~/.batchawsdeploy/config ; getcloudformationstack.sh testing2

const batchAwsConfigFile = "~/.batchawsdeploy/config"; //find a good place to put this
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
    res.write('Server Response: Malcolm Git Cloned!');
    res.end();
  } catch (e) {
    errorLogger.writeError(e.stack);
    console.error(e);
  }
});

router.post('/configureAws', function(req, res, next) {
  /*
  The components of the formData should be:
  AWSACCESSKEYID, AWSSECRETACCESSKEY, REGION, OUTPUTFORMAT, PROFILE
  */
  console.log('configureAws');
  try {
    console.log('req.body.formData: ', req.body.formData);
    const sys = require('util');
    const exec = require('child_process').exec;
      //then write credentials
    var spawn = require('child_process').spawn;
    console.log(batchAwsConfigFile);
    console.log(`source ${batchAwsConfigFile} ; configAWScredentials.sh write ${req.body.formData.PROFILE} ${req.body.formData.REGION} text ${req.body.formData.AWSACCESSKEYID} ${req.body.formData.AWSSECRETACCESSKEY}`);
    aws = spawn(`source ${batchAwsConfigFile} ; configAWScredentials.sh write ${req.body.formData.PROFILE} ${req.body.formData.REGION} text ${req.body.formData.AWSACCESSKEYID} ${req.body.formData.AWSSECRETACCESSKEY}`, {shell: '/bin/bash'});

    let response = "";

    aws.stdout.on('data', function (data) {
      console.log('stdout: ' + data.toString());
      response += data.toString();
    });

    aws.stderr.on('data', function (data) {
      console.log('stderr: ' + data.toString());
    });

    aws.on('exit', function (code) {
      res.setHeader('Content-Type', 'text/html');
      res.write(response);
      res.end();
      console.log(response);
      console.log('child process exited with code ' + code.toString());
    });

  } catch (e) {
    errorLogger.writeError(e.stack);
    console.error(e);
  }
});
router.post('/deployCloudInfrastructure', function(req, res, next) {
  /**
  stackname: string,
  dockerRepository: string,
  s3bucket: string,
  docker run --rm   --name pg-docker -e POSTGRES_PASSWORD=docker -d -p 5432:5432 -v /Users/aaronyerke/git/postgres:/var/lib/postgresql/data  postgres
  */
  console.log('req.body.formData: ', req.body.formData);
  console.log('deployCloudInfrastructure');
  try {
    const ms = 1000 * 10 * 60;
    req.connection.setTimeout(ms);
    const outputOptions = ['DELETE_FAILED','CREATE_COMPLETE', 'CREATE_FAILED', 'DELETE_COMPLETE'];
    let sent = false;
    const spawn = require('child_process').spawn;
    if (req.body.formData.deployArg === 'delete'){
      let aws = spawn(`source ${batchAwsConfigFile} ; deployCloudInfrastructure.sh delete ${req.body.formData.stackname}`, {shell: '/bin/bash'});
      //source ~/.batchawsdeploy/config ; deployCloudInfrastructure.sh delete testing
      aws.stdout.on('data', function (data) {
        console.log('stdout: ' + data.toString());
        if (sent === false){
          let output = data.toString().trim();

          for (let i = 0; i < outputOptions.length; i++) {
            if (output.includes(outputOptions[i])){
              console.log('output found YAY!', output, outputOptions[i]);
              console.log(outputOptions[i]);
              res.setHeader('Content-Type', 'text/html');
              res.write(outputOptions[i].toString());
              res.send();
              sent = true;
              break;
            } else{
              // res.writeHead(202, {
              //   'Content-Type': 'text/event-stream',
              //   'Cache-Control': 'no-cache',
              //   'Connection': 'keep-alive'
              // })
            }
          }
        }
      });

      aws.stderr.on('data', function (data) {
        console.log('stderr: ' + data.toString());
      });

      aws.on('exit', function (code) {
        console.log('child process exited with code ' + code.toString());
      });
    }
    if (req.body.formData.deployArg === 'create'){
      let aws = spawn(`source ${batchAwsConfigFile} ; deployCloudInfrastructure.sh create ${req.body.formData.stackname} ${req.body.formData.dockerRepository}`, {shell: '/bin/bash'});
          //source ~/.batchawsdeploy/config ; deployCloudInfrastructure.sh create testingblj biolockj
      aws.stdout.on('data', function (data) {
        console.log('stdout: ' + data.toString());
        if (sent === false){
          let output = data.toString().trim();
          for (let i = 0; i < outputOptions.length; i++) {
            if (output.includes(outputOptions[i])){
              found = true;
              console.log('output found asdfdasf asfdL \n', output, '\n', outputOptions[i]);
              console.log(outputOptions[i]);
              res.setHeader('Content-Type', 'text/html');
              res.write(outputOptions[i]);
              res.end();
              sent = true;
              break;
            } else{
              // res.status(202).send('AWSBatchGenomics is helping BLJ with your request');
            }
          }
        }
      });
      aws.stderr.on('data', function (data) {
        console.log('stderr: ' + data.toString());
      });
      aws.on('exit', function (code) {
        console.log('child process exited with code ' + code.toString());
      })
    }
  } catch (e) {
    errorLogger.writeError(e.stack);
    console.error(e);
  }
});
router.post('/launchEc2HeadNode', function(req, res, next) {
  /**
  Can be found with: source ~/.batchawsdeploy/config ; launchEC2HeadNode.sh
    launchEC2HeadNode.sh exist [STACKNAME] [INSTANCENAME]
    launchEC2HeadNode.sh directconnect [STACKNAME] [INSTANCENAME] [INSTANCETYPE]
    launchEC2HeadNode.sh runscript_attached [STACKNAME] [INSTANCENAME] [INSTANCETYPE] startHeadNodeGui.sh
    launchEC2HeadNode.sh runscript_detached [STACKNAME] [INSTANCENAME] [INSTANCETYPE] startHeadNodeGui.sh
    Usage:
    launchEC2HeadNode.sh exist [STACKNAME] [INSTANCENAME]
    launchEC2HeadNode.sh directconnect [STACKNAME] [INSTANCENAME] [INSTANCETYPE]
    launchEC2HeadNode.sh runscript_attached [STACKNAME] [INSTANCENAME] [INSTANCETYPE] startHeadNodeGui.sh
    launchEC2HeadNode.sh runscript_detached [STACKNAME] [INSTANCENAME] [INSTANCETYPE] startHeadNodeGui.sh

  launchEC2HeadNode.sh exist [STACKNAME] [INSTANCENAME]
  launchEC2HeadNode.sh directconnect [STACKNAME] [INSTANCENAME] [INSTANCETYPE]
  launchEC2HeadNode.sh directconnect [STACKNAME] [INSTANCENAME] [INSTANCETYPE]

  */
  console.log('launchEC2HeadNode.sh');
  try {
    console.log('req.body.formData: ', req.body.formData);
    let spawn = require('child_process').spawn
    if (req.body.formData.launchAction === 'exists') {;
      let aws = spawn(`source ${batchAwsConfigFile} ; launchEC2Node.sh ${req.body.formData.launchAction} ${req.body.formData.ec2StackName} ${req.body.formData.instanceName} ${req.body.formData.instanceType} ${req.body.formData.scriptName}`, {shell: '/bin/bash'});
      // source ~/.batchawsdeploy/config ; launchEC2Node.sh directconnect testthis HeadNode t2.micro
      aws.stdout.on('data', function (data) {
        console.log('stdout: ' + data.toString());
      });

      aws.stderr.on('data', function (data) {
        console.log('stderr: ' + data.toString());
      });

      aws.on('exit', function (code) {
        console.log('child process exited with code ' + code.toString());
      });
    }//end if exists
    else {

    }
    //launchEC2HeadNode.sh runscript_attached [STACKNAME] [INSTANCENAME] [INSTANCETYPE] startHeadNodeGui.sh
    let aws = spawn(`source ${batchAwsConfigFile} ; launchEC2.sh ${req.body.formData.launchAction} ${req.body.formData.ec2ec2StackName} ${req.body.formData.instanceName}`, {shell: '/bin/bash'});
    // source ~/.batchawsdeploy/config ; launchEC2Node.sh directconnect testthis HeadNode t2.micro
    // source ~/.batchawsdeploy/config ; EC2Node.sh directconnect testthis HeadNode t2.micro
    // launchEC2.sh STACKNAME IMAGEID INSTANCETYPE KEYNAME EBSVOLUMESIZEGB
    aws.stdout.on('data', function (data) {
      console.log('stdout: ' + data.toString());
    });

    aws.stderr.on('data', function (data) {
      console.log('stderr: ' + data.toString());
    });

    aws.on('exit', function (code) {
      console.log('child process exited with code ' + code.toString());
    });

  // source ~/.batchawsdeploy/config ;  launchEC2HeadNode.sh exist testing2 HeadNode

  } catch (e) {
    errorLogger.writeError(e.stack);
    console.error(e);
  }
});

router.post('/retrieveAwsStackLists', function(req, res, next) {
  try {
    console.log( 'in /retrieveAwsStackLists: ' );
    let aws = spawn(`$BLJ/web_app/AWSBatchGenomicsStack/support/getcloudformationstack.sh ALLAWSBATCHGENOMICSSTACKS`, {shell: '/bin/bash'});
    //source ~/.batchawsdeploy/config ; deployCloudInfrastructure.sh delete testing
    aws.stdout.on('data', function (data) {
      console.log('stdout: ' + data.toString());
      res.setHeader("Content-Type", "text/html");
      res.write(data);
      res.end();
    });

    aws.stderr.on('data', function (data) {
      console.log('stderr: ' + data.toString());
    });

    aws.on('exit', function (code) {
      console.log('child process exited with code ' + code.toString());
    });
  } catch (e) {
    console.log('error in /retrieveAwsStackLists: ', e);
    errorLogger.writeError(e.stack);
  }
})

// router.post('/validateAwsCreditials', function(req, res, next) {
//   try {
//     var spawn = require('child_process').spawn,
//     aws = spawn(`source ${batchAwsConfigFile} ; configAWScredentials.sh validate`, {shell: '/bin/bash'});
//
//     aws.stdout.on('data', function (data) {
//       console.log('stdout: ' + data.toString());
//       if (data.toString().trim() === "valid"){
//           res.setHeader('Content-Type', 'text/html');
//           res.write('Stored AWS Credentials Valid');
//           res.end();
//       }else {
//         console.log(data);
//         res.setHeader('Content-Type', 'text/html');
//         res.write('Stored AWS Credentials Invalid');
//         res.end();
//       }
//     });
//
//     aws.stderr.on('data', function (data) {
//       console.log('stderr: ' + data.toString());
//     });
//
//     aws.on('exit', function (code) {
//       console.log('child process exited with code ' + code.toString());
//     });
//   } catch (e) {
//     console.error(e);
//     errorLogger.writeError(e.stack);
//   }
// })


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

console.log(process.env.BLJ.toString());

console.log('index.js started');

console.log('index.js __dirname: ', __dirname);

//to actually run blj: https://stackoverflow.com/questions/1880198/how-to-execute-shell-command-in-javascript
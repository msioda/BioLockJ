/**
Author: Aaron Yerke
Purpose: Functions for AWS config properties related to AWS
Notes: Backend server functions
*/

function helloWorld(){
  console.log('%c hello world from config_aws', 'color: orange; font-weight: bold;');
}

exports.listAwsProfiles = function (req, res, next) {
  res.setHeader('Content-Type', 'text/html');
  res.write((JSON.stringify(['profile1', 'profile2', 'profile3'])));
  res.end();
}

exports.listS3Buckets = function (req, res, next) {
  res.setHeader('Content-Type', 'text/html');
  res.write((JSON.stringify(['profile1', 'profile2', 'profile3'])));
  res.end();
}
exports.listEc2InstanceIds = function (req, res, next) {
  res.setHeader('Content-Type', 'text/html');
  res.write((JSON.stringify(['profile1', 'profile2', 'profile3'])));
  res.end();
}

// let aws = spawn(`source ${batchAwsConfigFile} ; launchEC2Node.sh ${req.body.formData.launchAction} ${req.body.formData.ec2StackName} ${req.body.formData.instanceName} ${req.body.formData.instanceType} ${req.body.formData.scriptName}`, {shell: '/bin/bash'});
// // source ~/.batchawsdeploy/config ; launchEC2Node.sh directconnect testthis HeadNode t2.micro
// aws.stdout.on('data', function (data) {
//   console.log('stdout: ' + data.toString());
// });
//
// aws.stderr.on('data', function (data) {
//   console.log('stderr: ' + data.toString());
// });
//
// aws.on('exit', function (code) {
//   console.log('child process exited with code ' + code.toString());
// });

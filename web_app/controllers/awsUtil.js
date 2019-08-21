/**
Author: Aaron Yerke
Purpose: Functions for AWS config properties related to AWS
Notes: Backend server functions
*/

const { spawn } = require('child_process');//for running child processes
const   bljDir = process.env.BLJ;
const fs = require('fs'),
  path = require('path');

const errorLogger = require('./errorLogger.js');


exports.listAwsProfiles = function (req, res, next) {
  try {
    // let aws = spawn(`source ${path.join(bljDir, "file")} ; launchEC2Node.sh ${req.body.formData.launchAction} ${req.body.formData.ec2StackName} ${req.body.formData.instanceName} ${req.body.formData.instanceType} ${req.body.formData.scriptName}`, {shell: '/bin/bash'});
  } catch (error) {
    errorLogger.writeError(e.stack);
  }

  res.setHeader('Content-Type', 'text/html');
  res.write((JSON.stringify(['profile1', 'profile2', 'profile3'])));
  res.end();
}

exports.listS3Buckets = function (req, res, next) {
  try {
    let aws = spawn(`source ${path.join(bljDir, 'script', 'aws_functions')} ; list_s3_buckets`, {shell: '/bin/bash'});
    aws.stdout.on('data', function (data) {
      console.log('stdout: ' + data.toString());
      res.setHeader('Content-Type', 'text/html');
      res.write((JSON.stringify(data)));
      res.end();
    });
    aws.stderr.on('data', function (data) {
      console.log('stderr: ' + data.toString());
    });
    aws.on('exit', function (code) {
      console.log('child process exited with code ' + code.toString());
    });
  } catch (error) {
    console.error(error);
    errorLogger.writeError(e.stack);
  }
}
exports.listEc2InstanceIds = function (req, res, next) {
  res.setHeader('Content-Type', 'text/html');
  res.write((JSON.stringify(['profile1', 'profile2', 'profile3'])));
  res.end();
}

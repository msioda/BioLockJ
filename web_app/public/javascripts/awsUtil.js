/**
Author: Aaron Yerke
Purpose: Functions for AWS config properties related to AWS
Notes:Notes, not ready for this yet...
*/

function helloWorld(){
  console.log('%c hello world from config_aws', 'color: orange; font-weight: bold;');
}

helloWorld();

exports.listAwsProfiles = function (req, res, next) {
  res.setHeader('Content-Type', 'text/html');
  res.write((JSON.stringify(['profile1', 'profile2', 'profile3'])));
  res.end();
}

exports.list3Buckets = function (req, res, next) {
  res.setHeader('Content-Type', 'text/html');
  res.write((JSON.stringify(['profile1', 'profile2', 'profile3'])));
  res.end();
}
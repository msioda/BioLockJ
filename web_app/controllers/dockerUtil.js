/**
Author: Aaron Yerke
Purpose: Functions for docker related functions
Notes: Backend server functions
*/

const { spawn } = require('child_process');//for running child processes
const bljDir = process.env.BLJ;
const fs = require('fs');
const path = require('path');
const HOST_BLJ = process.env.HOST_BLJ;

exports.verifyHostDir = function (req, res, next) {
  try {//source ${BLJ}/script docker_functions ; verifyHostFile /Users/aaronyerke/git/BioLockJ/web_app/Dockerfile
    console.log('req.pth', req.body.path);
    console.log(`source ${path.join(bljDir, 'script', 'docker_functions')} ; verifyHostDir ${req.body.path}`);
    
    let spwn = spawn(`source ${path.join(bljDir, 'script', 'docker_functions')} ; verifyHostDir ${req.body.path}`, {shell: '/bin/bash'});
    spwn.stdout.on('data', function (data) {
      console.log('stdout: ' + data);
      res.setHeader('Content-Type', 'text/html');
      res.write(data);
      res.end();
    })
  } catch (error) {
    console.error(error);
  }
}
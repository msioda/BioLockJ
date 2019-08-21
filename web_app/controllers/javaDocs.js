/**
Author: Aaron Yerke
Purpose: Functions for reading the Java docs
Notes: Backend server functions
*/

const path = require('path'),
  fs = require('fs'),
  errorLogger = require('./errorLogger.js'),
  bljDir = process.env.BLJ;

exports.javaDocsModGetter =  function(req, res, next) {
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
    errorLogger.writeError(e.stack);
  }
}
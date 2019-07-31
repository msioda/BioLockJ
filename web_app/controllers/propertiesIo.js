/**
  *Functions for creating, retrieving, modifying, and deleting (CRUD) .properties files
  *Author: Aaron Yerke
  *
*/

const fs = require('fs'),
  path = require('path'),
  indexAux = require('../lib/indexAux.js'),
  errorLogger = require('./errorLogger.js'),
  bljProjDir = process.env.BLJ_PROJ, //path to blj_proj
  bljDir = process.env.BLJ,
  BLJ_CONFIG = process.env.BLJ_CONFIG,
  HOST_BLJ = process.env.HOST_BLJ;

exports.retrievePropertiesFiles = function(req, res, next) {
  console.log('/retrieveConfigs');
  try {
    let configs = [];
    fs.readdir(BLJ_CONFIG, (err, files) => {
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
    errorLogger.writeError(e.stack);
  } finally {
    //next();
  }
}

exports.retrievePropertiesFile = function(req, res, next){
  try {
    const propertiesFile = req.body.propertiesFile;
    console.log('propertiesFile: ', propertiesFile);
    if (propertiesFile.startsWith('/') || propertiesFile.startsWith('$BLJ')){
      console.log('entered contains / section of /retrievePropertiesFile');
      const datum = fs.readFileSync(propertiesFile, 'utf8');
      res.setHeader("Content-Type", "text/html");
      res.write(JSON.stringify({data : datum}));
      res.end();
    } else{
      console.log('no / found');
      const datum = fs.readFileSync(path.join(BLJ_CONFIG, propertiesFile), 'utf8');
      res.setHeader("Content-Type", "text/html");
      res.write(JSON.stringify({data : datum}));
      res.end();
    }
  } catch (e) {
    console.error(e);
    errorLogger.writeError(e.stack);
  }
}

exports.saveConfig = function(req, res, next) {
  console.log('made it to /saveConfigToGui');
  try {
    console.log(req.body.configName);
    if (req.body.configName.startsWith('/')){
      fs.writeFile(req.body.configName, req.body.configText, function(err){
        if (err) {
          errorLogger.writeError(e.stack);
          return console.log(err);
        }
      })
      res.setHeader('Content-Type', 'text/html');
      res.write('Server Response: config saved!');
      res.end();
    }else{
      indexAux.saveConfigToLocal(req.body.configName, req.body.configText);
      res.setHeader('Content-Type', 'text/html');
      res.write('Server Response: config saved!');
      res.end();}
  } catch (e) {
    errorLogger.writeError(e.stack);
    console.error(e);
  }
}

exports.getDefaultProperties = function(req, res, next) {
  try {
    console.log(req.body.file);
    var filePath = req.body.file;
    filePath = filePath.replace(/^\$BLJ/g, '');//replace BASH var with path
    const datum = fs.readFileSync(path.join(bljDir, filePath), 'utf8');
    res.setHeader("Content-Type", "text/html");
    res.write(datum);
    res.end();
  } catch (e) {
    errorLogger.writeError(e.stack);
    console.error(e);
  }
}
